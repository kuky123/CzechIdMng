package eu.bcvsolutions.idm.core.security.evaluator.role;

import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.model.entity.IdmRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmRequest_;
import eu.bcvsolutions.idm.core.security.api.domain.AbstractAuthentication;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.evaluator.AbstractAuthorizationEvaluator;

/**
 * Returns requests where is current user creator
 * 
 * @author svandav
 * @since 9.1.0
 *
 */
@Component(RequestByCreatorEvaluator.EVALUATOR_NAME)
@Description("Returns requests where is current user creator")
public class RequestByCreatorEvaluator extends AbstractAuthorizationEvaluator<IdmRequest> {

	public static final String EVALUATOR_NAME = "core-request-by-creator-evaluator";
	@Autowired
	private SecurityService securityService;

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

		if (securityService.getCurrentId() == null) {
			return null;
		}
		
		return builder.equal(root.get(IdmRequest_.creatorId), securityService.getCurrentId());
	}

	@Override
	public Set<String> getPermissions(IdmRequest entity, AuthorizationPolicy policy) {
		Set<String> permissions = super.getPermissions(entity, policy);
		if (entity == null || !securityService.isAuthenticated()) {
			return permissions;
		}
		if (securityService.getCurrentId() == null) {
			return permissions;
		}

		if (securityService.getCurrentId().equals(entity.getCreatorId())) {
			permissions.add(policy.getBasePermissions());
		}

		return permissions;
	}

	@Override
	public boolean supportsPermissions() {
		return true;
	}
}
