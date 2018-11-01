package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.IdentityState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for Identity filters
 * 
 * TODO: move to rest test
 *
 * @author Marek Klement
 * @author Radek Tomiška
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Transactional
public class IdentityFilterTest extends AbstractIntegrationTest{

	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmIdentityContractService identityContractService;
	
	@Before
	public void init() {
		getHelper().loginAdmin();
	}
	
	@After
	public void logout() {
		super.logout();
	}
	
	@Test
	/**
	 * Test find identity by all string fields
	 */
	public void testCorrelableFilter() {
		IdmIdentityDto identity = getHelper().createIdentity();
		identity.setTitleAfter(UUID.randomUUID().toString());
		identity.setTitleBefore(UUID.randomUUID().toString());
		identity.setDescription(UUID.randomUUID().toString());
		identity.setExternalCode(UUID.randomUUID().toString());
		identity.setExternalId(UUID.randomUUID().toString());
		identity.setPhone(UUID.randomUUID().toString().substring(0, 29));
		identity.setRealmId(UUID.randomUUID());
		identity.setBlockLoginDate(DateTime.now());
		IdmIdentityDto identityFull = identityService.save(identity);

		ArrayList<Field> fields = Lists.newArrayList(IdmIdentity_.class.getFields());
		IdmIdentityFilter filter = new IdmIdentityFilter();

		fields.forEach(field -> {
			filter.setProperty(field.getName());

			try {
				Object value = EntityUtils.getEntityValue(identityFull, field.getName());
				if (value == null || !(value instanceof String)) {
					return;
				}
				filter.setValue(value.toString());
				List<IdmIdentityDto> identities = identityService.find(filter, null).getContent();
				assertTrue(identities.contains(identityFull));

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| IntrospectionException e) {
				e.printStackTrace();
			}

		});
	}

	@Test(expected = ResultCodeException.class)
	public void testCorrelableFilterWrongField() {
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setProperty("notExistsField");
		filter.setValue(UUID.randomUUID().toString());
		identityService.find(filter, null).getContent();
	}
	
	@Test(expected = ResultCodeException.class)
	public void testCorrelableFilterWrongType() {
		// Only search by String is supported
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setProperty(IdmIdentity_.realmId.getName());
		filter.setValue(UUID.randomUUID().toString());
		identityService.find(filter, null).getContent();
	}

	@Test
	public void testCreateIdentity(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName000","ThisIsTestName000","ThisIsTestName000@gemail.eu", "000000000", false);
		assertNotNull(person);
	}

