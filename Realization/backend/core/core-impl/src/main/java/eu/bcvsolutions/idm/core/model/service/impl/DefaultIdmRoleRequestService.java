package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.ConceptRoleRequestOperation;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Loggable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestState;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestedByType;
import eu.bcvsolutions.idm.core.api.dto.IdmConceptRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleRequestFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.RoleRequestException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.ExceptionUtils;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest_;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent.IdentityRoleEventType;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent.RoleRequestEventType;
import eu.bcvsolutions.idm.core.model.event.processor.role.RoleRequestApprovalProcessor;
import eu.bcvsolutions.idm.core.model.repository.IdmRoleRequestRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowHistoricProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowHistoricProcessInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Default implementation of role request service
 * 
 * @author svandav
 *
 */
@Service("roleRequestService")
public class DefaultIdmRoleRequestService
		extends AbstractReadWriteDtoService<IdmRoleRequestDto, IdmRoleRequest, IdmRoleRequestFilter>
		implements IdmRoleRequestService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmRoleRequestService.class);
	private final IdmRoleRequestRepository repository;
	private final IdmConceptRoleRequestService conceptRoleRequestService;
	private final IdmIdentityRoleService identityRoleService;
	private final IdmIdentityService identityService;
	private final ObjectMapper objectMapper;
	private final SecurityService securityService;
	private final ApplicationContext applicationContext;
	private final WorkflowProcessInstanceService workflowProcessInstanceService;
	private final EntityEventManager entityEventManager;
	private IdmRoleRequestService roleRequestService;
	//
	@Autowired private WorkflowHistoricProcessInstanceService workflowHistoricProcessInstanceService; 

	@Autowired
	public DefaultIdmRoleRequestService(IdmRoleRequestRepository repository,
			IdmConceptRoleRequestService conceptRoleRequestService, IdmIdentityRoleService identityRoleService,
			IdmIdentityService identityService, ObjectMapper objectMapper, SecurityService securityService,
			ApplicationContext applicationContext, WorkflowProcessInstanceService workflowProcessInstanceService,
			EntityEventManager entityEventManager) {
		super(repository);
		//
		Assert.notNull(conceptRoleRequestService, "Concept role request service is required!");
		Assert.notNull(identityRoleService, "Identity role service is required!");
		Assert.notNull(identityService, "Identity service is required!");
		Assert.notNull(objectMapper, "Object mapper is required!");
		Assert.notNull(securityService, "Security service is required!");
		Assert.notNull(applicationContext, "Application context is required!");
		Assert.notNull(workflowProcessInstanceService, "Workflow process instance service is required!");
		Assert.notNull(entityEventManager, "Entity event manager is required!");
		//
		this.repository = repository;
		this.conceptRoleRequestService = conceptRoleRequestService;
		this.identityRoleService = identityRoleService;
		this.identityService = identityService;
		this.objectMapper = objectMapper;
		this.securityService = securityService;
		this.applicationContext = applicationContext;
		this.workflowProcessInstanceService = workflowProcessInstanceService;
		this.entityEventManager = entityEventManager;
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.ROLEREQUEST, getEntityClass());
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmRoleRequest> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			IdmRoleRequestFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		// applicant
		if (filter.getApplicantId() != null) {
			predicates.add(
					builder.equal(root.get(IdmRoleRequest_.applicant).get(IdmIdentity_.id), filter.getApplicantId()));
		}
		// duplicatedToRequestId
		if (filter.getDuplicatedToRequestId() != null) {
			predicates.add(builder.equal(root.get(IdmRoleRequest_.duplicatedToRequest).get(IdmRoleRequest_.id),
					filter.getDuplicatedToRequestId()));
		}
		//
		if (StringUtils.isNotEmpty(filter.getApplicant())) {
			predicates.add(builder.equal(root.get(IdmRoleRequest_.applicant).get(IdmIdentity_.username),
					filter.getApplicant()));
		}
		//
		if (filter.getState() != null) {
			predicates.add(builder.equal(root.get(IdmRoleRequest_.state), filter.getState()));
		}
		List<RoleRequestState> states = filter.getStates();
		if (!states.isEmpty()) {
			predicates.add(root.get(IdmRoleRequest_.state).in(states));
		}
		//
		List<UUID> applicants = filter.getApplicants();
		if (!applicants.isEmpty()) {
			predicates.add(root.get(IdmRoleRequest_.applicant).get(IdmIdentity_.id).in(applicants));
		}
		//
		if (filter.getCreatedFrom() != null) {
			predicates.add(builder.greaterThanOrEqualTo(root.get(IdmRoleRequest_.created), filter.getCreatedFrom()));
		}
		//
		if (filter.getCreatedTill() != null) {
			predicates.add(
					builder.lessThanOrEqualTo(root.get(IdmRoleRequest_.created), filter.getCreatedTill().plusDays(1)));
		}
		return predicates;
	}

	@Override
	@Transactional
	public IdmRoleRequestDto startRequest(UUID requestId, boolean checkRight) {
		try {
			IdmRoleRequestService service = this.getIdmRoleRequestService();
			if (!(service instanceof DefaultIdmRoleRequestService)) {
				throw new CoreException("We expects instace of DefaultIdmRoleRequestService!");
			}
			return ((DefaultIdmRoleRequestService) service).startRequestNewTransactional(requestId, checkRight);
		} catch (Exception ex) {
			LOG.error(ex.getLocalizedMessage(), ex);
			IdmRoleRequestDto request = get(requestId);
			Throwable exceptionToLog = ExceptionUtils.resolveException(ex);
			this.addToLog(request, Throwables.getStackTraceAsString(exceptionToLog));
			request.setState(RoleRequestState.EXCEPTION);
			return save(request);
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
	public IdmRoleRequestDto startRequestNewTransactional(UUID requestId, boolean checkRight) {
		return this.getIdmRoleRequestService().startRequestInternal(requestId, checkRight);
	}

	@Override
	@Transactional
	public IdmRoleRequestDto startRequestInternal(UUID requestId, boolean checkRight) {
		return startRequestInternal(requestId, checkRight, false);
	}
	
	@Override
	@Transactional
	public IdmRoleRequestDto startRequestInternal(UUID requestId, boolean checkRight, boolean immediate) {
		LOG.debug("Start role request [{}], checkRight [{}], immediate [{}]", requestId, checkRight, immediate);
		Assert.notNull(requestId, "Role request ID is required!");
		// Load request ... check right for read
		IdmRoleRequestDto request = get(requestId);
		Assert.notNull(request, "Role request DTO is required!");
		Assert.isTrue(
				RoleRequestState.CONCEPT == request.getState() || RoleRequestState.DUPLICATED == request.getState()
						|| RoleRequestState.EXCEPTION == request.getState(),
				"Only role request with CONCEPT or EXCEPTION or DUPLICATED state can be started!");

		IdmRoleRequestDto duplicant = validateOnDuplicity(request);

		if (duplicant != null) {
			request.setState(RoleRequestState.DUPLICATED);
			request.setDuplicatedToRequest(duplicant.getId());
			this.addToLog(request,
					MessageFormat.format("This request [{0}] is duplicated to another change permissions request [{1}]",
							request.getId(), duplicant.getId()));
			return this.save(request);
		}

		// Duplicant is fill, but request is not duplicated (maybe in past)
		if (request.getDuplicatedToRequest() != null) {
			request.setDuplicatedToRequest(null);
		}

		// Check on same applicants in all role concepts
		boolean identityNotSame = this.get(request.getId()).getConceptRoles().stream().anyMatch(concept -> {
			// get contract dto from embedded map
			IdmIdentityContractDto contract = (IdmIdentityContractDto) concept.getEmbedded()
					.get(IdmConceptRoleRequestService.IDENTITY_CONTRACT_FIELD);
			if (contract == null) {
				// If is contract from concept null, then contract via identity role must works
				contract = (IdmIdentityContractDto) identityRoleService.get(concept.getIdentityRole()).getEmbedded()
						.get(IdmConceptRoleRequestService.IDENTITY_CONTRACT_FIELD);
			}
			return !request.getApplicant().equals(contract.getIdentity());
		});

		if (identityNotSame) {
			throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_APPLICANTS_NOT_SAME,
					ImmutableMap.of("request", request, "applicant", request.getApplicant()));
		}

		// Convert whole request to JSON and persist (without logs and embedded data)
		try {
			IdmRoleRequestDto requestOriginal = get(requestId);
			trimRequest(requestOriginal);
			request.setOriginalRequest(objectMapper.writeValueAsString(requestOriginal));
		} catch (JsonProcessingException e) {
			throw new RoleRequestException(CoreResultCode.BAD_REQUEST, e);
		}

		// Request will be set on in progress state
		request.setState(RoleRequestState.IN_PROGRESS);
		IdmRoleRequestDto savedRequest = this.save(request);

		// Throw event
		Map<String, Serializable> variables = new HashMap<>();
		variables.put(RoleRequestApprovalProcessor.CHECK_RIGHT_PROPERTY, checkRight);
		RoleRequestEvent event = new RoleRequestEvent(RoleRequestEventType.EXCECUTE, savedRequest, variables);
		if (immediate) {
			event.setPriority(PriorityType.IMMEDIATE);
		}
		return entityEventManager
				.process(event)
				.getContent();
	}

	@Override
	@Transactional
	public boolean startApprovalProcess(IdmRoleRequestDto request, boolean checkRight,
			EntityEvent<IdmRoleRequestDto> event, String wfDefinition) {
		// If is request marked as executed immediately, then we will check right
		// and do realization immediately (without start approval process)
		if (request.isExecuteImmediately()) {
			boolean haveRightExecuteImmediately = securityService
					.hasAnyAuthority(CoreGroupPermission.ROLE_REQUEST_EXECUTE);

			if (checkRight && !haveRightExecuteImmediately) {
				throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_NO_EXECUTE_IMMEDIATELY_RIGHT,
						ImmutableMap.of("new", request));
			}

			// All concepts in progress state will be set on approved (we can
			// execute it immediately)
			request.getConceptRoles().stream().filter(concept -> {
				return RoleRequestState.IN_PROGRESS == concept.getState();
			}).forEach(concept -> {
				concept.setState(RoleRequestState.APPROVED);
				conceptRoleRequestService.save(concept);
			});

			// Execute request immediately
			return true;
		} else {
			IdmIdentityDto applicant = identityService.get(request.getApplicant());

			Map<String, Object> variables = new HashMap<>();
			// Minimize size of DTO persisting to WF
			IdmRoleRequestDto eventRequest = event.getContent();
			trimRequest(eventRequest);
			eventRequest.setConceptRoles(null);
			eventRequest.setOriginalRequest(null);
			variables.put(EntityEvent.EVENT_PROPERTY, event);

			ProcessInstance processInstance = workflowProcessInstanceService.startProcess(wfDefinition,
					IdmIdentity.class.getSimpleName(), applicant.getUsername(), applicant.getId().toString(),
					variables);
			// We have to refresh request (maybe was changed in wf process)
			request = this.get(request.getId());
			request.setWfProcessId(processInstance.getProcessInstanceId());
			this.save(request);
		}

		return false;
	}

	@Override
	@Transactional
	public IdmRoleRequestDto executeRequest(UUID requestId) {
		// We can`t catch and log exception to request, because this transaction will be
		// marked as to rollback.
		// We can`t run this method in new transaction, because changes on request
		// (state modified in WF for example) is in uncommited transaction!
		//
		// prepare request event
		Assert.notNull(requestId, "Role request ID is required!");
		IdmRoleRequestDto request = this.get(requestId);
		Assert.notNull(request, "Role request is required!");
		RoleRequestEvent event = new RoleRequestEvent(RoleRequestEventType.EXCECUTE, request);
		//
		return this.executeRequestInternal(event);
	}
	
	@Override
	@Transactional
	public IdmRoleRequestDto executeRequest(EntityEvent<IdmRoleRequestDto> requestEvent) {
		return this.executeRequestInternal(requestEvent);
	}

	private IdmRoleRequestDto executeRequestInternal(EntityEvent<IdmRoleRequestDto> requestEvent) {
		UUID requestId = requestEvent.getContent().getId();
		Assert.notNull(requestId, "Role request ID is required!");
		IdmRoleRequestDto request = this.get(requestId);
		Assert.notNull(request, "Role request is required!");

		List<IdmConceptRoleRequestDto> concepts = request.getConceptRoles();
		IdmIdentityDto identity = identityService.get(request.getApplicant());

		boolean identityNotSame = concepts.stream().anyMatch(concept -> {
			// get contract dto from embedded map
			IdmIdentityContractDto contract = (IdmIdentityContractDto) concept.getEmbedded()
					.get(IdmConceptRoleRequestService.IDENTITY_CONTRACT_FIELD);
			return !identity.getId().equals(contract.getIdentity());
		});

		if (identityNotSame) {
			throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_APPLICANTS_NOT_SAME,
					ImmutableMap.of("request", request, "applicant", identity.getUsername()));
		}
		
		// Persist event for whole request, listeners can intercept this event event is created as processed
		if (requestEvent.getPriority() == PriorityType.NORMAL) {
			requestEvent.setPriority(PriorityType.HIGH); // TODO: propagate from FE?
		}
		requestEvent.setSuperOwnerId(identity.getId());
		// start request event
		entityEventManager.saveEvent(requestEvent, new OperationResultDto.Builder(OperationState.RUNNING).build());
		//
		// Create new identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.ADD == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be
			// approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			IdmIdentityRoleDto identityRole = new IdmIdentityRoleDto();
			identityRole = convertConceptRoleToIdentityRole(conceptRoleRequestService.get(concept.getId()), identityRole);
			IdentityRoleEvent event = new IdentityRoleEvent(IdentityRoleEventType.CREATE, identityRole);
			
			// propagate event
			identityRole = identityRoleService.publish(event, requestEvent).getContent();
			
			// Save created identity role id
			concept.setIdentityRole(identityRole.getId());
			concept.setState(RoleRequestState.EXECUTED);
			IdmRoleDto roleDto = DtoUtils.getEmbedded(identityRole, IdmIdentityRole_.role);
			String message = MessageFormat.format("Role [{0}] was added to applicant. Requested in concept [{1}].",
					roleDto.getCode(), concept.getId());
			conceptRoleRequestService.addToLog(concept, message);
			conceptRoleRequestService.addToLog(request, message);
			conceptRoleRequestService.save(concept);
		});

		// Update identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.UPDATE == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be
			// approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			IdmIdentityRoleDto identityRole = identityRoleService.get(concept.getIdentityRole());
			identityRole = convertConceptRoleToIdentityRole(conceptRoleRequestService.get(concept.getId()), identityRole);
			IdentityRoleEvent event = new IdentityRoleEvent(IdentityRoleEventType.UPDATE, identityRole);
			
			// propagate event
			identityRole = identityRoleService.publish(event, requestEvent).getContent();

			// Save created identity role id
			concept.setIdentityRole(identityRole.getId());
			concept.setState(RoleRequestState.EXECUTED);
			IdmRoleDto roleDto = DtoUtils.getEmbedded(identityRole, IdmIdentityRole_.role);
			String message = MessageFormat.format("Role [{0}] was changed. Requested in concept [{1}].",
					roleDto.getCode(), concept.getId());
			conceptRoleRequestService.addToLog(concept, message);
			conceptRoleRequestService.addToLog(request, message);
			conceptRoleRequestService.save(concept);
		});

		// Delete identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.REMOVE == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be
			// approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			Assert.notNull(concept.getIdentityRole(), "IdentityRole is mandatory for delete!");
			IdmIdentityRoleDto identityRole = identityRoleService.get(concept.getIdentityRole());
			
			if (identityRole != null) {
				concept.setState(RoleRequestState.EXECUTED);
				concept.setIdentityRole(null); // we have to remove relation on
												// deleted identityRole
				String message = MessageFormat.format(
						"IdentityRole [{0}] (reqested in concept [{1}]) was deleted (from this role request).",
						identityRole.getId(), concept.getId());
				conceptRoleRequestService.addToLog(concept, message);
				conceptRoleRequestService.addToLog(request, message);
				conceptRoleRequestService.save(concept);
				
				IdentityRoleEvent event = new IdentityRoleEvent(IdentityRoleEventType.DELETE, identityRole);
				
				identityRoleService.publish(event, requestEvent);
			}
		});

		request.setState(RoleRequestState.EXECUTED);
		return this.save(request);

	}

	@Override
	public IdmRoleRequestDto toDto(IdmRoleRequest entity, IdmRoleRequestDto dto) {
		IdmRoleRequestDto requestDto = super.toDto(entity, dto);
		// Set concepts to request DTO
		if (requestDto != null) {
			requestDto.setConceptRoles(conceptRoleRequestService.findAllByRoleRequest(requestDto.getId()));
		}		
		// Load and add WF process DTO to embedded. Prevents of many requests
		// from FE.
		if (requestDto != null && requestDto.getWfProcessId() != null) {
			if (RoleRequestState.IN_PROGRESS == requestDto.getState()) {
				// Instance of process should exists only in 'IN_PROGRESS' state
				WorkflowProcessInstanceDto processInstanceDto = workflowProcessInstanceService
						.get(requestDto.getWfProcessId());
				// Trim a process variables - prevent security issues and too
				// high of response
				// size
				if (processInstanceDto != null) {
					processInstanceDto.setProcessVariables(null);
				}
				requestDto.getEmbedded().put(IdmRoleRequestDto.WF_PROCESS_FIELD, processInstanceDto);
			} else {
				// In others states we need load historic process
				WorkflowHistoricProcessInstanceDto processHistDto = workflowHistoricProcessInstanceService.get(requestDto.getWfProcessId());
				// Trim a process variables - prevent security issues and too
				// high of response
				// size
				if (processHistDto != null) {
					processHistDto.setProcessVariables(null);
				}
				requestDto.getEmbedded().put(IdmRoleRequestDto.WF_PROCESS_FIELD, processHistDto);
			}
		}

		return requestDto;
	}

	@Override
	public IdmRoleRequest toEntity(IdmRoleRequestDto dto, IdmRoleRequest entity) {
		// Set persisted value to read only properties
		// TODO: Create converter for skip fields mark as read only
		if (dto.getId() != null) {
			IdmRoleRequestDto dtoPersisited = this.get(dto.getId());
			if (dto.getState() == null) {
				dto.setState(dtoPersisited.getState());
			}
			if (dto.getLog() == null) {
				dto.setLog(dtoPersisited.getLog());
			}
			if (dto.getConceptRoles() == null) {
				dto.setConceptRoles(dtoPersisited.getConceptRoles());
			}
			if (dto.getWfProcessId() == null) {
				dto.setWfProcessId(dtoPersisited.getWfProcessId());
			}
			if (dto.getOriginalRequest() == null) {
				dto.setOriginalRequest(dtoPersisited.getOriginalRequest());
			}
		} else {
			dto.setState(RoleRequestState.CONCEPT);
		}

		return super.toEntity(dto, entity);

	}

	private boolean isDuplicated(IdmRoleRequestDto request, IdmRoleRequestDto duplicant) {

		if (request == duplicant) {
			return true;
		}
		if (request.getDescription() == null) {
			if (duplicant.getDescription() != null) {
				return false;
			}
		} else if (!request.getDescription().equals(duplicant.getDescription())) {
			return false;
		}

		if (request.getConceptRoles() == null) {
			if (duplicant.getConceptRoles() != null) {
				return false;
			}
		} else if (!request.getConceptRoles().equals(duplicant.getConceptRoles())) {
			return false;
		}
		if (request.getApplicant() == null) {
			if (duplicant.getApplicant() != null) {
				return false;
			}
		} else if (!request.getApplicant().equals(duplicant.getApplicant())) {
			return false;
		}
		return true;
	}

	@Override
	public void addToLog(Loggable logItem, String text) {
		StringBuilder sb = new StringBuilder();
		sb.append(DateTime.now());
		sb.append(": ");
		sb.append(text);
		text = sb.toString();
		logItem.addToLog(text);
		LOG.info(text);

	}

	@Override
	@Transactional
	public void deleteInternal(IdmRoleRequestDto dto) {
		// Find all request where is this request duplicated and remove relation
		IdmRoleRequestFilter conceptRequestFilter = new IdmRoleRequestFilter();
		conceptRequestFilter.setDuplicatedToRequestId(dto.getId());
		this.find(conceptRequestFilter, null).getContent().forEach(duplicant -> {
			duplicant.setDuplicatedToRequest(null);
			if (RoleRequestState.DUPLICATED == duplicant.getState()) {
				duplicant.setState(RoleRequestState.CONCEPT);
				duplicant.setDuplicatedToRequest(null);
			}
			String message = MessageFormat.format("Duplicated request [{0}] was deleted!", dto.getId());
			this.addToLog(duplicant, message);
			this.save(duplicant);
		});

		// Stop connected WF process
		cancelWF(dto);

		// First we have to delete all concepts for this request
		dto.getConceptRoles().forEach(concept -> {
			conceptRoleRequestService.delete(concept);
		});
		super.deleteInternal(dto);
	}

	@Override
	@Transactional
	public void cancel(IdmRoleRequestDto dto) {
		cancelWF(dto);
		dto.setState(RoleRequestState.CANCELED);
		this.save(dto);
	}

	@Override
	public IdmRoleRequestDto createRequest(IdmIdentityContractDto contract, IdmRoleDto... roles) {
		Assert.notNull(contract, "Contract must be filled for create role request!");
		IdmRoleRequestDto roleRequest = new IdmRoleRequestDto();
		roleRequest.setApplicant(contract.getIdentity());
		roleRequest.setRequestedByType(RoleRequestedByType.AUTOMATICALLY);
		roleRequest.setExecuteImmediately(true);
		roleRequest = this.save(roleRequest);
		if (roles != null) {
			for (IdmRoleDto role : roles) {
				createConcept(roleRequest, contract, role.getId(), ConceptRoleRequestOperation.ADD);
			}
		}
		return roleRequest;
	}

	private void cancelWF(IdmRoleRequestDto dto) {
		if (!Strings.isNullOrEmpty(dto.getWfProcessId())) {
			WorkflowFilterDto filter = new WorkflowFilterDto();
			filter.setProcessInstanceId(dto.getWfProcessId());

			@SuppressWarnings("deprecation")
			Collection<WorkflowProcessInstanceDto> resources = workflowProcessInstanceService
					.searchInternal(filter, false).getResources();
			if (resources.isEmpty()) {
				// Process with this ID not exist ... maybe was ended
				this.addToLog(dto, MessageFormat.format(
						"Workflow process with ID [{0}] was not deleted, because was not found. Maybe was ended before.",
						dto.getWfProcessId()));
				return;
			}

			workflowProcessInstanceService.delete(dto.getWfProcessId(),
					"Role request use this WF, was deleted. This WF was deleted too.");
			this.addToLog(dto,
					MessageFormat.format(
							"Workflow process with ID [{0}] was deleted, because this request is deleted/canceled",
							dto.getWfProcessId()));
		}
	}

	private IdmIdentityRoleDto convertConceptRoleToIdentityRole(IdmConceptRoleRequestDto conceptRole,
			IdmIdentityRoleDto identityRole) {
		if (conceptRole == null || identityRole == null) {
			return null;
		}
		identityRole.setRole(conceptRole.getRole());
		identityRole.setIdentityContract(conceptRole.getIdentityContract());
		identityRole.setValidFrom(conceptRole.getValidFrom());
		identityRole.setValidTill(conceptRole.getValidTill());
		identityRole.setOriginalCreator(conceptRole.getOriginalCreator());
		identityRole.setOriginalModifier(conceptRole.getOriginalModifier());
		identityRole.setAutomaticRole(conceptRole.getAutomaticRole());
		//
		// if exists role tree node, set automatic role
		if (conceptRole.getAutomaticRole() != null) {
			identityRole.setAutomaticRole(conceptRole.getAutomaticRole());
		}
		return identityRole;
	}

	private IdmRoleRequestService getIdmRoleRequestService() {
		if (this.roleRequestService == null) {
			this.roleRequestService = applicationContext.getBean(IdmRoleRequestService.class);
		}
		return this.roleRequestService;
	}

	private IdmRoleRequestDto validateOnDuplicity(IdmRoleRequestDto request) {
		List<IdmRoleRequestDto> potentialDuplicatedRequests = new ArrayList<>();

		potentialDuplicatedRequests.addAll(toDtos(
				repository.findAllByApplicant_IdAndState(request.getApplicant(), RoleRequestState.IN_PROGRESS), false));
		potentialDuplicatedRequests.addAll(toDtos(
				repository.findAllByApplicant_IdAndState(request.getApplicant(), RoleRequestState.APPROVED), false));

		Optional<IdmRoleRequestDto> duplicatedRequestOptional = potentialDuplicatedRequests.stream()
				.filter(requestDuplicate -> {
					return isDuplicated(request, requestDuplicate) && !(request.getId() != null
							&& requestDuplicate.getId() != null && request.getId().equals(requestDuplicate.getId()));
				}).findFirst();

		if (duplicatedRequestOptional.isPresent()) {
			return duplicatedRequestOptional.get();
		}
		return null;
	}

	/**
	 * Trim request and his role concepts. Remove embedded objects. It is important
	 * for minimize size of dto persisted for example in WF process.
	 * 
	 * @param requestOriginal
	 */
	private void trimRequest(IdmRoleRequestDto requestOriginal) {
		requestOriginal.setLog(null);
		requestOriginal.setEmbedded(null);
		requestOriginal.getConceptRoles().forEach(concept -> {
			concept.setEmbedded(null);
			concept.setLog(null);
		});
	}

	/**
	 * Method create {@link IdmConceptRoleRequestDto}
	 * 
	 * @param roleRequest
	 * @param contract
	 * @param roleId
	 * @param operation
	 * @return
	 */
	private IdmConceptRoleRequestDto createConcept(IdmRoleRequestDto roleRequest, IdmIdentityContractDto contract,
			UUID roleId, ConceptRoleRequestOperation operation) {
		IdmConceptRoleRequestDto conceptRoleRequest = new IdmConceptRoleRequestDto();
		conceptRoleRequest.setRoleRequest(roleRequest.getId());
		conceptRoleRequest.setIdentityContract(contract.getId());
		conceptRoleRequest.setValidFrom(contract.getValidFrom());
		conceptRoleRequest.setValidTill(contract.getValidTill());
		conceptRoleRequest.setRole(roleId);
		conceptRoleRequest.setOperation(operation);
		return conceptRoleRequestService.save(conceptRoleRequest);
	}

}
