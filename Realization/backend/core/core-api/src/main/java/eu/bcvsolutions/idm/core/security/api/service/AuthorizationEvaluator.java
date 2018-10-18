package eu.bcvsolutions.idm.core.security.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.core.Ordered;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.Configurable;
import eu.bcvsolutions.idm.core.security.api.domain.AuthorizationPolicy;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Authorization policy evaluator. Ensures data security.
 * 
 * @param <E> evaluated {@link Identifiable} type - evaluator is designed for domain type (superclasses could be used as wildcard). 
 * @author Radek Tomiška
 */
public interface AuthorizationEvaluator<E extends Identifiable> extends Ordered, Configurable {
	
	@Override
	default String getConfigurableType() {
		return "authorization-evaluator";
	}
	
	/**
	 *  bean name / unique identifier (spring bean name)
	 *  
	 * @return
	 */
	String getId();
	
	/**
	 * Returns entity class, which supports this processor
	 * 
	 * @return
	 */
	Class<E> getEntityClass();
	
	/**
	 * Returns true, when evaluator supports given authorizable type
	 * 
	 * @param authorizableType
	 * @return
	 */
	boolean supports(Class<?> authorizableType);
	
	/**
	 * Returns form parameter names for this task
	 * 
	 * @return
	 * @deprecated @since 8.2.0 use {@link #getFormDefinition()}
	 */
	@Deprecated 
	default List<String> getParameterNames() {
		return new ArrayList<>();
	}
	
	/**
	 * Returns configuration property names for this configurable object
	 */
	@Override
	default List<String> getPropertyNames() {
		return this.getParameterNames();
	}
	
	/**
	 * Returns jpa criteria predicate for given policy and all permissions, which can be used in queries - adds security on entities. 
	 * Predicate with "exist" subquery is recommended. 
	 * Could return {@code null}, if evaluator doesn't want to append a predicate. 
	 * 
	 * @param root evaluated {@link BaseEntity} type root 
	 * @param query
	 * @param builder
	 * @param policy
	 * @param permission permissions to evaluate (AND)
	 * @return predicate with "exists" subquery is recommended
	 */
	Predicate getPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder builder, AuthorizationPolicy policy, BasePermission... permission);
	
	/**
	 * Returns base permissions - what logged user could do with given authorizable object by given policy
	 * 
	 * @param authorizable object or {@code null}
	 * @param policy
	 * @return set of {@link BasePermission}s 
	 */
	Set<String> getPermissions(E authorizable, AuthorizationPolicy policy);
	
	/**
	 * Returns base authorities configured for given policy. Authorities are used as "what given identity" could do - without entity is defined.
	 * 
	 * @param identityId - identity (logged)
	 * @param policy
	 * @return
	 */
	Set<String> getAuthorities(UUID identityId, AuthorizationPolicy policy);
	
	/**
	 * Returns true, when currently logged user has all given permissions on given authorizable object by given policy.
	 * 
	 * @param authorizable
	 * @param policy
	 * @param permission permissions to evaluate (AND)
	 * @return
	 */
	boolean evaluate(E authorizable, AuthorizationPolicy policy, BasePermission... permission);
	
	/**
	 * Returns true, when evaluator supports base permissions from ui
	 * 
	 * @return
	 */
	boolean supportsPermissions();
}
