package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.activiti.engine.runtime.ProcessInstance;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Loggable;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.exception.RoleRequestException;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.model.domain.ConceptRoleRequestOperation;
import eu.bcvsolutions.idm.core.model.domain.IdmGroupPermission;
import eu.bcvsolutions.idm.core.model.domain.RoleRequestState;
import eu.bcvsolutions.idm.core.model.dto.IdmConceptRoleRequestDto;
import eu.bcvsolutions.idm.core.model.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.model.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.model.dto.filter.ConceptRoleRequestFilter;
import eu.bcvsolutions.idm.core.model.dto.filter.RoleRequestFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmConceptRoleRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent.RoleRequestEventType;
import eu.bcvsolutions.idm.core.model.event.processor.RoleRequestApprovalProcessor;
import eu.bcvsolutions.idm.core.model.service.api.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Default implementation of role request service
 * 
 * @author svandav
 *
 */
@Service("roleRequestService")
public class DefaultIdmRoleRequestService
		extends AbstractReadWriteDtoService<IdmRoleRequestDto, IdmRoleRequest, RoleRequestFilter>
		implements IdmRoleRequestService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmRoleRequestService.class);
	private final IdmConceptRoleRequestService conceptRoleRequestService;
	private final IdmIdentityRoleService identityRoleService;
	private final IdmIdentityService identityService;
	private final ObjectMapper objectMapper;
	private final SecurityService securityService;
	private final ApplicationContext applicationContext;
	private final WorkflowProcessInstanceService workflowProcessInstanceService;
	private final EntityEventManager entityEventManager;
	private IdmRoleRequestService roleRequestService;

	@Autowired
	public DefaultIdmRoleRequestService(AbstractEntityRepository<IdmRoleRequest, RoleRequestFilter> repository,
			IdmConceptRoleRequestService conceptRoleRequestService, IdmIdentityRoleService identityRoleService,
			IdmIdentityService identityService, ObjectMapper objectMapper, SecurityService securityService,
			ApplicationContext applicationContext, WorkflowProcessInstanceService workflowProcessInstanceService,
			EntityEventManager entityEventManager) {
		super(repository);

		Assert.notNull(conceptRoleRequestService, "Concept role request service is required!");
		Assert.notNull(identityRoleService, "Identity role service is required!");
		Assert.notNull(identityService, "Identity service is required!");
		Assert.notNull(objectMapper, "Object mapper is required!");
		Assert.notNull(securityService, "Security service is required!");
		Assert.notNull(applicationContext, "Application context is required!");
		Assert.notNull(workflowProcessInstanceService, "Workflow process instance service is required!");
		Assert.notNull(entityEventManager, "Entity event manager is required!");

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
	public IdmRoleRequestDto save(IdmRoleRequestDto dto) {
		boolean created = false;
		if (dto.getId() == null) {
			created = true;
		}
		// Load applicant (check read right)
		IdmIdentity applicant = identityService.get(dto.getApplicant());
		List<IdmConceptRoleRequestDto> concepts = dto.getConceptRoles();
		if (!created) {
			// validateOnDuplicity(dto);
		}
		IdmRoleRequestDto savedRequest = super.save(dto);

		// Concepts will be save only on create request
		if (created && concepts != null) {
			concepts.forEach(concept -> {
				concept.setRoleRequest(savedRequest.getId());
			});
			this.conceptRoleRequestService.saveAll(concepts);
		}

		// Check on same applicants in all role concepts
		boolean identityNotSame = this.getDto(savedRequest.getId()).getConceptRoles().stream().filter(concept -> {
			// get contract dto from embedded map
			IdmIdentityContractDto contract = (IdmIdentityContractDto) concept.getEmbedded()
					.get(IdmConceptRoleRequestService.IDENTITY_CONTRACT_FIELD);
			return !dto.getApplicant().equals(contract.getIdentity());
		}).findFirst().isPresent();

		if (identityNotSame) {
			throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_APPLICANTS_NOT_SAME,
					ImmutableMap.of("request", dto, "applicant", applicant.getUsername()));
		}

		if (created) {
			// TODO: Separate start request to own schedule task
			// this.startRequest(savedRequest.getId());
		}
		return this.getDto(savedRequest.getId());
	}

	@Override
	public IdmRoleRequestDto toDto(IdmRoleRequest entity, IdmRoleRequestDto dto) {
		IdmRoleRequestDto requestDto = super.toDto(entity, dto);
		if (requestDto != null) {
			ConceptRoleRequestFilter conceptFilter = new ConceptRoleRequestFilter();
			conceptFilter.setRoleRequestId(requestDto.getId());
			requestDto.setConceptRoles(conceptRoleRequestService.findDto(conceptFilter, null).getContent());
		}
		return requestDto;
	}

	@Override
	public IdmRoleRequest toEntity(IdmRoleRequestDto dto, IdmRoleRequest entity) {
		if (entity == null || entity.getId() == null) {
			try {
				dto.setOriginalRequest(objectMapper.writeValueAsString(dto));
			} catch (JsonProcessingException e) {
				throw new RoleRequestException(CoreResultCode.BAD_REQUEST, e);
			}
		}
		// Set persisted value to read only properties
		// TODO: Create converter for skip fields mark as read only
		if (dto.getId() != null) {
			IdmRoleRequestDto dtoPersisited = this.getDto(dto.getId());
			if (dto.getState() == null) {
				dto.setState(dtoPersisited.getState());
			}
			if (dto.getLog() == null) {
				dto.setLog(dtoPersisited.getLog());
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

	@Override
	public void startRequest(UUID requestId) {

		try {
			// Request will be started in new transaction
			this.getIdmRoleRequestService().startRequestInternal(requestId, true);
		} catch (Exception ex) {
			LOG.error(ex.getLocalizedMessage(), ex);
			IdmRoleRequestDto request = getDto(requestId);
			this.addToLog(request, Throwables.getStackTraceAsString(ex));
			request.setState(RoleRequestState.EXCEPTION);
			save(request);
		}
	}

	@Override
	public void startRequestInternal(UUID requestId, boolean checkRight) {
		LOG.debug("Start role request [{}]", requestId);
		Assert.notNull(requestId, "Role request ID is required!");
		// Load request ... check right for read
		IdmRoleRequestDto request = getDto(requestId);
		Assert.notNull(request, "Role request DTO is required!");
		Assert.isTrue(
				RoleRequestState.CONCEPT == request.getState() 
				|| RoleRequestState.DUPLICATED == request.getState() 
				|| RoleRequestState.EXCEPTION == request.getState(),
				"Only role request with CONCEPT or EXCEPTION or DUPLICATED state can be started!");

		IdmRoleRequestDto duplicant = validateOnDuplicity(request);

		if (duplicant != null) {
			request.setState(RoleRequestState.DUPLICATED);
			request.setDuplicatedToRequest(duplicant.getId());
			this.addToLog(request, MessageFormat.format("This request [{0}] is duplicated to another change permissions request [{1}]", request.getId(), duplicant.getId()));
			this.save(request);
			return;
		}
		// TODO: check on same identities

		// Request will be set on in progress state
		request.setState(RoleRequestState.IN_PROGRESS);
		IdmRoleRequestDto savedRequest = this.save(request);
		
		// Throw event
		Map<String, Serializable> variables = new HashMap<>();
		variables.put(RoleRequestApprovalProcessor.CHECK_RIGHT_PROPERTY, checkRight);
		entityEventManager.process(new RoleRequestEvent(RoleRequestEventType.EXCECUTE, savedRequest, variables));
	}
	

	@Override
	public boolean startApprovalProcess(IdmRoleRequestDto request, boolean checkRight, EntityEvent<IdmRoleRequestDto> event,  String wfDefinition){
		// If is request marked as executed immediately, then we will check right
		// and do realization immediately (without start approval process) 
		if (request.isExecuteImmediately()) {
			boolean haveRightExecuteImmediately = securityService
					.hasAnyAuthority(IdmGroupPermission.ROLE_REQUEST_IMMEDIATELY_WRITE);

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
		}else {
			IdmIdentity applicant = identityService.get(request.getApplicant());
			
			Map<String, Object> variables = new HashMap<>();
			variables.put(EntityEvent.EVENT_PROPERTY, event);
			
			ProcessInstance processInstance = workflowProcessInstanceService.startProcess(wfDefinition,
					IdmIdentity.class.getSimpleName(), applicant.getUsername(), applicant.getId().toString(), variables);
			request.setWfProcessId(processInstance.getProcessInstanceId());
			this.save(request);
		}
		
		return false;
	}
	
	private IdmRoleRequestDto validateOnDuplicity(IdmRoleRequestDto request) {
		List<IdmRoleRequestDto> potentialDuplicatedRequests = new ArrayList<>();

		RoleRequestFilter requestFilter = new RoleRequestFilter();
		requestFilter.setApplicantId(request.getApplicant());
		requestFilter.setState(RoleRequestState.IN_PROGRESS);
		potentialDuplicatedRequests.addAll(this.findDto(requestFilter, null).getContent());

		requestFilter.setState(RoleRequestState.APPROVED);
		potentialDuplicatedRequests.addAll(this.findDto(requestFilter, null).getContent());

		requestFilter.setState(RoleRequestState.CONCEPT);
		potentialDuplicatedRequests.addAll(this.findDto(requestFilter, null).getContent());

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

	@Override
	public IdmRoleRequestDto executeRequest(UUID requestId) {
		Assert.notNull(requestId, "Role request ID is required!");
		IdmRoleRequestDto request = this.getDto(requestId);
		Assert.notNull(request, "Role request is required!");

		List<IdmConceptRoleRequestDto> concepts = request.getConceptRoles();
		IdmIdentity identity = identityService.get(request.getApplicant());

		boolean identityNotSame = concepts.stream().filter(concept -> {
			// get contract dto from embedded map
			IdmIdentityContractDto contract = (IdmIdentityContractDto) concept.getEmbedded()
					.get(IdmConceptRoleRequestService.IDENTITY_CONTRACT_FIELD);
			return !identity.getId().equals(contract.getIdentity());
		}).findFirst().isPresent();

		if (identityNotSame) {
			throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_APPLICANTS_NOT_SAME,
					ImmutableMap.of("request", request, "applicant", identity.getUsername()));
		}

		List<IdmIdentityRole> identityRolesToSave = new ArrayList<>();
		List<IdmConceptRoleRequestDto> conceptsToSave = new ArrayList<>();

		// Create new identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.ADD == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			IdmIdentityRole identityRole = new IdmIdentityRole();
			identityRolesToSave.add(
					convertConceptRoleToIdentityRole(conceptRoleRequestService.get(concept.getId()), identityRole));
			concept.setState(RoleRequestState.EXECUTED);
			String message = MessageFormat.format("IdentityRole [{0}] was added to applicant. Requested in concept [{1}].",
					identityRole.getRole().getName(), concept.getId());
			conceptRoleRequestService.addToLog(concept, message);
			conceptRoleRequestService.addToLog(request, message);
			conceptsToSave.add(concept);
		});

		// Create new identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.UPDATE == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			IdmIdentityRole identityRole = identityRoleService.get(concept.getIdentityRole());
			identityRolesToSave.add(
					convertConceptRoleToIdentityRole(conceptRoleRequestService.get(concept.getId()), identityRole));
			concept.setState(RoleRequestState.EXECUTED);
			String message = MessageFormat.format("IdentityRole [{0}] was changed. Requested in concept [{1}].",
					identityRole.getRole().getName(), concept.getId());
			conceptRoleRequestService.addToLog(concept, message);
			conceptRoleRequestService.addToLog(request, message);
			conceptsToSave.add(concept);
		});

		// Delete identity role
		concepts.stream().filter(concept -> {
			return ConceptRoleRequestOperation.REMOVE == concept.getOperation();
		}).filter(concept -> {
			// Only approved concepts can be executed
			// Concepts in concept state will be executed too (for situation, when will be approval event disabled)
			return RoleRequestState.APPROVED == concept.getState() || RoleRequestState.CONCEPT == concept.getState();
		}).forEach(concept -> {
			IdmIdentityRole identityRole = identityRoleService.get(concept.getIdentityRole());
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
				identityRoleService.delete(identityRole);
			}
		});

		identityRoleService.saveAll(identityRolesToSave);
		conceptRoleRequestService.saveAll(conceptsToSave);
		request.setState(RoleRequestState.EXECUTED);
		return this.save(request);

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
	public void delete(IdmRoleRequestDto dto) {
		
		// Find all request where is this request duplicated and remove relation
		RoleRequestFilter conceptRequestFilter = new RoleRequestFilter();
		conceptRequestFilter.setDuplicatedToRequestId(dto.getId());
		this.findDto(conceptRequestFilter, null).getContent().forEach(duplicant -> {
			duplicant.setDuplicatedToRequest(null);
			duplicant.setState(RoleRequestState.CONCEPT);
			String message = MessageFormat.format("Duplicated request [{0}] was deleted!", dto.getId());
			this.addToLog(duplicant, message);
			this.save(duplicant);
		});
		super.delete(dto);
	}

	private IdmIdentityRole convertConceptRoleToIdentityRole(IdmConceptRoleRequest conceptRole,
			IdmIdentityRole identityRole) {
		if (conceptRole == null || identityRole == null) {
			return null;
		}
		identityRole.setRole(conceptRole.getRole());
		identityRole.setIdentityContract(conceptRole.getIdentityContract());
		identityRole.setValidFrom(conceptRole.getValidFrom());
		identityRole.setValidTill(conceptRole.getValidTill());
		identityRole.setOriginalCreator(conceptRole.getOriginalCreator());
		identityRole.setOriginalModifier(conceptRole.getOriginalModifier());
		return identityRole;
	}

	private IdmRoleRequestService getIdmRoleRequestService() {
		if (this.roleRequestService == null) {
			this.roleRequestService = applicationContext.getBean(IdmRoleRequestService.class);
		}
		return this.roleRequestService;
	}

}