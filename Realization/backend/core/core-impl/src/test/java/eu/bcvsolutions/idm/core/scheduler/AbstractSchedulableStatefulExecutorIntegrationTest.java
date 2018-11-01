package eu.bcvsolutions.idm.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.utils.AutowireHelper;
import eu.bcvsolutions.idm.core.scheduler.api.config.SchedulerConfiguration;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmScheduledTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableStatefulExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmLongRunningTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmProcessedTaskItemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmScheduledTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.scheduler.api.service.SchedulableTaskExecutor;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;
import eu.bcvsolutions.idm.test.api.utils.SchedulerTestUtils;

/**
 * Stateful tasks test:
 * - persist processed items
 * - test queue titems
 * - continueOnException
 * - requiresNewTransaction
 * 
 * @author Jan Helbich
 * @author Radek Tomiška
 *
 */
public class AbstractSchedulableStatefulExecutorIntegrationTest extends AbstractIntegrationTest {

	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmProcessedTaskItemService itemService;
	@Autowired private IdmScheduledTaskService scheduledTaskService;
	@Autowired private IdmLongRunningTaskService longRunningService;
	@Autowired private LongRunningTaskManager longRunningTaskManager;
	
	/**
	 * Tests the stateful execution method of the task by the following
	 * scenario:
	 *   1. find all identities to process
	 *   2. process retrieved identities
	 *   3. persist processing log
	 *   4. save processed identities into processed queue
	 * 
	 * Second execution run:
	 *   1. find all identities to process - same as first run
	 *   2. call process retrieved identities - all have been processed already
	 *   3. check processing log - nothing new was processed, therefore must be empty
	 *   4. check processed items queue did not change
	 *   
	 * Third run:
	 *   1. find all identities to process - returns empty list
	 *   2. check processing log - nothing was processed
	 *   3. check processed items queue - must be empty
	 * @throws Exception
	 */
	@Test
	public void testExecute() throws Exception {
		TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
		AutowireHelper.autowire(executor);
		// manually prepare control entities - normally scheduler will take care of it itself
		IdmScheduledTaskDto scheduledTask = createIdmScheduledTask(UUID.randomUUID().toString());
		IdmLongRunningTaskDto longRunningTask = createIdmLongRunningTask(scheduledTask, TestIdenityIntegrationExecutor.class);
		executor.setLongRunningTaskId(longRunningTask.getId());
		// first run
		List<IdmIdentityDto> itemsToProcess = findTestIdentities();
		// set executor data
		executor.dtos = itemsToProcess;
		//
		Boolean result = executor.process();
		Page<IdmProcessedTaskItemDto> queueItems = itemService.findQueueItems(scheduledTask, null);
		Page<IdmProcessedTaskItemDto> logItems = itemService.findLogItems(longRunningTask, null);
		//
		assertTrue(result);
		assertEquals(longRunningTask.getScheduledTask(), scheduledTask.getId());
		assertEquals(itemsToProcess.size(), queueItems.getTotalElements());
		assertEquals(itemsToProcess.size(), logItems.getTotalElements());
		assertEquals(Long.valueOf(itemsToProcess.size()), executor.getCount());
		assertEquals(Long.valueOf(itemsToProcess.size()), executor.getCounter());
		SchedulerTestUtils.checkLogItems(longRunningTask, IdmIdentityDto.class, logItems);
		SchedulerTestUtils.checkQueueItems(scheduledTask, IdmIdentityDto.class, queueItems);
		//
		// second run
		//
		longRunningTask = createIdmLongRunningTask(scheduledTask, TestIdenityIntegrationExecutor.class);
		executor.setLongRunningTaskId(longRunningTask.getId());
		executor.dtos = itemsToProcess;
		//
		result = executor.process();
		queueItems = itemService.findQueueItems(scheduledTask, null);
		logItems = itemService.findLogItems(longRunningTask, null);
		//
		assertTrue(result);
		assertEquals(itemsToProcess.size(), queueItems.getTotalElements());
		assertEquals(0, logItems.getTotalElements());
		assertEquals(Long.valueOf(0), executor.getCount());
		assertEquals(Long.valueOf(0), executor.getCounter());
		SchedulerTestUtils.checkQueueItems(scheduledTask, IdmIdentityDto.class, queueItems);
		//
		// third run
		//
		longRunningTask = createIdmLongRunningTask(scheduledTask, TestIdenityIntegrationExecutor.class);
		executor.setLongRunningTaskId(longRunningTask.getId());
		//
		result = executor.process();
		queueItems = itemService.findQueueItems(scheduledTask, null);
		logItems = itemService.findLogItems(longRunningTask, null);
		//
		assertTrue(result);
		assertEquals(0, queueItems.getTotalElements());
		assertEquals(0, logItems.getTotalElements());
		assertEquals(Long.valueOf(0), executor.getCount());
		assertEquals(Long.valueOf(0), executor.getCounter());
	}
	
