package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.utils.PasswordGenerator;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmLongRunningTaskFilter;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmLongRunningTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Long running tasks filter test
 *
 * @author Marek Klement
 *
 */
public class DefaultIdmLongRunningTaskServiceTest extends AbstractIntegrationTest {

	@Autowired private IdmLongRunningTaskService idmLongRunningTaskService;
	@Autowired private LongRunningTaskManager manager;

	@Before
	public void logIn(){
		loginAsAdmin();
	}

	@After
	public void logOut(){
		super.logout();
	}

	@Test
	public void statefulFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_01_M";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());
		task1.setStateful(false);
		idmLongRunningTaskService.save(task1);
		//
		LongRunningTaskExecutor<String> taskExecutor2 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor2.getLongRunningTaskId());
		manager.executeSync(taskExecutor2);
		idmLongRunningTaskService.get(taskExecutor2.getLongRunningTaskId());
		//
		LongRunningTaskExecutor<String> taskExecutor3 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor3.getLongRunningTaskId());
		manager.executeSync(taskExecutor3);
		idmLongRunningTaskService.get(taskExecutor3.getLongRunningTaskId());
		// set filter
		filter.setTaskType(task1.getTaskType());
		filter.setStateful(true);
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong StateFul true",2, result.getTotalElements());
		filter.setStateful(false);
		result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong StateFul false",1, result.getTotalElements());
	}

	@Test
	public void runningFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_02_M";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());
		task1.setRunning(true);
		idmLongRunningTaskService.save(task1);
		// set filter
		filter.setTaskType(task1.getTaskType());
		filter.setRunning(task1.isRunning());
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong Running",task1.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void typeFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_03_M";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());
		task1.setTaskType("Type0001");
		idmLongRunningTaskService.save(task1);
		filter.setTaskType(task1.getTaskType());
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong TaskType",task1.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void textFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_04_M";
		String type1 = "Type00001";
		String type2 = "Type00002";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());
		task1.setTaskType(type1);
		idmLongRunningTaskService.save(task1);

		LongRunningTaskExecutor<String> taskExecutor2 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor2.getLongRunningTaskId());
		manager.executeSync(taskExecutor2);
		IdmLongRunningTaskDto task2 = idmLongRunningTaskService.get(taskExecutor2.getLongRunningTaskId());
		task2.setTaskType(type1);
		idmLongRunningTaskService.save(task2);

		LongRunningTaskExecutor<String> taskExecutor3 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor3.getLongRunningTaskId());
		manager.executeSync(taskExecutor3);
		IdmLongRunningTaskDto task3 = idmLongRunningTaskService.get(taskExecutor3.getLongRunningTaskId());
		task3.setTaskType(type2);
		idmLongRunningTaskService.save(task3);

		LongRunningTaskExecutor<String> taskExecutor4 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor4.getLongRunningTaskId());
		manager.executeSync(taskExecutor4);
		IdmLongRunningTaskDto task4 = idmLongRunningTaskService.get(taskExecutor4.getLongRunningTaskId());
		task4.setTaskDescription(type1);
		idmLongRunningTaskService.save(task4);
		// set filter
		filter.setText(type1);
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong Text Type",3, result.getTotalElements());
		assertEquals("Wrong Text Type",true, result.getContent().contains(task4));
		assertEquals("Wrong Text Description",true, result.getContent().contains(task1));
	}

	@Test
	public void datesFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_05_M";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());

		LongRunningTaskExecutor<String> taskExecutor2 = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor2.getLongRunningTaskId());
		manager.executeSync(taskExecutor2);
		IdmLongRunningTaskDto task2 = idmLongRunningTaskService.get(taskExecutor2.getLongRunningTaskId());
		task2.setCreated(task1.getCreated());
		idmLongRunningTaskService.save(task2);

		filter.setFrom(task1.getCreated());
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong From Date",2, result.getTotalElements());

		filter.setTill(task1.getModified());
		result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong Till Date",2, result.getTotalElements());
	}

	@Test
	public void operationStateFilterTest(){
		IdmLongRunningTaskFilter filter = new IdmLongRunningTaskFilter();
		String expectedResult = "TEST_SUCCESS_06_M";
		// set tasks
		LongRunningTaskExecutor<String> taskExecutor = new TestSimpleLongRunningTaskExecutor(expectedResult);
		assertNull(taskExecutor.getLongRunningTaskId());
		manager.executeSync(taskExecutor);
		IdmLongRunningTaskDto task1 = idmLongRunningTaskService.get(taskExecutor.getLongRunningTaskId());
		// set filter
		filter.setOperationState(task1.getResultState());
		Page<IdmLongRunningTaskDto> result = idmLongRunningTaskService.find(filter, null);
		assertEquals("Wrong operationState id",true, result.getContent().contains(task1));
	}

	@Test
	public void descriptionLengthTest2000() {
		IdmLongRunningTaskDto task = new IdmLongRunningTaskDto();
		task.setTaskType("testType-" + System.currentTimeMillis());
		task.setInstanceId("testInstance-" + System.currentTimeMillis());
		task.setResult(new OperationResult.Builder(OperationState.EXECUTED).build());
		
		// this must past
		PasswordGenerator generator = new PasswordGenerator();
		String random = generator.generateRandom(2000, 2000, null, null, null, null);
		assertEquals(2000, random.length());
		task.setTaskDescription(random);

		IdmLongRunningTaskDto newSaved = idmLongRunningTaskService.save(task);
		assertNotNull(newSaved);
		assertEquals(random, newSaved.getTaskDescription());
		assertEquals(2000, newSaved.getTaskDescription().length());
	}

	@Test
	public void descriptionLengthTest2050() {
		IdmLongRunningTaskDto task = new IdmLongRunningTaskDto();
		task.setTaskType("testType-" + System.currentTimeMillis());
		task.setInstanceId("testInstance-" + System.currentTimeMillis());
		task.setResult(new OperationResult.Builder(OperationState.EXECUTED).build());
		
		// this must also past, but description will be cutoff
		PasswordGenerator generator = new PasswordGenerator();
		String random = generator.generateRandom(2001, 2050, null, null, null, null);
		if (random.length() <= 2000) {
			fail();
		}
		task.setTaskDescription(random);

		IdmLongRunningTaskDto newSaved = idmLongRunningTaskService.save(task);
		assertNotNull(newSaved);
		assertNotEquals(random, newSaved.getTaskDescription());
		assertEquals(2000, newSaved.getTaskDescription().length());
		assertTrue(newSaved.getTaskDescription().endsWith("..."));
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
}
