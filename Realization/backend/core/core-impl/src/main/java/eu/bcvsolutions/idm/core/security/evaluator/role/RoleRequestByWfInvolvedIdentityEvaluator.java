package eu.bcvsolutions.idm.core.security.evaluator.role;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.evaluator.AbstractAuthorizationEvaluator;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Currently logged user can work with role requests, when identity was involved to approving.
 * 
 * Search ({@link #getPermissions(IdmRoleRequest, AuthorizationPolicy)}) is not implemented.
 * 
 * @author Radek Tomiška
 */
@Component
@Description("Currently logged user can work with role requests, when identity was involved to approving.")
public class RoleRequestByWfInvolvedIdentityEvaluator extends AbstractAuthorizationEvaluator<IdmRoleRequest> {
	
	private final WorkflowProcessInstanceService processService;
	private final SecurityService securityService;
	
	@Autowired
	public RoleRequestByWfInvolvedIdentityEvaluator(
			SecurityService securityService,
			WorkflowProcessInstanceService processService) {
		Assert.notNull(securityService);
		Assert.notNull(processService);
		//
		this.securityService = securityService;
		this.processService = processService;
	}
	
	@Override
	public Set<String> getPermissions(IdmRoleRequest entity, AuthorizationPolicy policy) {
		Set<String> permissions = super.getPermissions(entity, policy);
		if (entity == null || !securityService.isAuthenticated() || entity.getWfProcessId() == null) {
			return permissions;
		}
		//
		// search process instance by role request - its returned, if currently logged identity was involved in wf
		WorkflowProcessInstanceDto processInstance = processService.get(entity.getWfProcessId());
		if (processInstance != null) {
			permissions.addAll(policy.getPermissions());
		}		
		return permissions;
	}
}
