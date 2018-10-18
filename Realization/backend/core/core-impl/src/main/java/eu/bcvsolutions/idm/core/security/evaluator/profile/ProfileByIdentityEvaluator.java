package eu.bcvsolutions.idm.core.security.evaluator.profile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmProfile;
import eu.bcvsolutions.idm.core.model.entity.IdmProfile_;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizationManager;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;
import eu.bcvsolutions.idm.core.security.evaluator.AbstractTransitiveEvaluator;

/**
 * Permissions to profiles by identity
 * 
 * @author Radek Tomiška
 *
 */
@Component(ProfileByIdentityEvaluator.EVALUATOR_NAME)
@Description("Permissions to profiles by identity")
public class ProfileByIdentityEvaluator extends AbstractTransitiveEvaluator<IdmProfile> {

	public static final String EVALUATOR_NAME = "core-profile-by-identity-evaluator";
	public static final String PARAMETER_IDENTITY_READ = "identity-read"; // can read identity => can edit profile
	//
	@Autowired private AuthorizationManager authorizationManager;
	@Autowired private SecurityService securityService;
	
	@Override
	public String getName() {
		return EVALUATOR_NAME;
	}
	
	@Override
	protected Identifiable getOwner(IdmProfile entity) {
		return entity.getIdentity();
	}
	
	@Override
	protected Class<? extends Identifiable> getOwnerType() {
		return IdmIdentity.class;
	}
	
	@Override
	public Predicate getPredicate(Root<IdmProfile> root, CriteriaQuery<?> query, CriteriaBuilder builder, AuthorizationPolicy policy, BasePermission... permission) {
		if (!hasAuthority(securityService.getCurrentId(), policy, permission)) {
			return null;
		}
		// identity subquery
		Subquery<IdmIdentity> subquery = query.subquery(IdmIdentity.class);
		Root<IdmIdentity> subRoot = subquery.from(IdmIdentity.class);
		subquery.select(subRoot);		
		subquery.where(builder.and(
				isIdentityRead(policy)
				?
				authorizationManager.getPredicate(subRoot, query, builder, IdmBasePermission.READ)
				:
				authorizationManager.getPredicate(subRoot, query, builder, permission),
				builder.equal(root.get(IdmProfile_.identity), subRoot) // correlation attribute
				));
		//
		return builder.exists(subquery);
	}
	
	@Override
	public Set<String> getPermissions(IdmProfile entity, AuthorizationPolicy policy) {
		Set<String> permissions = super.getPermissions(entity, policy);
		if (entity == null) {
			return permissions;
		}
		//
		// identity read
		if (isIdentityRead(policy)) {
			if (!PermissionUtils.hasPermission(permissions, IdmBasePermission.READ)) {
				return permissions;
			}
			permissions.addAll(policy.getPermissions());
			// + create, update
			permissions.add(IdmBasePermission.CREATE.getName());
			permissions.add(IdmBasePermission.UPDATE.getName());
		}
		//
		return permissions;
	}
	
	@Override
	public Set<String> getAuthorities(UUID identityId, AuthorizationPolicy policy) {
		Set<String> authorities = super.getAuthorities(identityId, policy);
		//
		if (isIdentityRead(policy)) {
			if (PermissionUtils.hasPermission(authorities, IdmBasePermission.READ)) {
				authorities.add(IdmBasePermission.CREATE.getName());
				authorities.add(IdmBasePermission.UPDATE.getName());
			}
		}
		return authorities;
	}
	
	@Override
	public List<String> getPropertyNames() {
		List<String> parameters = super.getPropertyNames();
		parameters.add(PARAMETER_IDENTITY_READ);
		return parameters;
	}
	
	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		return Lists.newArrayList(
				new IdmFormAttributeDto(PARAMETER_IDENTITY_READ, PARAMETER_IDENTITY_READ, PersistentType.BOOLEAN)
				);
	}
	
	private boolean isIdentityRead(AuthorizationPolicy policy) {
		return policy.getEvaluatorProperties().getBoolean(PARAMETER_IDENTITY_READ);
	}
}
