package eu.bcvsolutions.idm.core.workflow.service;

import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowHistoricTaskInstanceDto;

/**
 * Service for control workflow task instances.
 * 
 * @author svandav
 *
 */

@SuppressWarnings("deprecation")
public interface WorkflowHistoricTaskInstanceService extends ReadDtoService<WorkflowHistoricTaskInstanceDto, WorkflowFilterDto> {

	public static final String SORT_BY_CREATE_TIME = "createTime";
	public static final String SORT_BY_END_TIME = "endTime";
	public static final String TASK_COMPLETE_DECISION = "taskCompleteDecision";
	public static final String TASK_COMPLETE_MESSAGE = "taskCompleteMessage";

	/**
	 * Search historic tasks
	 * @param filter
	 * @return
	 */
	@Deprecated
	ResourcesWrapper<WorkflowHistoricTaskInstanceDto> search(WorkflowFilterDto filter);

	/**
	 * Search historic task by ID.
	 * @param historicTaskInstanceId
	 * @return
	 */
	WorkflowHistoricTaskInstanceDto get(String historicTaskInstanceId);

	/**
	 * Search historic task by process ID
	 * @param processId
	 * @return
	 */
	WorkflowHistoricTaskInstanceDto getTaskByProcessId(String processId);

}
