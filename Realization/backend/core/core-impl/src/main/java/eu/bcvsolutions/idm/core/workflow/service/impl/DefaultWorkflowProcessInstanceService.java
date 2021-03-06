package eu.bcvsolutions.idm.core.workflow.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcePage;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.rest.AbstractBaseDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowHistoricProcessInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Default implementation of workflow process instance service
 * 
 * @author svandav
 *
 */
@SuppressWarnings("deprecation")
@Service
public class DefaultWorkflowProcessInstanceService extends AbstractBaseDtoService<WorkflowProcessInstanceDto, WorkflowFilterDto> implements WorkflowProcessInstanceService {

	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private TaskService taskService;

	@Override
	@Transactional
	public ProcessInstance startProcess(String definitionKey, String objectType, String applicant,
			String objectIdentifier, Map<String, Object> variables) {
		Assert.hasText(definitionKey, "Definition key cannot be null!");
		UUID implementerId = securityService.getCurrentId();
 
		IdmIdentityDto applicantIdentity = null;
		if (applicant != null) {
			applicantIdentity = identityService.getByUsername(applicant);
		}
		ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder()
				.processDefinitionKey(definitionKey)//
				.addVariable(WorkflowProcessInstanceService.OBJECT_TYPE, objectType)
				.addVariable(WorkflowProcessInstanceService.ACTIVITI_SKIP_EXPRESSION_ENABLED, Boolean.TRUE) // Allow skip expression on user task
				.addVariable(WorkflowProcessInstanceService.OBJECT_IDENTIFIER, objectIdentifier)
				.addVariable(WorkflowProcessInstanceService.IMPLEMENTER_IDENTIFIER, implementerId == null ? null : implementerId.toString())
				.addVariable(WorkflowProcessInstanceService.APPLICANT_USERNAME, applicant)
				.addVariable(WorkflowProcessInstanceService.APPLICANT_IDENTIFIER,
						applicantIdentity != null ? applicantIdentity.getId() : null);
		if (variables != null) {
			for (Entry<String, Object> entry : variables.entrySet()) {
				builder.addVariable(entry.getKey(), entry.getValue());
			}
		}

		ProcessInstance instance = builder.start();
		if(!instance.isEnded()){
			// must explicit check null, else throw org.activiti.engine.ActivitiIllegalArgumentException: userId and groupId cannot both be null
			if (applicantIdentity != null) {
				// Set applicant as owner of process
				runtimeService.addUserIdentityLink(instance.getId(), applicantIdentity.getId().toString(), IdentityLinkType.OWNER);
			}
			if (implementerId != null) {
				// Set current logged user (implementer) as starter of process
				runtimeService.addUserIdentityLink(instance.getId(), implementerId.toString(), IdentityLinkType.STARTER);
			}
		}
		return instance;
	}
	
	@Override
	public WorkflowProcessInstanceDto get(Serializable id, BasePermission... permission) {
		Assert.notNull(id);
		return this.get(String.valueOf(id));
	}
	
	@Override
	public Page<WorkflowProcessInstanceDto> find(WorkflowFilterDto filter, Pageable pageable,
			BasePermission... permission) {
		// we must call original method search because is there check flag checkRight
		if (pageable != null) {
			filter.setPageNumber(pageable.getPageNumber());
			filter.setPageSize(pageable.getPageSize());
			//
			String fieldForSort = null;
			boolean ascSort = false;
			boolean descSort = false;
			if (pageable != null) {
				Sort sort = pageable.getSort();
				if (sort != null) {
					for (Order order : sort) {
						if (!StringUtils.isEmpty(order.getProperty())) {
							// TODO: now is implemented only one property sort 
							fieldForSort = order.getProperty();
							if (order.getDirection() == Direction.ASC) {
								ascSort = true;
							} else if (order.getDirection() == Direction.DESC) {
								descSort = true;
							}
							break;
						}
						
					}
				}
			}
			filter.setSortAsc(ascSort);
			filter.setSortDesc(descSort);
			filter.setSortByFields(fieldForSort);
		}
		ResourcesWrapper<WorkflowProcessInstanceDto> search = this.search(filter);
		//
		ResourcePage pages = search.getPage();
		List<WorkflowProcessInstanceDto> processes = (List<WorkflowProcessInstanceDto>) search.getResources();
		//
		return new PageImpl<WorkflowProcessInstanceDto>(processes, pageable, pages.getTotalElements());
	}
	
