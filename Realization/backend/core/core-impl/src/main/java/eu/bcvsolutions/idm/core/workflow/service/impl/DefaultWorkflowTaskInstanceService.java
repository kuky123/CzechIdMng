package eu.bcvsolutions.idm.core.workflow.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.activiti.engine.FormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.rest.AbstractBaseDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.domain.formtype.AbstractComponentFormType;
import eu.bcvsolutions.idm.core.workflow.domain.formtype.DecisionFormType;
import eu.bcvsolutions.idm.core.workflow.domain.formtype.TaskHistoryFormType;
import eu.bcvsolutions.idm.core.workflow.model.dto.DecisionFormTypeDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.FormDataDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.IdentityLinkDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowHistoricTaskInstanceDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessDefinitionDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowTaskInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowHistoricTaskInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessDefinitionService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowTaskDefinitionService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowTaskInstanceService;

/**
 * Default workflow task instance service
 * 
 * @author svandav
 *
 */
@SuppressWarnings("deprecation")
@Service
public class DefaultWorkflowTaskInstanceService extends
		AbstractBaseDtoService<WorkflowTaskInstanceDto, WorkflowFilterDto> implements WorkflowTaskInstanceService {

	@Autowired
	private SecurityService securityService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private FormService formService;
	@Autowired
	private LookupService lookupService;
	@Autowired
	private WorkflowTaskDefinitionService workflowTaskDefinitionService;
	@Autowired
	private WorkflowProcessDefinitionService workflowProcessDefinitionService;
	@Autowired
	private WorkflowHistoricTaskInstanceService historicTaskInstanceService;

	@Override
	public Page<WorkflowTaskInstanceDto> find(WorkflowFilterDto filter, Pageable pageable,
			BasePermission... permission) {
	
		return internalSearch(filter, pageable, permission);
	}

	@Override
	public ResourcesWrapper<WorkflowTaskInstanceDto> search(WorkflowFilterDto filter) {
		Pageable pageable = null;
		// get pageable setting from filter - backward compatibility
		if (StringUtils.isNotEmpty(filter.getSortByFields())) {
			Sort sort = null;
			if (filter.isSortAsc()) {
				sort = new Sort(Direction.ASC, filter.getSortByFields());
			} else {
				sort = new Sort(Direction.DESC, filter.getSortByFields());
			}
			pageable = new PageRequest(filter.getPageNumber(), filter.getPageSize(), sort);
		} else {
			pageable = new PageRequest(filter.getPageNumber(), filter.getPageSize());
		}
		filter.setCandidateOrAssigned(securityService.getCurrentId().toString());
		Page<WorkflowTaskInstanceDto> page = this.find(filter, pageable, IdmBasePermission.READ);

		return new ResourcesWrapper<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
				filter.getPageNumber(), filter.getPageSize());
	}


	@Override
	public void completeTask(String taskId, String decision) {
		completeTask(taskId, decision, null);
	}

	@Override
	public void completeTask(String taskId, String decision, Map<String, String> formData) {
		completeTask(taskId, decision, null, null);
	}

	@Override
	public void completeTask(String taskId, String decision, Map<String, String> formData,
			Map<String, Object> variables) {
		BasePermission[] permission = {IdmBasePermission.UPDATE};
		this.completeTask(taskId, decision, formData, variables,  permission);
	}
	
	@Override
	public void completeTask(String taskId, String decision, Map<String, String> formData,
			Map<String, Object> variables, BasePermission[] permission) {
		String loggedUser = securityService.getCurrentId().toString();
		// Check if user can complete this task
		if (!canExecute(this.get(taskId, permission), permission)) {
			throw new ResultCodeException(CoreResultCode.FORBIDDEN,
					"You do not have permission for execute task with ID: %s !", ImmutableMap.of("taskId", taskId));
		}
		taskService.setAssignee(taskId, loggedUser);
		taskService.setVariables(taskId, variables);
		taskService.setVariableLocal(taskId, WorkflowHistoricTaskInstanceService.TASK_COMPLETE_DECISION, decision);
		if (formData.containsKey(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_MESSAGE)) {
			taskService.setVariableLocal(taskId, WorkflowHistoricTaskInstanceService.TASK_COMPLETE_MESSAGE,
					formData.get(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_MESSAGE));
		}
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(WorkflowTaskInstanceService.WORKFLOW_DECISION, decision);
		if (formData != null) {
			properties.putAll(formData);
		}
		formService.submitTaskFormData(taskId, properties);
	}

	/**
	 * Check if user can complete this task
	 * 
	 * @param taskId
	 * @return
	 */
	private boolean canExecute(WorkflowTaskInstanceDto task, BasePermission[] permission) {
		return permission == null || permission.length == 0  || this.getPermissions(task).contains(IdmBasePermission.EXECUTE.getName());
	}

	@Override
	public Set<String> getPermissions(Serializable id) {
		Assert.notNull(id);
		return this.getPermissions(this.get(id));
	}

	@Override
	public WorkflowTaskInstanceDto get(Serializable id, BasePermission... permission) {
		Assert.notNull(id);
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setId(UUID.fromString(String.valueOf(id)));
		List<WorkflowTaskInstanceDto> tasks = internalSearch(filter, null,  permission).getContent();

		return tasks.isEmpty() ? null : tasks.get(0);
	}

	@Override
	public Set<String> getPermissions(WorkflowTaskInstanceDto dto) {
		Assert.notNull(dto);
		//
		final Set<String> permissions = new HashSet<>();
		String loggedUserId = securityService.getCurrentId().toString();

		// TODO: user with admin permission can execute any tasks.
		// Set<GrantedAuthority> defaultAuthorities =
		// autorizationPolicyService.getDefaultAuthorities(securityService.getCurrentId());
		// defaultAuthorities.contains(CoreGroupPermission.WORKFLOW_TASK_ADMIN);
		//
		for (IdentityLinkDto identity : dto.getIdentityLinks()) {
			if ((identity.getType().equals(IdentityLinkType.ASSIGNEE)
					|| identity.getType().equals(IdentityLinkType.CANDIDATE))
					&& identity.getUserId().equals(loggedUserId)) {
				permissions.add(IdmBasePermission.EXECUTE.getName());
			}
		}
		return permissions;
	}

	private WorkflowTaskInstanceDto toResource(Task task, BasePermission[] permission) {
		if (task == null) {
			return null;
		}

		WorkflowTaskInstanceDto dto = new WorkflowTaskInstanceDto();
		dto.setId(task.getId());
		dto.setCreated(task.getCreateTime());
		dto.setFormKey(task.getFormKey());
		dto.setAssignee(task.getAssignee());
		dto.setName(task.getName());
		dto.setDescription(task.getDescription());
		dto.setProcessInstanceId(task.getProcessInstanceId());

		Map<String, Object> taksVariables = task.getTaskLocalVariables();
		Map<String, Object> processVariables = task.getProcessVariables();

		// Add applicant username to task dto (for easier work)
		if (processVariables != null
				&& processVariables.containsKey(WorkflowProcessInstanceService.APPLICANT_IDENTIFIER)) {
			dto.setApplicant(
					(String) processVariables.get(WorkflowProcessInstanceService.APPLICANT_IDENTIFIER).toString());
		}

		dto.setVariables(processVariables);
		convertToDtoVariables(dto, taksVariables);

		dto.setDefinition(workflowTaskDefinitionService.searchTaskDefinitionById(task.getProcessDefinitionId(),
				task.getTaskDefinitionKey()));

		if (!Strings.isNullOrEmpty(task.getProcessDefinitionId())) {
			WorkflowProcessDefinitionDto processDefinition = workflowProcessDefinitionService
					.get(task.getProcessDefinitionId());
			if (processDefinition != null) {
				dto.setProcessDefinitionKey(processDefinition.getKey());
			}
		}

		TaskFormData taskFormData = formService.getTaskFormData(task.getId());

		// Add form data (it means form properties and value from WF)
		List<FormProperty> formProperties = taskFormData.getFormProperties();

		// Search and add identity links to dto (It means all user
		// (assigned/candidates/group) for this task)
		List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
		if (identityLinks != null) {
			List<IdentityLinkDto> identityLinksDtos = new ArrayList<>();
			for (IdentityLink il : identityLinks) {
				identityLinksDtos.add(toResource(il));
			}
			dto.getIdentityLinks().addAll(identityLinksDtos);
		}

		// Check if the logged user can complete this task
		boolean canExecute = this.canExecute(dto, permission);
		if (formProperties != null && !formProperties.isEmpty()) {
			for (FormProperty property : formProperties) {
				resovleFormProperty(property, dto, canExecute);
			}
		}

		return dto;
	}

	/**
	 * Convert form property and add to result dto
	 * 
	 * @param property
	 * @param dto
	 * @param canExecute
	 */
	@SuppressWarnings("unchecked")
	private void resovleFormProperty(FormProperty property, WorkflowTaskInstanceDto dto, boolean canExecute) {
		FormType formType = property.getType();

		if (formType instanceof DecisionFormType) {
			// Decision buttons will be add only if logged user can execute/complete this
			// task
			if (!canExecute) {
				return;
			}
			DecisionFormTypeDto decisionDto = (DecisionFormTypeDto) ((DecisionFormType) formType)
					.convertFormValueToModelValue(property.getValue());
			if (decisionDto != null) {
				decisionDto.setId(property.getId());
				dto.getDecisions().add(decisionDto);
			}
		} else if (formType instanceof TaskHistoryFormType) {
			WorkflowFilterDto filterDto = new WorkflowFilterDto();
			filterDto.setProcessInstanceId(dto.getProcessInstanceId());
			List<WorkflowHistoricTaskInstanceDto> tasks = historicTaskInstanceService.find(filterDto, new PageRequest(0, 50)).getContent();

			List<WorkflowHistoricTaskInstanceDto> history = tasks.stream()
					.filter(workflowHistoricTaskInstanceDto -> workflowHistoricTaskInstanceDto.getEndTime() != null)
					.sorted((o1, o2) -> {
						if (o1.getEndTime().before(o2.getEndTime())) {
							return -1;
						} else if (o1.getEndTime().after(o2.getEndTime())) {
							return 1;
						}
						return 0;
					})
					.collect(Collectors.toList());
			dto.getFormData().add(historyToResource(property, history));
		} else if (formType instanceof AbstractFormType) {
			// To rest will be add only component form type marked as "exportable to rest".
			if (formType instanceof AbstractComponentFormType
					&& !((AbstractComponentFormType) formType).isExportableToRest()) {
				return;
			}
			Object values = ((AbstractFormType) formType).getInformation("values");
			if (values instanceof Map<?, ?>) {
				dto.getFormData().add(toResource(property, (Map<String, String>) values));
			} else {
				dto.getFormData().add(toResource(property, null));
			}
		}
	}

	private void convertToDtoVariables(WorkflowTaskInstanceDto dto, Map<String, Object> taksVariables) {
		if (taksVariables != null) {
			for (Entry<String, Object> entry : taksVariables.entrySet()) {
				Object value = entry.getValue();
				dto.getVariables().put(entry.getKey(), value == null ? null : value.toString());
			}
		}
	}

	private FormDataDto historyToResource(FormProperty property, List<WorkflowHistoricTaskInstanceDto> history) {
		FormDataDto dto = new FormDataDto();
		dto.setId(property.getId());
		dto.setName(property.getName());
		String value = "";
		try {
			value = new ObjectMapper().writeValueAsString(history);
		} catch (JsonProcessingException e) {
			throw new CoreException(e);
		}
		dto.setValue(value);
		dto.setType(property.getType().getName());
		dto.setReadable(property.isReadable());
		dto.setRequired(property.isRequired());
		dto.setWritable(property.isWritable());
		return dto;
	}

	private FormDataDto toResource(FormProperty property, Map<String, String> additionalInformations) {
		FormDataDto dto = new FormDataDto();
		dto.setId(property.getId());
		dto.setName(property.getName());
		dto.setValue(property.getValue());
		dto.setType(property.getType().getName());
		dto.setReadable(property.isReadable());
		dto.setRequired(property.isRequired());
		dto.setWritable(property.isWritable());
		// Add all additional informations (come from form properties - form
		// values)
		if (additionalInformations != null) {
			// extra add tooltip to dto
			if (additionalInformations.containsKey(WorkflowTaskInstanceService.FORM_PROPERTY_TOOLTIP_KEY)) {
				dto.setTooltip(additionalInformations.get(WorkflowTaskInstanceService.FORM_PROPERTY_TOOLTIP_KEY));
			}
			// extra add placeholder to dto
			if (additionalInformations.containsKey(WorkflowTaskInstanceService.FORM_PROPERTY_PLACEHOLDER_KEY)) {
				dto.setPlaceholder(
						additionalInformations.get(WorkflowTaskInstanceService.FORM_PROPERTY_PLACEHOLDER_KEY));
			}
			dto.getAdditionalInformations().putAll(additionalInformations);
		}
		return dto;
	}

	private IdentityLinkDto toResource(IdentityLink link) {
		if (link == null) {
			return null;
		}

		IdentityLinkDto dto = new IdentityLinkDto();
		dto.setGroupId(link.getGroupId());
		dto.setType(link.getType());
		dto.setUserId(link.getUserId());
		return dto;
	}

	/**
	 * Method return true if it is possible read all tasks.
	 * 
	 * @return
	 */
	private boolean canReadAllTask(BasePermission... permission) {
		// TODO: Implement check on permission (READ, UPDATE ...). Permissions are uses only for skip rights check now!
		if (permission == null || securityService.isAdmin() || securityService.hasAnyAuthority(CoreGroupPermission.WORKFLOW_TASK_ADMIN)) {
			return true;
		}
		return false;
	}

	private PageImpl<WorkflowTaskInstanceDto> internalSearch(WorkflowFilterDto filter, Pageable pageable, BasePermission... permission) {
		
		// if currently logged user can read all task continue
		if (!canReadAllTask(permission)) {
			// if user can't read all task check filter
			if (filter.getCandidateOrAssigned() == null) {
				filter.setCandidateOrAssigned(securityService.getCurrentId().toString());
			} else {
				IdmIdentityDto identity = (IdmIdentityDto) lookupService.lookupDto(IdmIdentityDto.class, filter.getCandidateOrAssigned());
				if (!identity.getId().equals(securityService.getCurrentId())) {
					throw new ResultCodeException(CoreResultCode.FORBIDDEN,
							"You do not have permission for access to all tasks!");
				}
			}
			// else is filled candidate and it is equals currently logged user
		}

		String processDefinitionId = filter.getProcessDefinitionId();
		Map<String, Object> equalsVariables = filter.getEqualsVariables();

		TaskQuery query = taskService.createTaskQuery();

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
		if (filter.getId() != null) {
			query.taskId(filter.getId().toString());
		}
		if (filter.getCreatedAfter() != null) {
			query.taskCreatedAfter(filter.getCreatedAfter().toDate());
		}
		if (filter.getCreatedBefore() != null) {
			query.taskCreatedBefore(filter.getCreatedBefore().toDate());
		}
		if (equalsVariables != null) {
			for (Entry<String, Object> entry : equalsVariables.entrySet()) {
				query.processVariableValueEquals(entry.getKey(), entry.getValue());
			}
		}

		if (filter.getCandidateOrAssigned() != null) {
			BaseDto dto = lookupService.lookupDto(IdmIdentityDto.class, filter.getCandidateOrAssigned());
			Assert.notNull(dto);
			query.taskCandidateOrAssigned(String.valueOf(dto.getId()));
		}

		query.orderByTaskCreateTime();
		query.desc();
		long count = query.count();

		// it's possible that pageable is null
		List<Task> tasks = null;
		if (pageable == null) {
			tasks = query.list();
		} else {
			tasks = query.listPage((pageable.getPageNumber()) * pageable.getPageSize(), pageable.getPageSize());
		}

		List<WorkflowTaskInstanceDto> dtos = new ArrayList<>();
		if (tasks != null) {
			for (Task task : tasks) {
				dtos.add(toResource(task, permission));
			}
		}
		return new PageImpl<WorkflowTaskInstanceDto>(dtos, pageable, count);
	}
}
