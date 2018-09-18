package eu.bcvsolutions.idm.core.model.service.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestFilterPredicate;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestAttributeValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemChangesDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto.Builder;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestItemFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.exception.AcceptedException;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.exception.RoleRequestException;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmRequestItemService;
import eu.bcvsolutions.idm.core.api.service.IdmRequestService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.api.utils.ExceptionUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.FormValueService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmRequestItem;
import eu.bcvsolutions.idm.core.model.event.RequestEvent;
import eu.bcvsolutions.idm.core.model.event.RequestEvent.RequestEventType;
import eu.bcvsolutions.idm.core.model.event.processor.role.RoleRequestApprovalProcessor;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Default implementation of universal request manager
 *
 * @author svandav
 */
@Service("requestManager")
public class DefaultRequestManager implements RequestManager {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultRequestManager.class);

	@Autowired
	private SecurityService securityService;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private WorkflowProcessInstanceService workflowProcessInstanceService;
	@Autowired
	private EntityEventManager entityEventManager;
	@Autowired
	private IdmRequestService requestService;
	@Autowired
	private IdmRequestItemService requestItemService;
	@Autowired
	private LookupService lookupService;
	@Autowired
	private FormService formService;
	@Autowired
	private IdmFormDefinitionService formDefinitionService;
	@Autowired
	private ConfidentialStorage confidentialStorage;
	@Autowired
	@Qualifier("objectMapper")
	private ObjectMapper mapper;
	private RequestManager requestManager;

	@Override
	@Transactional
	public IdmRequestDto startRequest(UUID requestId, boolean checkRight) {

		IdmRequestDto request = requestService.get(requestId);
		Assert.notNull(request, "Request is required!");

		try {
			RequestManager service = this.getRequestManager();
			if (!(service instanceof DefaultRequestManager)) {
				throw new CoreException("We expects instace of DefaultRequestManager!");
			}
			return ((DefaultRequestManager) service).startRequestNewTransactional(requestId, checkRight);
		} catch (Exception ex) {
			LOG.error(ex.getLocalizedMessage(), ex);
			request = requestService.get(requestId);
			Throwable exceptionToLog = ExceptionUtils.resolveException(ex);

			if (exceptionToLog instanceof ResultCodeException) {
				request.setResult( //
						new OperationResultDto //
								.Builder(OperationState.EXCEPTION) //
										.setException((ResultCodeException) exceptionToLog) //
										.build()); //
			} else {
				request.setResult( //
						new OperationResultDto //
								.Builder(OperationState.EXCEPTION) //
										.setCause(exceptionToLog) //
										.build()); //
			}
			request.setState(RequestState.EXCEPTION);
			return requestService.save(request);
		}
	}

	/**
	 * Internal start request. Start in new transaction
	 *
	 * @param requestId
	 * @param checkRight
	 *            - If is true, then will be check right for immediately execution
	 *            (if is requires)
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdmRequestDto startRequestNewTransactional(UUID requestId, boolean checkRight) {
		return (IdmRequestDto) this.getRequestManager().startRequestInternal(requestId, checkRight);
	}

	@Override
	@Transactional
	public IdmRequestDto startRequestInternal(UUID requestId, boolean checkRight) {
		LOG.debug("Start role request [{}]", requestId);
		Assert.notNull(requestId, "Role request ID is required!");
		// Load request ... check right for read
		IdmRequestDto request = requestService.get(requestId);
		Assert.notNull(request, "Role request DTO is required!");
		Assert.isTrue(
				RequestState.CONCEPT == request.getState() || RequestState.DUPLICATED == request.getState()
						|| RequestState.EXCEPTION == request.getState(),
				"Only role request with CONCEPT or EXCEPTION or DUPLICATED state can be started!");

		// Request will be set on in progress state
		request.setState(RequestState.IN_PROGRESS);
		request.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		IdmRequestDto savedRequest = requestService.save(request);

		// Throw event
		Map<String, Serializable> variables = new HashMap<>();
		variables.put(RoleRequestApprovalProcessor.CHECK_RIGHT_PROPERTY, checkRight);
		return entityEventManager.process(new RequestEvent(RequestEventType.EXECUTE, savedRequest, variables))
				.getContent();
	}

	@Override
	@Transactional
	public boolean startApprovalProcess(IdmRequestDto request, boolean checkRight, EntityEvent<IdmRequestDto> event,
			String wfDefinition) {
		// If is request marked as executed immediately, then we will check right
		// and do realization immediately (without start approval process)
		if (request.isExecuteImmediately()) {
			boolean haveRightExecuteImmediately = securityService.hasAnyAuthority(CoreGroupPermission.REQUEST_ADMIN);

			if (checkRight && !haveRightExecuteImmediately) {
				throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_NO_EXECUTE_IMMEDIATELY_RIGHT,
						ImmutableMap.of("new", request));
			}

			// Execute request immediately
			return true;
		} else {

			Map<String, Object> variables = new HashMap<>();
			// Minimize size of DTO persisting to WF
			IdmRequestDto eventRequest = event.getContent();
			eventRequest.setEmbedded(null);
			variables.put(EntityEvent.EVENT_PROPERTY, event);

			ProcessInstance processInstance = workflowProcessInstanceService.startProcess(wfDefinition,
					IdmRoleDto.class.getSimpleName(), request.getCreator(), request.getCreatorId().toString(),
					variables);
			// We have to refresh request (maybe was changed in wf process)
			request = requestService.get(request.getId());
			request.setWfProcessId(processInstance.getProcessInstanceId());
			requestService.save(request);
		}

		return false;
	}

	@Override
	@Transactional
	public IdmRequestDto executeRequest(UUID requestId) {
		// We can`t catch and log exception to request, because this transaction will be
		// marked as to rollback.
		// We can`t run this method in new transaction, because changes on request
		// (state modified in WF for example) is in uncommited transaction!
		return this.executeRequestInternal(requestId);
	}

	@Override
	@Transactional
	public void cancel(IdmRequestDto dto) {
		requestService.cancel(dto);
	}

	@Override
	@Transactional
	public <R extends Requestable> R post(Serializable requestId, R dto, BasePermission... permission) {
		ReadDtoService<R, ?> dtoReadService = getDtoService(dto);
		boolean isNew = dtoReadService.isNew(dto);
		// Check permissions
		dtoReadService.checkAccess(dto, permission);
		return this.post(requestId, dto, isNew);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public <R extends Requestable> R delete(Serializable requestId, R dto, BasePermission... permission) {
		Assert.notNull(dto, "DTO is required!");
		Assert.notNull(requestId, "Request ID is required!");

		IdmRequestDto request = requestService.get(requestId);
		Assert.notNull(request, "Request is required!");

		// Only request in CONCEPT or IN_PROGRESS state could creates new item or
		// update existing item
		if (request != null && !(RequestState.CONCEPT == request.getState()
				|| RequestState.IN_PROGRESS == request.getState() || RequestState.EXCEPTION == request.getState())) {
			throw new ResultCodeException(CoreResultCode.REQUEST_ITEM_CANNOT_BE_CREATED,
					ImmutableMap.of("dto", dto.toString(), "state", request.getState().name()));
		}
		// Exists item for same original owner?
		IdmRequestItemDto item = this.findRequestItem(request.getId(), dto);
		// If this item already exists for ADD/UPDATE/REMOVE, then we want to delete
		// him.
		if (item != null) {
			requestItemService.delete(item);
			return this.get(request.getId(), (UUID) dto.getId(), (Class<R>) dto.getClass(), permission);
		}

		// Check permissions on the target service
		ReadDtoService<R, ?> dtoReadService = getDtoService(dto);
		dtoReadService.checkAccess(dto, permission);

		if (item == null) {
			item = createRequestItem(request.getId(), dto);
		}
		item.setOwnerId((UUID) dto.getId());
		item.setOperation(RequestOperationType.REMOVE);
		item.setData(null);
		// Update or create new request item
		item = requestItemService.save(item);
		// Set ID of request item to result DTO
		dto.setRequestItem(item.getId());

		return get(request.getId(), dto);
	}

	@Override
	public <R extends Requestable> R get(UUID requestId, UUID dtoId, Class<R> dtoClass, BasePermission... permission) {
		Assert.notNull(dtoId, "DTO ID is required!");
		Assert.notNull(dtoClass, "DTO class is required!");
		Assert.notNull(requestId, "Request ID is required!");

		// Check permissions on the target service
		ReadDtoService<R, ?> dtoReadService = getDtoService(dtoClass);
		R dto = dtoReadService.get(dtoId, permission);
		if (dto == null) {
			try {
				dto = dtoClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CoreException(e);
			}
			dto.setId(dtoId);
		}

		return get(requestId, dto);
	}

	@Override
	public <R extends Requestable> Page<R> find(Class<? extends R> dtoClass, Serializable requestId, BaseFilter filter,
			Pageable pageable, IdmBasePermission... permission) {
		ReadDtoService<R, BaseFilter> dtoReadService = getDtoService(dtoClass);
		Page<R> originalPage = dtoReadService.find(filter, pageable, permission);
		List<R> originals = originalPage.getContent();
		List<R> results = new ArrayList<>();

		IdmRequestDto request = requestService.get(requestId);
		List<IdmRequestItemDto> items = this.findRequestItems(request.getId(), dtoClass);

		originals.stream().forEach(dto -> {
			IdmRequestItemDto item = items.stream() //
					.filter(i -> dto.getId().equals(i.getOwnerId())) //
					.findFirst() //
					.orElse(null); //
			if (item == null) {
				// None item found -> result is original DTO
				results.add(dto);
				return;
			}
			if (Strings.isNullOrEmpty(item.getData())) {
				// Item found, but does not contains any DTO. So original DTO will be result
				// (with connected item)
				addRequestItemToDto(dto, item);
				results.add(dto);
				return;
			}

			try {
				// Item with data found. Data in the request is result
				R requestedDto = this.convertItemToDto(item, dtoClass);
				addEmbedded((AbstractDto) requestedDto, request.getId());
				addRequestItemToDto((Requestable) requestedDto, item);
				results.add(requestedDto);
				return;

			} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| InstantiationException | IntrospectionException | ClassNotFoundException e) {
				throw new ResultCodeException(CoreResultCode.JSON_CANNOT_BE_CONVERT_TO_DTO,
						ImmutableMap.of("json", item.getData()));
			}
		});

		// !!Searching of added DTOs are very naive!!
		// We use all UUID value in the filter and try to find it in the DTOs. It means
		// only equals is implemented.

		// Find potential parents
		List<RequestPredicate> potencialParents = this.findPotencialParents(filter);

		results.addAll(this.findRelatedAddedItems(request, potencialParents, items, dtoClass));

		// Set all results as trimmed = true. FE expects trimmed value in the table.
		results.forEach(result -> ((AbstractDto) result).setTrimmed(true));

		return new PageImpl<>(results, pageable, originalPage.getTotalElements());
	}

	@Override
	@Transactional
	public <R extends Requestable> IdmRequestDto createRequest(R dto, BasePermission... permission) {
		Assert.notNull(dto, "DTO is required!");

		boolean createNew = false;
		if (dto.getId() == null) {
			dto.setId(UUID.randomUUID());
			createNew = true;
		}
		IdmRequestDto request = new IdmRequestDto();
		// I need set creator id here, because is checked in the SelfRequestEvaluator
		request.setCreatorId(securityService.getCurrentId());

		initRequest(request, dto);
		// Create request
		request = requestService.save(request, permission);
		// Create item
		if (createNew) {
			this.post(request.getId(), dto, permission);
		}

		return request;
	}

	@Override
	@Transactional
	public <R extends Requestable> IdmFormInstanceDto saveFormInstance(UUID requestId, R owner,
			IdmFormDefinitionDto formDefinition, List<IdmFormValueDto> newValues, BasePermission... permission) {

		IdmFormInstanceDto formInstance = new IdmFormInstanceDto(owner, formDefinition, newValues);

		// Load all current form values (includes requested items)
		IdmFormInstanceDto previousRequestFormInstance = getFormInstance(requestId, owner, formDefinition, permission);

		Map<UUID, Map<UUID, IdmFormValueDto>> previousValues = new HashMap<>(); // values by attributes
		previousRequestFormInstance.getValues().forEach(formValue -> {
			if (!previousValues.containsKey(formValue.getFormAttribute())) {
				previousValues.put(formValue.getFormAttribute(), new LinkedHashMap<>()); // sort by seq
			}
			previousValues.get(formValue.getFormAttribute()).put(formValue.getId(), formValue);
		});

		List<IdmFormValueDto> results = new ArrayList<>();
		for (Entry<String, List<IdmFormValueDto>> attributeEntry : formInstance.toValueMap().entrySet()) {
			IdmFormAttributeDto attribute = formInstance.getMappedAttributeByCode(attributeEntry.getKey());
			List<IdmFormValueDto> attributePreviousValues = new ArrayList<>();
			if (previousValues.containsKey(attribute.getId())) {
				attributePreviousValues.addAll(previousValues.get(attribute.getId()).values());
			}
			// Save attributes
			results.addAll(saveAttributeValues(requestId, owner, attribute, attributePreviousValues,
					attributeEntry.getValue(), permission));
		}

		return new IdmFormInstanceDto(owner, formDefinition, results);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R extends Requestable> IdmFormInstanceDto getFormInstance(UUID requestId, R owner,
			IdmFormDefinitionDto formDefinition, BasePermission... permission) {

		Assert.notNull(requestId, "ID of request is required!");
		Assert.notNull(owner, "Owner is required!");
		Assert.notNull(formDefinition, "Form definition is required!");

		boolean isNew = getDtoService(owner).isNew(owner);
		List<IdmFormValueDto> requestValues = new ArrayList<>();

		// If owner does not exists in the DB. We cannot call form service - exception
		// (owner does not exist) would be throw.
		if (!isNew) {
			IdmFormInstanceDto previousFormInstance = formService.getFormInstance(owner, formDefinition, permission);
			List<IdmFormValueDto> originalValues = previousFormInstance.getValues();

			originalValues.forEach(value -> {
				requestValues.add((IdmFormValueDto) this.get(requestId, (R) value));
			});
		}

		IdmRequestDto request = requestService.get(requestId);
		// Load all added items for that request
		List<IdmRequestItemDto> addedItems = this.findRequestItems(request.getId(), IdmFormValueDto.class,
				RequestOperationType.ADD);
		// Find added items for that owner ID
		List<R> relatedAddedItems = this.findRelatedAddedItems(request,
				ImmutableList.of(new RequestPredicate((UUID) owner.getId(), "ownerId")), addedItems,
				(Class<? extends R>) IdmFormValueDto.class);

		requestValues.addAll((Collection<? extends IdmFormValueDto>) relatedAddedItems);

		return new IdmFormInstanceDto(owner, formDefinition, requestValues);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IdmRequestItemChangesDto getChanges(IdmRequestItemDto item,
			BasePermission... permission) {
		LOG.debug(MessageFormat.format("Start read request item with changes [{0}].", item));
		Assert.notNull(item, "Idm request item cannot be null!");
		if (Strings.isNullOrEmpty(item.getOwnerType()) || item.getOwnerId() == null) {
			return null;
		}

		List<IdmRequestItemAttributeDto> resultAttributes = new ArrayList<>();
		Class<? extends Requestable> dtoClass;
		try {
			dtoClass = (Class<? extends Requestable>) Class.forName(item.getOwnerType());
		} catch (ClassNotFoundException e) {
			throw new CoreException(e);
		}
		ReadDtoService<?, ?> readService = getServiceByItem(item, dtoClass);

		Requestable currentDto = (Requestable) readService.get(item.getOwnerId(), permission);
		if (currentDto == null) {
			try {
				currentDto = (Requestable) dtoClass.newInstance();
				currentDto.setId(item.getOwnerId());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CoreException(e);
			}
		}
		Requestable changedDto = this.get(item.getRequest(), currentDto);
		Map<String, Object> currentFieldsValues = this.dtoToMap((AbstractDto) currentDto);
		Map<String, Object> changedFieldsValues = this.dtoToMap((AbstractDto) changedDto);

		// First add all new attributes
		changedFieldsValues.keySet().stream().forEach(changedAttribute -> {
			if (!currentFieldsValues.containsKey(changedAttribute)) {
				Object value = changedFieldsValues.get(changedAttribute);
				IdmRequestItemAttributeDto attribute = new IdmRequestItemAttributeDto(changedAttribute,
						value instanceof List, true);
				if (attribute.isMultivalue()) {
					if (value instanceof List) {
						((List<?>) value).forEach(v -> {
							attribute.getValues()
									.add(new IdmRequestAttributeValueDto(v, null, RequestOperationType.ADD));
						});
					}
				} else {
					attribute.setValue(new IdmRequestAttributeValueDto(value, null, RequestOperationType.ADD));
				}
				resultAttributes.add(attribute);
			}
		});

		// Second add all already exists attributes
		currentFieldsValues.keySet().forEach(currentAttribute -> {
			Object changedValue = changedFieldsValues.get(currentAttribute);
			IdmRequestItemAttributeDto attribute;
			Object currentValue = currentFieldsValues.get(currentAttribute);
			attribute = new IdmRequestItemAttributeDto(currentAttribute, changedValue instanceof List, false);

			if (attribute.isMultivalue()) {
				if (changedValue instanceof List) {
					((List<?>) changedValue).forEach(value -> {
						if (currentValue instanceof List && ((List<?>) currentValue).contains(value)) {
							attribute.getValues().add(new IdmRequestAttributeValueDto(value, value, null));
						} else {
							attribute.setChanged(true);
							attribute.getValues()
									.add(new IdmRequestAttributeValueDto(value, null, RequestOperationType.ADD));
						}
					});
				}
				if (currentValue instanceof List) {
					((List<?>) currentValue).forEach(value -> {
						if (changedValue == null || !((List<?>) changedValue).contains(value)) {
							attribute.setChanged(true);
							attribute.getValues()
									.add(new IdmRequestAttributeValueDto(value, value, RequestOperationType.REMOVE));
						}
					});
				}
			} else {
				if ((changedValue == null && currentValue == null)
						|| (changedValue != null && changedValue.equals(currentValue))
						|| (currentValue != null && currentValue.equals(changedValue))) {
					attribute.setChanged(RequestOperationType.UPDATE == item.getOperation() ? false : true);
					attribute.setValue(new IdmRequestAttributeValueDto(changedValue, currentValue,
							RequestOperationType.UPDATE == item.getOperation() ? null : item.getOperation()));
				} else {
					attribute.setChanged(true);
					attribute
							.setValue(new IdmRequestAttributeValueDto(changedValue, currentValue, item.getOperation()));
				}
			}
			resultAttributes.add(attribute);
		});

		// Make all values nicer
		resultAttributes.forEach(attribute -> {
			attribute.getValue().setValue(this.makeNiceValue(attribute.getValue().getValue()));
			attribute.getValue().setOldValue(this.makeNiceValue(attribute.getValue().getOldValue()));

			List<IdmRequestAttributeValueDto> attributeValues = attribute.getValues();
			attributeValues.forEach(attributeValue -> {
				attributeValue.setValue(this.makeNiceValue(attributeValue.getValue()));
				attributeValue.setOldValue(this.makeNiceValue(attributeValue.getOldValue()));
			});
		});

		IdmRequestItemChangesDto result = new IdmRequestItemChangesDto();
		result.setRequestItem(item);
		result.getAttributes().addAll(resultAttributes);

		LOG.debug(MessageFormat.format("End of reading the request item with changes [{0}].", item));
		return result;
	}

	@Override
	@Transactional
	public <R extends Requestable> void onDeleteRequestable(R requestable) {
		Assert.notNull(requestable, "Requestable DTO cannot be null!");

		// Search requests with that deleting owner
		IdmRequestFilter requestFilter = new IdmRequestFilter();
		requestFilter.setOwnerType(requestable.getClass().getName());
		requestFilter.setOwnerId((UUID) requestable.getId());
		List<IdmRequestDto> requests = requestService.find(requestFilter, null).getContent();
		requests.stream() //
				.filter(request -> RequestState.APPROVED != request.getState()) // We need filtered request which
																				// invoked that delete.
																				// Because we cannot cancel his workflow
																				// (throw exception).
				.forEach(request -> { //
					request = changeRequestState(requestable, request, //
							new ResultCodeException( //
									CoreResultCode.REQUEST_OWNER_WAS_DELETED, //
									ImmutableMap.of("owner", requestable.toString()) //
					));
					requestService.save(request);
				});

		// Search request items with that deleting owner
		IdmRequestItemFilter requestItemFilter = new IdmRequestItemFilter();
		requestItemFilter.setOwnerType(requestable.getClass().getName());
		requestItemFilter.setOwnerId((UUID) requestable.getId());
		List<IdmRequestItemDto> requestItems = requestItemService.find(requestItemFilter, null).getContent();
		requestItems.stream() //
				.filter(item -> RequestState.APPROVED != item.getState()) // We need filtered request which invoked that
																			// delete.
																			// Because we cannot cancel his workflow
																			// (throw exception).
				.forEach(item -> { //
					item = changeItemState(requestable, item, //
							new ResultCodeException( //
									CoreResultCode.REQUEST_OWNER_WAS_DELETED, //
									ImmutableMap.of("owner", requestable.toString()) //
					));
					requestItemService.save(item);

					IdmRequestItemFilter subItemFilter = new IdmRequestItemFilter();
					subItemFilter.setRequestId(item.getRequest());
					// Search all items for that request
					List<IdmRequestItemDto> subItems = requestItemService.find(subItemFilter, null).getContent();
					// TODO: This can be (maybe) removed ... because that 'cancel' is implemented
					// during realization of item

					// Check if items in same request does not contains same ID of deleting owner in
					// the DATA Json.
					// If yes, then state will be changed to cancel.
					subItems.stream() //
							.filter(subItem -> RequestState.APPROVED != subItem.getState()) // We need filtered request
																							// which invoked that
																							// delete.
																							// Because we cannot cancel
																							// his workflow (throw
																							// exception).
							.filter(subItem -> !requestable.getId().equals(subItem.getOwnerId())) //
							.filter(subItem -> subItem.getData() != null) //
							.filter(subItem -> subItem.getData() //
									.indexOf(requestable.getId().toString()) != -1) //
							.forEach(subItem -> { //
								subItem = changeItemState(requestable, subItem, //
										new ResultCodeException( //
												CoreResultCode.REQUEST_OWNER_FROM_OTHER_REQUEST_WAS_DELETED, //
												ImmutableMap.of("owner", requestable.toString(), "otherRequest",
														subItem.toString()) //
								));
								requestItemService.save(subItem);
							});

				}); //
	}

	@Override
	@Transactional(noRollbackFor = { AcceptedException.class })
	public <R extends Requestable> IdmRequestDto deleteRequestable(R dto, boolean executeImmediately) {
		Assert.notNull(dto);
		Assert.notNull(dto.getId(), "Requestable DTO cannot be null!");

		// Create and save request
		IdmRequestDto request = new IdmRequestDto();
		this.initRequest(request, dto);
		request.setExecuteImmediately(executeImmediately);
		request = requestService.save(request);
		// Create item
		this.delete(request.getId(), dto);

		// Start request
		return this.startRequestInternal(request.getId(), true);
	}

	@Override
	public <R extends Requestable> List<R> filterDtosByPredicates(List<R> requestables, Class<? extends R> dtoClass,
			List<RequestPredicate> predicates) {

		List<MethodDescriptor> descriptors;
		try {
			descriptors = Lists.newArrayList(Introspector.getBeanInfo(dtoClass).getMethodDescriptors()) //
					.stream() //
					.filter(methodDescriptor -> UUID.class.equals(methodDescriptor.getMethod().getReturnType())
							// Serializable too, because some UUID are in DTO as Serializable :-)
							|| Serializable.class.equals(methodDescriptor.getMethod().getReturnType())) //
					.filter(methodDescriptor -> methodDescriptor.getMethod().getParameterTypes() == null
							|| methodDescriptor.getMethod().getParameterTypes().length == 0) //
					.collect(Collectors.toList());
		} catch (IntrospectionException e) {
			throw new CoreException(e);
		} //

		return requestables.stream() // Find all DTOs with UUID fields when values are equals to values in filter
				.filter(requestable -> {
					return predicates.stream().allMatch(predicate -> {
						return descriptors.stream() //
								.filter(descriptor -> {
									if (predicate.getField() == null) {
										return true;
									}
									return getFieldName(descriptor.getName()).equals(predicate.getField());
								}) //
								.anyMatch(descriptor -> { //
									try {
										Object value = descriptor.getMethod().invoke(requestable, new Object[] {});
										if (value == null) {
											return false;
										}
										return value.equals(predicate.getValue());
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										throw new CoreException(e);
									}
								});
					});
				}).collect(Collectors.toList());
	}

	@Override
	public List<IdmRequestItemDto> findRequestItems(UUID requestId, Class<? extends Requestable> dtoClass) {
		return this.findRequestItems(requestId, dtoClass, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R extends Requestable> R convertItemToDto(IdmRequestItemDto item, Class<? extends R> type)
			throws IOException, ClassNotFoundException {
		R dto = convertStringToDto(item.getData(), type);
		if (dto instanceof IdmFormValueDto) {
			IdmFormValueDto formValueDto = (IdmFormValueDto) dto;
			formValueDto.setOwnerType((Class<? extends FormableEntity>) Class.forName(item.getSuperOwnerType()));
			formValueDto.setOwnerId(item.getSuperOwnerId());
		}
		return dto;
	}

	@SuppressWarnings("unchecked")
	private IdmRequestDto executeRequestInternal(UUID requestId) {
		Assert.notNull(requestId, "Role request ID is required!");
		IdmRequestDto request = requestService.get(requestId);
		Assert.notNull(request, "Role request is required!");

		// Validate request
		List<IdmRequestItemDto> items = this.findRequestItems(request.getId(), null);
		if (items.isEmpty()) {
			throw new ResultCodeException(CoreResultCode.REQUEST_CANNOT_BE_EXECUTED_NONE_ITEMS,
					ImmutableMap.of("request", request.toString()));
		}
		List<IdmRequestItemDto> sortedItems = items.stream().sorted(Comparator.comparing(IdmRequestItemDto::getCreated))
				.collect(Collectors.toList());

		// Validate items
		sortedItems.stream() //
				.filter(item -> !item.getState().isTerminatedState()) //
				.filter(item -> !(RequestState.CONCEPT == item.getState() || RequestState.APPROVED == item.getState())) //
				.forEach(item -> { //
					throw new ResultCodeException(CoreResultCode.REQUEST_ITEM_CANNOT_BE_EXECUTED,
							ImmutableMap.of("item", item.toString(), "state", item.getState()));
				});

		sortedItems.stream() //
				.filter(item -> RequestOperationType.ADD == item.getOperation()
						|| RequestOperationType.UPDATE == item.getOperation()) //
				.forEach(item -> { //
					// Get DTO service
					Requestable dto = null;
					try {
						Class<? extends Requestable> dtoClass = (Class<? extends Requestable>) Class.forName(item.getOwnerType());
						ReadWriteDtoService<Requestable, BaseFilter> dtoService = (ReadWriteDtoService<Requestable, BaseFilter>) getServiceByItem(
								item, dtoClass);
						dto = this.convertItemToDto(item, dtoClass);
						dtoService.validateDto((Requestable) dto);
					} catch (Exception e) {
						throw new RoleRequestException(CoreResultCode.REQUEST_ITEM_IS_NOT_VALID,
								ImmutableMap.of("dto", dto != null ? dto.toString() : null, "item", item.toString()),
								e);
					}
				});

		// We have to ensure the referential integrity, because some items (his DTOs)
		// could be child of terminated item (DTO)
		sortedItems.stream() //
				.filter(item -> item.getState().isTerminatedState()) // We check terminated ADDed items
				.filter(item -> RequestOperationType.ADD == item.getOperation()) //
				.filter(item -> item.getOwnerId() != null) //
				.forEach(terminatedItem -> {
					// Create predicate - find all DTOs with that UUID value in any fields
					ImmutableList<RequestPredicate> predicates = ImmutableList
							.of(new RequestPredicate(terminatedItem.getOwnerId(), null));
					sortedItems.stream() //
							.filter(item -> !item.getState().isTerminatedState()) //
							.filter(item -> { // Is that item child of terminated item?
								try {
									Class<? extends Requestable> ownerType = (Class<? extends Requestable>) Class
											.forName(item.getOwnerType());
									Requestable requestable = requestManager.convertItemToDto(item, ownerType);
									List<Requestable> filteredDtos = requestManager.filterDtosByPredicates(
											ImmutableList.of(requestable), ownerType, predicates);
									return filteredDtos.contains(requestable);
								} catch (ClassNotFoundException | IOException e) {
									throw new CoreException(e);
								}
							}).forEach(itemToCancel -> { // This item could be not executed, because is use in other
															// already terminated (added) item.
								itemToCancel.setState(RequestState.CANCELED);
								itemToCancel.setResult(new OperationResultDto.Builder(OperationState.NOT_EXECUTED)
										.setException(new RoleRequestException(
												CoreResultCode.REQUEST_ITEM_NOT_EXECUTED_PARENT_CANCELED,
												ImmutableMap.of("item", itemToCancel.toString(), "terminatedItem",
														terminatedItem.toString())))
										.build());
								requestItemService.save(itemToCancel);
							});
				});

		// Reload items ... could be changed
		items = this.findRequestItems(request.getId(), null);
		List<IdmRequestItemDto> sortedItemsResult = items.stream()
				.sorted(Comparator.comparing(IdmRequestItemDto::getCreated)).collect(Collectors.toList());

		sortedItemsResult.stream() //
				.filter(item -> !item.getState().isTerminatedState()) //
				.forEach(item -> {
					try {
						this.resolveItem(item);
					} catch (ClassNotFoundException | IOException e) {
						throw new CoreException(e);
					}
				});

		request.setState(RequestState.EXECUTED);
		request.setResult(new OperationResultDto.Builder(OperationState.EXECUTED).build());
		return requestService.save(request);
	}

	/**
	 * Change item state
	 * 
	 * @param requestable
	 * @param item
	 * @param ex
	 * @return
	 */
	private <R extends Requestable> IdmRequestItemDto changeItemState(R requestable, IdmRequestItemDto item,
			ResultCodeException ex) {
		if (item.getState().isTerminatedState()) {
			// If is item in the terminated state, then we only add result code exception,
			// but don't modify the result state
			item.setResult(new Builder(item.getResult().getState()) //
					.setException(ex) //
					.build()); //
		} else {
			item = requestItemService.cancel(item);
			item.setResult(new Builder(OperationState.CANCELED) //
					.setException(ex) //
					.build()); //
		}
		return item;
	}

	/**
	 * Change request state
	 * 
	 * @param requestable
	 * @param request
	 * @param ex
	 * @return
	 */
	private <R extends Requestable> IdmRequestDto changeRequestState(R requestable, IdmRequestDto request,
			ResultCodeException ex) {
		if (request.getState().isTerminatedState()) {
			// If is request in the terminated state, then we only add result code
			// exception,
			// but don't modify the result state
			request.setResult(new Builder(request.getResult().getState()) //
					.setException(ex) //
					.build()); //
		} else {
			request = requestService.cancel(request);
			request.setResult(new Builder(OperationState.CANCELED) //
					.setException(ex) //
					.build()); //
		}
		if (requestable instanceof Codeable && ((Codeable) requestable).getCode() != null) {
			request.setName(((Codeable) requestable).getCode());
		}
		return request;
	}

	private <R extends Requestable> R get(UUID requestId, R dto) {
		Assert.notNull(dto, "DTO is required!");
		Assert.notNull(requestId, "Request ID is required!");
		// Exists item for same original owner?
		IdmRequestItemDto item = this.findRequestItem(requestId, dto);

		if (item == null || dto == null) {
			return dto;
		} else if (RequestOperationType.REMOVE == item.getOperation()) {
			addRequestItemToDto(dto, item);
			return dto;
		}

		try {
			@SuppressWarnings("unchecked")
			R requestedDto = this.convertItemToDto(item, (Class<? extends R>) dto.getClass());
			addRequestItemToDto((R) requestedDto, item);
			this.addEmbedded((AbstractDto) requestedDto, requestId);
			return (R) requestedDto;

		} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| IntrospectionException | InstantiationException | ClassNotFoundException e) {
			throw new ResultCodeException(CoreResultCode.JSON_CANNOT_BE_CONVERT_TO_DTO,
					ImmutableMap.of("json", item.getData()), e);
		}
	}

	private void resolveItem(IdmRequestItemDto item)
			throws ClassNotFoundException, IOException {
		Assert.notNull(item, "Item is mandatory!");

		RequestOperationType type = item.getOperation();
		// Get DTO service
		@SuppressWarnings("unchecked")
		Class<? extends Requestable> dtoClass = (Class<? extends Requestable>) Class.forName(item.getOwnerType());
		// Get service
		@SuppressWarnings("unchecked")
		ReadWriteDtoService<Requestable, BaseFilter> dtoService = (ReadWriteDtoService<Requestable, BaseFilter>) this.getServiceByItem(item,
				dtoClass);

		// Create or Update DTO
		if (RequestOperationType.ADD == type || RequestOperationType.UPDATE == type) {
			Requestable dto = this.convertItemToDto(item, dtoClass);
			// If is DTO form value and confidential, then we need to load value form
			// confidential storage
			if (dto instanceof IdmFormValueDto) {
				IdmFormValueDto formValueDto = (IdmFormValueDto) dto;
				if (formValueDto.isConfidential()) {
					formValueDto.setValue(this.getConfidentialPersistentValue(item));
				}
			}
			// Save without check a permissions
			dto = dtoService.save(dto);
			item.setResult(new OperationResultDto(OperationState.EXECUTED));
			item.setState(RequestState.EXECUTED);
			requestItemService.save(item);
			return;
		}
		// Delete DTO
		if (RequestOperationType.REMOVE == type) {
			Assert.notNull(item.getOwnerId(), "Id in item is required for delete!");

			Requestable dtoToDelete = dtoService.get(item.getOwnerId());
			if (dtoToDelete == null) {
				item.setResult(new OperationResultDto //
						.Builder(OperationState.NOT_EXECUTED) //
								.setException(new ResultCodeException(CoreResultCode.NOT_FOUND,
										ImmutableMap.of("entity", item.getOriginalCreatorId()))) //
								.build()); //
				requestItemService.save(item);
				return;
			}
			// Delete without check a permissions
			dtoService.deleteById(dtoToDelete.getId());
			item.setResult(new OperationResultDto(OperationState.EXECUTED));
			item.setState(RequestState.EXECUTED);
			requestItemService.save(item);
			return;
		}

	}

	private <R extends Requestable> R post(Serializable requestId, R dto, boolean isNew) {
		Assert.notNull(dto, "DTO is required!");
		Assert.notNull(requestId, "Request ID is required!");

		IdmRequestDto request = requestService.get(requestId);
		Assert.notNull(request, "Request is required!");

		// Only request in CONCEPT or IN_PROGRESS state could creates new item or
		// update existing item
		if (request != null && !(RequestState.CONCEPT == request.getState()
				|| RequestState.IN_PROGRESS == request.getState() || RequestState.EXCEPTION == request.getState())) {
			throw new ResultCodeException(CoreResultCode.REQUEST_ITEM_CANNOT_BE_CREATED,
					ImmutableMap.of("dto", dto.toString(), "state", request.getState().name()));
		}

		// Exists item for same original owner?
		IdmRequestItemDto item = this.findRequestItem(request.getId(), dto);
		try {
			if (dto.getId() == null) {
				dto.setId(UUID.randomUUID());
			}
			if (item == null) {
				item = createRequestItem(request.getId(), dto);
				item.setOperation(isNew ? RequestOperationType.ADD : RequestOperationType.UPDATE);
				item.setOwnerId((UUID) dto.getId());
			} else {
				item.setOperation(isNew ? RequestOperationType.ADD : RequestOperationType.UPDATE);
			}

			String dtoString = this.convertDtoToString(dto);
			item.setData(dtoString);
			// Update or create new request item
			item = requestItemService.save(item);
			return this.get(request.getId(), dto);

		} catch (JsonProcessingException e) {
			throw new ResultCodeException(CoreResultCode.DTO_CANNOT_BE_CONVERT_TO_JSON,
					ImmutableMap.of("dto", dto.toString()), e);
		}
	}

	/**
	 * Init new request (without save)
	 * 
	 * @param request
	 * @param dto
	 */
	private <R extends Requestable> void initRequest(IdmRequestDto request, R dto) {
		request.setState(RequestState.CONCEPT);
		request.setOwnerId((UUID) dto.getId());
		request.setOwnerType(dto.getClass().getName());
		request.setExecuteImmediately(false);
		request.setRequestType(dto.getClass().getSimpleName());
		request.setResult(new OperationResultDto(OperationState.CREATED));
		if (dto instanceof Codeable && ((Codeable) dto).getCode() != null) {
			request.setName(((Codeable) dto).getCode());
		}
	}

	private Object makeNiceValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Codeable) {
			Codeable codeable = (Codeable) value;
			return codeable.getCode();
		}
		if (value instanceof Identifiable) {
			Identifiable identifiable = (Identifiable) value;
			return identifiable.getId();
		}
		if (value instanceof ConfigurationMap) {
			ConfigurationMap configurationMap = (ConfigurationMap) value;
			Map<String, Serializable> map = configurationMap.toMap();
			return map.toString();

		}
		return value;
	}

	private Map<String, Object> dtoToMap(AbstractDto dto) {
		Map<String, Object> results = new HashMap<>();
		if (dto == null) {
			return results;
		}
		try {
			List<PropertyDescriptor> descriptors = Lists
					.newArrayList(Introspector.getBeanInfo(dto.getClass()).getPropertyDescriptors());
			List<Field> fields = Lists.newArrayList(dto.getClass().getDeclaredFields()) //
					.stream() //
					.filter(field -> !Requestable.REQUEST_ITEM_FIELD.equals(field.getName())) //
					.filter(field -> !Requestable.REQUEST_FIELD.equals(field.getName())) //
					.filter(field -> !field.isAnnotationPresent(JsonIgnore.class)) //
					.collect(Collectors.toList()); //
			// Embedded objects
			fields.stream() //
					.filter(field -> field.isAnnotationPresent(Embedded.class)) //
					.forEach(field -> {
						results.put(field.getName(), dto.getEmbedded().get(field.getName()));
					});

			// Others objects
			fields.stream() //
					.filter(field -> !field.isAnnotationPresent(Embedded.class)) //
					.forEach(field -> {
						try {
							PropertyDescriptor fieldDescriptor = descriptors.stream() //
									.filter(descriptor -> field.getName().equals(descriptor.getName())) //
									.findFirst() //
									.orElse(null); //
							if (fieldDescriptor != null) {
								Object value = fieldDescriptor.getReadMethod().invoke(dto);
								results.put(field.getName(), value);
							}
						} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
							throw new CoreException(e);
						}
					});
		} catch (IntrospectionException e) {
			throw new CoreException(e);
		}

		return results;
	}

	/**
	 * Find related added DTOs by given parents. !!Searching of added DTOs are very
	 * naive!! We use all UUID value in the filter and try to find it in the DTOs.
	 * It means only equals is implemented.
	 * 
	 * @param request
	 * @param predicates
	 * @param items
	 * @param dtoClass
	 * @return
	 */
	private <R extends Requestable> List<R> findRelatedAddedItems(IdmRequestDto request,
			List<RequestPredicate> predicates, List<IdmRequestItemDto> items, Class<? extends R> dtoClass) {
		List<R> requestables = new ArrayList<>();

		items.stream() //
				.filter(i -> RequestOperationType.ADD == i.getOperation()) //
				.forEach(item -> { //
					try {
						R requestedDto = this.convertItemToDto(item, dtoClass);
						AbstractDto requested = (AbstractDto) requestedDto;
						addEmbedded(requested, request.getId());
						addRequestItemToDto((Requestable) requested, item);
						requestables.add((R) requestedDto);
						return;

					} catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
							| IntrospectionException | InstantiationException | ClassNotFoundException e) {
						throw new ResultCodeException(CoreResultCode.JSON_CANNOT_BE_CONVERT_TO_DTO,
								ImmutableMap.of("json", item.getData()), e);
					}
				});

		return filterDtosByPredicates(requestables, dtoClass, predicates);
	}

	private String getFieldName(String methodName) {
		// Assume the method starts with either get or is.
		return Introspector.decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
	}

	/**
	 * Find potential parents. Invokes all method with UUID return type and without
	 * input parameters.
	 * 
	 * @param filter
	 * @return
	 */
	private List<RequestPredicate> findPotencialParents(BaseFilter filter) {
		Assert.notNull(filter, "Filter is mandatory!");

		List<MethodDescriptor> descriptors;
		try {
			descriptors = Lists.newArrayList(Introspector.getBeanInfo(filter.getClass()).getMethodDescriptors()) //
					.stream() //
					.filter(methodDescriptor -> UUID.class.equals(methodDescriptor.getMethod().getReturnType())) //
					.filter(methodDescriptor -> methodDescriptor.getMethod().getParameterTypes() == null
							|| methodDescriptor.getMethod().getParameterTypes().length == 0) //
					.collect(Collectors.toList());
		} catch (IntrospectionException e) {
			throw new CoreException(e);
		} //

		List<RequestPredicate> results = new ArrayList<>();
		descriptors.stream().forEach(descriptor -> {
			try {
				Object value = descriptor.getMethod().invoke(filter, new Object[] {});
				if (value == null) {
					return;
				}
				RequestFilterPredicate filterPredicate = descriptor.getMethod()
						.getAnnotation(RequestFilterPredicate.class);

				results.add(
						new RequestPredicate((UUID) value, filterPredicate != null ? filterPredicate.field() : null));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new CoreException(e);
			}
		});
		return results;
	}

	private String convertDtoToString(BaseDto dto) throws JsonProcessingException {
		return mapper.writerFor(dto.getClass()).writeValueAsString(dto);
	}

	private <R extends Requestable> R convertStringToDto(String data, Class<? extends R> type) throws IOException {
		if (Strings.isNullOrEmpty(data)) {
			return null;
		}
		return mapper.readValue(data, type);
	}

	@SuppressWarnings("unchecked")
	private <R extends Requestable> IdmRequestItemDto findRequestItem(UUID requestId, R dto) {
		Assert.notNull(dto, "DTO is required!");
		if (dto.getId() == null) {
			return null;
		}
		return findRequestItem(requestId, (UUID) dto.getId(), (Class<R>) dto.getClass());
	}

	private <R extends Requestable> IdmRequestItemDto findRequestItem(UUID requestId, UUID dtoId,
			Class<? extends R> dtoClass) {
		Assert.notNull(dtoClass, "DTO class is required!");
		if (dtoId == null) {
			return null;
		}
		IdmRequestItemFilter itemFilter = new IdmRequestItemFilter();
		itemFilter.setRequestId(requestId);
		itemFilter.setOwnerId(dtoId);
		itemFilter.setOwnerType(dtoClass.getName());
		List<IdmRequestItemDto> items = requestItemService.find(itemFilter, null).getContent();
		if (items.size() > 0) {
			return items.get(0);
		}
		return null;
	}

	private List<IdmRequestItemDto> findRequestItems(UUID requestId, Class<? extends Requestable> dtoClass,
			RequestOperationType operation) {
		IdmRequestItemFilter itemFilter = new IdmRequestItemFilter();
		itemFilter.setRequestId(requestId);
		itemFilter.setOwnerType(dtoClass != null ? dtoClass.getName() : null);
		itemFilter.setOperationType(operation);

		return requestItemService.find(itemFilter, null).getContent();
	}

	private IdmRequestItemDto createRequestItem(UUID requestId, Requestable dto) {
		IdmRequestItemDto item = new IdmRequestItemDto();
		if (dto instanceof IdmFormValueDto) {
			IdmFormValueDto formValueDto = (IdmFormValueDto) dto;
			item.setSuperOwnerType(formValueDto.getOwnerType().getName());
			item.setSuperOwnerId((UUID) formValueDto.getOwnerId());
		}
		item.setRequest(requestId);
		item.setOwnerType(dto.getClass().getName());
		item.setResult(new OperationResultDto(OperationState.CREATED));
		return item;
	}

	/**
	 * Add request item to DTO and to embedded map
	 *
	 * @param dto
	 * @param item
	 */
	private void addRequestItemToDto(Requestable dto, IdmRequestItemDto item) {
		dto.setRequestItem(item.getId());
		if (dto instanceof AbstractDto) {
			((AbstractDto) dto).getEmbedded().put(Requestable.REQUEST_ITEM_FIELD, item);
		}
	}

	/**
	 * Get read form value service for given class
	 *
	 * @param dto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private FormValueService<FormableEntity> getFormValueService(Class<? extends FormableEntity> formableEntityClass) {
		return formService.getFormValueService((Class<FormableEntity>) formableEntityClass);
	}

	/**
	 * Get read DTO service for given DTO
	 *
	 * @param dto
	 * @return
	 */
	private <R extends Requestable> ReadDtoService<R, BaseFilter> getDtoService(R dto) {
		@SuppressWarnings("unchecked")
		Class<? extends R> dtoClass = (Class<? extends R>) dto.getClass();
		return this.getDtoService(dtoClass);
	}

	/**
	 * Get read DTO service for given DTO class
	 *
	 * @param dto
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <R extends Requestable> ReadDtoService<R, BaseFilter> getDtoService(Class<? extends R> dtoClass) {
		return (ReadDtoService<R, BaseFilter>) lookupService.getDtoService(dtoClass);
	}

	/**
	 * Get service by give item. If item solving form-value, then form value service
	 * is returned
	 * 
	 * @param item
	 * @param dtoClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <R extends Requestable> ReadDtoService<?, ?> getServiceByItem(IdmRequestItemDto item,
			Class<? extends R> dtoClass) {
		ReadDtoService<?, ?> readService = null;
		if (IdmFormValueDto.class.getName().equals(item.getOwnerType())) {
			// EAV value .. we need find form value service by super owner type
			String superOwnerType = item.getSuperOwnerType();
			Assert.notNull(superOwnerType, "Super owner type is mandatory for EAV value!");
			try {
				readService = this.getFormValueService((Class<? extends FormableEntity>) Class.forName(superOwnerType));
			} catch (ClassNotFoundException e) {
				throw new CoreException(e);
			}
		} else {
			// Standard DTO service
			readService = this.getDtoService(dtoClass);
		}
		return readService;
	}

	/**
	 * Loads and adds DTOs by embedded annotation
	 * 
	 * @param dto
	 * @param requestId
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws IntrospectionException
	 * @throws InstantiationException
	 */
	private void addEmbedded(AbstractDto dto, UUID requestId) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IntrospectionException, InstantiationException {
		Assert.notNull(dto, "DTO is required!");

		Field[] fields = dto.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(Embedded.class)) {
				Embedded embeddedAnnotation = field.getAnnotation(Embedded.class);
				if (embeddedAnnotation.enabled()) {
					// If DTO class is abstract then continue
					if (Modifier.isAbstract(embeddedAnnotation.dtoClass().getModifiers())) {
						continue;
					}
					Object value = EntityUtils.getEntityValue(dto, field.getName());
					if (value instanceof UUID) {
						// Create mock instance of embedded DTO only with ID
						UUID id = (UUID) value;
						AbstractDto embeddedDto = null;
						if (Requestable.class.isAssignableFrom(embeddedAnnotation.dtoClass())) {
							embeddedDto = embeddedAnnotation.dtoClass().newInstance();
							embeddedDto.setId(id);
							Requestable originalEmbeddedDto = this.getDtoService((Requestable) embeddedDto).get(embeddedDto.getId());
							if (originalEmbeddedDto != null) {
								// Call standard method for load request's DTO with original DTO
								embeddedDto = (AbstractDto) this.get(requestId, originalEmbeddedDto);
							} else {
								// Call standard method for load request's DTO with mock DTO (only with ID)
								embeddedDto = (AbstractDto) this.get(requestId, (Requestable) embeddedDto);
							}
						} else {
							// If embedded DTO is not Requestable, then standard service is using
							embeddedDto = (AbstractDto) lookupService.getDtoService(embeddedAnnotation.dtoClass())
									.get(id);
						}
						if (embeddedDto == null) {
							continue;
						}
						embeddedDto.setTrimmed(true);
						dto.getEmbedded().put(field.getName(), embeddedDto);
					}
				}
			}
		}
	}

	/**
	 * Save single attribute values
	 * 
	 * @param owner
	 * @param attribute
	 * @param previousValues
	 * @param newValues
	 * @param permission
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <R extends Requestable> List<IdmFormValueDto> saveAttributeValues(UUID requestId, R owner,
			IdmFormAttributeDto attribute, List<IdmFormValueDto> previousValues, List<IdmFormValueDto> newValues,
			BasePermission... permission) {

		ReadDtoService<R, ?> dtoReadService = getDtoService(owner);
		Class<? extends BaseEntity> entityClass = dtoReadService.getEntityClass();
		if (!FormableEntity.class.isAssignableFrom(entityClass)) {
			throw new IllegalArgumentException(MessageFormat.format("Form owner [{0}] is not FormableEntity [{1}]!",
					owner.toString(), entityClass));
		}
		Class<? extends FormableEntity> ownerClass = (Class<? extends FormableEntity>) entityClass;

		IdmFormDefinitionDto formDefinition = formDefinitionService.get(attribute.getFormDefinition());

		List<IdmFormValueDto> results = new ArrayList<>();
		Map<UUID, IdmFormValueDto> unprocessedPreviousValues = new LinkedHashMap<>(); // ordered by seq
		if (CollectionUtils.isNotEmpty(previousValues)) {
			previousValues.forEach(previousValue -> {
				// Set owner to the form value
				previousValue.setOwnerAndAttribute(null, attribute);
				previousValue.setOwnerId(owner.getId());
				previousValue.setOwnerType(ownerClass);

				unprocessedPreviousValues.put(previousValue.getId(), previousValue);
			});
		}
		//
		if (newValues == null || newValues.isEmpty()) {
			// confidential values has to removed directly, they could not be sent with form
			// (only changed values)
			if (!attribute.isConfidential()) {
				// delete previous attributes
				unprocessedPreviousValues.values().forEach(value -> {
					results.add((IdmFormValueDto) this.delete(requestId, (R) value));
				});
			}
			return results;
		}
		//
		if (!attribute.isMultiple() && newValues.size() > 1) {
			throw new IllegalArgumentException(
					MessageFormat.format("Form attribute [{0}:{1}] does not support multivalue, sent [{2}] values.",
							formDefinition.getCode(), attribute.getCode(), newValues.size()));
		}
		//
		// compare values
		IdmFormValueDto[] sortedPreviousValues = formService.resolvePreviousValues(unprocessedPreviousValues,
				newValues);
		for (short index = 0; index < newValues.size(); index++) {
			IdmFormValueDto previousValue = sortedPreviousValues[index];
			IdmFormValueDto newValue = newValues.get(index);
			newValue.setOwnerAndAttribute(null, attribute);
			newValue.setOwnerId(owner.getId());
			newValue.setOwnerType(ownerClass);
			newValue.setSeq(index);
			//
			if (previousValue == null) {
				if (!newValue.isNull()) { // null values are not saved
					if (newValue.isConfidential()) {
						// Confidential value has to be persisted in the confidential storage
						results.add(saveConfidentialEavValue(requestId, newValue));
					} else {
						results.add((IdmFormValueDto) this.post(requestId, (R) newValue,
								this.isFormValueNew(previousValue)));
					}
				}
			} else {
				//
				// We using filled value only and set her into previous value => value id is
				// preserved
				// the same value should not be updated
				// confidential value is always updated - only new values are sent from client
				if (newValue.isConfidential() || !previousValue.isEquals(newValue)) {
					// set value for the previous value
					previousValue.setValue(newValue.getValue());
					// attribute persistent type could be changed
					previousValue.setOwnerAndAttribute(null, attribute);
					previousValue.setOwnerId(owner.getId());
					previousValue.setOwnerType(ownerClass);
					previousValue.setSeq(index);
					if (!previousValue.isNull()) { // null values are not saved

						if (previousValue.isConfidential()) {
							// Confidential value has to be persisted in the confidential storage
							results.add(saveConfidentialEavValue(requestId, previousValue));
						} else {
							results.add((IdmFormValueDto) this.post(requestId, (R) previousValue,
									this.isFormValueNew(previousValue)));
						}
					} else {
						results.add((IdmFormValueDto) this.delete(requestId, (R) previousValue));
					}
				}
			}
		}
		// remove unprocessed values
		// confidential property will be removed too => none or all confidential values
		// have to be given for multiple attributes
		unprocessedPreviousValues.values().forEach(previousValue -> {
			results.add((IdmFormValueDto) this.delete(requestId, (R) previousValue));
		});

		return results;
	}

	/**
	 * Save confidential FormValueDto. Value is persists to the confidential
	 * storage. DTO persisted in the request item contains 'asterixed' value only.
	 * 
	 * @param requestId
	 * @param confidentialFormValue
	 * @return
	 */
	private IdmFormValueDto saveConfidentialEavValue(UUID requestId,
			IdmFormValueDto confidentialFormValue) {
		// check, if value has to be persisted in confidential storage
		Serializable confidentialValue = confidentialFormValue.getValue();
		if (confidentialFormValue.isConfidential()) {
			confidentialFormValue.clearValues();
			if (confidentialValue != null) {
				// we need only to know, if value was filled
				confidentialFormValue.setStringValue(GuardedString.SECRED_PROXY_STRING);
				confidentialFormValue.setShortTextValue(GuardedString.SECRED_PROXY_STRING);
			}
		}
		Assert.notNull(confidentialFormValue);
		// Save DTO without confidential value
		Requestable persistedRequestDto = this.post(requestId, (Requestable) confidentialFormValue,
				this.isFormValueNew(confidentialFormValue));
		UUID requestItem = persistedRequestDto.getRequestItem();
		Assert.notNull(requestItem);

		// Save confidential value to ConfidentialStorage - owner is request item
		confidentialStorage.save(requestItem, IdmRequestItem.class,
				RequestManager.getConfidentialStorageKey(requestItem), confidentialValue);
		LOG.debug("Confidential FormValue [{}]  is persisted in RequestItem [{}] and value in the confidential storage",
				confidentialFormValue.getId(), requestItem);

		return (IdmFormValueDto) persistedRequestDto;
	}

	/**
	 * Check if given FormValue is new.
	 * 
	 * @param formValue
	 * @return
	 */
	private boolean isFormValueNew(IdmFormValueDto formValue) {
		if (formValue == null) {
			return true;
		}
		if (formValue.getId() == null) {
			return true;
		}
		if (formValue.getRequestItem() != null) {
			IdmRequestItemDto requestItemDto = DtoUtils.getEmbedded(formValue, Requestable.REQUEST_ITEM_FIELD,
					IdmRequestItemDto.class, null);
			if (requestItemDto == null) {
				requestItemDto = requestItemService.get(formValue.getRequestItem());
			}
			if (requestItemDto != null && RequestOperationType.ADD == requestItemDto.getOperation()) {
				return true;
			}
		}
		return false;
	}

	private RequestManager getRequestManager() {
		if (this.requestManager == null) {
			this.requestManager = applicationContext.getBean(RequestManager.class);
		}
		return this.requestManager;
	}

	private Serializable getConfidentialPersistentValue(IdmRequestItemDto confidentialItem) {
		Assert.notNull(confidentialItem);
		//
		return confidentialStorage.get(confidentialItem.getId(), IdmRequestItem.class,
				RequestManager.getConfidentialStorageKey(confidentialItem.getId()));
	}

}
