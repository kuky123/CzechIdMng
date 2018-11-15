package eu.bcvsolutions.idm.core.workflow.service;

import java.util.Map;

import org.activiti.engine.runtime.ProcessInstance;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.api.script.ScriptEnabled;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
/**
 * Service for control workflow process instances.
 * 
 * @author svandav
 *
 */
@SuppressWarnings("deprecation")
public interface WorkflowProcessInstanceService extends ReadWriteDtoService<WorkflowProcessInstanceDto, WorkflowFilterDto> {
	
	final static String OBJECT_TYPE = "objectType";
	final static String OBJECT_IDENTIFIER = "objectIdentifier";
	final static String APPLICANT_USERNAME = "applicantUsername";
	final static String APPLICANT_IDENTIFIER = "applicantIdentifier";
	final static String ACTIVITI_SKIP_EXPRESSION_ENABLED = "_ACTIVITI_SKIP_EXPRESSION_ENABLED";
	final static String PROCESS_INSTANCE_ID = "processInstanceId"; // we need processInstanceId have accessible in subprocess (set in wf start listener)
	
	/**
	 * Implementer is user really start the process (For example implementer is administrator given permission to user)
	 */
	final static String IMPLEMENTER_IDENTIFIER = "implementerIdentifier";
	
	/**
	 * Name of the workflow variable which stores the operation result
	 * 
	 * of type {@link OperationResult}.
	 */
	final static String VARIABLE_OPERATION_RESULT = "operationResult";
	
	/**
	 * Name of the workflow variable which stores the input dto
	 * 
	 * of type {@link BaseDto}.
	 */
	final static String VARIABLE_DTO = "dto";
	
	/**
	 * Start new workflow process. To new process instance will be put variables objectType, objectIdentifier, applicant, implementer
	 * and add two involved users (applicant and implementer).
	 * 
	 * @param definitionKey
	 * @param objectType - Mostly type of applicant 
	 * @param applicant  - User readable applicant name
	 * @param objectIdentifier - Mostly identifier of applicant
	 * @param variables
	 * @return
	 */
	ProcessInstance startProcess(String definitionKey, String objectType, String applicant, String objectIdentifier,
			Map<String, Object> variables);

	/**
	 * Search process instance by filter
	 * @param filter
	 * @return
	 */
	@Deprecated
	ResourcesWrapper<WorkflowProcessInstanceDto> search(WorkflowFilterDto filter);

	/**
	 * Delete specific process instance. Only applicant or implementor can delete process instance.
	 * @param processInstanceId
	 * @param deleteReason
	 * @return
	 */
	WorkflowProcessInstanceDto delete(String processInstanceId, String deleteReason);

	/**
	 * Search process instance by ID process instance
	 * @param processInstanceId
	 * @return
	 */
	WorkflowProcessInstanceDto get(String processInstanceId);
	
	/**
	 * Search process instance by ID process instance and check permission
	 * 
	 * @param processInstanceId
	 * @param checkRight
	 * @return
	 */
	WorkflowProcessInstanceDto get(String processInstanceId, boolean checkRight);

	@Deprecated
	ResourcesWrapper<WorkflowProcessInstanceDto> searchInternal(WorkflowFilterDto filter, boolean checkRight);


}
