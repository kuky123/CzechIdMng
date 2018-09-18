package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.AbstractRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestItemFilter;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.IdmRequestItemService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.service.RequestManager.RequestPredicate;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmRequestItem;
import eu.bcvsolutions.idm.core.model.entity.IdmRequestItem_;
import eu.bcvsolutions.idm.core.model.repository.IdmRequestItemRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowHistoricProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowHistoricProcessInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;
import groovy.lang.Lazy;

/**
 * Default implementation of request's item service
 * 
 * @author svandav
 *
 */
@Service("requestItemService")
public class DefaultIdmRequestItemService
		extends AbstractReadWriteDtoService<IdmRequestItemDto, IdmRequestItem, IdmRequestItemFilter>
		implements IdmRequestItemService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmRequestItemService.class);

	@Autowired
	private ConfidentialStorage confidentialStorage;
	@Autowired
	private WorkflowProcessInstanceService workflowProcessInstanceService;
	@Autowired
	private WorkflowHistoricProcessInstanceService workflowHistoricProcessInstanceService;
	@Autowired
	@Lazy
	private RequestManager requestManager;

	@Autowired
	public DefaultIdmRequestItemService(IdmRequestItemRepository repository) {
		super(repository);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.REQUESTITEM, getEntityClass());
	}

	@Override
	public IdmRequestItemDto toDto(IdmRequestItem entity, IdmRequestItemDto dto) {
		IdmRequestItemDto requestItemDto = super.toDto(entity, dto);
		// Load and add WF process DTO to embedded. Prevents of many requests from FE.
		if (requestItemDto != null && requestItemDto.getWfProcessId() != null) {
			if (RequestState.IN_PROGRESS == requestItemDto.getState()) {
				// Instance of process should exists only in 'IN_PROGRESS' state
				WorkflowProcessInstanceDto processInstanceDto = workflowProcessInstanceService
						.get(requestItemDto.getWfProcessId());
				// Trim a process variables - prevent security issues and too high of response
				// size
				if (processInstanceDto != null) {
					processInstanceDto.setProcessVariables(null);
				}
				requestItemDto.getEmbedded().put(AbstractRequestDto.WF_PROCESS_FIELD, processInstanceDto);
			} else {
				// In others states we need load historic process
				WorkflowHistoricProcessInstanceDto processHistDto = workflowHistoricProcessInstanceService
						.get(requestItemDto.getWfProcessId());
				// Trim a process variables - prevent security issues and too high of response
				// size
				if (processHistDto != null) {
					processHistDto.setProcessVariables(null);
				}
				requestItemDto.getEmbedded().put(AbstractRequestDto.WF_PROCESS_FIELD, processHistDto);
			}
		}

		// Load and add owner DTO to embedded. Prevents of many requests from FE.
		if (requestItemDto != null && requestItemDto.getOwnerId() != null && requestItemDto.getOwnerType() != null) {
			try {
				@SuppressWarnings("unchecked")
				Requestable requestable = requestManager.convertItemToDto(requestItemDto,
						(Class<Requestable>) Class.forName(requestItemDto.getOwnerType()));
				if (requestable == null) {
					// Entity was not found ... maybe was deleted or not exists yet
					LOG.debug(MessageFormat.format("Owner [{0}, {1}] not found for request {2}.",
							requestItemDto.getOwnerType(), requestItemDto.getOwnerId(), requestItemDto.getId()));
				}
				requestItemDto.getEmbedded().put(IdmRequestDto.OWNER_FIELD, requestable);
			} catch (ClassNotFoundException e) {
				// Only print warning
				LOG.warn(MessageFormat.format("Class not found for request item {0}.", requestItemDto.getId()), e);
			} catch (JsonParseException e) {
				// Only print warning
				LOG.warn(MessageFormat.format("JsonParseException for request item {0}.", requestItemDto.getId()), e);
			} catch (JsonMappingException e) {
				// Only print warning
				LOG.warn(MessageFormat.format("JsonMappingException for request item {0}.", requestItemDto.getId()), e);
			} catch (IOException e) {
				// Only print warning
				LOG.warn(MessageFormat.format("IOException for request item {0}.", requestItemDto.getId()), e);
			}
		}

		return requestItemDto;
	}

	@Override
	@Transactional
	public void deleteInternal(IdmRequestItemDto dto) {

		if (dto.getId() != null) {
			// We try to find value in the confidential storage and delete it
			String storageKey = RequestManager.getConfidentialStorageKey(dto.getId());
			confidentialStorage.delete(dto, storageKey);
		}
		super.deleteInternal(dto);
		
		// We have to ensure the referential integrity, because some item (his DTOs) could be child of that item (DTO)
		if (dto.getId() != null && dto.getOwnerId() != null && RequestOperationType.ADD == dto.getOperation()) {
			if (dto.getRequest() != null) {
				IdmRequestItemFilter requestItemFilter = new IdmRequestItemFilter();
				requestItemFilter.setRequestId(dto.getRequest());
				// Find all items
				List<IdmRequestItemDto> items = this.find(requestItemFilter, null).getContent();
				
				// Create predicate - find all DTOs with that UUID value in any fields
				ImmutableList<RequestPredicate> predicates = ImmutableList
						.of(new RequestPredicate(dto.getOwnerId(), null));

				List<IdmRequestItemDto> itemsToDelete = items.stream() // Search items to delete
						.filter(item -> {
							try {
								@SuppressWarnings("unchecked")
								Class<? extends Requestable> ownerType = (Class<? extends Requestable>) Class
										.forName(item.getOwnerType());
								Requestable requestable;
								requestable = requestManager.convertItemToDto(item, ownerType);
								List<Requestable> filteredDtos = requestManager
										.filterDtosByPredicates(ImmutableList.of(requestable), ownerType, predicates);
								return filteredDtos.contains(requestable);
							} catch (ClassNotFoundException | IOException e) {
								throw new CoreException(e);
							}
						}).collect(Collectors.toList());
				itemsToDelete.forEach(item -> {
					this.delete(item);
				});
			}
		}
	}

	@Override
	@Transactional
	public IdmRequestItemDto cancel(IdmRequestItemDto dto) {
		cancelWF(dto);
		dto.setState(RequestState.CANCELED);
		dto.setResult(new OperationResultDto(OperationState.CANCELED));
		return this.save(dto);
	}

	@Override
	protected IdmRequestItem toEntity(IdmRequestItemDto dto, IdmRequestItem entity) {

		if (this.isNew(dto)) {
			dto.setResult(new OperationResultDto(OperationState.CREATED));
		}
		IdmRequestItem requestEntity = super.toEntity(dto, entity);

		return requestEntity;
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmRequestItem> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			IdmRequestItemFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		if (filter.getRequestId() != null) {
			predicates.add(
					builder.equal(root.get(IdmRequestItem_.request).get(IdmRequestItem_.id), filter.getRequestId()));
		}
		if (filter.getOwnerId() != null) {
			predicates.add(builder.equal(root.get(IdmRequestItem_.ownerId), filter.getOwnerId()));
		}
		if (filter.getOwnerType() != null) {
			predicates.add(builder.equal(root.get(IdmRequestItem_.ownerType), filter.getOwnerType()));
		}
		if (filter.getOperationType() != null) {
			predicates.add(builder.equal(root.get(IdmRequestItem_.operation), filter.getOperationType()));
		}

		return predicates;
	}

	/**
	 * Cancel unfinished workflow process for this automatic role.
	 *
	 * @param dto
	 */
	private void cancelWF(IdmRequestItemDto dto) {
		if (!Strings.isNullOrEmpty(dto.getWfProcessId())) {
			WorkflowFilterDto filter = new WorkflowFilterDto();
			filter.setProcessInstanceId(dto.getWfProcessId());

			Collection<WorkflowProcessInstanceDto> resources = workflowProcessInstanceService.find(filter, null)
					.getContent();
			if (resources.isEmpty()) {
				// Process with this ID not exist ... maybe was ended
				return;
			}

			workflowProcessInstanceService.delete(dto.getWfProcessId(), "Request item was canceled.");
		}
	}

}
