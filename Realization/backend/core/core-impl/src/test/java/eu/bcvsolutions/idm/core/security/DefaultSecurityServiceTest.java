package eu.bcvsolutions.idm.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.security.api.domain.AbstractAuthentication;
import eu.bcvsolutions.idm.core.security.api.domain.DefaultGrantedAuthority;
import eu.bcvsolutions.idm.core.security.api.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.service.impl.DefaultSecurityService;
import eu.bcvsolutions.idm.test.api.AbstractUnitTest;

/**
 * Test for {@link DefaultSecurityService}
 * 
 * @author Radek Tomiška 
 *
 */
public class DefaultSecurityServiceTest extends AbstractUnitTest {
	
	private static final String CURRENT_USERNAME = "current_username";
	private static final String ORIGINAL_USERNAME = "original_username";
	private static final String TEST_AUTHORITY = "TEST_AUTHORITY";
	private static final Collection<GrantedAuthority> AUTHORITIES = Arrays.asList(new DefaultGrantedAuthority(TEST_AUTHORITY));	
	private static final IdmJwtAuthentication AUTHENTICATION = new IdmJwtAuthentication(
			new IdmIdentityDto(CURRENT_USERNAME), 
			new IdmIdentityDto(ORIGINAL_USERNAME), 
			DateTime.now(),
			DateTime.now(),
			AUTHORITIES,
			"test");
	
	@Mock
	private SecurityContext securityContext;
	@Mock
	private RoleHierarchy authorityHierarchy; 
	
	private DefaultSecurityService defaultSecurityService;

	@Before
	public void init() {
		SecurityContextHolder.setContext(securityContext);
		defaultSecurityService = new DefaultSecurityService(authorityHierarchy);
	}
	
	@After
	public void logout() {
		SecurityContextHolder.clearContext();
	}
	
	@Test
	public void testIsLoggedIn() {
		// setup static authentication
		when(securityContext.getAuthentication()).thenReturn(AUTHENTICATION);
		//
		AbstractAuthentication result = defaultSecurityService.getAuthentication();
		//
		assertEquals(result.getCurrentUsername(), AUTHENTICATION.getCurrentUsername());
		assertEquals(result.getOriginalUsername(), AUTHENTICATION.getOriginalUsername());
		assertEquals(result.getAuthorities(), AUTHENTICATION.getAuthorities());
		assertEquals(result.getDetails(), AUTHENTICATION.getDetails());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testHasTestAuthority() {
		// setup static authentication
		when(securityContext.getAuthentication()).thenReturn(AUTHENTICATION);
		when(authorityHierarchy.getReachableGrantedAuthorities(any())).thenReturn((Collection) AUTHORITIES);
		//
		boolean result = defaultSecurityService.hasAnyAuthority(TEST_AUTHORITY);
		//
		assertTrue(result);
	}	
}
