package eu.bcvsolutions.idm.core.workflow.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.task.IdentityLinkType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.base.Strings;

import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.rest.AbstractBaseDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowHistoricTaskInstanceDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessDefinitionDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowHistoricTaskInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessDefinitionService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowTaskDefinitionService;

/**
 * Default implementation of workflow process historic service
 * 
 * @author svandav
 *
 */
@SuppressWarnings("deprecation")
@Service
public class DefaultWorkflowHistoricTaskInstanceService extends AbstractBaseDtoService<WorkflowHistoricTaskInstanceDto, WorkflowFilterDto> implements WorkflowHistoricTaskInstanceService {

	@Autowired
	private HistoryService historyService;
	@Autowired
	private SecurityService securityService;
	@Autowired
	private WorkflowTaskDefinitionService workflowTaskDefinitionService;
	@Autowired
	private WorkflowProcessDefinitionService workflowProcessDefinitionService;

	@Override
	public Page<WorkflowHistoricTaskInstanceDto> find(Pageable pageable, BasePermission... permission) {
		return this.find(new WorkflowFilterDto(), pageable, permission);
	}
	
	@Override
	public Page<WorkflowHistoricTaskInstanceDto> find(WorkflowFilterDto filter, Pageable pageable,
			BasePermission... permission) {
		String processDefinitionId = filter.getProcessDefinitionId();
		String processInstanceId = filter.getProcessInstanceId();

		HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables();

		query.includeProcessVariables();
		
		if (filter.getId() != null){
			query.taskId(filter.getId().toString());
		}
		if (processInstanceId != null) {
			query.processInstanceId(processInstanceId);
		}
		if (processDefinitionId != null) {
			query.processDefinitionId(processDefinitionId);
		}
		if (filter.getProcessDefinitionKey() != null) {
			query.processDefinitionKey(filter.getProcessDefinitionKey());
		}
		// check security ... only assigneed user to task or process applicant or implementer can work with
		// historic task instance ... admin can see all historic tasks every time
		if(!securityService.isAdmin()) {
			// TODO Now we don't have detail for historic task. When we need detail, then we will need create different projection (detail can't be read by applicant)
			
			String loggedUserId = securityService.getCurrentId().toString();
			query.taskInvolvedUser(loggedUserId);
		}
		
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
		if (WorkflowHistoricTaskInstanceService.SORT_BY_CREATE_TIME.equals(fieldForSort)) {
			query.orderByTaskCreateTime();
		} else if (WorkflowHistoricTaskInstanceService.SORT_BY_END_TIME.equals(fieldForSort)) {
			query.orderByHistoricTaskInstanceEndTime();
		} else {
			query.orderByProcessDefinitionId();
			// there must be default order
			query.asc();
		}
		if (ascSort) {
			query.asc();
		}
		if (descSort) {
			query.desc();
		}
		long count = query.count();
		
		List<HistoricTaskInstance> processInstances = null;
		if (pageable == null) {
			processInstances = query.list();
		} else {
			processInstances = query.listPage((pageable.getPageNumber()) * pageable.getPageSize(),
					pageable.getPageSize());
		}
		List<WorkflowHistoricTaskInstanceDto> dtos = new ArrayList<>();

		if (processInstances != null) {
			for (HistoricTaskInstance instance : processInstances) {
				dtos.add(toResource(instance));
			}
		}

		return new PageImpl<WorkflowHistoricTaskInstanceDto>(dtos, pageable, count);
	}
	
	@Override
	public ResourcesWrapper<WorkflowHistoricTaskInstanceDto> search(WorkflowFilterDto filter) {
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
		
		Page<WorkflowHistoricTaskInstanceDto> page = this.find(filter, pageable);

		return new ResourcesWrapper<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
				filter.getPageNumber(), filter.getPageSize());
	}
	
	@Override
	public WorkflowHistoricTaskInstanceDto get(Serializable id, BasePermission... permission) {
		Assert.notNull(id);
		return this.get(String.valueOf(id));
	}

	@Override
	public WorkflowHistoricTaskInstanceDto get(String historicTaskInstanceId) {
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setId(UUID.fromString(historicTaskInstanceId));
		filter.setSortAsc(true);
		Collection<WorkflowHistoricTaskInstanceDto> resources = this.search(filter).getResources();
		return !resources.isEmpty() ? resources.iterator().next() : null;
	}
	
	@Override
	public WorkflowHistoricTaskInstanceDto getTaskByProcessId(String processId) {
		WorkflowFilterDto filter = new WorkflowFilterDto();
		filter.setProcessInstanceId(processId);
		filter.setSortDesc(true);
		List<WorkflowHistoricTaskInstanceDto> resources = (List<WorkflowHistoricTaskInstanceDto>) this.search(filter).getResources();
		return !resources.isEmpty() ? resources.get(resources.size()-1) : null;
	}

	private WorkflowHistoricTaskInstanceDto toResource(HistoricTaskInstance instance) {
		if (instance == null) {
			return null;
		}

		WorkflowHistoricTaskInstanceDto dto = new WorkflowHistoricTaskInstanceDto();
		// Not working ... variables are not local but global in process scope
		// ... may be logged level?
		// TODO can be slow
		if (instance.getTaskLocalVariables() != null) {
			if(instance.getTaskLocalVariables().containsKey(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_DECISION)) {
				dto.setCompleteTaskDecision((String)
						instance.getTaskLocalVariables().get(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_DECISION));
			}
			if(instance.getTaskLocalVariables().containsKey(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_MESSAGE)) {
				dto.setCompleteTaskMessage((String)
						instance.getTaskLocalVariables().get(WorkflowHistoricTaskInstanceService.TASK_COMPLETE_MESSAGE));
			}
		}
		dto.setId(instance.getId());
		dto.setName(instance.getName());
		dto.setProcessDefinitionId(instance.getProcessDefinitionId());
		dto.setTaskVariables(instance.getTaskLocalVariables());
		dto.setDeleteReason(instance.getDeleteReason());
		dto.setDurationInMillis(instance.getDurationInMillis());
		dto.setEndTime(instance.getEndTime());
		dto.setStartTime(instance.getStartTime());
		dto.setPriority(instance.getPriority());
		dto.setAssignee(instance.getAssignee());
		dto.setCreateTime(instance.getCreateTime());
		dto.setDueDate(instance.getDueDate());

		List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(instance.getId());
		if (identityLinks != null && !identityLinks.isEmpty()) {
			List<String> candicateUsers = new ArrayList<>();
			for	(HistoricIdentityLink identity : identityLinks) {
				if (IdentityLinkType.CANDIDATE.equals(identity.getType())) {
					candicateUsers.add(identity.getUserId());
				}
			}
			dto.setCandicateUsers(candicateUsers);
		}
		
		dto.setDefinition(workflowTaskDefinitionService.searchTaskDefinitionById(dto.getProcessDefinitionId(),
				instance.getTaskDefinitionKey()));

		if (!Strings.isNullOrEmpty(dto.getProcessDefinitionId())) {
			WorkflowProcessDefinitionDto processDefinition = workflowProcessDefinitionService
					.get(dto.getProcessDefinitionId());
			if (processDefinition != null) {
				dto.setProcessDefinitionKey(processDefinition.getKey());
			}
		}

		return dto;
	}

}
