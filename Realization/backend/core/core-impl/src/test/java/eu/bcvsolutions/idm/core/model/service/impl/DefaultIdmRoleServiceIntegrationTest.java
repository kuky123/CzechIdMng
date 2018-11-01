package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.config.domain.RoleConfiguration;
import eu.bcvsolutions.idm.core.api.domain.RoleType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAuthorizationPolicyFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeRoleFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmRole_;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Basic role service operations
 * TODO: move filter tests to rest test
 * 
 * @author Radek Tomiška
 * @author Marek Klement
 *
 */
@Transactional
public class DefaultIdmRoleServiceIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ApplicationContext context;
	@Autowired private IdmRoleCatalogueRoleService idmRoleCatalogueRoleService;
	@Autowired private IdmRoleGuaranteeService roleGuaranteeService;
	@Autowired private IdmRoleGuaranteeRoleService roleGuaranteeRoleService;
	@Autowired private IdmAuthorizationPolicyService authorizationPolicyService;
	@Autowired private RoleConfiguration roleConfiguration;
	//
	private DefaultIdmRoleService roleService;

	@Before
	public void init() {
		 roleService = context.getAutowireCapableBeanFactory().createBean(DefaultIdmRoleService.class);
	}
	
	@Test
	public void testReferentialIntegrity() {
		IdmIdentityDto identity = getHelper().createIdentity(new GuardedString("heslo"));
		// role
		IdmRoleDto role = getHelper().createRole();
		getHelper().createRoleGuarantee(role, identity);
		//
		// guarantee by role
		IdmRoleGuaranteeRoleDto roleGuaranteeOne = new IdmRoleGuaranteeRoleDto();
		roleGuaranteeOne.setRole(role.getId());
		roleGuaranteeOne.setGuaranteeRole(getHelper().createRole().getId());
		roleGuaranteeRoleService.save(roleGuaranteeOne);
		IdmRoleGuaranteeRoleDto roleGuaranteeTwo = new IdmRoleGuaranteeRoleDto();
		roleGuaranteeTwo.setRole(getHelper().createRole().getId());
		roleGuaranteeTwo.setGuaranteeRole(role.getId());
		roleGuaranteeRoleService.save(roleGuaranteeTwo);
		//
		// after save
		IdmRoleGuaranteeFilter guaranteeFilter = new IdmRoleGuaranteeFilter();
		guaranteeFilter.setRole(role.getId());
		IdmRoleGuaranteeRoleFilter guaranteeRoleFilter = new IdmRoleGuaranteeRoleFilter();
		guaranteeRoleFilter.setRole(role.getId());
		IdmRoleGuaranteeRoleFilter guaranteeRoleRoleFilter = new IdmRoleGuaranteeRoleFilter();
		guaranteeRoleRoleFilter.setGuaranteeRole(role.getId());
		//
		Assert.assertNotNull(roleService.getByCode(role.getCode()));
		Assert.assertEquals(1, roleGuaranteeService.find(guaranteeFilter, null).getTotalElements());
		Assert.assertEquals(1, roleGuaranteeRoleService.find(guaranteeRoleFilter, null).getTotalElements());
		Assert.assertEquals(1, roleGuaranteeRoleService.find(guaranteeRoleFilter, null).getTotalElements());
		
		roleService.delete(role);
		//
		// after delete
		Assert.assertNull(roleService.getByCode(role.getCode()));
		Assert.assertEquals(0, roleGuaranteeService.find(guaranteeFilter, null).getTotalElements());
		Assert.assertEquals(0, roleGuaranteeRoleService.find(guaranteeRoleFilter, null).getTotalElements());
		Assert.assertEquals(0, roleGuaranteeRoleService.find(guaranteeRoleFilter, null).getTotalElements());
	}
	
	@Test(expected = ResultCodeException.class)
	public void testReferentialIntegrityAssignedRoles() {
		// prepare data
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmRoleDto role = getHelper().createRole();
		// assigned role
		getHelper().createIdentityRole(identity, role);
		//
		roleService.delete(role);
	}
	
	@Test(expected = ResultCodeException.class)
	public void testReferentialIntegrityHasRoleComposition() {
		// prepare data
		IdmRoleDto role = getHelper().createRole();
		IdmRoleDto subrole = getHelper().createRole();
		// assigned role
		getHelper().createRoleComposition(role, subrole);
		//
		roleService.delete(role);
	}
	
	@Test
	public void testReferentialIntegrityAuthorizationPolicies() {
		// prepare data
		IdmRoleDto role = getHelper().createRole();
		// policy
		getHelper().createBasePolicy(role.getId(), IdmBasePermission.ADMIN);
		//
		roleService.delete(role);
		//
		IdmAuthorizationPolicyFilter policyFilter = new IdmAuthorizationPolicyFilter();
		policyFilter.setRoleId(role.getId());
		Assert.assertEquals(0, authorizationPolicyService.find(policyFilter, null).getTotalElements());
	}

	@Test
	public void textFilterTest(){
		getHelper().createRole("SomeName001");
		getHelper().createRole("SomeName002");
		getHelper().createRole("SomeName003");
		getHelper().createRole("SomeName104");

		IdmRoleDto role5 = new IdmRoleDto();
		role5.setDescription("SomeName005");
		role5.setCode("SomeName105");
		role5 = roleService.save(role5);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setText("SomeName00");
		Page<IdmRoleDto> result = roleService.find(filter,null);
		Assert.assertEquals("Wrong text filter", 4, result.getTotalElements());
		Assert.assertEquals("Wrong text filter description", true, result.getContent().contains(role5));
	}

	@Test
	public void typeFilterTest(){
		IdmRoleDto role = getHelper().createRole();
		IdmRoleDto role2 = getHelper().createRole();
		IdmRoleDto role3 = getHelper().createRole();

		RoleType type = RoleType.LOGIN;
		RoleType type2 = RoleType.BUSINESS;

		role = roleService.get(role.getId());
		role.setRoleType(type);
		role = roleService.save(role);

		role2 = roleService.get(role2.getId());
		role2.setRoleType(type);
		role2 = roleService.save(role2);

		role3 = roleService.get(role3.getId());
		role3.setRoleType(type2);
		role3 = roleService.save(role3);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setRoleType(type);
		Page<IdmRoleDto> result = roleService.find(filter,null);
		Assert.assertEquals("Wrong type #1", 2, result.getTotalElements());
		Assert.assertTrue("Wrong type #1 contains", result.getContent().contains(role));
		filter.setRoleType(type2);
		result = roleService.find(filter,null);
		Assert.assertEquals("Wrong type #2", 1, result.getTotalElements());
		Assert.assertTrue("Wrong type #2 contains", result.getContent().contains(role3));
	}

	@Test
	public void guaranteeFilterTest(){
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmRoleDto role = getHelper().createRole();
		getHelper().createRoleGuarantee(role, identity);
		//
		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setGuaranteeId(identity.getId());
		Page<IdmRoleDto> result = roleService.find(filter, null);
		Assert.assertEquals("Wrong guarantee", 1, result.getTotalElements());
		Assert.assertEquals("Wrong guarantee id", role.getId(), result.getContent().get(0).getId());
	}
	
	@Test
	public void guaranteeRoleFilterTest(){
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identityOther = getHelper().createIdentity((GuardedString) null);
		IdmRoleDto role = getHelper().createRole();
		IdmRoleDto guaranteeRole = getHelper().createRole();
		IdmRoleGuaranteeRoleDto roleGuaranteeRole = new IdmRoleGuaranteeRoleDto();
		roleGuaranteeRole.setRole(role.getId());
		roleGuaranteeRole.setGuaranteeRole(guaranteeRole.getId());
		roleGuaranteeRoleService.save(roleGuaranteeRole);
		getHelper().createIdentityRole(identity, guaranteeRole);
		//
		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setGuaranteeId(identity.getId());
		Page<IdmRoleDto> result = roleService.find(filter, null);
		Assert.assertEquals("Wrong guarantee", 1, result.getTotalElements());
		Assert.assertEquals("Wrong guarantee id", role.getId(), result.getContent().get(0).getId());
		//
		filter.setGuaranteeId(identityOther.getId());
		result = roleService.find(filter, null);
	}

	@Test
	public void catalogueFilterTest(){
		try {
			loginAsAdmin();
		
			IdmRoleDto role = getHelper().createRole();
	
			IdmRoleCatalogueDto catalogue = getHelper().createRoleCatalogue();
			IdmRoleCatalogueRoleDto catalogueRole = new IdmRoleCatalogueRoleDto();
			catalogueRole.setRole(role.getId());
			catalogueRole.setRoleCatalogue(catalogue.getId());
			catalogueRole = idmRoleCatalogueRoleService.save(catalogueRole);
	
			IdmRoleFilter filter = new IdmRoleFilter();
			filter.setRoleCatalogueId(catalogue.getId());
			Page<IdmRoleDto> result = roleService.find(filter,null);
			Assert.assertEquals("Wrong catalogue", 1, result.getTotalElements());
			Assert.assertTrue("Wrong catalogue id #1", result.getContent().contains(role));
		} finally {
			logout();
		}
	}
	
	/**
	 * Test find role by all string fields
	 */
	@Test
	public void testCorrelableFilter() {
		IdmRoleDto role = getHelper().createRole();
		role.setExternalId(getHelper().createName());
		role.setCode(getHelper().createName());
		role.setDescription(getHelper().createName());
		IdmRoleDto roleFull = roleService.save(role);

		ArrayList<Field> fields = Lists.newArrayList(IdmRole_.class.getFields());
		IdmRoleFilter filter = new IdmRoleFilter();

		fields.forEach(field -> {
			filter.setProperty(field.getName());

			try {
				Object value = EntityUtils.getEntityValue(roleFull, field.getName());
				if (value == null || !(value instanceof String)) {
					return;
				}
				filter.setValue(value.toString());
				List<IdmRoleDto> identities = roleService.find(filter, null).getContent();
				Assert.assertTrue(identities.contains(roleFull));

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| IntrospectionException e) {
				e.printStackTrace();
			}

		});

	}
	
	@Test
	public void testAutoFillCodeAndName() {
		IdmRoleDto role = new IdmRoleDto();
		role.setName(getHelper().createName());
		//
		role = roleService.save(role);
		Assert.assertEquals(role.getName(), role.getCode());
		//
		role = new IdmRoleDto();
		role.setCode(getHelper().createName());
		//
		role = roleService.save(role);
		Assert.assertEquals(role.getCode(), role.getName());
		//
		role = new IdmRoleDto();
		role.setCode(getHelper().createName());
		role.setName(getHelper().createName());
		//
		role = roleService.save(role);
		Assert.assertNotEquals(role.getCode(), role.getName());
	}
	
	@Test
	public void testGetAdminRole() {
		Assert.assertEquals(roleConfiguration.getAdminRole(), roleService.getAdminRole());
	}
	
	@Test
	public void testGetRolesByIds() {
		IdmRoleDto roleOne = getHelper().createRole();
		IdmRoleDto roleTwo = getHelper().createRole();
		//
		List<IdmRoleDto> roles = roleService.getRolesByIds(StringUtils.join(Lists.newArrayList(roleOne.getId(), roleTwo.getId()), ','));
		//
		Assert.assertEquals(2, roles.size());
		Assert.assertTrue(roles.stream().anyMatch(r -> r.getId().equals(roleOne.getId())));
		Assert.assertTrue(roles.stream().anyMatch(r -> r.getId().equals(roleTwo.getId())));
	}

	@Test
	public void testGetByCode() {
		String roleName = "role-name " + System.currentTimeMillis();
		String roleCode = "roleCode" + System.currentTimeMillis();

		IdmRoleDto role = new IdmRoleDto();
		role.setName(roleName);
		role.setCode(roleCode);
		role = roleService.save(role);
		
		IdmRoleDto roleDto = roleService.getByCode("notRoleCode" + System.currentTimeMillis());
		assertNull(roleDto);

		roleDto = roleService.getByCode(roleName);
		assertNull(roleDto);

		roleDto = roleService.getByCode(roleCode);
		assertNotNull(roleDto);
		assertEquals(role.getId(), roleDto.getId());
	}
}
