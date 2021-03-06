package eu.bcvsolutions.idm.core.workflow.service;

import java.util.Map;
import java.util.Set;

import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowTaskInstanceDto;

/**
 * Service for control workflow task instances.
 * 
 * @author svandav
 *
 */
@SuppressWarnings("deprecation")
public interface WorkflowTaskInstanceService extends ReadDtoService<WorkflowTaskInstanceDto, WorkflowFilterDto> {

	public static final String WORKFLOW_DECISION = "decision";
	public static final String FORM_PROPERTY_TOOLTIP_KEY = "tooltip";
	public static final String FORM_PROPERTY_PLACEHOLDER_KEY = "placeholder";
	public static final String WORKFLOW_DECISION_APPROVE = "approve";
	public static final String WORKFLOW_DECISION_DISAPPROVE = "disapprove";

	/**
	 * Complete task
	 * 
	 * @param taskId
	 * @param decision
	 */
	void completeTask(String taskId, String decision);

	void completeTask(String taskId, String decision, Map<String, String> map);

	/**
	 * Search tasks by filter. Only candidate or assigned user can read task.
	 * 
	 * @param filter
	 * @return
	 */
	@Deprecated
	ResourcesWrapper<WorkflowTaskInstanceDto> search(WorkflowFilterDto filter);

	/**
	 * Complete task
	 * 
	 * @param taskId
	 * @param decision
	 * @param formData
	 * @param variables
	 */
	void completeTask(String taskId, String decision, Map<String, String> formData, Map<String, Object> variables);

	/**
	 * Get permissions for given {@link WorkflowTaskInstanceDto}
	 * 
	 * @param dto
	 * @return
	 */
	Set<String> getPermissions(WorkflowTaskInstanceDto dto);

	/**
	 * Complete given task
	 * 
	 * @param taskId
	 * @param decision
	 * @param formData
	 * @param variables
	 * @param permission
	 */
	void completeTask(String taskId, String decision, Map<String, String> formData, Map<String, Object> variables,
			BasePermission[] permission);
}
