package eu.bcvsolutions.idm.core.security.evaluator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizationEvaluator;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Abstract authorization evaluator template.
 * 
 * @author Radek Tomiška
 *
 * @param <E> evaluated {@link Identifiable} type - evaluator is designed for one domain type. 
 */
public abstract class AbstractAuthorizationEvaluator<E extends Identifiable> implements 
		AuthorizationEvaluator<E>,
		BeanNameAware {

	private final Class<E> entityClass;
	private String beanName; // spring bean name - used as id
	//
	@Autowired(required = false)
	private ConfigurationService configurationService; // optional internal dependency - checks for processor is enabled

	@SuppressWarnings({ "unchecked" })
	public AbstractAuthorizationEvaluator() {
		this.entityClass = (Class<E>) GenericTypeResolver.resolveTypeArgument(getClass(), AuthorizationEvaluator.class);
	}
	
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
	
	@Override
	public String getId() {
		return beanName;
	}

	@Override
	public Class<E> getEntityClass() {
		return entityClass;
	}

	/**
	 * Could be used for {@link #evaluate(BaseEntity, BasePermission)} ordering,
	 * when more evaluators supports the same entity type (if the first one
	 * disapprove, then we dont need to continue etc.).
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public boolean supports(Class<?> authorizableType) {
		Assert.notNull(authorizableType);
		//
		return entityClass.isAssignableFrom(authorizableType);
	}
	
	/**
	 * Returns null as default. Supposed to
	 * be overriden.
	 */
	@Override
	public Predicate getPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder builder, AuthorizationPolicy policy, BasePermission... permission) {
		return null;
	}

	/**
	 * Returns empty set - no data will be available. Supposed to be overriden.
	 */
	@Override
	public Set<String> getPermissions(E authorizable, AuthorizationPolicy policy) {
		return new HashSet<>();
	}
	
	/**
	 * Returns configured policy permissions
	 */
	@Override
	public Set<String> getAuthorities(UUID identityId, AuthorizationPolicy policy) {
		Assert.notNull(policy);
		//
		return policy.getPermissions();
	}

	/**
	 * Supposed to be overriden for orderable optimalizations.
	 */
	@Override
	public boolean evaluate(E authorizable, AuthorizationPolicy policy, BasePermission... permission) {
		Assert.notEmpty(permission);
		//		
		return PermissionUtils.hasPermission(getPermissions(authorizable, policy), permission);
	}
	
	@Override
	public boolean supportsPermissions() {
		return true;
	}
	
	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}
	
	/**
	 * Returns true, when policy has all given permissions
	 * 
	 * @param policy
	 * @param permission permissions to evaluate (AND)
	 * @return
	 */
	protected boolean hasPermission(AuthorizationPolicy policy, BasePermission... permission) {
		Assert.notNull(permission);
		//
		return PermissionUtils.hasPermission(policy.getPermissions(), permission);
	}
	
	/**
	 * Returns true, when policy has all given authority
	 * 
	 * @param policy
	 * @param authority authority to evaluate (AND)
	 * @return
	 */
	protected boolean hasAuthority(UUID identityId, AuthorizationPolicy policy, BasePermission... authority) {
		Assert.notNull(authority);
		Set<String> authorities = getAuthorities(identityId, policy);
		//
		return authorities.contains(IdmBasePermission.ADMIN.getName())
				|| hasAuthority(authorities, authority);
	}
	
	/**
	 * Returns true, when authorities have all given authority
	 * 
	 * @param authorities
	 * @param authority authority to evaluate (AND)
	 * @return
	 */
	protected boolean hasAuthority(Collection<String> authorities, BasePermission... authority) {
		return authorities.containsAll(Arrays.stream(authority).map(Object::toString).collect(Collectors.toList()));
	}
}