	@Test
	public void testUsernameFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName001","ThisIsTestName001","ThisIsTestName001@gemail.eu", "000000001", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setUsername(person.getUsername());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Username",1, result.getTotalElements());
		assertEquals("Wrong Username user ID",person.getId(),result.getContent().get(0).getId());
	}

	@Test
	public void testFirstnameFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName002","ThisIsTestName002","ThisIsTestName002@gemail.eu", "000000002", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(person.getFirstName());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Firstname",1, result.getTotalElements());
		assertEquals("Wrong Firstname user ID",person.getId(),result.getContent().get(0).getId());
	}

	@Test
	public void testLastnameFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName003","ThisIsTestName003","ThisIsTestName003@gemail.eu", "000000003", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setLastName(person.getLastName());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Lastname",1, result.getTotalElements());
		assertEquals("Wrong Lastname user ID",person.getId(),result.getContent().get(0).getId());
	}

	@Test
	public void testDisabledFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName004","ThisIsTestName004","ThisIsTestName004@gemail.eu", "000000004", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setDisabled(false);
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		person.setState(IdentityState.DISABLED);
		identityService.save(person);
		//
		Page<IdmIdentityDto> result2 = identityService.find(filter, null);
		int changed = (int) (result.getTotalElements() - result2.getTotalElements());
		assertEquals("Wrong Disabled",1, changed);
	}

	@Test
	public void testPropertyValueFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName012","ThisIsTestName012","ThisIsTestName012@gemail.eu", "100000012", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setProperty(IdmIdentity_.username.getName());
		filter.setValue(person.getUsername());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Set Property and Value - username",1, result.getTotalElements());
		assertEquals("Wrong Property an Value - username user ID",person.getId(),result.getContent().get(0).getId());

		filter.setProperty(IdmIdentity_.firstName.getName());
		filter.setValue(person.getFirstName());
		result = identityService.find(filter, null);
		assertEquals("Wrong Set Property and Value - firstname",1, result.getTotalElements());
		assertEquals("Wrong Property an Value - firstname user ID",person.getId(),result.getContent().get(0).getId());

		filter.setProperty(IdmIdentity_.lastName.getName());
		filter.setValue(person.getLastName());
		result = identityService.find(filter, null);
		assertEquals("Wrong Set Property and Value - lastname",1, result.getTotalElements());
		assertEquals("Wrong Property an Value - lastname user ID",person.getId(),result.getContent().get(0).getId());

		filter.setProperty(IdmIdentity_.email.getName());
		filter.setValue(person.getEmail());
		result = identityService.find(filter, null);
		assertEquals("Wrong Set Property and Value - email",1, result.getTotalElements());
		assertEquals("Wrong Property an Value - email user ID",person.getId(),result.getContent().get(0).getId());

	}

	@Test
	public void testRoleFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName013","ThisIsTestName013","ThisIsTestName013@gemail.eu", "100000013", false);
		IdmTreeTypeDto type = getHelper().createTreeType("ThisIsTestType007");
		IdmTreeNodeDto node = getHelper().createTreeNode(type,"ThisIsTestNode008",null);
		IdmIdentityContractDto contract = getHelper().createIdentityContact(person,node);
		IdmRoleDto somerole = getHelper().createRole();
		getHelper().createIdentityRole(contract,somerole);
		UUID roleId = somerole.getId();
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setRoles(Collections.singletonList(roleId));
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Roles",1, result.getTotalElements());
		assertEquals("Wrong Roles user ID",person.getId(),result.getContent().get(0).getId());
	}

	@Test
	public void testSubordinatesForFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName005","ThisIsTestName005","ThisIsTestName005@gemail.eu", "000000005", false);
		IdmIdentityDto manager = getIdmIdentity("ThisIsTestName006","ThisIsTestName006","ThisIsTestName006@gemail.eu", "000000006", false);
		UUID manager_id = manager.getId();
		IdmTreeTypeDto type1 = getHelper().createTreeType("ThisIsTestType001");
		IdmTreeNodeDto node2 = getHelper().createTreeNode(type1,"Somename002",null);
		IdmTreeNodeDto node1 = getHelper().createTreeNode(type1,"ThisIsTestNode001",node2);
		getHelper().createIdentityContact(manager,node2);
		getHelper().createIdentityContact(person,node1);
		//contract.setGuarantee(manager_id);
		//IdmIdentityContractDto contract2 = idmIdentityContractService.save(contract);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setSubordinatesFor(manager_id);
		filter.setSubordinatesByTreeType(type1.getId());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong SubordinatesFor",1, result.getTotalElements());
		assertEquals("Wrong SuborganizedFor user ID",person.getId(),result.getContent().get(0).getId());

	}

	@Test
	public void testManagersForFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName009","ThisIsTestName009","ThisIsTestName009@gemail.eu", "000000009", false);
		IdmIdentityDto manager = getIdmIdentity("ThisIsTestName010","ThisIsTestName010","ThisIsTestName010@gemail.eu", "000000010", false);
		UUID person_id = person.getId();
		manager.getId();
		IdmTreeTypeDto type1 = getHelper().createTreeType("ThisIsTestType004");
		IdmTreeNodeDto node2 = getHelper().createTreeNode(type1,"Somename001",null);
		IdmTreeNodeDto node1 = getHelper().createTreeNode(type1,"ThisIsTestNode004",node2);
		getHelper().createIdentityContact(manager,node2);
		getHelper().createIdentityContact(person,node1);
		//contract.setGuarantee(manager_id);
		//IdmIdentityContractDto contract2 = idmIdentityContractService.save(contract);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		//filter.setIncludeGuarantees(true);
		filter.setManagersFor(person_id);
		filter.setManagersByTreeType(type1.getId());
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong ManagersFor",1, result.getTotalElements());
		assertEquals("Wrong ManagersFor user ID",manager.getId(),result.getContent().get(0).getId());

	}

	@Test
	public void testIncludeGuaranteesFilter(){
		IdmIdentityDto person = getIdmIdentity("aThisIsTestName009","ThisIsTestName009","ThisIsTestName009@gemail.eu", "000000009", false);
		IdmIdentityDto manager2 = getIdmIdentity("manager009","ThisIsTestName009","ThisIsTestName009@gemail.eu", "000000009", false);
		IdmIdentityDto manager = getIdmIdentity("ThisIsTestName010","ThisIsTestName010","ThisIsTestName010@gemail.eu", "000000010", false);
		IdmTreeTypeDto type1 = getHelper().createTreeType("ThisIsTestType0004");
		IdmTreeNodeDto node1 = getHelper().createTreeNode(type1,"ThisIsTestNode0004",null);
		IdmTreeNodeDto node2 = getHelper().createTreeNode(type1,"NextThisIsTestNode0004",node1);
		IdmIdentityContractDto contract = getHelper().createIdentityContact(person,node2);
		getHelper().createContractGuarantee(contract.getId(),manager2.getId());
		getHelper().createIdentityContact(manager,node1);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setManagersFor(person.getId());
		filter.setIncludeGuarantees(true);
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Managers2For",2, result.getTotalElements());
		filter.setIncludeGuarantees(false);
		result = identityService.find(filter, null);
		assertEquals("Wrong Managers2For test 2",1, result.getTotalElements());
	}

	@Test
	public void testRecursivelyFilter(){
		// setting employees
		IdmIdentityDto person1 = getIdmIdentity("Klement","Marek","ThisIsTestName011@gemail.eu", "000000011", false);
		IdmIdentityDto person2 = getIdmIdentity("Klement","Marek","ThisIsTestName014@gemail.eu", "000000014", false);
		IdmIdentityDto person3 = getIdmIdentity("Klement","Marek","ThisIsTestName015@gemail.eu", "000000015", false);
		IdmIdentityDto person4 = getIdmIdentity("Klement","Marek","ThisIsTestName016@gemail.eu", "000000016", false);
		// setting structure
		IdmTreeTypeDto type1 = getHelper().createTreeType("ThisIsTestType005x");
		IdmTreeTypeDto type2 = getHelper().createTreeType("ThisIsTestType006x");
		/*
		      r1  o
		         /
		     n1 o
		       /
	       n2 o
		 */
		IdmTreeNodeDto node1 = getHelper().createTreeNode(type1,"ThisIsTestNode005",null);
		IdmTreeNodeDto node11 = getHelper().createTreeNode(type1,"ThisIsTestNode006",node1);
		IdmTreeNodeDto node12 = getHelper().createTreeNode(type1,"ThisIsTestNode007",node11);
		/*
		    r2  o
		 */
		IdmTreeNodeDto node2 = getHelper().createTreeNode(type2,"ThisIsTestNode008",null);
		// contracts
		getHelper().createIdentityContact(person1,node1);
		getHelper().createIdentityContact(person2,node11);
		getHelper().createIdentityContact(person3,node12);
		getHelper().createIdentityContact(person4,node2);
		// node1 UUID
		UUID node1id = node1.getId();
		// test
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(person1.getFirstName());
		filter.setRecursively(true);
		filter.setTreeNode(node1id);
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong Recursive firstname", 4, result.getTotalElements());
		filter.setRecursively(false);
		result = identityService.find(filter, null);
		assertEquals("Wrong NonRecursive firstname", 1, result.getTotalElements());
	}

	@Test
	public void testTreeTypeFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName007","ThisIsTestName007","ThisIsTestName007@gemail.eu", "000000007", false);
		IdmTreeTypeDto type = getHelper().createTreeType("ThisIsTestType002");
		IdmTreeNodeDto node = getHelper().createTreeNode(type,"ThisIsTestNode002",null);
		UUID typeUuid = type.getId();
		getHelper().createIdentityContact(person,node);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setTreeType(typeUuid);
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong TreeType",1, result.getTotalElements());
	}

	@Test
	public void testTreeNodeFilter(){
		IdmIdentityDto person = getIdmIdentity("ThisIsTestName008","ThisIsTestName008","ThisIsTestName008@gemail.eu", "000000008", false);
		IdmTreeTypeDto type = getHelper().createTreeType("ThisIsTestType003");
		IdmTreeNodeDto node = getHelper().createTreeNode(type,"ThisIsTestNode003",null);
		UUID nodeUuid = node.getId();
		getHelper().createIdentityContact(person,node);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setTreeNode(nodeUuid);
		filter.setRecursively(false);
		Page<IdmIdentityDto> result = identityService.find(filter, null);
		assertEquals("Wrong TreeNode",1, result.getTotalElements());
	}

	@Test
	public void testNothingToFind(){
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName("Adolf");
		Page<IdmIdentityDto> result = identityService.find(filter,null);
		assertEquals("Wrong blank filter",0, result.getTotalElements());
	}

	@Test
	public void testMoreToFindInFilter(){
		IdmIdentityDto person1 = getIdmIdentity("tn00","tn00","tn01@a.eu", "000000001", false);
		getIdmIdentity("tn00","tn00","tn01@a.eu", "000000001", false);
		getIdmIdentity("tn00","tn00","tn01@a.eu", "000000001", false);
		getIdmIdentity("tn001","tn00","tn01@a.eu", "000000001", false);

		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(person1.getFirstName());
		Page<IdmIdentityDto> result = identityService.find(filter,null);
		assertEquals("Wrong more to find filter",3, result.getTotalElements());
	}

	@Test
	public void testMoreFilters(){
		IdmIdentityDto person1 = getIdmIdentity("tn01","tn01","tn02@a.eu", "000010001", false);
		IdmIdentityDto person2 = getIdmIdentity("tn01","tn01","tn02@a.eu", "000010001", false);
		IdmIdentityDto person3 = getIdmIdentity("tn01","tn01","tn02@a.eu", "000010001", false);
		getIdmIdentity("tn0001","tn01","tn02@a.eu", "000010001", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(person1.getFirstName());
		filter.setLastName(person2.getLastName());
		filter.setDisabled(false);
		Page<IdmIdentityDto> result = identityService.find(filter,null);
		assertEquals("Wrong more filter",3, result.getTotalElements());
		filter.setUsername(person3.getUsername());
		result = identityService.find(filter,null);
		assertEquals("Wrong more filter",1, result.getTotalElements());
		assertEquals("Wrong more filter user ID",person3.getId(),result.getContent().get(0).getId());
	}

	@Test
	public void testSameButDifferentTreeTypeFilter(){
		IdmIdentityDto person1 = getIdmIdentity("thistn01","thistn01","thistn02@a.eu", "000010001", false);
		getIdmIdentity("thistn01","thistn01","thistn02@a.eu", "000010001", false);
		IdmTreeTypeDto type1 = getHelper().createTreeType("typeName001");
		IdmTreeTypeDto type2 = getHelper().createTreeType("typeName002");
		IdmTreeNodeDto	 node1 = getHelper().createTreeNode(type1,"nodeName001",null);
		getHelper().createTreeNode(type2,"nodeName002",null);
		getHelper().createIdentityContact(person1,node1);
		getHelper().createIdentityContact(person1,node1);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(person1.getFirstName());
		Page<IdmIdentityDto> result = identityService.find(filter,null);
		assertEquals("Wrong sameButDiff filter",2, result.getTotalElements());
		filter.setTreeType(type1.getId());
		result = identityService.find(filter,null);
		assertEquals("Wrong sameButDiff filter 2",1, result.getTotalElements());
	}

	@Test
	public void testTextFilter(){
		IdmIdentityDto person1 = getIdmIdentity("a1testuser","b1testuser","b1testuser@a.eu", "333010001", false);
		IdmIdentityDto person2 = getIdmIdentity("a1testuser","b2testuser","b2testuser@a.eu", "333010003", false);
		IdmIdentityDto person3 = getIdmIdentity("a1testuser","b2testuser","b3testuser@a.eu", "333010004", false);
		IdmIdentityDto person4 = getIdmIdentity("a2testuser","b3testuser","b4testuser@a.eu", "333010005", false);
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setText(person1.getFirstName());
		Page<IdmIdentityDto> result = identityService.find(filter,null);
		assertEquals("Wrong Text filter - firstname",3, result.getTotalElements());
		filter.setText(person2.getLastName());
		result = identityService.find(filter,null);
		assertEquals("Wrong Text filter - lastname",2, result.getTotalElements());
		filter.setText(person3.getEmail());
		result = identityService.find(filter,null);
		assertEquals("Wrong Text filter - email",1, result.getTotalElements());
		filter.setText(person4.getUsername());
		result = identityService.find(filter,null);
		assertEquals("Wrong Text filter - username",person4.getId(), result.getContent().get(0).getId());
		person1.setDescription("This is describtion");
		IdmIdentityDto person5 = identityService.save(person1);
		filter.setText(person5.getDescription());
		result = identityService.find(filter,null);
		assertEquals("Wrong Text filter - describtion",person1.getId(), result.getContent().get(0).getId());
	}
	
	@Test
	public void testStateFilter() {
		String firstname = getHelper().createName();
		IdmIdentityDto identityOne = new IdmIdentityDto();
		identityOne.setUsername(getHelper().createName());
		identityOne.setFirstName(firstname);
		identityOne = identityService.save(identityOne);
		identityContractService.delete(getHelper().getPrimeContract(identityOne.getId()));
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		identity.setFirstName(firstname);
		identity.setState(IdentityState.DISABLED_MANUALLY);
		identityService.save(identity);
		//
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setFirstName(firstname);
		filter.setState(IdentityState.VALID);
		Page<IdmIdentityDto> results = identityService.find(filter, null);
		Assert.assertTrue(results.getTotalElements() == 0);
		//
		filter.setState(IdentityState.NO_CONTRACT);
		results = identityService.find(filter, null);
		Assert.assertTrue(results.getTotalElements() == 1);
		Assert.assertEquals(identityOne.getUsername(), results.getContent().get(0).getUsername());
		//
		filter.setState(IdentityState.DISABLED_MANUALLY);
		results = identityService.find(filter, null);
		Assert.assertTrue(results.getTotalElements() == 1);
		Assert.assertEquals(identity.getUsername(), results.getContent().get(0).getUsername());
	}
	
	@Test
	public void testExternalCodeFilterOne() {
		String testExternalCode = "externalCodeTest-" + System.currentTimeMillis();
		IdmIdentityDto identity = getHelper().createIdentity();
		identity.setExternalCode(testExternalCode);
		identity = identityService.save(identity);
		//
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setExternalCode("nonExistingCode" + System.currentTimeMillis());
		List<IdmIdentityDto> content = identityService.find(filter, null).getContent();
		//
		assertEquals(0, content.size());
		filter.setExternalCode(testExternalCode);
		content = identityService.find(filter, null).getContent();
		assertEquals(1, content.size());
		//
		IdmIdentityDto founded = content.get(0);
		assertEquals(testExternalCode, founded.getExternalCode());
		assertEquals(identity.getExternalCode(), founded.getExternalCode());
		assertEquals(identity.getId(), founded.getId());
	}

	@Test
	public void testExternalCodeFilterMany() {
		String testExternalCode = "externalCodeTest-" + System.currentTimeMillis();
		IdmIdentityDto identity = getHelper().createIdentity();
		identity.setExternalCode(testExternalCode);
		identity = identityService.save(identity);
		//
		IdmIdentityDto identity2 = getHelper().createIdentity();
		identity2.setExternalCode(getHelper().createName());
		identity2 = identityService.save(identity2);
		//
		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setExternalCode(getHelper().createName());
		List<IdmIdentityDto> content = identityService.find(filter, null).getContent();
		//
		assertEquals(0, content.size());
		filter.setExternalCode(testExternalCode);
		content = identityService.find(filter, null).getContent();
		assertEquals(1, content.size());
		//
		IdmIdentityDto founded = content.get(0);
		//
		assertEquals(testExternalCode, founded.getExternalCode());
		assertNotEquals(identity2.getId(), founded.getId());
	}

	@Test
	public void testIdentifiers() {
		List<IdmIdentityDto> identities = createIdentities(10);
		
		IdmIdentityDto identityOne = identities.get(1);
		IdmIdentityDto identityTwo = identities.get(2);
		IdmIdentityDto identityFive = identities.get(5);
		IdmIdentityDto identityNine = identities.get(9);
		
		identityOne.setExternalCode("identityOneExternalCode" + System.currentTimeMillis());
		
		identityTwo.setUsername("identityTwoUsername" + System.currentTimeMillis());
		
		identityFive.setUsername("identityFiveUsername" + System.currentTimeMillis());
		identityFive.setExternalCode("identityFiveExternalCode" + System.currentTimeMillis());
		
		identityNine.setExternalCode("identityNineExternalCode" + System.currentTimeMillis());
		identityNine.setUsername("identityNineUsername" + System.currentTimeMillis());
		
		identityOne = identityService.save(identityOne);
		identityTwo = identityService.save(identityTwo);
		identityFive = identityService.save(identityFive);
		identityNine = identityService.save(identityNine);
		
		IdmIdentityFilter filter = new IdmIdentityFilter();
		List<String> identifiers = new ArrayList<>();
		
		identifiers.add(identityOne.getExternalCode());
		identifiers.add(identityTwo.getUsername());
		identifiers.add(identityFive.getExternalCode());
		identifiers.add(identityFive.getUsername());
		identifiers.add(identityNine.getExternalCode());
		identifiers.add(identityNine.getUsername());
		
		filter.setIdentifiers(identifiers);
		
		List<IdmIdentityDto> result = identityService.find(filter, null).getContent();
		assertEquals(4, result.size());
	}

	@Test
	public void testFilteringByEmail() {
		String email = System.currentTimeMillis() + "-test-email@example.tld";

		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		identity.setEmail(email);
		identity = identityService.save(identity);
		
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add(IdmIdentityFilter.PARAMETER_EMAIL, email);
		IdmIdentityFilter filter = new IdmIdentityFilter(data);
		List<IdmIdentityDto> identities = identityService.find(filter, null).getContent();

		assertEquals(1, identities.size());
		IdmIdentityDto identityDto = identities.get(0);
		assertEquals(identity.getId(), identityDto.getId());
		assertEquals(identity.getEmail(), identityDto.getEmail());
	}

	@Test
	public void testFilteringByEmailSimilar() {
		String email = System.currentTimeMillis() + "-test-email@example.tld";

		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		identity.setEmail(email);
		identity = identityService.save(identity);
		
		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add(IdmIdentityFilter.PARAMETER_EMAIL, "test-email@example.tld");
		IdmIdentityFilter filter = new IdmIdentityFilter(data);
		List<IdmIdentityDto> identities = identityService.find(filter, null).getContent();

		assertEquals(0, identities.size());
	}

	/**
	 * Create X identities without password
	 *
	 * @param count
	 * @return
	 */
	private List<IdmIdentityDto> createIdentities(int count) {
		List<IdmIdentityDto> identities = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			identities.add(getHelper().createIdentity(getHelper().createName(), null));
		}
		return identities;
	}
	private IdmIdentityDto getIdmIdentity(String firstName, String lastName, String email, String phone, boolean disabled){
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		identity.setFirstName(firstName);
		identity.setLastName(lastName);
		identity.setEmail(email);
		identity.setState(disabled ? IdentityState.DISABLED : IdentityState.VALID);
		identity.setPhone(phone);
		return identityService.save(identity);
	}

}
