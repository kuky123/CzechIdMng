package eu.bcvsolutions.idm.core.scheduler.service.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmScheduledTaskDto;
import eu.bcvsolutions.idm.core.scheduler.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.entity.IdmLongRunningTask_;
import eu.bcvsolutions.idm.core.scheduler.entity.IdmProcessedTaskItem;
import eu.bcvsolutions.idm.core.scheduler.entity.IdmProcessedTaskItem_;
import eu.bcvsolutions.idm.core.scheduler.entity.IdmScheduledTask_;
import eu.bcvsolutions.idm.core.scheduler.repository.IdmProcessedTaskItemRepository;
import eu.bcvsolutions.idm.core.scheduler.service.api.IdmProcessedTaskItemService;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Default implementation of processed items service.
 * 
 * @author Jan Helbich
 *
 */
public class DefaultIdmProcessedTaskItemService
	extends AbstractReadWriteDtoService<IdmProcessedTaskItemDto, IdmProcessedTaskItem, IdmProcessedTaskItemFilter>
	implements IdmProcessedTaskItemService {
	
	private final IdmProcessedTaskItemRepository repository;

	@Autowired
	public DefaultIdmProcessedTaskItemService(IdmProcessedTaskItemRepository repository) {
		super(repository);
		//
		this.repository = repository;
	}
	
	@Transactional
	@Override
	public IdmProcessedTaskItemDto saveInternal(IdmProcessedTaskItemDto dto) {
		Assert.notNull(dto);
		//
		if (dto.getLongRunningTask() != null && dto.getScheduledTaskQueueOwner() != null) {
			throw new CoreException("Item cannot be in both scheduled task queue and long running task log.");
		}
		if (dto.getLongRunningTask() == null && dto.getScheduledTaskQueueOwner() == null) {
			throw new CoreException("Item must have either queue (IdmScheduledTask) or log (IdmLongRunningTask) association.");
		}
		return super.saveInternal(dto);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.SCHEDULER, getEntityClass());
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmProcessedTaskItem> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, IdmProcessedTaskItemFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		//
		// items queue filter
		if (!StringUtils.isEmpty(filter.getReferencedEntityType())) {
			predicates.add(builder.equal(root.get(IdmProcessedTaskItem_.referencedDtoType), filter.getReferencedEntityType()));
		}
		if (filter.getReferencedEntityId() != null) {
			predicates.add(builder.equal(root.get(IdmProcessedTaskItem_.referencedEntityId), filter.getReferencedEntityId()));
		}
		if (filter.getLongRunningTaskId() != null) {
			predicates.add(builder.equal(root.get(
					IdmProcessedTaskItem_.longRunningTask)
					.get(IdmLongRunningTask_.id),
					filter.getLongRunningTaskId()));
		}
		if (filter.getScheduledTaskId() != null) {
			predicates.add(builder.equal(root.get(
					IdmProcessedTaskItem_.scheduledTaskQueueOwner)
					.get(IdmScheduledTask_.id),
					filter.getScheduledTaskId()));
		}
		return predicates;
	}

	@Transactional
	@Override
	public void deleteAllByLongRunningTask(IdmLongRunningTaskDto dto) {
		Assert.notNull(dto);
		//
		repository.deleteAllByLongRunningTaskId(dto.getId());
	}

	@Transactional
	@Override
	public void deleteAllByScheduledTask(IdmScheduledTaskDto dto) {
		Assert.notNull(dto);
		//
		repository.deleteAllByScheduledTaskId(dto.getId());
	}

	@Transactional(readOnly = true)
	@Override
	public List<UUID> findAllRefEntityIdsInQueueByScheduledTask(IdmScheduledTaskDto dto) {
		Assert.notNull(dto);
		//
		return repository.findAllRefEntityIdsByScheduledTaskId(dto.getId());
	}

	@Transactional(readOnly = true)
	@Override
	public Page<IdmProcessedTaskItemDto> findQueueItems(IdmScheduledTaskDto scheduledTask, Pageable pageable) {
		Assert.notNull(scheduledTask);
		//
		IdmProcessedTaskItemFilter f = new IdmProcessedTaskItemFilter();
		f.setScheduledTaskId(scheduledTask.getId());
		return this.find(f, pageable);
	}

	@Transactional(readOnly = true)
	@Override
	public Page<IdmProcessedTaskItemDto> findLogItems(IdmLongRunningTaskDto longRunningTask, Pageable pageable) {
		Assert.notNull(longRunningTask);
		//
		IdmProcessedTaskItemFilter f = new IdmProcessedTaskItemFilter();
		f.setLongRunningTaskId(longRunningTask.getId());
		return this.find(f, pageable);
	}
	
	@Transactional
	@Override
	public <E extends AbstractDto> IdmProcessedTaskItemDto createLogItem(E processedItem, OperationResult result,
			IdmLongRunningTaskDto lrt) {
		//
		Assert.notNull(processedItem);
		Assert.notNull(result);
		Assert.notNull(lrt);
		//
		IdmProcessedTaskItemDto item = createProcessedItemDto(processedItem, result);
		item.setLongRunningTask(lrt.getId());
		return this.saveInternal(item);
	}

	@Transactional
	@Override
	public <E extends AbstractDto> IdmProcessedTaskItemDto createQueueItem(E processedItem, OperationResult result,
			IdmScheduledTaskDto st) {
		//
		Assert.notNull(processedItem);
		Assert.notNull(result);
		Assert.notNull(st);
		//
		IdmProcessedTaskItemDto item = createProcessedItemDto(processedItem, result);
		item.setScheduledTaskQueueOwner(st.getId());
		return this.saveInternal(item);
	}

	private <E extends AbstractDto> IdmProcessedTaskItemDto createProcessedItemDto(E dto, OperationResult opResult) {
		IdmProcessedTaskItemDto item = new IdmProcessedTaskItemDto();
		item.setReferencedEntityId(dto.getId());
		item.setReferencedDtoType(dto.getClass().getCanonicalName());
		item.setOperationResult(opResult);
		return item;
	}
	
}