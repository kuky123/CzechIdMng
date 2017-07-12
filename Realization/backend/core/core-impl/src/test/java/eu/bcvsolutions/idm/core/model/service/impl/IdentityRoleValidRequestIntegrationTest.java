package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleValidRequestDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRoleValidRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeType;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleValidRequestService;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleService;
import eu.bcvsolutions.idm.core.model.service.api.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.model.service.api.IdmTreeTypeService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Integration test for {@link IdmIdentityRoleValidRequest}
 * Delete:
 * - Role
 * - Identity
 * - IdentityRole
 * - IdentityContract
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class IdentityRoleValidRequestIntegrationTest extends AbstractIntegrationTest {
	
	@Autowired
	private IdmIdentityService identityService;
	
	@Autowired
	private IdmRoleService roleService;
	
	@Autowired
	private IdmTreeNodeService treeNodeService;
	
	@Autowired
	private IdmTreeTypeService treeTypeService;
	
	@Autowired
	private IdmIdentityContractService identityContractService;
	
	@Autowired
	private IdmIdentityRoleService idmIdentityRoleSerivce;
	
	@Autowired
	private IdmIdentityRoleValidRequestService identityRoleValidRequestService;
	
	@Before
	public void loginAndInit() {
		loginAsAdmin("admin");
	}
	
	@After
	public void logout() {
		super.logout();
	}
	
	@Test
	public void deleteIdentity() {
		IdmIdentityDto identity = createAndSaveIdentity();
		IdmRole role = createAndSaveRole();
		IdmTreeType treeType = createAndSaveTreeType();
		IdmTreeNode treeNode = createAndSaveTreeNode(treeType);
		IdmIdentityContractDto identityContract = createAndSaveIdentityContract(identity, treeNode);
		LocalDate from = new LocalDate();
		from = from.plusDays(5);
		createAndSaveIdentityRole(identityContract, role, null, from);
		
		List<IdmIdentityRoleValidRequestDto> list = identityRoleValidRequestService.find(null).getContent();
		int size = list.size();
		
		identityService.delete(identity);
		
		list = identityRoleValidRequestService.find(null).getContent();
		
		assertNotEquals(size, list.size());
		list = identityRoleValidRequestService.findAllValidRequestForIdentityId(identity.getId());
		assertEquals(true, list.isEmpty());
	}
	
	@Test(expected = ResultCodeException.class)
	public void deleteRole() {
		IdmIdentityDto identity = createAndSaveIdentity();
		IdmRole role = createAndSaveRole();
		IdmTreeType treeType = createAndSaveTreeType();
		IdmTreeNode treeNode = createAndSaveTreeNode(treeType);
		IdmIdentityContractDto identityContract = createAndSaveIdentityContract(identity, treeNode);
		LocalDate from = new LocalDate();
		from = from.plusDays(5);
		createAndSaveIdentityRole(identityContract, role, null, from);
		
		List<IdmIdentityRoleValidRequestDto> list = identityRoleValidRequestService.find(null).getContent();
		int size = list.size();
		
		// role has identity, ok - throw error
		roleService.delete(role);
		
		list = identityRoleValidRequestService.find(null).getContent();
		
		assertNotEquals(size, list.size());
		list = identityRoleValidRequestService.findAllValidRequestForRoleId(role.getId());
		assertEquals(true, list.isEmpty());
	}
	
	@Test
	public void deleteIdentityContract() {
		IdmIdentityDto identity = createAndSaveIdentity();
		IdmRole role = createAndSaveRole();
		IdmTreeType treeType = createAndSaveTreeType();
		IdmTreeNode treeNode = createAndSaveTreeNode(treeType);
		IdmIdentityContractDto identityContract = createAndSaveIdentityContract(identity, treeNode);
		LocalDate from = new LocalDate();
		from = from.plusDays(5);
		createAndSaveIdentityRole(identityContract, role, null, from);
		
		List<IdmIdentityRoleValidRequestDto> list = identityRoleValidRequestService.find(null).getContent();
		int size = list.size();
		
		identityContractService.delete(identityContract);
		
		list = identityRoleValidRequestService.find(null).getContent();
		
		assertNotEquals(size, list.size());
		list = identityRoleValidRequestService.findAllValidRequestForIdentityContractId(identityContract.getId());
		assertEquals(true, list.isEmpty());
	}
	
	@Test
	public void deleteIdentityRole() {
		IdmIdentityDto identity = createAndSaveIdentity();
		IdmRole role = createAndSaveRole();
		IdmTreeType treeType = createAndSaveTreeType();
		IdmTreeNode treeNode = createAndSaveTreeNode(treeType);
		IdmIdentityContractDto identityContract = createAndSaveIdentityContract(identity, treeNode);
		LocalDate from = new LocalDate();
		from = from.plusDays(5);
		IdmIdentityRoleDto identityRole = createAndSaveIdentityRole(identityContract, role, null, from);
		
		List<IdmIdentityRoleValidRequestDto> list = identityRoleValidRequestService.find(null).getContent();
		int size = list.size();
		
		idmIdentityRoleSerivce.delete(identityRole);
		
		list = identityRoleValidRequestService.find(null).getContent();
		
		assertNotEquals(size, list.size());
		list = identityRoleValidRequestService.findAllValidRequestForIdentityRoleId(identityRole.getId());
		assertEquals(true, list.isEmpty());
	}
	
	private IdmIdentityDto createAndSaveIdentity() {
		IdmIdentityDto entity = new IdmIdentityDto();
		entity.setUsername("valid_identity_" + System.currentTimeMillis());
		entity.setLastName("valid_last_name");
		return saveInTransaction(entity, identityService);
	}
	
	private IdmRole createAndSaveRole() {
		IdmRole entity = new IdmRole();
		entity.setName("valid_role_" + System.currentTimeMillis());
		return saveInTransaction(entity, roleService);
	}

	
	private IdmTreeType createAndSaveTreeType() {
		IdmTreeType entity = new IdmTreeType();
		entity.setName("valid_tree_type_" + System.currentTimeMillis());
		entity.setCode("valid_tree_type_" + System.currentTimeMillis());
		return saveInTransaction(entity, treeTypeService);
	}
	
	private IdmTreeNode createAndSaveTreeNode(IdmTreeType treeType) {
		IdmTreeNode entity = new IdmTreeNode();
		entity.setCode("valid_tree_node_" + System.currentTimeMillis());
		entity.setName("valid_tree_node_" + System.currentTimeMillis());
		entity.setTreeType(treeType);
		return saveInTransaction(entity, treeNodeService);
	}
	
	private IdmIdentityContractDto createAndSaveIdentityContract(IdmIdentityDto user, IdmTreeNode node) {
		IdmIdentityContractDto entity = new IdmIdentityContractDto();
		entity.setIdentity(user.getId());
		entity.setWorkPosition(node == null ? null : node.getId());
		return saveInTransaction(entity, identityContractService);
	}
	
	private IdmIdentityRoleDto createAndSaveIdentityRole(IdmIdentityContractDto identityContract, IdmRole role, LocalDate validTill, LocalDate validFrom) {
		IdmIdentityRoleDto entity = new IdmIdentityRoleDto();
		entity.setValidTill(validTill);
		entity.setValidFrom(validFrom);
		entity.setRole(role.getId());
		entity.setIdentityContract(identityContract.getId());
		return saveInTransaction(entity, idmIdentityRoleSerivce);
	}
}