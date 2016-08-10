package eu.bcvsolutions.idm.security;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import eu.bcvsolutions.idm.core.AbstractUnitTest;
import eu.bcvsolutions.idm.core.security.domain.AbstractAuthentication;
import eu.bcvsolutions.idm.core.security.domain.DefaultGrantedAuthority;
import eu.bcvsolutions.idm.core.security.domain.IdmJwtAuthentication;
import eu.bcvsolutions.idm.core.security.service.impl.DefaultSecurityService;

/**
 * Test for {@link DefaultSecurityService}
 * 
 * @author Radek Tomiška <radek.tomiska@bcvsolutions.eu>
 *
 */
public class DefaultSecurityServiceTest extends AbstractUnitTest {
	
	private static final String CURRENT_USERNAME = "current_username";
	private static final String ORIGINAL_USERNAME = "original_username";
	private static final String TEST_AUTHORITY = "TEST_AUTHORITY";
	private static final Collection<DefaultGrantedAuthority> AUTHORITIES = Arrays.asList(new DefaultGrantedAuthority(TEST_AUTHORITY));	
	private static final IdmJwtAuthentication AUTHENTICATION = new IdmJwtAuthentication(CURRENT_USERNAME, ORIGINAL_USERNAME, new Date(), AUTHORITIES);
	
	@Mock
	private SecurityContext securityContext;
	
	@InjectMocks
	private DefaultSecurityService defaultSecurityService = new DefaultSecurityService();

	@Before
	public void init() {
		SecurityContextHolder.setContext(securityContext);
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
		//
		verify(securityContext).getAuthentication();
	}
	
	@Test
	public void testHasTestAuthority() {
		// setup static authentication
		when(securityContext.getAuthentication()).thenReturn(AUTHENTICATION);
		//
		boolean result = defaultSecurityService.hasAnyAuthority(TEST_AUTHORITY);
		//
		assertTrue(result);
		//
		verify(securityContext).getAuthentication();
	}	
}
