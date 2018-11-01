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

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.api.exception.DryRunNotSupportedException;

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
	@Autowired private PlatformTransactionManager platformTransactionManager;
	@Autowired private EntityManager entityManager;

	@Override
	public Boolean process() {
		this.counter = 0L;
		executeProcess();
		return Boolean.TRUE;
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
		return itemService.createQueueItem(dto, opResult, this.getScheduledTaskId());
	}
	
	@Override
	public Collection<UUID> getProcessedItemRefsFromQueue() {
		if (this.getScheduledTaskId() == null) {
			LOG.warn("Running stateful tasks outside scheduler is not recommended.");
			return new ArrayList<>();
		}
		return itemService.findAllRefEntityIdsInQueueByScheduledTaskId(this.getScheduledTaskId());
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
	
	/**
	 * Returns true, if given task supports dry run mode. Returns {@code true} as default.
	 * 
	 * @since 7.8.3
	 */
	@Override
	public boolean supportsDryRun() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Returns {@code false} by default (backward compatibility - each task can solve this his own way).
	 * @since 9.3.0
	 */
	@Override
	public boolean continueOnException() {
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Returns {@code false} by default (backward compatibility - each task can solve this his own way).
	 * @since 9.3.0
	 */
	@Override
	public boolean requireNewTransaction() {
		return false;
	}

	private void executeProcess() {
		Set<UUID> retrievedRefs = new HashSet<>();
		//
		int page = 0;
		boolean canContinue = true;
		boolean dryRun = longRunningTaskService.get(this.getLongRunningTaskId()).isDryRun();
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
				processCandidate(candidate, dryRun);
 				canContinue &= this.updateState();
 				//
 				// flush and clear session - if LRT is wrapped in parent transaction, we need to clear it
 				if (getHibernateSession().isOpen()) {
 					getHibernateSession().flush();
 					getHibernateSession().clear();
 				}
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
	
	private Session getHibernateSession() {
		return (Session) this.entityManager.getDelegate();
	}

	private void processCandidate(DTO candidate, boolean dryRun) {
		if (isInProcessedQueue(candidate)) {
			// item was processed earlier - just drop the count by one
			// FIXME: this is confusing => task ends with 0 count, if all 
			--count;
			return;
		}
		//
		if (requireNewTransaction()) {
			this.processItemInternalNewTransaction(candidate, dryRun);
		} else {
			this.processItemInternal(candidate, dryRun);
		}
	}
	
	private Optional<OperationResult> processItemInternalNewTransaction(DTO candidate, boolean dryRun) {
		TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		//
		return template.execute(new TransactionCallback<Optional<OperationResult>>() {
			public Optional<OperationResult> doInTransaction(TransactionStatus transactionStatus) {
				return processItemInternal(candidate, dryRun);
			}
		});
	}
	
	private Optional<OperationResult> processItemInternal(DTO candidate, boolean dryRun) {
		Optional<OperationResult> result;
		try {
			if (dryRun) {
				if (!supportsDryRun()) {
					throw new DryRunNotSupportedException(getName());
				}
				// dry run mode - operation is not executed with dry run code (no content)
				result = Optional.of(new OperationResult
						.Builder(OperationState.NOT_EXECUTED)
						.setModel(new DefaultResultModel(CoreResultCode.DRY_RUN))
						.build());
			} else {
				result = this.processItem(candidate);
			}			
		} catch (Exception ex) {
			// convert exception to result code exception with model
			ResultCodeException resultCodeException;
			if (ex instanceof ResultCodeException) {
				resultCodeException = (ResultCodeException) ex;
			} else {
				resultCodeException = new ResultCodeException(
						CoreResultCode.LONG_RUNNING_TASK_ITEM_FAILED, 
						ImmutableMap.of(
								"referencedEntityId", candidate.getId()),
						ex);	
			}
			LOG.error("[" + resultCodeException.getId() + "] ", resultCodeException);
			//
			result = Optional.of(new OperationResult
						.Builder(OperationState.EXCEPTION)
						.setException(resultCodeException)
						.build());
		}
		//
		++counter;
		if (result.isPresent()) {
			OperationResult opResult = result.get();
			this.logItemProcessed(candidate, opResult);
			if (OperationState.isSuccessful(opResult.getState())) {
				this.addToProcessedQueue(candidate, opResult);
			}
			LOG.debug("Statefull process [{}] intermediate result: [{}], count: [{}/{}]",
					getClass().getSimpleName(), opResult, count, counter);
			if (!continueOnException() && opResult.getException() != null) {
				throw (ResultCodeException) opResult.getException();
			}
		} else {
			LOG.debug("Statefull process [{}] processed item [{}] without result.",
					getClass().getSimpleName(), candidate);
		}
		//
		return result;
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
