package eu.bcvsolutions.idm.core.workflow.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import eu.bcvsolutions.idm.core.model.domain.ResourceWrapper;
import eu.bcvsolutions.idm.core.model.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.workflow.domain.WorkflowDefinitionAssembler;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowDeploymentDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessDefinitionDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowDeploymentService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessDefinitionService;

/**
 * Rest controller for workflow task instance
 * 
 * @author svandav
 *
 */
@RestController
@RequestMapping(value = "/api/workflow/definitions/")
public class WorkflowDefinitionController {

	@Autowired
	private WorkflowDeploymentService deploymentService;

	@Autowired
	private WorkflowProcessDefinitionService definitionService;

	/**
	 * Upload new deployment to Activiti engine
	 * 
	 * @param name
	 * @param fileName
	 * @param data
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(method = RequestMethod.POST)
	public ResourceWrapper<WorkflowDeploymentDto> create(String name, String fileName, MultipartFile data)
			throws IOException {
		
		return new ResourceWrapper<>(deploymentService.create(name, fileName, data.getInputStream()));
	}

	/**
	 * Search all last version and active process definitions
	 * 
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<ResourcesWrapper<ResourceWrapper<WorkflowProcessDefinitionDto>>> findAllProcessDefinitions() {
		List<WorkflowProcessDefinitionDto> definitions = definitionService.findAllProcessDefinitions();
		List<ResourceWrapper<WorkflowProcessDefinitionDto>> wrappers = new ArrayList<>();
		WorkflowDefinitionAssembler assembler = new WorkflowDefinitionAssembler();
	
		for (WorkflowProcessDefinitionDto definition : definitions) {
			wrappers.add(assembler.toResource(definition));
		}
		ResourcesWrapper<ResourceWrapper<WorkflowProcessDefinitionDto>> resources = new ResourcesWrapper<ResourceWrapper<WorkflowProcessDefinitionDto>>(wrappers, definitions.size());
		return new ResponseEntity<ResourcesWrapper<ResourceWrapper<WorkflowProcessDefinitionDto>>>(resources, HttpStatus.OK);
	}

	/**
	 * Search last version process by key
	 * @param definitionId
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "{definitionId}")
	public ResponseEntity<ResourceWrapper<WorkflowProcessDefinitionDto>> get(@PathVariable String definitionId) {
		WorkflowProcessDefinitionDto definition = definitionService.get(definitionId);
		WorkflowDefinitionAssembler assembler = new WorkflowDefinitionAssembler();
		ResourceWrapper<WorkflowProcessDefinitionDto> resource = assembler.toResource(definition);
		
		return new ResponseEntity<ResourceWrapper<WorkflowProcessDefinitionDto>>(resource, HttpStatus.OK);
	}

}
