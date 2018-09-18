package eu.bcvsolutions.idm.core.security.evaluator.role;

import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.model.entity.IdmRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmRequest_;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRole_;
import eu.bcvsolutions.idm.core.security.api.domain.AbstractAuthentication;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizationManager;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.evaluator.AbstractAuthorizationEvaluator;

/**
 * Returns requests by rights on the request' owner (target object)
 * 
 * TODO: Only supports IdmRole as owner now!
 * 
 * @author svandav
 * @since 9.1.0
 *
 */
@Component(RequestByOwnerEvaluator.EVALUATOR_NAME)
@Description("Returns requests by rights on the request' owner (target object). Only supports IdmRole as owner now!")
public class RequestByOwnerEvaluator extends AbstractAuthorizationEvaluator<IdmRequest> {

	public static final String EVALUATOR_NAME = "core-request-by-owner-evaluator";
	//
	@Autowired
	private SecurityService securityService;
	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public String getName() {
		return EVALUATOR_NAME;
	}

	@Override
	public Predicate getPredicate(Root<IdmRequest> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			AuthorizationPolicy policy, BasePermission... permission) {
		AbstractAuthentication authentication = securityService.getAuthentication();
		if (authentication == null || authentication.getCurrentIdentity() == null) {
			return null;
		}
		
			// by IdmRole
			Subquery<IdmRole> roleSubquery = query.subquery(IdmRole.class);
			Root<IdmRole> subRoot = roleSubquery.from(IdmRole.class);
			Predicate rolePredicate = authorizationManager.getPredicate(subRoot, query, builder, permission);
			roleSubquery.select(subRoot);

			roleSubquery.where(builder.and(
					builder.equal(subRoot.get(IdmRole_.id), root.get(IdmRequest_.ownerId)), // correlation attr);
					rolePredicate));
		
			return builder.or(builder.exists(roleSubquery));
	}

	@Override
	public Set<String> getPermissions(IdmRequest entity, AuthorizationPolicy policy) {
		Set<String> permissions = super.getPermissions(entity, policy);
		if (entity == null || !securityService.isAuthenticated()) {
			return permissions;
		}

		// By IdmRole
		if (IdmRoleDto.class.getName().equals(entity.getOwnerType())) {
			IdmRoleFilter roleFilter = new IdmRoleFilter();
			roleFilter.setId(entity.getOwnerId());

			if (roleService.find(roleFilter, new PageRequest(0, 1)).getTotalElements() > 0) {
				permissions.addAll(policy.getPermissions());
				return permissions;
			}
		}
		//
		return permissions;
	}
	
	@Override
	public boolean supportsPermissions() {
		return false;
	}
}
