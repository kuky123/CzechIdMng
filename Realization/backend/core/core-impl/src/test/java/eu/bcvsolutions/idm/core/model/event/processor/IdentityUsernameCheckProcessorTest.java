package eu.bcvsolutions.idm.core.model.event.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Integration tests for check username duplicities
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class IdentityUsernameCheckProcessorTest extends AbstractIntegrationTest {

	@Autowired
	private IdmIdentityService identityService;

	@Before
	public void login() {
		super.loginAsAdmin();
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void testUniqueUsername() {
		String testUsername = "testUsernameUnique" + System.currentTimeMillis();

		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(testUsername);
		
		identityService.save(identity);
		
		IdmIdentityDto byUsername = identityService.getByUsername(testUsername);
		assertNotNull(byUsername);
		assertEquals(testUsername, byUsername.getUsername());
		identityService.delete(byUsername);
	}

	@Test
	public void testDuplcityUsername() {
		String testUsername = "testUsernameUnique" + System.currentTimeMillis();

		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(testUsername);
		
		identityService.save(identity);
		
		IdmIdentityDto byUsername = identityService.getByUsername(testUsername);
		assertNotNull(byUsername);
		assertEquals(testUsername, byUsername.getUsername());

		IdmIdentityDto identitySecond = new IdmIdentityDto();
		identitySecond.setUsername(testUsername);

		try {
			identityService.save(identity);
			fail("Save was success");
		} catch (ResultCodeException ex) {
			// message must contains duplcity username
			assertTrue(ex.getMessage().contains(testUsername));
		}catch (Exception e) {
			fail("Bad exception: " + e.getMessage());
		}
	}
}