	@Override
	public Page<WorkflowProcessInstanceDto> find(Pageable pageable, BasePermission... permission) {
		return this.find(new WorkflowFilterDto(), pageable, permission);
	}

	@Override
	public ResourcesWrapper<WorkflowProcessInstanceDto> search(WorkflowFilterDto filter) {
		return searchInternal(filter, true);
	}
	
	@Override
	public ResourcesWrapper<WorkflowProcessInstanceDto> searchInternal(WorkflowFilterDto filter, boolean checkRight) {
		String processDefinitionId = filter.getProcessDefinitionId();

		Map<String, Object> equalsVariables = filter.getEqualsVariables();

		ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

		query.active();
		query.includeProcessVariables();

		if (processDefinitionId != null) {
			query.processDefinitionId(processDefinitionId);
		}
		if (filter.getProcessDefinitionKey() != null) {
			query.processDefinitionKey(filter.getProcessDefinitionKey());
		}
		if (filter.getProcessInstanceId() != null) {
			query.processInstanceId(filter.getProcessInstanceId());
		}
		if (filter.getCategory() != null) {
			// Find definitions with this category (use double sided like)
			// We have to find definitions first, because process instance can't
			// be find by category.
			ProcessDefinitionQuery queryDefinition = repositoryService.createProcessDefinitionQuery();
			queryDefinition.active();
			queryDefinition.latestVersion();
			queryDefinition.processDefinitionCategoryLike(filter.getCategory() + "%");
			List<ProcessDefinition> processDefinitions = queryDefinition.list();
			Set<String> processDefinitionKeys = new HashSet<>();
			processDefinitions.forEach(p -> processDefinitionKeys.add(p.getKey()));
			if (processDefinitionKeys.isEmpty()) {
				// We don`t have any definitions ... nothing must be returned
				processDefinitionKeys.add("-1");
			}
			query.processDefinitionKeys(processDefinitionKeys);
		}
		if (equalsVariables != null) {
			for (Entry<String, Object> entry : equalsVariables.entrySet()) {
				query.variableValueEquals(entry.getKey(), entry.getValue());
			}
		}
		// check security ... only involved user or applicant can work with
		// historic process instance
		// Applicant and Implementer is added to involved user after process
		// (subprocess) started. This modification allow not use OR clause.
		if(checkRight && !securityService.isAdmin()){
			query.involvedUser(securityService.getCurrentId().toString());
		}

		query.orderByProcessDefinitionId();
		query.desc();
		long count = query.count();
		List<ProcessInstance> processInstances = query.listPage((filter.getPageNumber()) * filter.getPageSize(),
				filter.getPageSize());
		List<WorkflowProcessInstanceDto> dtos = new ArrayList<>();

		if (processInstances != null) {
			for (ProcessInstance instance : processInstances) {
				dtos.add(toResource(instance));
			}
		}
		double totalPageDouble = ((double) count / filter.getPageSize());
		double totlaPageFlorred = Math.floor(totalPageDouble);
		long totalPage = 0;
		if (totalPageDouble > totlaPageFlorred) {
			totalPage = (long) (totlaPageFlorred + 1);
		}

		return new ResourcesWrapper<>(dtos, count, totalPage,
				filter.getPageNumber(), filter.getPageSize());
	}
	
	@Override
	public WorkflowProcessInstanceDto get(String processInstanceId) {
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setProcessInstanceId(processInstanceId);
		filter.setSortAsc(true);
		Collection<WorkflowProcessInstanceDto> resources = this.search(filter).getResources();
		return !resources.isEmpty() ? resources.iterator().next() : null;
	}
	
