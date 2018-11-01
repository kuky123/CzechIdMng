package eu.bcvsolutions.idm.core.workflow.rest;

import java.io.Serializable;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.workflow.model.dto.FormDataWrapperDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowTaskInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowTaskInstanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Rest controller for workflow instance tasks
 * 
 * TODO: secure endpoints
 * 
 * @author svandav
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/workflow-tasks")
@Api(
		value = WorkflowTaskInstanceController.TAG,  
		tags = { WorkflowTaskInstanceController.TAG }, 
		description = "Running WF tasks",
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class WorkflowTaskInstanceController extends AbstractReadDtoController<WorkflowTaskInstanceDto, WorkflowFilterDto> {


	protected static final String TAG = "Workflow - task instances";
	//
	private final WorkflowTaskInstanceService workflowTaskInstanceService;

	@Autowired
	public WorkflowTaskInstanceController(
			WorkflowTaskInstanceService entityService) {
		super(entityService);
		//
		this.workflowTaskInstanceService = entityService;
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.WORKFLOW_TASK_READ + "')")
	@ApiOperation(
			value = "Search task instances", 
			nickname = "searchTaskInstances", 
			tags = { WorkflowTaskInstanceController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.WORKFLOW_TASK_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.WORKFLOW_TASK_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{backendId}")
	@ApiOperation(
			value = "Historic task instance detail", 
			nickname = "getHistoricTaskInstance", 
			response = WorkflowTaskInstanceDto.class, 
			tags = { WorkflowTaskInstanceController.TAG })
	public ResponseEntity<?> get(
			@ApiParam(value = "Task instance id.", required = true)
			@PathVariable String backendId) {
		return super.get(backendId);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{backendId}/complete")
	@ApiOperation(
			value = "Complete task instance", 
			nickname = "completeTaskInstance",
			tags = { WorkflowTaskInstanceController.TAG },
			notes = "Complete task with given decision.")
	public void completeTask(
			@ApiParam(value = "Task instance id.", required = true)
			@PathVariable String backendId, 
			@ApiParam(value = "Complete decision, variables etc.", required = true)
			@RequestBody FormDataWrapperDto formData) {
		workflowTaskInstanceService.completeTask(backendId, formData.getDecision(), formData.getFormData(), formData.getVariables());
		// 
		// TODO: no content should be returned
		// return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{backendId}/permissions")
	@ApiOperation(
			value = "Historic task instance detail", 
			nickname = "getHistoricTaskInstance", 
			response = WorkflowTaskInstanceDto.class, 
			tags = { WorkflowTaskInstanceController.TAG })
	public Set<String> getPermissions(
			@ApiParam(value = "Task instance id.", required = true)
			@PathVariable String backendId) {
		WorkflowTaskInstanceDto taskInstanceDto = workflowTaskInstanceService.get(backendId);
		return workflowTaskInstanceService.getPermissions(taskInstanceDto);
	}
	
	@Override
	// We need override that method (#1320). Parent using lookup service for get DTO. Lookup
	// service get DTO without permissions and after that check permission on READ.
	// Without permissions is loaded Task with all buttons (canExecute is true). We
	// need call direct service with Permission.READ!
	public WorkflowTaskInstanceDto getDto(Serializable backendId) {
		return getService().get(backendId, IdmBasePermission.READ);
	}
}