	@Test
	public void testTransaction() throws Exception {
		List<IdmIdentityDto> identities = createTestIdentities(3);
		String lastName = getHelper().createName();
		try {
			getTransactionTemplate().execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus transactionStatus) {
					IdmIdentityDto identity = identities.get(0);
					identity.setLastName(lastName);
					identityService.save(identity);
					throw new CoreException("fail");
				}
			});
		} catch(Exception ex) {
			// nothing
		} finally {
			Assert.assertNotEquals(lastName, identityService.get(identities.get(0)).getLastName());
		}
	}
	
	@Test
	public void testNotContinueWithoutRequiresNew() throws Exception {
		List<IdmIdentityDto> identities = createTestIdentities(3);
		//
		TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
		executor.dtos = identities;
		executor.continueOnException = false;
		executor.requireNewTransaction = false;
		String changeLastName = "last-name-update";
		executor.changeLastName = changeLastName;
		executor.exceptionOnItem = 2;
		//
		try {
			getTransactionTemplate().execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus transactionStatus) {
					longRunningTaskManager.execute(executor);
					// lookout: long running task never fails on exception => processed with exception. Rollback has to be controlled manually.
					throw new CoreException("fail");
				}
			});
		} catch (Exception ex) {
			// nothing
		}
		//
		IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
		Assert.assertEquals(3, taskDto.getCount().intValue());
		Assert.assertEquals(1, taskDto.getSuccessItemCount().intValue());
		Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
		// nothing committed
		for(IdmIdentityDto i : identities) {
			IdmIdentityDto identity = identityService.get(i);
			//
			Assert.assertNotEquals(changeLastName, identity.getLastName());
		}
	}
	
	@Test
	public void testNotContinueWithoutRequiresNewAsync() throws Exception {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
		try {
			List<IdmIdentityDto> identities = createTestIdentities(3);
			//
			TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
			executor.dtos = identities;
			executor.continueOnException = false;
			executor.requireNewTransaction = false;
			String changeLastName = "last-name-update";
			executor.changeLastName = changeLastName;
			executor.exceptionOnItem = 2;
			//
			longRunningTaskManager.execute(executor);
			Function<String, Boolean> continueFunction = res -> {
				return !longRunningTaskManager.getLongRunningTask(executor).isRunning();
			};
			getHelper().waitForResult(continueFunction);
			//
			IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
			Assert.assertEquals(3, taskDto.getCount().intValue());
			Assert.assertEquals(1, taskDto.getSuccessItemCount().intValue());
			Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
			
			// TODO: asynchronously => in new transaction each item => commited. How to solve it?
		} finally {
			getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	@Test
	public void testContinueWithoutRequiresNew() throws Exception {
		List<IdmIdentityDto> identities = createTestIdentities(3);
		//
		TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
		executor.dtos = identities;
		executor.continueOnException = true;
		executor.requireNewTransaction = false;
		String changeLastName = "last-name-update";
		executor.changeLastName = changeLastName;
		executor.exceptionOnItem = 2;
		//
		try {
			getTransactionTemplate().execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus transactionStatus) {
					longRunningTaskManager.execute(executor);
					// lookout: long running task never fails on exception => processed with exception. Rollback has to be controlled manually.
					throw new CoreException("fail");
				}
			});
		} catch (Exception ex) {
			// nothing
		}
		//
		IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
		Assert.assertEquals(3, taskDto.getCount().intValue());
		Assert.assertEquals(2, taskDto.getSuccessItemCount().intValue());
		Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
		// nothing committed
		for(IdmIdentityDto i : identities) {
			IdmIdentityDto identity = identityService.get(i);
			//
			Assert.assertNotEquals(changeLastName, identity.getLastName());
		}
	}
	
	@Test
	public void testContinueWithoutRequiresNewAsync() throws Exception {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
		try {
			List<IdmIdentityDto> identities = createTestIdentities(3);
			//
			TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
			executor.dtos = identities;
			executor.continueOnException = true;
			executor.requireNewTransaction = false;
			String changeLastName = "last-name-update";
			executor.changeLastName = changeLastName;
			executor.exceptionOnItem = 2;
			//
			longRunningTaskManager.execute(executor);
			Function<String, Boolean> continueFunction = res -> {
				return !longRunningTaskManager.getLongRunningTask(executor).isRunning();
			};
			getHelper().waitForResult(continueFunction);
			//
			IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
			Assert.assertEquals(3, taskDto.getCount().intValue());
			Assert.assertEquals(2, taskDto.getSuccessItemCount().intValue());
			Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
			
			// TODO: asynchronously => in new transaction each item => commited. How to solve it?
		} finally {
			getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	@Test
	public void testNotContinueWithRequiresNew() throws Exception {
		List<IdmIdentityDto> identities = createTestIdentities(3);
		//
		TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
		executor.dtos = identities;
		executor.continueOnException = false;
		executor.requireNewTransaction = true;
		String changeLastName = "last-name-update";
		executor.changeLastName = changeLastName;
		executor.exceptionOnItem = 2;
		//
		try {
			getTransactionTemplate().execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus transactionStatus) {
					longRunningTaskManager.execute(executor);
					// lookout: long running task never fails on exception => processed with exception. Rollback has to be controlled manually.
					throw new CoreException("fail");
				}
			});
		} catch (Exception ex) {
			// nothing
		}
		//
		IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
		Assert.assertEquals(3, taskDto.getCount().intValue());
		Assert.assertEquals(1, taskDto.getSuccessItemCount().intValue());
		Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
		// first commited only
		Assert.assertEquals(changeLastName, identityService.get(identities.get(0)).getLastName());
		Assert.assertNotEquals(changeLastName, identityService.get(identities.get(1)).getLastName());
		Assert.assertNotEquals(changeLastName, identityService.get(identities.get(2)).getLastName());
	}
	
	@Test
	public void testNotContinueWithRequiresNewAsync() throws Exception {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
		try {
			List<IdmIdentityDto> identities = createTestIdentities(3);
			//
			TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
			executor.dtos = identities;
			executor.continueOnException = false;
			executor.requireNewTransaction = true;
			String changeLastName = "last-name-update";
			executor.changeLastName = changeLastName;
			executor.exceptionOnItem = 2;
			//
			longRunningTaskManager.execute(executor);
			Function<String, Boolean> continueFunction = res -> {
				return !longRunningTaskManager.getLongRunningTask(executor).isRunning();
			};
			getHelper().waitForResult(continueFunction);
			//
			IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
			Assert.assertEquals(3, taskDto.getCount().intValue());
			Assert.assertEquals(1, taskDto.getSuccessItemCount().intValue());
			Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
			
			// TODO: asynchronously => in new transaction each item => commited. How to solve it?
		} finally {
			getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	@Test
	public void testContinueWithRequiresNew() throws Exception {
		List<IdmIdentityDto> identities = createTestIdentities(3);
		//
		TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
		executor.dtos = identities;
		executor.continueOnException = true;
		executor.requireNewTransaction = true;
		String changeLastName = "last-name-update";
		executor.changeLastName = changeLastName;
		executor.exceptionOnItem = 2;
		//
		try {
			getTransactionTemplate().execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus transactionStatus) {
					longRunningTaskManager.execute(executor);
					// lookout: long running task never fails on exception => processed with exception. Rollback has to be controlled manually.
					throw new CoreException("fail");
				}
			});
		} catch (Exception ex) {
			// nothing
		}
		//
		IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
		Assert.assertEquals(3, taskDto.getCount().intValue());
		Assert.assertEquals(2, taskDto.getSuccessItemCount().intValue());
		Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
		// first, third commited
		Assert.assertEquals(changeLastName, identityService.get(identities.get(0)).getLastName());
		Assert.assertNotEquals(changeLastName, identityService.get(identities.get(1)).getLastName());
		Assert.assertEquals(changeLastName, identityService.get(identities.get(2)).getLastName());
	}
	
	@Test
	public void testContinueWithRequiresNewAsync() throws Exception {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
		try {
			List<IdmIdentityDto> identities = createTestIdentities(3);
			//
			TestIdenityIntegrationExecutor executor = new TestIdenityIntegrationExecutor();
			executor.dtos = identities;
			executor.continueOnException = true;
			executor.requireNewTransaction = true;
			String changeLastName = "last-name-update";
			executor.changeLastName = changeLastName;
			executor.exceptionOnItem = 2;
			//
			longRunningTaskManager.execute(executor);
			Function<String, Boolean> continueFunction = res -> {
				return !longRunningTaskManager.getLongRunningTask(executor).isRunning();
			};
			getHelper().waitForResult(continueFunction);
			//
			IdmLongRunningTaskDto taskDto = longRunningService.get(executor.getLongRunningTaskId());
			Assert.assertEquals(3, taskDto.getCount().intValue());
			Assert.assertEquals(2, taskDto.getSuccessItemCount().intValue());
			Assert.assertEquals(1, taskDto.getFailedItemCount().intValue());
			
			// TODO: asynchronously => in new transaction each item => commited. How to solve it?
		} finally {
			getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	private List<IdmIdentityDto> findTestIdentities() {
		return identityService.find(null, new PageRequest(0, 10)).getContent();
	}
	
	private List<IdmIdentityDto> createTestIdentities(int count) {
		List<IdmIdentityDto> identities = new ArrayList<>();
		for (int i = 0; i< count; i++) {
			identities.add(getHelper().createIdentity((GuardedString) null));
		}
		return identities;
	}
	

	private IdmScheduledTaskDto createIdmScheduledTask(String taskName) {
		return scheduledTaskService.save(SchedulerTestUtils.createIdmScheduledTask(taskName));
	}

	private IdmLongRunningTaskDto createIdmLongRunningTask(IdmScheduledTaskDto taskDto,
			Class<? extends SchedulableTaskExecutor<Boolean>> clazz) {
		return longRunningService.save(SchedulerTestUtils.createIdmLongRunningTask(taskDto, clazz));
	}
	
	private static class TestIdenityIntegrationExecutor extends AbstractSchedulableStatefulExecutor<IdmIdentityDto> {
		
		@Autowired private IdmIdentityService identityService;
		//
		private List<IdmIdentityDto> dtos = null;
		private boolean continueOnException;
		private boolean requireNewTransaction;
		private Integer exceptionOnItem = null;
		private String changeLastName = null;
		
		@Override
		public boolean continueOnException() {
			return continueOnException;
		}
		
		@Override
		public boolean requireNewTransaction() {
			return requireNewTransaction;
		}
		
		@Override
		public Page<IdmIdentityDto> getItemsToProcess(Pageable pageable) {
			PageImpl<IdmIdentityDto> res = new PageImpl<>(dtos);
			dtos = new ArrayList<>();
			return res;
		}

		@Override
		public Optional<OperationResult> processItem(IdmIdentityDto dto) {
			if (exceptionOnItem != null) {
				if (Integer.valueOf(counter.intValue()).equals(exceptionOnItem - 1)) {
					throw new CoreException("fail");
				}
			}
			if (changeLastName != null) {
				dto.setLastName(changeLastName);
				identityService.save(dto);
			}
			return Optional.of(new OperationResult.Builder(OperationState.EXECUTED).build());
		}	
	}	
}
