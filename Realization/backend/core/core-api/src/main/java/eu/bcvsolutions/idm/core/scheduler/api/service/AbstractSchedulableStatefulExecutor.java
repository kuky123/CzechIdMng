package eu.bcvsolutions.idm.core.scheduler.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmProcessedTaskItemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.SchedulableStatefulExecutor;

/**
 * Abstract base class for statefull tasks, which handles common
 * process flow for context-less and stateful processes (the ones
 * with inner memory.
 * 
 * All stateful processes work with entity IDs (of type UUID) as
 * references to already processed items. 
 * 
 * @author Jan Helbich
 * @author Radek Tomiška
 *
 * @param <DTO> process DTO type, 
 * @since 7.6.0
 */
public abstract class AbstractSchedulableStatefulExecutor<DTO extends AbstractDto>
	extends AbstractSchedulableTaskExecutor<Boolean>
	implements SchedulableStatefulExecutor<DTO, Boolean> {
	
	private static final Logger LOG = LoggerFactory.getLogger(AbstractSchedulableStatefulExecutor.class);
	private static final int PAGE_SIZE = 100;
	//
	@Autowired private IdmProcessedTaskItemService itemService;

	@Override
	public Boolean process() {
		this.counter = 0L;
		executeProcess();
		return Boolean.TRUE;
	}

	@Override
	public IdmProcessedTaskItemDto logItemProcessed(DTO dto, OperationResult opResult) {
		Assert.notNull(dto);
		Assert.notNull(opResult);
		//
		return itemService.createLogItem(dto, opResult, longRunningTaskService.get(this.getLongRunningTaskId()));
	}

	@Override
	public IdmProcessedTaskItemDto addToProcessedQueue(DTO dto, OperationResult opResult) {
		Assert.notNull(dto);
		Assert.notNull(opResult);
		//
		if (this.getScheduledTaskId() == null) {
			// manually executed task -> ignore stateful queue
			LOG.warn("Running stateful tasks outside scheduler is not recommended.");
			return null;
		}
		return itemService.createQueueItem(dto, opResult, scheduledTaskService.get(this.getScheduledTaskId()));
	}
	
	@Override
	public Collection<UUID> getProcessedItemRefsFromQueue() {
		if (this.getScheduledTaskId() == null) {
			LOG.warn("Running stateful tasks outside scheduler is not recommended.");
			return new ArrayList<>();
		}
		return itemService.findAllRefEntityIdsInQueueByScheduledTask(scheduledTaskService.get(this.getScheduledTaskId()));
	}
	
	@Override
	public boolean isInProcessedQueue(DTO dto) {
		Assert.notNull(dto);
		//
		Page<IdmProcessedTaskItemDto> p = getItemFromQueue(dto.getId());
		return p.getTotalElements() > 0;
	}

	@Override
	public void removeFromProcessedQueue(UUID entityRef) {
		Assert.notNull(entityRef);
		//
		Page<IdmProcessedTaskItemDto> p = getItemFromQueue(entityRef);
		//
		Assert.isTrue(p.getTotalElements() == 1);
		itemService.deleteInternal(p.iterator().next());
	}

	@Override
	public void removeFromProcessedQueue(DTO dto) {
		Assert.notNull(dto);
		//
		removeFromProcessedQueue(dto.getId());
	}

	private void executeProcess() {
		Set<UUID> retrievedRefs = new HashSet<>();
		//
		int page = 0;
		boolean canContinue = true;
		//
		do {
			Page<DTO> candidates = this.getItemsToProcess(new PageRequest(page, PAGE_SIZE));
			//
			if (count == null) {
				count = candidates.getTotalElements();
			}
			//
			for (Iterator<DTO> i = candidates.iterator(); i.hasNext() && canContinue;) {
				DTO candidate = i.next();
				Assert.notNull(candidate);
				Assert.notNull(candidate.getId());
				//
				retrievedRefs.add(candidate.getId());
				processCandidate(candidate);
				canContinue &= this.updateState();
			}
			canContinue &= candidates.hasNext();			
			++page;
			//
		} while (canContinue);
		//
		List<UUID> queueEntityRefs = Lists.newArrayList(this.getProcessedItemRefsFromQueue());
		queueEntityRefs.removeAll(retrievedRefs);
		queueEntityRefs.forEach(entityRef -> this.removeFromProcessedQueue(entityRef));
	}

	private void processCandidate(DTO candidate) {
		if (isInProcessedQueue(candidate)) {
			// item was processed earlier - just drop the count by one
			--count;
			return;
		}
		Optional<OperationResult> result = this.processItem(candidate);
		++counter;
		if (result.isPresent()) {
			OperationResult opResult = result.get();
			this.logItemProcessed(candidate, opResult);
			if (OperationState.isSuccessful(opResult.getState())) {
				this.addToProcessedQueue(candidate, opResult);
			}
			LOG.debug("Statefull process [{}] intermediate result: [{}], count: [{}/{}]",
					getClass().getSimpleName(), opResult, count, counter);
		} else {
			LOG.debug("Statefull process [{}] processed item [{}] without result.",
					getClass().getSimpleName(), candidate);
		}
			
	}
	
	private Page<IdmProcessedTaskItemDto> getItemFromQueue(UUID entityRef) {
		// if scheduled task is null process all item including already processed items 
		// TODO: this is probably not good idea, but for now it is only choice
		if (this.getScheduledTaskId() == null) {
			return new PageImpl<>(Collections.emptyList());
		}
		IdmProcessedTaskItemFilter filter = new IdmProcessedTaskItemFilter();
		filter.setReferencedEntityId(entityRef);
		filter.setScheduledTaskId(this.getScheduledTaskId());
		Page<IdmProcessedTaskItemDto> p = itemService.find(filter, new PageRequest(0, 1));
		if (p.getTotalElements() > 1) {
			LOG.warn("Multiple same item references found in [{}] process queue.", this.getClass());
		}
		return p;
	}
	
	/**
	 * Persists LRT items
	 * 
	 * @return
	 */
	protected IdmProcessedTaskItemService getItemService() {
		return itemService;
	}
	
}