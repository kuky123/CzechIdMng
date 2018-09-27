package eu.bcvsolutions.idm.core.security.evaluator.role;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleCatalogueRole;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Permission to role catalogue relations by relation's role
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class RoleCatalogueRoleByRoleEvaluatorIntegrationTest extends AbstractIntegrationTest {

	@Autowired private IdmRoleCatalogueRoleService service;
	@Autowired private IdmRoleService roleService;
	
	@Test
	public void canReadByRole() {
		IdmIdentityDto identity = getHelper().createIdentity();
		List<IdmRoleCatalogueRoleDto> roleCatalougueRoles = null;
		IdmRoleDto role = getHelper().createRole();
		IdmRoleCatalogueRoleDto roleCatalogueRole;
		try {
			getHelper().loginAdmin();
			IdmRoleDto roleOther = getHelper().createRole();
			IdmRoleCatalogueDto roleCatalogueOne = getHelper().createRoleCatalogue();
			roleCatalogueRole = getHelper().createRoleCatalogueRole(role, roleCatalogueOne);
			getHelper().createRoleCatalogueRole(roleOther, roleCatalogueOne); // other - without access
			getHelper().createIdentityRole(identity, role);
			getHelper().createUuidPolicy(role.getId(), role.getId(), IdmBasePermission.READ);
		} finally {
			logout();
		}
		//
		// check created identity doesn't have compositions
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			Assert.assertEquals(role.getId(), roleService.get(role.getId(), IdmBasePermission.READ).getId());
			roleCatalougueRoles = service.find(null, IdmBasePermission.READ).getContent();
			Assert.assertTrue(roleCatalougueRoles.isEmpty());	
		} finally {
			logout();
		}
		//
		// create authorization policy - assign to role
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.ROLECATALOGUEROLE,
				IdmRoleCatalogueRole.class,
				RoleCatalogueRoleByRoleEvaluator.class);
		//
		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			//
			// evaluate	access
			roleCatalougueRoles = service.find(null, IdmBasePermission.READ).getContent();
			Assert.assertEquals(1, roleCatalougueRoles.size());	
			Assert.assertEquals(roleCatalogueRole.getId(), roleCatalougueRoles.get(0).getId());
			//
			Set<String> permissions = service.getPermissions(roleCatalogueRole);
			Assert.assertEquals(1, permissions.size());
			Assert.assertEquals(IdmBasePermission.READ.name(), permissions.iterator().next());
		} finally {
			logout();
		}
		//
		getHelper().createUuidPolicy(role.getId(), role.getId(), IdmBasePermission.UPDATE);
		//
		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			//
			Set<String> permissions = service.getPermissions(roleCatalogueRole);
			Assert.assertEquals(4, permissions.size());
			Assert.assertTrue(permissions.stream().anyMatch(p -> p.equals(IdmBasePermission.READ.name())));
			Assert.assertTrue(permissions.stream().anyMatch(p -> p.equals(IdmBasePermission.UPDATE.name())));
			Assert.assertTrue(permissions.stream().anyMatch(p -> p.equals(IdmBasePermission.CREATE.name())));
			Assert.assertTrue(permissions.stream().anyMatch(p -> p.equals(IdmBasePermission.DELETE.name())));
		} finally {
			logout();
		}
	}
}
