package eu.bcvsolutions.idm.example.repository.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for {@link UsernameIdentityFilter}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Transactional
public class UsernameIdentityFilterIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private UsernameIdentityFilter usernameIdentityFilter;
	
	@Test
	public void testFilteringFound() {
		String username = getHelper().createName();
		IdmIdentityDto identityOne = getHelper().createIdentity(username);
		IdmIdentityDto identityTwo = getHelper().createIdentity(getHelper().createName() + username + getHelper().createName());
		IdmIdentityDto identityThree = getHelper().createIdentity(getHelper().createName() + username + getHelper().createName());

		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setUsername(username);
		List<IdmIdentity> identities = usernameIdentityFilter.find(filter, null).getContent();

		assertEquals(3, identities.size());

		IdmIdentity identity = identities.stream().filter(ident -> ident.getId().equals(identityOne.getId())).findFirst().get();
		assertNotNull(identity);
		
		identity = identities.stream().filter(ident -> ident.getId().equals(identityTwo.getId())).findFirst().get();
		assertNotNull(identity);
		
		identity = identities.stream().filter(ident -> ident.getId().equals(identityThree.getId())).findFirst().get();
		assertNotNull(identity);
	}

	@Test
	public void testFilteringNotFound() {
		String username = "usernameValue" + System.currentTimeMillis();
		getHelper().createIdentity(username);
		getHelper().createIdentity("123" + username + getHelper().createName());
		getHelper().createIdentity(getHelper().createName() + username + getHelper().createName());

		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setUsername("username1Value"); // value is different than variable username
		List<IdmIdentity> identities = usernameIdentityFilter.find(filter, null).getContent();

		assertEquals(0, identities.size());
	}
}