	@Override
	public WorkflowProcessInstanceDto get(String processInstanceId, boolean checkRight) {
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setProcessInstanceId(processInstanceId);
		filter.setSortAsc(true);
		Collection<WorkflowProcessInstanceDto> resources = this.searchInternal(filter, checkRight).getResources();
		return !resources.isEmpty() ? resources.iterator().next() : null;
	}
	
	@Override
	public void delete(WorkflowProcessInstanceDto dto, BasePermission... permission) {
		this.delete(dto.getId(), null);
	}
	
	@Override
	public void deleteById(Serializable id, BasePermission... permission) {
		this.delete(String.valueOf(id), null);
	}
	
	@Override
	public void deleteInternalById(Serializable id) {
		this.delete(String.valueOf(id), null);
	}
	
	@Override
	public void deleteInternal(WorkflowProcessInstanceDto dto) {
		this.delete(dto.getId(), null);
	}

	@Override
	public WorkflowProcessInstanceDto delete(String processInstanceId, String deleteReason) {
		if (processInstanceId == null) {
			return null;
		}
		if (deleteReason == null) {
			deleteReason = "Deleted by " + securityService.getUsername();
		}
		
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setProcessInstanceId(processInstanceId);
		
		Collection<WorkflowProcessInstanceDto> resources = this.searchInternal(filter, false).getResources();
		WorkflowProcessInstanceDto processInstanceToDelete = null;
		if(!resources.isEmpty()){
			processInstanceToDelete = resources.iterator().next();
		}

		if (processInstanceToDelete == null) {
			throw new ResultCodeException(CoreResultCode.FORBIDDEN,
					"You do not have permission for delete process instance with ID: %s !",
					ImmutableMap.of("processInstanceId", processInstanceId));
		}
		runtimeService.deleteProcessInstance(processInstanceToDelete.getProcessInstanceId(), deleteReason);

		return processInstanceToDelete;
	}

	private WorkflowProcessInstanceDto toResource(ProcessInstance instance) {
		if (instance == null) {
			return null;
		}

		String instanceName = instance.getName();
		// If we don't have process name, then we try variable with key
		// processInstanceName
		if (instanceName == null && instance.getProcessVariables() != null && instance.getProcessVariables()
				.containsKey(WorkflowHistoricProcessInstanceService.PROCESS_INSTANCE_NAME)) {
			instanceName = (String) instance.getProcessVariables()
					.get(WorkflowHistoricProcessInstanceService.PROCESS_INSTANCE_NAME);
		}
		if (instanceName == null || instanceName.isEmpty()) {
			instanceName = instance.getProcessDefinitionName();
		}

		WorkflowProcessInstanceDto dto = new WorkflowProcessInstanceDto();
		dto.setId(instance.getId());
		dto.setActivityId(instance.getActivityId());
		dto.setBusinessKey(instance.getBusinessKey());
		dto.setName(instanceName);
		dto.setProcessDefinitionId(instance.getProcessDefinitionId());
		dto.setProcessDefinitionKey(instance.getProcessDefinitionKey());
		dto.setProcessDefinitionName(instance.getProcessDefinitionName());
		dto.setProcessVariables(instance.getProcessVariables());
		dto.setEnded(instance.isEnded());
		dto.setProcessInstanceId(instance.getProcessInstanceId());
		// Add current activity name and documentation
		BpmnModel model = repositoryService.getBpmnModel(instance.getProcessDefinitionId());
		
		for (FlowElement element : model.getMainProcess().getFlowElements()) {
			if (element.getId().equals(instance.getActivityId())) {
				dto.setCurrentActivityName(element.getName());
				dto.setCurrentActivityDocumentation(element.getDocumentation());
			}
		}
		
		Task task = taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).singleResult();
		
		if (task != null) {
			List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
			if (identityLinks != null && !identityLinks.isEmpty()) {
				List<String> candicateUsers = new ArrayList<>();
				for	(HistoricIdentityLink identity : identityLinks) {
					if (IdentityLinkType.CANDIDATE.equals(identity.getType())) {
						candicateUsers.add(identity.getUserId());
					}
				}
				dto.setCandicateUsers(candicateUsers);
			}
		}
		
		return dto;
	}
}
