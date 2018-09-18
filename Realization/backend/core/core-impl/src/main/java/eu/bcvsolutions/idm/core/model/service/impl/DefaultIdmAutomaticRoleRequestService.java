package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleRequestType;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.AbstractIdmAutomaticRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeRuleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeRuleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAutomaticRoleAttributeRuleRequestFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAutomaticRoleRequestFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.exception.AcceptedException;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.exception.RoleRequestException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeRuleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeRuleService;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeService;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.utils.ExceptionUtils;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmAutomaticRoleAttribute;
import eu.bcvsolutions.idm.core.model.entity.IdmAutomaticRoleRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmAutomaticRoleRequest_;
import eu.bcvsolutions.idm.core.model.entity.IdmAutomaticRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleTreeNode;
import eu.bcvsolutions.idm.core.model.entity.IdmRole_;
import eu.bcvsolutions.idm.core.model.event.AutomaticRoleRequestEvent;
import eu.bcvsolutions.idm.core.model.event.AutomaticRoleRequestEvent.AutomaticRoleRequestEventType;
import eu.bcvsolutions.idm.core.model.event.processor.role.RoleRequestApprovalProcessor;
import eu.bcvsolutions.idm.core.model.repository.IdmAutomaticRoleRequestRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Default implementation of automatic role request service
 * 
 * @author svandav
 *
 */
