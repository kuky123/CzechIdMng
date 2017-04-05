package eu.bcvsolutions.idm.core.security.evaluator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.bcvsolutions.idm.core.api.dto.IdentityDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.model.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleGuarantee;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.test.api.AbstractUnitTest;

/**
 * Authorization evaluator's unit tests
 * 
 * @author Radek Tomiška
 *
 */
public class RoleGuaranteeEvaluatorUnitTest extends AbstractUnitTest {
	
	@Mock
	private SecurityService securityService;
	
	private RoleGuaranteeEvaluator evaluator;
	
	@Before
	public void init() {
		evaluator = new RoleGuaranteeEvaluator(securityService);
	}
	
	@Test
	public void testSupportsType() {
		assertFalse(evaluator.supports(BaseEntity.class));
		assertFalse(evaluator.supports(AbstractEntity.class));
		assertTrue(evaluator.supports(IdmRole.class));
	}
	
	@Test
	public void testEvaluateFalse() {		
		IdmAuthorizationPolicyDto policy = new IdmAuthorizationPolicyDto();
		IdmRole authorizable = new IdmRole();
		IdmRoleGuarantee guarantee = new IdmRoleGuarantee();
		guarantee.setGuarantee(new IdmIdentity(UUID.randomUUID()));
		authorizable.getGuarantees().add(guarantee);
		policy.setPermissions(IdmBasePermission.READ);
		//
		when(securityService.getAuthentication()).thenReturn(new IdmJwtAuthentication(new IdentityDto(UUID.randomUUID(), null), null, null, null, null));
		//
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.READ));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.UPDATE));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.ADMIN));
	}
	
	@Test
	public void testEvaluateEmptyGuarantee() {		
		IdmAuthorizationPolicyDto policy = new IdmAuthorizationPolicyDto();
		IdmRole authorizable = new IdmRole();
		policy.setPermissions(IdmBasePermission.READ);
		//
		when(securityService.getAuthentication()).thenReturn(new IdmJwtAuthentication(new IdentityDto(UUID.randomUUID(), null), null, null, null, null));
		//
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.READ));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.UPDATE));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.ADMIN));
	}
	
	@Test
	public void testEvaluateReadOnly() {		
		IdmAuthorizationPolicyDto policy = new IdmAuthorizationPolicyDto();
		UUID uuid = UUID.randomUUID();
		IdmRole authorizable = new IdmRole();
		IdmRoleGuarantee guarantee = new IdmRoleGuarantee();
		guarantee.setGuarantee(new IdmIdentity(uuid));
		authorizable.getGuarantees().add(guarantee);
		policy.setPermissions(IdmBasePermission.READ);
		//
		when(securityService.getAuthentication()).thenReturn(new IdmJwtAuthentication(new IdentityDto(uuid, null), null, null, null, null));
		//
		assertTrue(evaluator.evaluate(policy, authorizable, IdmBasePermission.READ));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.UPDATE));
		assertFalse(evaluator.evaluate(policy, authorizable, IdmBasePermission.ADMIN));
	}
}
	