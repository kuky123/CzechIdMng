package eu.bcvsolutions.idm.core.scheduler.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.utils.AutowireHelper;
import eu.bcvsolutions.idm.core.scheduler.api.config.SchedulerConfiguration;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.LongRunningFutureTask;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmLongRunningTaskFilter;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmLongRunningTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Default implementation {@link LongRunningTaskManager}
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultLongRunningTaskManager implements LongRunningTaskManager {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultLongRunningTaskManager.class);
	private final IdmLongRunningTaskService service;
	private final Executor executor;
	private final ConfigurationService configurationService;
	private final SecurityService securityService;
	private final EntityEventManager entityEventManager;
	
	@Autowired
	public DefaultLongRunningTaskManager(
			IdmLongRunningTaskService service,
			Executor executor,
			EntityEventManager entityEventManager,
			ConfigurationService configurationService,
			SecurityService securityService) {
		Assert.notNull(service);
		Assert.notNull(executor);
		Assert.notNull(entityEventManager);
		Assert.notNull(configurationService);
		Assert.notNull(securityService);
		//
		this.service = service;
		this.executor = executor;
		this.entityEventManager = entityEventManager;
		this.configurationService = configurationService;
		this.securityService = securityService;
	}
	
	/**
	 * Cancel all previously ran tasks
	 */
	@Override
	@Transactional
	public void init() {
		LOG.info("Cancel unprocessed long running task - tasks was interrupt during instance restart");
		//
		// task prepared for run - they can be in running state, but process physically doesn't started yet (running flag is still set to false)
		String instanceId = configurationService.getInstanceId();
		service
			.findAllByInstance(instanceId, OperationState.RUNNING)
			.forEach(this::cancelTaskByRestart);
		//
		// running tasks - they can be marked as canceled, but they were not killed before server was restarted
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		filter.setInstanceId(instanceId);
		filter.setRunning(Boolean.TRUE);
		service
			.find(filter, null)
			.forEach(this::cancelTaskByRestart);
	}
	
	/**
	 * Schedule {@link #processCreated()} only
	 */
	@Transactional
	@Scheduled(fixedDelayString = "${" + SchedulerConfiguration.PROPERTY_TASK_QUEUE_PROCESS + ":" + SchedulerConfiguration.DEFAULT_TASK_QUEUE_PROCESS + "}")
	public void scheduleProcessCreated() {
		if (!isAsynchronous()) {
			// asynchronous processing is disabled
			// prevent to debug some messages into log - usable for devs
			return;
		}
		processCreated();
	}

	/**
	 * Executes long running task on this instance
	 */
	@Override
	@Transactional
	public List<LongRunningFutureTask<?>> processCreated() {
		String instanceId = configurationService.getInstanceId();
		LOG.debug("Processing created tasks from long running task queue on instance id [{}]", instanceId);
		// run as system - called from scheduler internally
		securityService.setSystemAuthentication();
		//
		List<LongRunningFutureTask<?>> taskList = new ArrayList<LongRunningFutureTask<?>>();
		service.findAllByInstance(instanceId, OperationState.CREATED).forEach(task -> {
			LongRunningFutureTask<?> futureTask = processCreated(task.getId());
			if (futureTask != null) {
				taskList.add(futureTask);
			}
		});
		return taskList;
	}

	@Override
	@Transactional
	public LongRunningFutureTask<?> processCreated(UUID longRunningTaskId) {
		LOG.debug("Processing created task [{}] from long running task queue", longRunningTaskId);
		IdmLongRunningTaskDto task = service.get(longRunningTaskId);
		// task cannot be started twice  
		if (task.isRunning() || OperationState.RUNNING == task.getResultState()) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_IS_RUNNING, ImmutableMap.of("taskId", task.getId()));
		}
		if (OperationState.CREATED != task.getResultState()) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_IS_PROCESSED, ImmutableMap.of("taskId", task.getId()));
		}
		//
		LongRunningTaskExecutor<?> taskExecutor = createTaskExecutor(task);
		if (taskExecutor == null) {
			return null;
		}
		return execute(taskExecutor);
	}
	
	@Override
	@Transactional
	public synchronized <V> LongRunningFutureTask<V> execute(LongRunningTaskExecutor<V> taskExecutor) {
		if (!isAsynchronous()) {
			V result = executeSync(taskExecutor);
			// construct simple "sync" task
			return new LongRunningFutureTask<>(taskExecutor, new FutureTask<V>(() -> { return result; } ) {
				@Override
				public V get() {
					return result;
				}
			});
		}
		//
		// autowire task properties
		AutowireHelper.autowire(taskExecutor);	
		// persist LRT
		taskExecutor.validate(persistTask(taskExecutor));
		//
		LongRunningFutureTask<V> longRunnigFutureTask = new LongRunningFutureTask<>(taskExecutor, new FutureTask<>(taskExecutor));
		// execute - after original transaction is commited
		entityEventManager.publishEvent(longRunnigFutureTask);
		//
		return longRunnigFutureTask;
	}
	
	/**
	 * Executes given initialized task asynchronously. 
	 * We need to wait to transaction commit, when asynchronous task is executed - data is prepared in previous transaction mainly
	 * 
	 * @param futureTask
	 */
	@TransactionalEventListener
	public synchronized<V> void executeInternal(LongRunningFutureTask<V> futureTask) {
		Assert.notNull(futureTask);
		Assert.notNull(futureTask.getExecutor());
		Assert.notNull(futureTask.getFutureTask());
		//
		markTaskAsRunning(getValidTask(futureTask.getExecutor()));
		//
		LOG.debug("Execute task [{}] asynchronously", futureTask.getExecutor().getLongRunningTaskId());
		executor.execute(futureTask.getFutureTask());
	}
	
	@Override
	@Transactional
	public <V> V executeSync(LongRunningTaskExecutor<V> taskExecutor) {
		// autowire task properties
		AutowireHelper.autowire(taskExecutor);
		// persist LRT
		IdmLongRunningTaskDto task = persistTask(taskExecutor);
		//
		markTaskAsRunning(getValidTask(taskExecutor));
		//
		LOG.debug("Execute task [{}] synchronously", task.getId());
		// execute
		try {
			return taskExecutor.call();
		} catch (Exception ex) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_FAILED, 
					ImmutableMap.of(
							"taskId", task.getId(), 
							"taskType", task.getTaskType(),
							"instanceId", task.getInstanceId()), ex);
		} finally {
			LOG.debug("Executing task [{}] synchronously ended", task.getId());
		}
	}

	@Override
	@Transactional
	public void cancel(UUID longRunningTaskId) {
		Assert.notNull(longRunningTaskId);
		IdmLongRunningTaskDto task = service.get(longRunningTaskId);
		Assert.notNull(longRunningTaskId);
		//
		if (!OperationState.isRunnable(task.getResult().getState())) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_NOT_RUNNING, 
					ImmutableMap.of(
							"taskId", longRunningTaskId, 
							"taskType", task.getTaskType(), 
							"instanceId", task.getInstanceId())
					);
		}
		//
		task.setResult(new OperationResult.Builder(OperationState.CANCELED).build());
		task.setRunning(false);
		LOG.info("Long running task with id: [{}] was canceled.", task.getId());
		// running to false will be set by task himself
		service.save(task);
	}
	
	@Override
	@Transactional
	public boolean interrupt(UUID longRunningTaskId) {
		Assert.notNull(longRunningTaskId);
		IdmLongRunningTaskDto task = service.get(longRunningTaskId);
		Assert.notNull(longRunningTaskId);
		String instanceId = configurationService.getInstanceId();
		if (!task.getInstanceId().equals(instanceId)) {			
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_DIFFERENT_INSTANCE, 
					ImmutableMap.of(
							"taskId", longRunningTaskId, 
							"taskInstanceId", task.getInstanceId(), 
							"currentInstanceId", instanceId));
		}
		if (OperationState.RUNNING != task.getResult().getState()) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_NOT_RUNNING, 
					ImmutableMap.of(
							"taskId", longRunningTaskId,
							"taskType", task.getTaskType(), 
							"instanceId", task.getInstanceId()));
		}
		//
		// interrupt thread
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for (Thread thread : threadSet) {
			if (thread.getId() == task.getThreadId()) {
				ResultModel resultModel = new DefaultResultModel(CoreResultCode.LONG_RUNNING_TASK_INTERRUPT, 
						ImmutableMap.of(
								"taskId", task.getId(), 
								"taskType", task.getTaskType(),
								"instanceId", task.getInstanceId()));
				Exception ex = null;
				try {
					thread.interrupt();
				} catch(Exception e) {
					ex = e;
					LOG.error(resultModel.toString(), e);
				}
				task.setRunning(false);
				//
				if (ex == null) {
					LOG.info("Long running task with id: [{}], was interrupted.", task.getId());
					task.setResult(new OperationResult.Builder(OperationState.CANCELED).setModel(resultModel).build());
				} else {
					LOG.info("Long running task with id: [{}], has some exception during interrupt.", task.getId());
					task.setResult(new OperationResult.Builder(OperationState.EXCEPTION).setModel(resultModel).setCause(ex).build());
				}				
				service.save(task);
				return true;
			}
		}
		LOG.warn("For long running task with id: [{}], has not found running thread.", task.getId());
		return false;
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmLongRunningTaskDto getLongRunningTask(UUID longRunningTaskId, BasePermission... permission) {
		Assert.notNull(longRunningTaskId, "Long running task id is required. Long running task has to be executed at first.");
		//
		return service.get(longRunningTaskId, permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmLongRunningTaskDto getLongRunningTask(LongRunningTaskExecutor<?> taskExecutor, BasePermission... permission) {
		Assert.notNull(taskExecutor, "Long running task execturor is required.");
		Assert.notNull(taskExecutor.getLongRunningTaskId(), "Long running task id is required. Long running task has to be executed at first.");
		//
		return getLongRunningTask(taskExecutor.getLongRunningTaskId());
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmLongRunningTaskDto getLongRunningTask(LongRunningFutureTask<?> futureTask, BasePermission... permission) {
		Assert.notNull(futureTask, "Long running future task is required.");
		//
		return getLongRunningTask(futureTask.getExecutor(), permission);
	}
	
	@Override
	public boolean isAsynchronous() {
		return configurationService.getBooleanValue(
				SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, 
				SchedulerConfiguration.DEFAULT_TASK_ASYNCHRONOUS_ENABLED);
	}
	
	/**
	 * Prepares executor's LRT
	 * 
	 * @param taskExecutor
	 * @return
	 */
	private IdmLongRunningTaskDto persistTask(LongRunningTaskExecutor<?> taskExecutor) {
		// prepare task
		IdmLongRunningTaskDto task;
		if (taskExecutor.getLongRunningTaskId() == null) {
			task = new IdmLongRunningTaskDto();
			task.setTaskType(taskExecutor.getName());
			task.setTaskProperties(taskExecutor.getProperties());
			task.setTaskDescription(taskExecutor.getDescription());	
			task.setInstanceId(configurationService.getInstanceId());
			task.setResult(new OperationResult.Builder(OperationState.CREATED).build());
			task = service.save(task);
			taskExecutor.setLongRunningTaskId(task.getId());
		} else {
			task = service.get(taskExecutor.getLongRunningTaskId());
		}
		return task;
	}
	
	private synchronized IdmLongRunningTaskDto markTaskAsRunning(IdmLongRunningTaskDto task) {
		task.setResult(new OperationResult.Builder(OperationState.RUNNING).build());
		return service.save(task);
	}
	
	/**
	 * Returns executor's LRT task, when task is valid. Throws exception otherwise. 
	 * 
	 * @param taskExecutor
	 * @return
	 */
	private IdmLongRunningTaskDto getValidTask(LongRunningTaskExecutor<?> taskExecutor) {
		IdmLongRunningTaskDto task = service.get(taskExecutor.getLongRunningTaskId());
		Assert.notNull(task);
		//
		if (!task.getInstanceId().equals(configurationService.getInstanceId())) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_DIFFERENT_INSTANCE, 
					ImmutableMap.of("taskId", task.getId(), "taskInstanceId", task.getInstanceId(), "currentInstanceId", configurationService.getInstanceId()));
		}
		taskExecutor.validate(task);
		return task;
	}
	
	/**
	 * Create new LongRunningTaskExecutor from given LRT.
	 * Handles exceptions, when task already processed, task type is removed or task initialization failed
	 * 
	 * @param task
	 * @return
	 */
	private LongRunningTaskExecutor<?> createTaskExecutor(IdmLongRunningTaskDto task) {
		Assert.notNull(task, "Long running task instance is required!");
		if (!OperationState.isRunnable(task.getResultState())) {
			throw new ResultCodeException(CoreResultCode.LONG_RUNNING_TASK_IS_PROCESSED, ImmutableMap.of("taskId", task.getId()));
		}
		//
		LongRunningTaskExecutor<?> taskExecutor = null;
		ResultModel resultModel = null;
		Exception ex = null;
		try {
			taskExecutor = (LongRunningTaskExecutor<?>) AutowireHelper.createBean(Class.forName(task.getTaskType()));
			taskExecutor.setLongRunningTaskId(task.getId());
			taskExecutor.init((Map<String, Object>) task.getTaskProperties());
		} catch (ClassNotFoundException e) {
			ex = e;
			resultModel = new DefaultResultModel(CoreResultCode.LONG_RUNNING_TASK_NOT_FOUND,
					ImmutableMap.of(
							"taskId", task.getId(),
							"taskType", task.getTaskType(),
							"instanceId", task.getInstanceId()));

		} catch (Exception e) {
			ex = e;
			resultModel = new DefaultResultModel(CoreResultCode.LONG_RUNNING_TASK_INIT_FAILED,
					ImmutableMap.of(
							"taskId", task.getId(),
							"taskType", task.getTaskType(),
							"instanceId", task.getInstanceId()));
		}
		if (ex != null) {
			LOG.error(resultModel.toString(), ex);
			task.setResult(new OperationResult.Builder(OperationState.EXCEPTION).setModel(resultModel).setCause(ex).build());
			service.save(task);
			return null;
		} else {
			return taskExecutor;
		}
	}
	
	private void cancelTaskByRestart(IdmLongRunningTaskDto task) {
		LOG.info("Cancel unprocessed long running task [{}] - tasks was interrupt during instance [{}] restart", task, task.getInstanceId());
		task.setRunning(false);
		ResultModel resultModel = new DefaultResultModel(CoreResultCode.LONG_RUNNING_TASK_CANCELED_BY_RESTART, 
				ImmutableMap.of(
						"taskId", task.getId(), 
						"taskType", task.getTaskType(),
						"instanceId", task.getInstanceId()));			
		task.setResult(new OperationResult.Builder(OperationState.CANCELED).setModel(resultModel).build());
		service.saveInternal(task);
	}
}