@Service("automaticRoleRequestService")
public class DefaultIdmAutomaticRoleRequestService extends
		AbstractReadWriteDtoService<IdmAutomaticRoleRequestDto, IdmAutomaticRoleRequest, IdmAutomaticRoleRequestFilter>
		implements IdmAutomaticRoleRequestService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
			.getLogger(DefaultIdmAutomaticRoleRequestService.class);

	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private WorkflowProcessInstanceService workflowProcessInstanceService;
	@Autowired
	private EntityEventManager entityEventManager;
	@Autowired
	private LookupService lookupService;
	@Autowired
	private IdmAutomaticRoleAttributeRuleRequestService automaticRoleRuleRequestService;
	@Autowired
	private IdmAutomaticRoleAttributeRuleService automaticRoleRuleService;
	@Autowired
	private IdmAutomaticRoleAttributeService automaticRoleAttributeService;
	@Autowired
	private IdmRoleTreeNodeService automaticRoleTreeService;
	private IdmAutomaticRoleRequestService roleRequestService;

	@Autowired
	public DefaultIdmAutomaticRoleRequestService(IdmAutomaticRoleRequestRepository repository) {
		super(repository);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.AUTOMATICROLEREQUEST, getEntityClass());
	}

	@Override
	@Transactional
	public IdmAutomaticRoleRequestDto startRequest(UUID requestId, boolean checkRight) {
		IdmAutomaticRoleRequestDto request = get(requestId);
		Assert.notNull(request, "Request is required!");

		// Validation on exist some rule
		if (AutomaticRoleRequestType.ATTRIBUTE == request.getRequestType()
				&& RequestOperationType.REMOVE != request.getOperation()) {
			IdmAutomaticRoleAttributeRuleRequestFilter ruleFilter = new IdmAutomaticRoleAttributeRuleRequestFilter();
			ruleFilter.setRoleRequestId(requestId);

			List<IdmAutomaticRoleAttributeRuleRequestDto> ruleConcepts = automaticRoleRuleRequestService
					.find(ruleFilter, null).getContent();
			if (ruleConcepts.isEmpty()) {
				throw new RoleRequestException(CoreResultCode.AUTOMATIC_ROLE_REQUEST_START_WITHOUT_RULE,
						ImmutableMap.of("request", request.getName()));
			}
		}

		try {
			IdmAutomaticRoleRequestService service = this.getIdmAutomaticRoleRequestService();
			if (!(service instanceof DefaultIdmAutomaticRoleRequestService)) {
				throw new CoreException("We expects instace of DefaultIdmAutomaticRoleRequestService!");
			}
			return ((DefaultIdmAutomaticRoleRequestService) service).startRequestNewTransactional(requestId,
					checkRight);
		} catch (Exception ex) {
			LOG.error(ex.getLocalizedMessage(), ex);
			request = get(requestId);
			Throwable exceptionToLog = ExceptionUtils.resolveException(ex);

			// TODO: I set only cause of exception, not code and properties. If are
			// properties set, then request cannot be save!
			request.setResult(
					new OperationResultDto.Builder(OperationState.EXCEPTION).setCause(exceptionToLog).build());
			request.setState(RequestState.EXCEPTION);
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
	public IdmAutomaticRoleRequestDto startRequestNewTransactional(UUID requestId, boolean checkRight) {
		return this.getIdmAutomaticRoleRequestService().startRequestInternal(requestId, checkRight);
	}

	@Override
	@Transactional
	public IdmAutomaticRoleRequestDto startRequestInternal(UUID requestId, boolean checkRight) {
		LOG.debug("Start role request [{}]", requestId);
		Assert.notNull(requestId, "Role request ID is required!");
		// Load request ... check right for read
		IdmAutomaticRoleRequestDto request = get(requestId);
		Assert.notNull(request, "Role request DTO is required!");
		Assert.isTrue(
				RequestState.CONCEPT == request.getState() || RequestState.DUPLICATED == request.getState()
						|| RequestState.EXCEPTION == request.getState(),
				"Only role request with CONCEPT or EXCEPTION or DUPLICATED state can be started!");

		// Request will be set on in progress state
		request.setState(RequestState.IN_PROGRESS);
		request.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		IdmAutomaticRoleRequestDto savedRequest = this.save(request);

		// Throw event
		Map<String, Serializable> variables = new HashMap<>();
		variables.put(RoleRequestApprovalProcessor.CHECK_RIGHT_PROPERTY, checkRight);
		return entityEventManager
				.process(new AutomaticRoleRequestEvent(AutomaticRoleRequestEventType.EXECUTE, savedRequest, variables))
				.getContent();
	}

	@Override
	@Transactional
	public boolean startApprovalProcess(IdmAutomaticRoleRequestDto request, boolean checkRight,
			EntityEvent<IdmAutomaticRoleRequestDto> event, String wfDefinition) {
		// If is request marked as executed immediately, then we will check right
		// and do realization immediately (without start approval process)
		if (request.isExecuteImmediately()) {
			boolean haveRightExecuteImmediately = securityService
					.hasAnyAuthority(CoreGroupPermission.AUTOMATIC_ROLE_REQUEST_ADMIN);

			if (checkRight && !haveRightExecuteImmediately) {
				throw new RoleRequestException(CoreResultCode.ROLE_REQUEST_NO_EXECUTE_IMMEDIATELY_RIGHT,
						ImmutableMap.of("new", request));
			}

			// Execute request immediately
			return true;
		} else {

			Map<String, Object> variables = new HashMap<>();
			// Minimize size of DTO persisting to WF
			IdmAutomaticRoleRequestDto eventRequest = event.getContent();
			eventRequest.setEmbedded(null);
			variables.put(EntityEvent.EVENT_PROPERTY, event);
			variables.put("approvalForAutomaticRole", Boolean.TRUE);

			ProcessInstance processInstance = workflowProcessInstanceService.startProcess(wfDefinition,
					IdmRoleDto.class.getSimpleName(), request.getCreator(), request.getCreatorId().toString(),
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
	public IdmAutomaticRoleRequestDto executeRequest(UUID requestId) {
		// We can`t catch and log exception to request, because this transaction will be
		// marked as to rollback.
		// We can`t run this method in new transaction, because changes on request
		// (state modified in WF for example) is in uncommited transaction!
		return this.executeRequestInternal(requestId);
	}

	private IdmAutomaticRoleRequestDto executeRequestInternal(UUID requestId) {
		Assert.notNull(requestId, "Role request ID is required!");
		IdmAutomaticRoleRequestDto request = this.get(requestId);
		Assert.notNull(request, "Role request is required!");

		IdmAutomaticRoleAttributeRuleRequestFilter ruleFilter = new IdmAutomaticRoleAttributeRuleRequestFilter();
		ruleFilter.setRoleRequestId(requestId);

		List<IdmAutomaticRoleAttributeRuleRequestDto> ruleConcepts = automaticRoleRuleRequestService
				.find(ruleFilter, null).getContent();
		UUID automaticRoleId = request.getAutomaticRole();

		if (AutomaticRoleRequestType.ATTRIBUTE == request.getRequestType()) {
			// Automatic role by attributes
			if (RequestOperationType.REMOVE == request.getOperation()) {
				// Remove automatic role by attributes
				Assert.notNull(automaticRoleId, "Id of automatic role in the request (for delete) is required!");
				automaticRoleAttributeService.delete(automaticRoleAttributeService.get(automaticRoleId));
				request.setAutomaticRole(null);
			} else {
				// Add new or update (rules) for automatic role by attributes
				IdmAutomaticRoleAttributeDto automaticRole = null;
				if (automaticRoleId != null) {
					automaticRole = automaticRoleAttributeService.get(automaticRoleId);
				} else {
					automaticRole = new IdmAutomaticRoleAttributeDto();
					automaticRole = initAttributeAutomaticRole(request, automaticRole);
					automaticRole = automaticRoleAttributeService.save(automaticRole);
					request.setAutomaticRole(automaticRole.getId());
				}
				UUID roleId = automaticRole.getRole() != null ? automaticRole.getRole() : request.getRole();
				Assert.notNull(roleId, "Id of role is required in the automatic role request!");

				IdmRoleDto role = roleService.get(request.getRole());
				Assert.notNull(role, "Role is required in the automatic role request!");

				// Before we do any change, we have to sets the automatic role to concept state
				automaticRole.setConcept(true);
				automaticRoleAttributeService.save(automaticRole);

				// Realize changes for rules
				realizeAttributeRules(request, automaticRole, ruleConcepts);

				// Sets automatic role as no concept -> execute recalculation this role
				automaticRole.setConcept(false);
				automaticRoleAttributeService.recalculate(automaticRoleAttributeService.save(automaticRole).getId());

			}
		} else if (AutomaticRoleRequestType.TREE == request.getRequestType()) {
			// Automatic role by node in a tree
			if (RequestOperationType.REMOVE == request.getOperation()) {
				// Remove tree automatic role
				Assert.notNull(automaticRoleId, "Id of automatic role in the request (for delete) is required!");
				// Recount (remove) assigned roles ensures LRT during delete
				automaticRoleTreeService.delete(automaticRoleTreeService.get(automaticRoleId));
				request.setAutomaticRole(null);

			} else if (RequestOperationType.ADD == request.getOperation()) {
				// Create new tree automatic role
				IdmRoleTreeNodeDto treeAutomaticRole = new IdmRoleTreeNodeDto();
				treeAutomaticRole = initTreeAutomaticRole(request, treeAutomaticRole);
				// Recount of assigned roles ensures LRT after save 
				treeAutomaticRole = automaticRoleTreeService.save(treeAutomaticRole);
				request.setAutomaticRole(treeAutomaticRole.getId());
			} else {
				// Update is not supported
				throw new ResultCodeException(CoreResultCode.METHOD_NOT_ALLOWED,
						"Tree automatic role update is not supported");
			}
		}

		request.setState(RequestState.EXECUTED);
		request.setResult(new OperationResultDto.Builder(OperationState.EXECUTED).build());
		return this.save(request);

	}

	@Override
	public IdmAutomaticRoleRequestDto toDto(IdmAutomaticRoleRequest entity, IdmAutomaticRoleRequestDto dto) {
		IdmAutomaticRoleRequestDto requestDto = super.toDto(entity, dto);

		if (requestDto != null && requestDto.getWfProcessId() != null) {
			WorkflowProcessInstanceDto processDto = workflowProcessInstanceService.get(requestDto.getWfProcessId(),
					false);
			// TODO: create trimmed variant in workflow process instance service
			if (processDto != null) {
				processDto.setProcessVariables(null);
			}
			requestDto.getEmbedded().put(IdmRoleRequestDto.WF_PROCESS_FIELD, processDto);
		}

		return requestDto;
	}

	@Override
	@Transactional
	public void deleteInternal(IdmAutomaticRoleRequestDto dto) {
		// Stop connected WF process
		cancelWF(dto);

		// First we have to delete all rule concepts for this request
		if (dto.getId() != null) {
			IdmAutomaticRoleAttributeRuleRequestFilter ruleFilter = new IdmAutomaticRoleAttributeRuleRequestFilter();
			ruleFilter.setRoleRequestId(dto.getId());
			List<IdmAutomaticRoleAttributeRuleRequestDto> ruleConcepts = automaticRoleRuleRequestService
					.find(ruleFilter, null).getContent();
			ruleConcepts.forEach(concept -> {
				automaticRoleRuleRequestService.delete(concept);
			});
		}
		super.deleteInternal(dto);
	}

	@Override
	@Transactional
	public void cancel(IdmAutomaticRoleRequestDto dto) {
		cancelWF(dto);
		dto.setState(RequestState.CANCELED);
		dto.setResult(new OperationResultDto(OperationState.CANCELED));
		this.save(dto);
	}

	@Override
	protected IdmAutomaticRoleRequest toEntity(IdmAutomaticRoleRequestDto dto, IdmAutomaticRoleRequest entity) {

		if (this.isNew(dto)) {
			dto.setResult(new OperationResultDto(OperationState.CREATED));
			dto.setState(RequestState.CONCEPT);
			if (dto.getRequestType() == null) {
				dto.setRequestType(AutomaticRoleRequestType.ATTRIBUTE);
			}
		} else if (dto != null && dto.getResult() == null) {
			IdmAutomaticRoleRequestDto persistedDto = this.get(dto.getId());
			dto.setResult(persistedDto.getResult());
		}
		
		IdmAutomaticRoleRequest requestEntity = super.toEntity(dto, entity);

		// Convert type of automatic role
		if (dto != null) {
			UUID automaticRoleId = dto.getAutomaticRole();
			if (automaticRoleId != null && AutomaticRoleRequestType.ATTRIBUTE == dto.getRequestType()) {
				requestEntity.setAutomaticRole((IdmAutomaticRoleAttribute) lookupService
						.lookupEntity(IdmAutomaticRoleAttribute.class, automaticRoleId));
			}
			if (automaticRoleId != null && AutomaticRoleRequestType.TREE == dto.getRequestType()) {
				requestEntity.setAutomaticRole(
						(IdmRoleTreeNode) lookupService.lookupEntity(IdmRoleTreeNode.class, automaticRoleId));
			}
		}
		if (requestEntity != null && requestEntity.getAutomaticRole() != null) {
			requestEntity.setRole(requestEntity.getAutomaticRole().getRole());
			if (Strings.isNullOrEmpty(requestEntity.getName())) {
				requestEntity.setName(requestEntity.getAutomaticRole().getName());
			}
		}
		return requestEntity;
	}
	
	@Override
	public void deleteAutomaticRole(AbstractIdmAutomaticRoleDto automaticRole, AutomaticRoleRequestType type) {
		Assert.notNull(automaticRole);
		Assert.notNull(type);

		IdmAutomaticRoleRequestDto request = new IdmAutomaticRoleRequestDto();
		request.setAutomaticRole(automaticRole.getId());
		request.setName(automaticRole.getName());
		request.setRequestType(type);
		request.setOperation(RequestOperationType.REMOVE);
		request.setResult(new OperationResultDto.Builder(OperationState.CREATED).build());
		request = this.save(request);

		this.getIdmAutomaticRoleRequestService().startRequest(request.getId(), true);
	}
	
	@Override
	public IdmRoleTreeNodeDto createTreeAutomaticRole(IdmRoleTreeNodeDto automaticRole) {
		Assert.notNull(automaticRole);

		IdmAutomaticRoleRequestDto request = new IdmAutomaticRoleRequestDto();
		request.setName(automaticRole.getName());
		request.setTreeNode(automaticRole.getTreeNode());
		request.setRecursionType(automaticRole.getRecursionType());
		request.setRole(automaticRole.getRole());
		request.setRequestType(AutomaticRoleRequestType.TREE);
		request.setOperation(RequestOperationType.ADD);
		request.setResult(new OperationResultDto.Builder(OperationState.CREATED).build());
		request = this.save(request);

		IdmAutomaticRoleRequestDto result = this.getIdmAutomaticRoleRequestService().startRequest(request.getId(), true);
		if(RequestState.EXECUTED == result.getState()) {
			UUID createdAutomaticRoleId = result.getAutomaticRole();
			Assert.notNull(createdAutomaticRoleId);
			return automaticRoleTreeService.get(createdAutomaticRoleId);
		}
		if(RequestState.IN_PROGRESS == result.getState()) {
			throw new AcceptedException();
		}
		if(RequestState.EXCEPTION == result.getState()) {
			throw new AcceptedException();
		}
		return null;
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmAutomaticRoleRequest> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, IdmAutomaticRoleRequestFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		// Role
		if (filter.getRoleId() != null) {
			predicates.add(builder.equal(root.get(IdmAutomaticRoleRequest_.role).get(IdmRole_.id), filter.getRoleId()));
		}
		// Automatic role
		if (filter.getAutomaticRoleId() != null) {
			predicates.add(builder.equal(root.get(IdmAutomaticRoleRequest_.automaticRole).get(IdmAutomaticRole_.id),
					filter.getAutomaticRoleId()));
		}
		// Request type
		if (filter.getRequestType() != null) {
			predicates.add(builder.equal(root.get(IdmAutomaticRoleRequest_.requestType), filter.getRequestType()));
		}
		// Role code
		if (filter.getRole() != null) {
			predicates.add(builder.equal(root.get(IdmAutomaticRoleRequest_.role).get(IdmRole_.code), filter.getRole()));
		}
		// States
		List<RequestState> states = filter.getStates();
		if (!states.isEmpty()) {
			predicates.add(root.get(IdmAutomaticRoleRequest_.state).in(states));
		}
		return predicates;
	}

	private IdmRoleTreeNodeDto initTreeAutomaticRole(IdmAutomaticRoleRequestDto request,
			IdmRoleTreeNodeDto treeAutomaticRole) {
		treeAutomaticRole.setName(request.getName());
		treeAutomaticRole.setRole(request.getRole());
		treeAutomaticRole.setTreeNode(request.getTreeNode());
		treeAutomaticRole.setRecursionType(request.getRecursionType());
		// Fill the audit fields. We want to use original creator from request,
		// otherwise the creator from the last approver would be used.
		fillAuditFields(request, treeAutomaticRole);
		return treeAutomaticRole;
	}

	private IdmAutomaticRoleAttributeDto initAttributeAutomaticRole(IdmAutomaticRoleRequestDto request,
			IdmAutomaticRoleAttributeDto automaticRole) {
		automaticRole.setRole(request.getRole());
		automaticRole.setConcept(true);
		automaticRole.setName(request.getName());
		// Fill the audit fields. We want to use original creator from request,
		// otherwise the creator from the last approver would be used.
		fillAuditFields(request, automaticRole);
		return automaticRole;
	}

	/**
	 * Fill the audit fields. We want to use original creator from request,
	 * otherwise the creator from the last approver would be used.
	 * 
	 * @param request
	 * @param automaticRole
	 */
	private void fillAuditFields(IdmAutomaticRoleRequestDto request, AbstractDto automaticRole) {
		automaticRole.setOriginalCreator(request.getOriginalCreator());
		automaticRole.setOriginalModifier(request.getOriginalModifier());
	}

	/**
	 * Execute change of the request for attribute automatic role
	 * 
	 * @param request
	 * @param automaticRoleId
	 * @param ruleConcepts
	 */
	private void realizeAttributeRules(IdmAutomaticRoleRequestDto request, IdmAutomaticRoleAttributeDto automaticRole,
			List<IdmAutomaticRoleAttributeRuleRequestDto> ruleConcepts) {

		// Create new rule
		ruleConcepts.stream().filter(concept -> {
			return RequestOperationType.ADD == concept.getOperation();
		}).forEach(concept -> {
			IdmAutomaticRoleAttributeRuleDto rule = new IdmAutomaticRoleAttributeRuleDto();
			rule.setAutomaticRoleAttribute(automaticRole.getId());
			rule = automaticRoleRuleService.save(convertConceptRuleToRule(concept, rule));
			// Save created identity role id
			concept.setRule(rule.getId());
			automaticRoleRuleRequestService.save(concept);
		});
		// Update rule
		ruleConcepts.stream().filter(concept -> {
			return RequestOperationType.UPDATE == concept.getOperation();
		}).filter(concept -> {
			return concept.getRule() != null;
		}).forEach(concept -> {
			IdmAutomaticRoleAttributeRuleDto rule = automaticRoleRuleService.get(concept.getRule());
			rule = automaticRoleRuleService.save(convertConceptRuleToRule(concept, rule));
			// Save created identity role id
			concept.setRule(rule.getId());
			automaticRoleRuleRequestService.save(concept);
		});

		// Delete rule
		ruleConcepts.stream().filter(concept -> {
			return RequestOperationType.REMOVE == concept.getOperation();
		}).filter(concept -> {
			return concept.getRule() != null;
		}).forEach(concept -> {
			IdmAutomaticRoleAttributeRuleDto rule = automaticRoleRuleService.get(concept.getRule());
			if (rule != null) {
				concept.setRule(rule.getId());
				automaticRoleRuleRequestService.save(concept);
				// Finally delete of the rule
				automaticRoleRuleService.delete(rule);
			}
		});
	}

	/**
	 * Cancel unfinished workflow process for this automatic role.
	 * 
	 * @param dto
	 */
	private void cancelWF(IdmAutomaticRoleRequestDto dto) {
		if (!Strings.isNullOrEmpty(dto.getWfProcessId())) {
			WorkflowFilterDto filter = new WorkflowFilterDto();
			filter.setProcessInstanceId(dto.getWfProcessId());

			Collection<WorkflowProcessInstanceDto> resources = workflowProcessInstanceService.find(filter, null)
					.getContent();
			if (resources.isEmpty()) {
				// Process with this ID not exist ... maybe was ended
				return;
			}

			workflowProcessInstanceService.delete(dto.getWfProcessId(),
					"Role request use this WF, was deleted. This WF was deleted too.");
		}
	}

	private IdmAutomaticRoleAttributeRuleDto convertConceptRuleToRule(
			IdmAutomaticRoleAttributeRuleRequestDto conceptRule, IdmAutomaticRoleAttributeRuleDto rule) {
		if (conceptRule == null || rule == null) {
			return null;
		}
		rule.setAttributeName(conceptRule.getAttributeName());
		rule.setComparison(conceptRule.getComparison());
		rule.setFormAttribute(conceptRule.getFormAttribute());
		rule.setType(conceptRule.getType());
		rule.setValue(conceptRule.getValue());
		// Fill the audit fields. We want to use original creator from request,
		// otherwise the creator from the last approver would be used.
		rule.setOriginalCreator(conceptRule.getOriginalCreator());
		rule.setOriginalModifier(conceptRule.getOriginalModifier());
		return rule;
	}

	private IdmAutomaticRoleRequestService getIdmAutomaticRoleRequestService() {
		if (this.roleRequestService == null) {
			this.roleRequestService = applicationContext.getBean(IdmAutomaticRoleRequestService.class);
		}
		return this.roleRequestService;
	}

}
