package eu.bcvsolutions.idm.core.security.evaluator.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestFilter;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.service.IdmRequestService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.evaluator.UuidEvaluator;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Request by owner target DTO
 * 
 * @author Vít Švanda
 *
 */
@Transactional
public class RequestByOwnerEvaluatorTest extends AbstractIntegrationTest {

	@Autowired
	private RequestManager requestManager;
	@Autowired
	private IdmRequestService requestService;

	@Test(expected = ForbiddenEntityException.class)
	public void testNotRightOnRequest() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto roleForRequest = getHelper().createRole();
		IdmRequestDto request = requestManager.createRequest(roleForRequest);

		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			assertNull(requestService.get(request.getId(), IdmBasePermission.READ));
		} finally {
			logout();
		}
	}

	@Test
	public void testRightOnRequest() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto roleForRequest = getHelper().createRole();
		IdmRequestDto requestWithOwneredRole = requestManager.createRequest(roleForRequest);
		IdmRoleDto roleForRequestWithoutRight = getHelper().createRole();
		IdmRequestDto requestWithoutOwneredRole = requestManager.createRequest(roleForRequestWithoutRight);
		IdmRoleDto role = getHelper().createRole();
		getHelper().createRoleGuaranteeRole(role, role);
		getHelper().createIdentityRole(identity, role);

		getHelper().createAuthorizationPolicy(role.getId(), CoreGroupPermission.REQUEST, IdmRequest.class,
				RequestByOwnerEvaluator.class, IdmBasePermission.READ);

		// User will have rights on the roleForRequest
		ConfigurationMap properties = new ConfigurationMap();
		properties.put(UuidEvaluator.PARAMETER_UUID, roleForRequest.getId());
		getHelper().createAuthorizationPolicy(role.getId(), CoreGroupPermission.ROLE, IdmRole.class,
				UuidEvaluator.class, properties, IdmBasePermission.READ);

		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			try {
				requestService.get(requestWithoutOwneredRole.getId(), IdmBasePermission.READ);
				fail();
			} catch (ForbiddenEntityException ex) {
				// It is OK
			}
			assertNotNull(requestService.get(requestWithOwneredRole.getId(), IdmBasePermission.READ));
			IdmRequestFilter requestFilter = new IdmRequestFilter();
			// We do not have right to that request
			requestFilter.setId(requestWithoutOwneredRole.getId());
			assertEquals(0, requestService.find(requestFilter, null, IdmBasePermission.READ).getContent().size());
			// We have right to that request
			requestFilter.setId(requestWithOwneredRole.getId());
			assertEquals(1, requestService.find(requestFilter, null, IdmBasePermission.READ).getContent().size());
		} finally {
			logout();
		}
	}
}
