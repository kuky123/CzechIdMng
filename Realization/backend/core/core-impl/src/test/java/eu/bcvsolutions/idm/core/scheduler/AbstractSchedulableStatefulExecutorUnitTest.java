package eu.bcvsolutions.idm.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmScheduledTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableStatefulExecutor;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmLongRunningTaskService;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmScheduledTaskService;
import eu.bcvsolutions.idm.core.scheduler.service.impl.DefaultIdmProcessedTaskItemService;
import eu.bcvsolutions.idm.test.api.AbstractVerifiableUnitTest;

/**
 * Stateful tasks test.
 * 
 * @author Jan Helbich
 * @author Radek Tomiška
 *
 */
public class AbstractSchedulableStatefulExecutorUnitTest extends AbstractVerifiableUnitTest {

	@Spy @InjectMocks
	private TestIdenityUnitExecutor executor;
	@Mock private DefaultIdmProcessedTaskItemService itemService;
	@Mock private IdmScheduledTaskService scheduledTaskService;
	@Mock private IdmLongRunningTaskService longRunningTaskService;
	@Mock private EntityManager entityManager;
	@Mock private Session hiberanteSession;

	@Test
	public void testParentMocking() {
		when(itemService.find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(Lists.newArrayList()));
		//
		Optional<OperationResult> result = executor.processItem(getTestIdentityDto());
		//
		assertTrue(result.isPresent());
		assertEquals(OperationState.NOT_EXECUTED, result.get().getState());
		verify(executor, times(1)).processItem(any(IdmIdentityDto.class));
		verify(itemService, times(1)).find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class));
	}
	
	@Test
	public void testIsInProcessedQueue() {
		IdmScheduledTaskDto scheduledTask = new IdmScheduledTaskDto();
		UUID randomId = UUID.randomUUID();
		scheduledTask.setId(randomId);
		when(scheduledTaskService.findByLongRunningTaskId(randomId))
			.thenReturn(scheduledTask);
		//
		List<IdmProcessedTaskItemDto> items = Lists.newArrayList(new IdmProcessedTaskItemDto());
		//
		when(itemService.find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(items));
		//
		when(executor.getLongRunningTaskId()).thenReturn(randomId);
		//
		boolean inProcessedQueue = executor.isInProcessedQueue(getTestIdentityDto());
		assertTrue(inProcessedQueue);
		//
		when(itemService.find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(Lists.newArrayList()));
		//
		inProcessedQueue = executor.isInProcessedQueue(getTestIdentityDto());
		assertFalse(inProcessedQueue);
		//
		verify(executor, times(2)).isInProcessedQueue(any(IdmIdentityDto.class));
		verify(scheduledTaskService, times(1)).findByLongRunningTaskId(randomId);
		verify(itemService, times(2)).find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class));
	}
	
	@Test
	public void testAddToProcessedQueue() {
		IdmScheduledTaskDto scheduledTask = new IdmScheduledTaskDto();
		scheduledTask.setId(UUID.randomUUID());
		when(scheduledTaskService.findByLongRunningTaskId(any(UUID.class)))
			.thenReturn(scheduledTask);
		when(scheduledTaskService.get(any(UUID.class)))
			.thenReturn(scheduledTask);
		when(itemService.saveInternal(any(IdmProcessedTaskItemDto.class)))
			.then(AdditionalAnswers.returnsFirstArg());
		when(itemService.createQueueItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class)))
			.thenCallRealMethod();
		//
		IdmIdentityDto dto = getTestIdentityDto();
		OperationResult opResult = new OperationResult.Builder(OperationState.EXCEPTION).build();
		IdmProcessedTaskItemDto qItem = executor.addToProcessedQueue(dto, opResult);
		//
		assertEquals(scheduledTask.getId(), qItem.getScheduledTaskQueueOwner());
		assertEquals(opResult, qItem.getOperationResult());
		assertEquals(dto.getClass().getCanonicalName(), qItem.getReferencedDtoType());
		assertEquals(dto.getId(), qItem.getReferencedEntityId());
		assertNull(qItem.getLongRunningTask());
		//
		verify(itemService, times(1)).saveInternal(any(IdmProcessedTaskItemDto.class));
		verify(executor, times(1)).addToProcessedQueue(any(IdmIdentityDto.class), any(OperationResult.class));
		verify(scheduledTaskService, times(1)).findByLongRunningTaskId(any(UUID.class));
		verify(itemService, times(1)).createQueueItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class));
	}
	
	@Test
	public void testLogItemProcessed() {
		IdmLongRunningTaskDto lrt = new IdmLongRunningTaskDto();
		lrt.setId(UUID.randomUUID());
		executor.setLongRunningTaskId(lrt.getId());
		when(itemService.saveInternal(any(IdmProcessedTaskItemDto.class)))
			.then(AdditionalAnswers.returnsFirstArg());
		when(longRunningTaskService.get(any(UUID.class)))
			.thenReturn(lrt);
		when(itemService.createLogItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class)))
			.thenCallRealMethod();
		//
		IdmIdentityDto dto = getTestIdentityDto();
		OperationResult opResult = new OperationResult.Builder(OperationState.EXCEPTION).build();
		IdmProcessedTaskItemDto qItem = executor.logItemProcessed(dto, opResult);
		//
		assertEquals(lrt.getId(), qItem.getLongRunningTask());
		assertEquals(opResult, qItem.getOperationResult());
		assertEquals(dto.getClass().getCanonicalName(), qItem.getReferencedDtoType());
		assertEquals(dto.getId(), qItem.getReferencedEntityId());
		assertNull(qItem.getScheduledTaskQueueOwner());
		//
		verify(itemService, times(1)).saveInternal(any(IdmProcessedTaskItemDto.class));
		verify(executor, times(1)).logItemProcessed(any(IdmIdentityDto.class), any(OperationResult.class));
		verify(executor, times(1)).getLongRunningTaskId();
		verify(itemService, times(1)).createLogItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class));
	}

	private IdmIdentityDto getTestIdentityDto() {
		IdmIdentityDto dto = new IdmIdentityDto();
		dto.setId(UUID.randomUUID());
		return dto;
	}
	
	@Test
	public void testProcess() {
		IdmScheduledTaskDto scheduledTask = new IdmScheduledTaskDto();
		scheduledTask.setId(UUID.randomUUID());
		IdmLongRunningTaskDto lrt = new IdmLongRunningTaskDto();
		//
		IdmIdentityDto dto1 = getTestIdentityDto();
		IdmIdentityDto dto2 = getTestIdentityDto();
		IdmIdentityDto dto3 = getTestIdentityDto();
		// stubs
		doReturn(Optional.of(new OperationResult.Builder(OperationState.EXECUTED).build()))
			.when(executor).processItem(any(IdmIdentityDto.class));
		doReturn(false)
			.when(executor).isInProcessedQueue(any(IdmIdentityDto.class));
		doNothing()
			.when(itemService).deleteInternal(any(IdmProcessedTaskItemDto.class));
		doReturn(Lists.newArrayList(UUID.randomUUID(), UUID.randomUUID()))
			.when(executor).getProcessedItemRefsFromQueue();
		doReturn(scheduledTask.getId())
			.when(executor).getScheduledTaskId();
		// matchers
		when(longRunningTaskService.get(any(UUID.class)))
			.thenReturn(lrt);
		when(scheduledTaskService.get(any(UUID.class)))
			.thenReturn(scheduledTask);
		when(executor.getItemsToProcess(any(Pageable.class)))
			.thenReturn(new PageImpl<>(Lists.newArrayList(dto1, dto2), new PageRequest(0, 2), 6))
			.thenReturn(new PageImpl<>(Lists.newArrayList(dto3), new PageRequest(0, 2), 6))
			.thenReturn(new PageImpl<>(Lists.newArrayList()));
		when(itemService.saveInternal(any(IdmProcessedTaskItemDto.class)))
			.then(AdditionalAnswers.returnsFirstArg());
		when(itemService.find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(Lists.newArrayList(new IdmProcessedTaskItemDto())));
		when(entityManager.getDelegate()).thenReturn(hiberanteSession);
		when(hiberanteSession.isOpen()).thenReturn(false);
		//
		//
		Boolean processingResult = executor.process();
		assertTrue(processingResult);
		//
		//
		verify(longRunningTaskService, times(1)).get(any(UUID.class));
		//
		verify(executor, times(3)).getItemsToProcess(any(Pageable.class));
		verify(executor, times(1)).isInProcessedQueue(dto1);
		verify(executor, times(1)).isInProcessedQueue(dto2);
		verify(executor, times(1)).isInProcessedQueue(dto3);
		verify(executor, times(1)).getProcessedItemRefsFromQueue();
		verify(executor, times(2)).removeFromProcessedQueue(any(UUID.class));
		verify(executor, times(3)).addToProcessedQueue(any(IdmIdentityDto.class), any(OperationResult.class));
		verify(executor, times(3)).logItemProcessed(any(IdmIdentityDto.class), any(OperationResult.class));
		// session
		verify(entityManager, times(3)).getDelegate();
		verify(hiberanteSession, times(3)).isOpen();
		// 6x addToProcessQueue, 2x removeItemFromQueue, 2x stubbed
		verify(executor, times(10)).getScheduledTaskId();
		verify(executor, times(3)).processItem(any(IdmIdentityDto.class));
		// 3x addToProcessQueue, 3x logItemProcessed
		verify(itemService, times(2)).deleteInternal(any(IdmProcessedTaskItemDto.class));
		verify(itemService, times(3)).createLogItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class));
		verify(itemService, times(3)).createQueueItem(any(AbstractDto.class), any(OperationResult.class), any(UUID.class));
		// 2x from removeFromProcessedQueue, other invocations are stubbed
		verify(itemService, times(2)).find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class));		
	}
	
	@Test
	public void testDontTouchProcessed() {
		IdmLongRunningTaskDto lrt = new IdmLongRunningTaskDto();
		IdmScheduledTaskDto scheduledTask = new IdmScheduledTaskDto();
		scheduledTask.setId(UUID.randomUUID());
		//
		IdmIdentityDto dto1 = getTestIdentityDto();
		IdmIdentityDto dto2 = getTestIdentityDto();
		// stubs
		doReturn(true)
			.when(executor).isInProcessedQueue(any(IdmIdentityDto.class));
		doReturn(Lists.newArrayList(dto1.getId(), dto2.getId()))
			.when(executor).getProcessedItemRefsFromQueue();
		doReturn(scheduledTask.getId())
			.when(executor).getScheduledTaskId();

		// matchers
		when(longRunningTaskService.get(any(UUID.class)))
				.thenReturn(lrt);
		when(executor.getItemsToProcess(any(Pageable.class)))
			.thenReturn(new PageImpl<>(Lists.newArrayList(dto1, dto2)))
			.thenReturn(new PageImpl<>(Lists.newArrayList()));
		when(entityManager.getDelegate()).thenReturn(hiberanteSession);
		when(hiberanteSession.isOpen()).thenReturn(false);
		//
		//
		Boolean processingResult = executor.process();
		assertTrue(processingResult);
		//
		//
		verify(longRunningTaskService, times(1)).get(any(UUID.class));
		//
		verify(executor, times(1)).getItemsToProcess(any(Pageable.class));
		verify(executor, times(1)).isInProcessedQueue(dto1);
		verify(executor, times(1)).isInProcessedQueue(dto2);
		verify(executor, times(1)).getProcessedItemRefsFromQueue();
		verify(entityManager, times(2)).getDelegate();
		verify(hiberanteSession, times(2)).isOpen();
		verify(executor, never()).getScheduledTaskId();
		verify(executor, never()).removeFromProcessedQueue(any(UUID.class));
		verify(executor, never()).addToProcessedQueue(any(IdmIdentityDto.class), any(OperationResult.class));
		verify(executor, never()).processItem(any(IdmIdentityDto.class));
		verify(itemService, never()).saveInternal(any(IdmProcessedTaskItemDto.class));
		verify(itemService, never()).deleteInternal(any(IdmProcessedTaskItemDto.class));
		verify(itemService, never()).find(any(IdmProcessedTaskItemFilter.class), any(Pageable.class));
	}
	
	
	public static class TestIdenityUnitExecutor extends AbstractSchedulableStatefulExecutor<IdmIdentityDto> {
		
		@Override
		public Page<IdmIdentityDto> getItemsToProcess(Pageable pageable) {
			return null;
		}

		@Override
		public Optional<OperationResult> processItem(IdmIdentityDto dto) {
			IdmProcessedTaskItemFilter f = new IdmProcessedTaskItemFilter();
			f.setReferencedEntityId(dto.getId());
			Page<IdmProcessedTaskItemDto> p = getItemService().find(f, null);
			OperationState s = p.getTotalElements() > 1 ? OperationState.EXECUTED : OperationState.NOT_EXECUTED;
			return Optional.of(new OperationResult.Builder(s).build());
		}
		
	}

}
