package eu.bcvsolutions.idm.core.scheduler.api.service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;

/**
 * Basic interface for stateful task executors, which need to keep track of already
 * processed items.
 * 
 * @author Jan Helbich
 *
 * @param <DTO> entity DTO type
 * @param <V> return value type
 */
public interface SchedulableStatefulExecutor<DTO extends AbstractDto, V> extends SchedulableTaskExecutor<V> {
	
	/**
	 * Name of the workflow variable which stores the operation result
	 * TODO: use WorkflowProcessInstanceService.VARIABLE_OPERATION_RESULT after wf api will be moved to core api
	 * 
	 * of type {@link OperationResult}.
	 */
	static final String OPERATION_RESULT_VAR = "operationResult";
	
	/**
	 * Returns a pageable result set of DTOs the task should process.
	 * @param pageable
	 * @return
	 */
	Page<DTO> getItemsToProcess(Pageable pageable);
	
	/**
	 * Processes single entity DTO. If operation result is not returned,
	 * the worker shall handle queue item addition or removal itself, i.e.
	 * by an asynchronous callback.
	 * 
	 * @param dto
	 * @return
	 */
	Optional<OperationResult> processItem(DTO dto);
	
	/**
	 * Each item will be processed in new transaction
	 * 
	 * @return
	 * @since 9.3.0
	 */
	boolean requireNewTransaction();
	
	/**
	 * If process of one item fails on exception, then continue with the next item.
	 *  
	 * @return true - continue with next item, false - end on the first exception
	 * @since 9.3.0
	 */
	boolean continueOnException();
	
	/**
	 * Returns all entity references (of type {@link AbstractDto#getId()}
	 * from processed items queue, including items that were processed in earlier
	 * runs of this executor.
	 * 
	 * @return a collection of entity ID references
	 */
	Collection<UUID> getProcessedItemRefsFromQueue();
	
	/**
	 * Checks whether the given entity DTO has been already processed
	 * and is active in processed items queue.
	 * 
	 * @param dto
	 * @return
	 */
	boolean isInProcessedQueue(DTO dto);
	
	/**
	 * Adds entity DTO among processed items.
	 * 
	 * @param dto
	 */
	IdmProcessedTaskItemDto addToProcessedQueue(DTO dto, OperationResult opResult);
	
	/**
	 * Removes entity reference from active items in processed queue by its ID.
	 * 
	 * @param entityRef
	 */
	void removeFromProcessedQueue(UUID entityRef);
	
	/**
	 * Removes entity reference from active items in processed queue.
	 * 
	 * @param dto
	 */
	void removeFromProcessedQueue(DTO dto);

}
