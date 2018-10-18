package eu.bcvsolutions.idm.core.security.evaluator.profile;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmProfileDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.service.IdmProfileService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmProfile;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Permission to profile
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class SelfProfileEvaluatorIntegrationTest extends AbstractIntegrationTest {

	@Autowired private IdmProfileService service;
	
	@Test
	public void testPolicy() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmIdentityDto identityOther = getHelper().createIdentity();
		IdmRoleDto role = getHelper().createRole();
		getHelper().createIdentityRole(identity, role);
		//
		List<IdmProfileDto> profiles = null;
		IdmProfileDto profile = getHelper().createProfile(identity);
		getHelper().createProfile(identityOther); // other
		//
		// check created identity doesn't have compositions
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			profiles = service.find(null, IdmBasePermission.READ).getContent();
			Assert.assertTrue(profiles.isEmpty());	
		} finally {
			logout();
		}
		//
		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.PROFILE,
				IdmProfile.class,
				SelfProfileEvaluator.class,
				IdmBasePermission.READ);
		//
		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			//
			// evaluate	access
			profiles = service.find(null, IdmBasePermission.READ).getContent();
			Assert.assertEquals(1, profiles.size());	
			Assert.assertEquals(profile.getId(), profiles.get(0).getId());
			//
			Set<String> permissions = service.getPermissions(profile);
			Assert.assertEquals(1, permissions.size());
			Assert.assertEquals(IdmBasePermission.READ.name(), permissions.iterator().next());
		} finally {
			logout();
		}
	}
}
