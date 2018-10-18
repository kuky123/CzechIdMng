package eu.bcvsolutions.idm.core.security.evaluator.role;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmRequest;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Request by creator
 * 
 * @author Vít Švanda
 *
 */
@Transactional
public class RequestByCreatorEvaluatorTest extends AbstractIntegrationTest {

	@Autowired
	private RequestManager requestManager;

	@Test(expected = ForbiddenEntityException.class)
	public void testNotRightOnRequest() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto roleForRequest = getHelper().createRole();

		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			requestManager.createRequest(roleForRequest, IdmBasePermission.CREATE);
		} finally {
			logout();
		}
	}

	@Test
	public void testRightOnRequest() {
		IdmIdentityDto identity = getHelper().createIdentity();
		IdmRoleDto roleForRequest = getHelper().createRole();
		IdmRoleDto role = getHelper().createRole();
		getHelper().createRoleGuaranteeRole(role, role);
		getHelper().createIdentityRole(identity, role);

		getHelper().createAuthorizationPolicy(role.getId(), CoreGroupPermission.REQUEST, IdmRequest.class,
				RequestByCreatorEvaluator.class, IdmBasePermission.CREATE);

		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			requestManager.createRequest(roleForRequest, IdmBasePermission.CREATE);
		} finally {
			logout();
		}
	}
}
