package eu.bcvsolutions.idm.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.scheduler.api.config.SchedulerConfiguration;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.LongRunningFutureTask;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.api.exception.ConcurrentExecutionException;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmLongRunningTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmProcessedTaskItemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.scheduler.service.impl.DefaultLongRunningTaskManager;
import eu.bcvsolutions.idm.core.scheduler.task.impl.TestTaskExecutor;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Long running tasks test
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultLongRunningTaskManagerIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ApplicationContext context;
	@Autowired private IdmLongRunningTaskService service;
	@Autowired private ConfigurationService configurationService;
	@Autowired private IdmProcessedTaskItemService itemService;
	//
	private LongRunningTaskManager manager;
	
	@Before
	public void init() {		
		manager = context.getAutowireCapableBeanFactory().createBean(DefaultLongRunningTaskManager.class);
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
	}
	
	@After
	public void after() {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
	}
	
	@Test
	public void testRunSimpleTaskAsync() throws InterruptedException, ExecutionException {
		String result = "TEST_SUCCESS_01";
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(result);
		assertNull(taskExecutor.getLongRunningTaskId());
		//
		LongRunningFutureTask<String> futureTask = manager.execute(taskExecutor);
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertNotNull(longRunningTask);
		assertEquals(result, longRunningTask.getTaskDescription());
		assertEquals(taskExecutor.getClass().getCanonicalName(), longRunningTask.getTaskType());
		assertEquals(configurationService.getInstanceId(), longRunningTask.getInstanceId());
		//
		assertEquals(result, futureTask.getFutureTask().get());
		//
		longRunningTask = service.get(longRunningTask.getId());
		assertEquals(OperationState.EXECUTED, longRunningTask.getResult().getState());
	}
	
	@Test
	public void testRunSimpleTaskSync() throws InterruptedException, ExecutionException {
		String expectedResult = "TEST_SUCCESS_01_S";
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		//
		String result = manager.executeSync(taskExecutor);
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertNotNull(longRunningTask);
		assertEquals(expectedResult, longRunningTask.getTaskDescription());
		assertEquals(taskExecutor.getClass().getCanonicalName(), longRunningTask.getTaskType());
		assertEquals(configurationService.getInstanceId(), longRunningTask.getInstanceId());
		//
		assertEquals(expectedResult, result);
		//
		longRunningTask = service.get(longRunningTask.getId());
		assertEquals(OperationState.EXECUTED, longRunningTask.getResult().getState());
	}
	
	@Test
	public void testRunCountableTask() throws InterruptedException, ExecutionException {
		String result = "TEST_SUCCESS_02";
		Long count = 10L;
		LongRunningTaskExecutor<String> taskExecutor = new TestCountableLongRunningTaskExecutor(result, count);
		//
		LongRunningFutureTask<String> futureTask = manager.execute(taskExecutor);
		//
		assertEquals(result, futureTask.getFutureTask().get());
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertEquals(OperationState.EXECUTED, longRunningTask.getResult().getState());
		assertEquals(count, longRunningTask.getCount());
		assertEquals(count, longRunningTask.getCounter());
	}
	
	// TODO: locking - start event override canceled state
	// @Test
	public void testCancelTaskBeforeStart() throws InterruptedException, ExecutionException {
		String result = "TEST_SUCCESS_03";
		Long count = 50L;
		LongRunningTaskExecutor<String> taskExecutor = new TestStopableLongRunningTaskExecutor(result, count);
		//
		LongRunningFutureTask<String> futureTask = manager.execute(taskExecutor);
		manager.cancel(taskExecutor.getLongRunningTaskId());
		//	
		assertEquals(result, futureTask.getFutureTask().get());
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertEquals(OperationState.CANCELED, longRunningTask.getResult().getState());
		assertEquals(count, longRunningTask.getCount());
		assertNotEquals(count, longRunningTask.getCounter());
	}
	
	@Test
	public void testCancelRunningTask() throws InterruptedException, ExecutionException {
		String result = "TEST_SUCCESS_04";
		Long count = 100L;
		LongRunningTaskExecutor<String> taskExecutor = new TestStopableLongRunningTaskExecutor(result, count);
		//
		LongRunningFutureTask<String> futureTask = manager.execute(taskExecutor);
		//
		Function<String, Boolean> continueFunction = res -> {
			return !manager.getLongRunningTask(futureTask).isRunning();
		};
		getHelper().waitForResult(continueFunction);
		//
		manager.cancel(taskExecutor.getLongRunningTaskId());
		//
		assertEquals(result, futureTask.getFutureTask().get());
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertEquals(OperationState.CANCELED, longRunningTask.getResult().getState());
		assertEquals(count, longRunningTask.getCount());
		assertNotEquals(count, longRunningTask.getCounter());
	}
	
	@Test
	public void testInterruptRunningTask() throws InterruptedException, ExecutionException {
		String result = "TEST_SUCCESS_05";
		Long count = 100L;
		LongRunningTaskExecutor<String> taskExecutor = new TestStopableLongRunningTaskExecutor(result, count);
		//
		manager.execute(taskExecutor);
		//
		Function<String, Boolean> continueFunction = res -> {
			return !manager.getLongRunningTask(taskExecutor).isRunning();
		};
		getHelper().waitForResult(continueFunction);
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertEquals(OperationState.RUNNING, longRunningTask.getResult().getState());
		assertEquals(count, longRunningTask.getCount());
		assertTrue(longRunningTask.isRunning());
		//
		assertTrue(manager.interrupt(taskExecutor.getLongRunningTaskId()));
		//
		longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertNotEquals(OperationState.RUNNING, longRunningTask.getResult().getState());
		assertEquals(count, longRunningTask.getCount());
		assertNotEquals(count, longRunningTask.getCounter());
		assertFalse(longRunningTask.isRunning());
	}
	
	@Test
	public void testCancelPreviouslyRunnedTask() {
		IdmLongRunningTaskDto taskOne = new IdmLongRunningTaskDto();
		taskOne.setResult(new OperationResult.Builder(OperationState.RUNNING).build());
		taskOne.setInstanceId(configurationService.getInstanceId());
		taskOne.setTaskType(TestSimpleLongRunningTaskExecutor.class.getCanonicalName());
		taskOne.setRunning(true);
		taskOne = service.save(taskOne);
		//
		IdmLongRunningTaskDto taskTwo = new IdmLongRunningTaskDto();
		taskTwo.setResult(new OperationResult.Builder(OperationState.RUNNING).build());
		taskTwo.setInstanceId("different-instance");
		taskTwo.setTaskType(TestSimpleLongRunningTaskExecutor.class.getCanonicalName());
		taskTwo.setRunning(true);
		taskTwo = service.save(taskTwo);
		//
		manager.init();
		//
		taskOne = service.get(taskOne.getId());
		taskTwo = service.get(taskTwo.getId());
		//
		assertEquals(OperationState.CANCELED, taskOne.getResultState());
		assertFalse(taskOne.isRunning());
		assertEquals(OperationState.RUNNING, taskTwo.getResultState());
		assertTrue(taskTwo.isRunning());
	}
	
	@Test(expected = ConcurrentExecutionException.class)
	public void testDisallowConcurrentExecution() {
		TestTaskExecutor executorOne = new TestTaskExecutor();
		executorOne.setCount(30L);
		LongRunningFutureTask<Boolean> longRunningFutureTask = manager.execute(executorOne);
		getHelper().waitForResult(res -> {
			return !service.get(longRunningFutureTask.getExecutor().getLongRunningTaskId()).isRunning();
		});		
		TestTaskExecutor executorTwo = new TestTaskExecutor();
		executorTwo.setCount(10L);
		manager.executeSync(executorTwo);
	}
	
	@Test
	public void testCheckLogs() throws InterruptedException, ExecutionException {
		IdmIdentityDto identity1 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity2 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity3 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity4 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity5 = getHelper().createIdentity((GuardedString) null);
		//
		TestLogItemLongRunningTaskExecutor taskExecutor = new TestLogItemLongRunningTaskExecutor();
		taskExecutor.addIdentityToProcess(identity1, identity2, identity3, identity4, identity5);
		taskExecutor.addRemovedIdentity(identity2, identity5);
		assertNull(taskExecutor.getLongRunningTaskId());
		//
		LongRunningFutureTask<Boolean> futureTask = manager.execute(taskExecutor);
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertNotNull(longRunningTask);
		assertEquals(taskExecutor.getClass().getCanonicalName(), longRunningTask.getTaskType());
		assertEquals(configurationService.getInstanceId(), longRunningTask.getInstanceId());
		//
		assertEquals(Boolean.TRUE, futureTask.getFutureTask().get());
		List<IdmProcessedTaskItemDto> content = itemService.findLogItems(longRunningTask, null).getContent();
		//
		longRunningTask = service.get(longRunningTask.getId());
		assertEquals(OperationState.EXECUTED, longRunningTask.getResult().getState());
		//
		assertEquals(5, content.size());
		//
		IdmProcessedTaskItemFilter filter = new IdmProcessedTaskItemFilter();
		filter.setLongRunningTaskId(taskExecutor.getLongRunningTaskId());
		filter.setOperationState(OperationState.EXECUTED);
		content = itemService.find(filter, null).getContent();
		assertEquals(3, content.size());
		//
		filter.setOperationState(OperationState.NOT_EXECUTED);
		content = itemService.find(filter, null).getContent();
		assertEquals(2, content.size());
		//
		content = itemService.findLogItems(longRunningTask, null).getContent();
		Set<UUID> entityIdsList = content.stream().map(IdmProcessedTaskItemDto::getReferencedEntityId).collect(Collectors.toSet());
		assertEquals(5, entityIdsList.size());
		assertTrue(entityIdsList.contains(identity1.getId()));
		assertTrue(entityIdsList.contains(identity2.getId()));
		assertTrue(entityIdsList.contains(identity3.getId()));
		assertTrue(entityIdsList.contains(identity4.getId()));
		assertTrue(entityIdsList.contains(identity5.getId()));
	}
	
	@Test
	public void testCheckDisableLogs() throws InterruptedException, ExecutionException {
		IdmIdentityDto identity1 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity2 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity3 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity4 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity5 = getHelper().createIdentity((GuardedString) null);
		//
		TestLogItemLongRunningTaskExecutor taskExecutor = new TestLogItemLongRunningTaskExecutor();
		taskExecutor.addIdentityToProcess(identity1, identity2, identity3, identity4, identity5);
		taskExecutor.addRemovedIdentity(identity2, identity5);
		taskExecutor.setLog(false);
		assertNull(taskExecutor.getLongRunningTaskId());
		//
		LongRunningFutureTask<Boolean> futureTask = manager.execute(taskExecutor);
		//
		IdmLongRunningTaskDto longRunningTask = service.get(taskExecutor.getLongRunningTaskId());
		assertNotNull(longRunningTask);
		assertEquals(taskExecutor.getClass().getCanonicalName(), longRunningTask.getTaskType());
		assertEquals(configurationService.getInstanceId(), longRunningTask.getInstanceId());
		//
		assertEquals(Boolean.TRUE, futureTask.getFutureTask().get());
		List<IdmProcessedTaskItemDto> content = itemService.findLogItems(longRunningTask, null).getContent();
		//
		longRunningTask = service.get(longRunningTask.getId());
		assertEquals(OperationState.EXECUTED, longRunningTask.getResult().getState());
		//
		assertEquals(0, content.size());
	}
	
	private class TestLogItemLongRunningTaskExecutor extends AbstractLongRunningTaskExecutor<Boolean> {
		
		private final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory
				.getLogger(DefaultLongRunningTaskManagerIntegrationTest.TestLogItemLongRunningTaskExecutor.class);
		
		List<IdmIdentityDto> identities = new ArrayList<>();
		List<IdmIdentityDto> removedIdentities = new ArrayList<>();
		boolean log = true;
		
		@Override
		public Boolean process() {
			for (IdmIdentityDto identity : identities) {
				LOGGER.debug("Execute identity username: {} and id: {}" + identity.getUsername(), identity.getId());
				OperationResult result = new OperationResult();
				if (removedIdentities.contains(identity)) {
					result.setState(OperationState.NOT_EXECUTED);
				} else {
					result.setState(OperationState.EXECUTED);
				}
				if(this.log) {
					this.logItemProcessed(identity, result);
				}
			}
			return Boolean.TRUE;
		}

		public void addIdentityToProcess(IdmIdentityDto ...identities) {
			for (IdmIdentityDto identity : identities) {
				this.identities.add(identity);
			}
		}
		
		public void addRemovedIdentity(IdmIdentityDto ...identities) {
			for (IdmIdentityDto identity : identities) {
				this.removedIdentities.add(identity);
			}
		}
		
		public void setLog(boolean log) {
			this.log = log;
		}
	}
	
	private class TestSimpleLongRunningTaskExecutor extends AbstractLongRunningTaskExecutor<String> {

		private final String result;
		
		public TestSimpleLongRunningTaskExecutor(String result) {
			this.result = result;
		}
		
		@Override
		public String getDescription() {
			return result;
		}
		
		@Override
		public String process() {
			return result;
		}
		
	}
	
	private class TestCountableLongRunningTaskExecutor extends AbstractLongRunningTaskExecutor<String> {

		private final String result;
		
		public TestCountableLongRunningTaskExecutor(String result, Long count) {
			this.result = result;
			this.count = count;
			counter = 0L;
		}
		
		@Override
		public String process() {
			for (long i = 0; i < count; i++) {
				counter++;
				if(!updateState()) {
					break;
				}
			}
			return result;
		}
		
	}
	
	private class TestStopableLongRunningTaskExecutor extends AbstractLongRunningTaskExecutor<String> {

		private final String result;
		
		public TestStopableLongRunningTaskExecutor(String result, Long count) {
			this.result = result;
			this.count = count;
			counter = 0L;
		}
		
		@Override
		public String process() {
			for (long i = 0; i < count; i++) {
				counter++;
				if(!updateState()) {
					break;
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					throw new CoreException("text executor was interruped", ex);
				}
			}
			return result;
		}
		
	}
}
