package eu.bcvsolutions.idm.core.security.evaluator.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Tests for {@link SelfIdentityRoleEvaluator}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.3.0
 *
 */
@Transactional
public class SelfIdentityRoleEvaluatorTest extends AbstractIntegrationTest {

	@Autowired
	private IdmIdentityRoleService identityRoleService;
	
	@Test
	public void testGreenLine() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto role = getHelper().createRole();
		IdmIdentityRoleDto identityRole = getHelper().createIdentityRole(identity, role);

		// try get identity role
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertTrue(identityRoles.isEmpty());	
		} finally {
			logout();
		}

		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.IDENTITYROLE,
				IdmIdentityRole.class,
				SelfIdentityRoleEvaluator.class,
				IdmBasePermission.READ);

		// get identity role after add authorization policy
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertFalse(identityRoles.isEmpty());
			assertEquals(1, identityRoles.size());
			IdmIdentityRoleDto foundedIdentityRoleDto = identityRoles.get(0);
			assertEquals(identityRole.getId(), foundedIdentityRoleDto.getId());
		} finally {
			logout();
		}
	}

	@Test
	public void testWithoutRead() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto role = getHelper().createRole();
		getHelper().createIdentityRole(identity, role);

		// try get identity role
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertTrue(identityRoles.isEmpty());	
		} finally {
			logout();
		}

		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.IDENTITYROLE,
				IdmIdentityRole.class,
				SelfIdentityRoleEvaluator.class,
				IdmBasePermission.DELETE);

		// get identity role after add authorization policy
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertTrue(identityRoles.isEmpty());
		} finally {
			logout();
		}
	}

	@Test
	public void testGreenLineCheckAnotherIdentityRole() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmIdentityDto identityTwo = getHelper().createIdentity();
		IdmRoleDto role = getHelper().createRole();
		IdmIdentityRoleDto identityRole = getHelper().createIdentityRole(identity, role);
		IdmIdentityRoleDto identityRoleTwo = getHelper().createIdentityRole(identityTwo, role);

		// try get identity role
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertTrue(identityRoles.isEmpty());	
		} finally {
			logout();
		}

		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.IDENTITYROLE,
				IdmIdentityRole.class,
				SelfIdentityRoleEvaluator.class,
				IdmBasePermission.READ);

		// get identity role after add authorization policy
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertFalse(identityRoles.isEmpty());
			assertEquals(1, identityRoles.size());
			IdmIdentityRoleDto foundedIdentityRoleDto = identityRoles.get(0);
			assertEquals(identityRole.getId(), foundedIdentityRoleDto.getId());
			
			try {
				identityRoleService.get(identityRoleTwo.getId(), IdmBasePermission.READ);
				fail();
			} catch (ForbiddenEntityException e) {
				// correct exception
			} catch (Exception e) {
				fail();
			}
		} finally {
			logout();
		}
	}

	@Test
	public void testGreenLineDelete() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto role = getHelper().createRole();
		IdmIdentityRoleDto identityRole = getHelper().createIdentityRole(identity, role);

		// try get identity role
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(null, IdmBasePermission.READ).getContent();
			assertTrue(identityRoles.isEmpty());	
		} finally {
			logout();
		}

		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.IDENTITYROLE,
				IdmIdentityRole.class,
				SelfIdentityRoleEvaluator.class,
				IdmBasePermission.READ);

		// get identity role after add authorization policy
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			try {
				identityRoleService.delete(identityRole, IdmBasePermission.DELETE);
				fail();
			} catch (ForbiddenEntityException e) {
				// correct exception
			} catch (Exception e) {
				fail();
			}
		} finally {
			logout();
		}
	}
}
