package eu.bcvsolutions.idm.acc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.dto.SysAttributeControlledValueDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysAttributeControlledValueFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.service.api.SysAttributeControlledValueService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for provisioning merge
 *
 * @author Vít Švanda
 *
 */
public class ProvisioningMergeTest extends AbstractIntegrationTest {
	private static final String RIGHTS_ATTRIBUTE = "RIGHTS";
	private static final String ONE_VALUE = "ONE";
	private static final String TWO_VALUE = "TWO";

	@Autowired
	private SysSystemAttributeMappingService attributeMappingService;
	@Autowired
	private SysSchemaObjectClassService schemaService;
	@Autowired
	private SysSchemaAttributeService schemaAttributeService;
	@Autowired
	private SysRoleSystemAttributeService roleSystemAttributeService;
	@Autowired
	private SysAttributeControlledValueService attributeControlledValueService;
	@Autowired
	private TestHelper helper;

	@Before
	public void login() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void testAttribteControlledValues() {
		SysSystemDto system = helper.createSystem("test_resource");
		SysSystemMappingDto mapping = helper.createMapping(system);
		IdmRoleDto roleOne = helper.createRole();
		IdmRoleDto roleTwo = helper.createRole();
		IdmRoleDto roleOneDuplicated = helper.createRole();

		SysRoleSystemDto roleSystemOne = helper.createRoleSystem(roleOne, system);
		SysRoleSystemDto roleSystemTwo = helper.createRoleSystem(roleTwo, system);
		SysRoleSystemDto roleSystemOneDuplicated = helper.createRoleSystem(roleOneDuplicated, system);

		SysSchemaAttributeDto rightsSchemaAttribute = new SysSchemaAttributeDto();
		rightsSchemaAttribute.setObjectClass(mapping.getObjectClass());
		rightsSchemaAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsSchemaAttribute.setMultivalued(true);
		rightsSchemaAttribute.setClassType(String.class.getName());
		rightsSchemaAttribute.setReadable(true);
		rightsSchemaAttribute.setUpdateable(true);

		rightsSchemaAttribute = schemaAttributeService.save(rightsSchemaAttribute);

		SysSystemAttributeMappingDto rightsAttribute = new SysSystemAttributeMappingDto();
		rightsAttribute.setSchemaAttribute(rightsSchemaAttribute.getId());
		rightsAttribute.setSystemMapping(mapping.getId());
		rightsAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);
		rightsAttribute = attributeMappingService.save(rightsAttribute);

		SysRoleSystemAttributeDto roleAttributeOne = new SysRoleSystemAttributeDto();
		roleAttributeOne.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOne.setRoleSystem(roleSystemOne.getId());
		roleAttributeOne.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOne.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOne.setTransformToResourceScript("return '" + ONE_VALUE + "';");
		roleAttributeOne = roleSystemAttributeService.saveInternal(roleAttributeOne);

		List<Serializable> controlledAttributeValues = attributeMappingService
				.getControlledAttributeValues(system.getId(), mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(1, controlledAttributeValues.size());
		assertEquals(ONE_VALUE, controlledAttributeValues.get(0));

		SysRoleSystemAttributeDto roleAttributeOneDuplicated = new SysRoleSystemAttributeDto();
		roleAttributeOneDuplicated.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOneDuplicated.setRoleSystem(roleSystemOneDuplicated.getId());
		roleAttributeOneDuplicated.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOneDuplicated.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOneDuplicated.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeOneDuplicated = roleSystemAttributeService.saveInternal(roleAttributeOneDuplicated);

		SysRoleSystemAttributeDto roleAttributeTwo = new SysRoleSystemAttributeDto();
		roleAttributeTwo.setName(RIGHTS_ATTRIBUTE);
		roleAttributeTwo.setRoleSystem(roleSystemTwo.getId());
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));
	}

	@Test
	public void testChangeValueDefinition() {
		SysSystemDto system = helper.createSystem("test_resource");
		SysSystemMappingDto mapping = helper.createMapping(system);
		IdmRoleDto roleOne = helper.createRole();
		IdmRoleDto roleTwo = helper.createRole();

		SysRoleSystemDto roleSystemOne = helper.createRoleSystem(roleOne, system);
		SysRoleSystemDto roleSystemTwo = helper.createRoleSystem(roleTwo, system);

		SysSchemaAttributeDto rightsSchemaAttribute = new SysSchemaAttributeDto();
		rightsSchemaAttribute.setObjectClass(mapping.getObjectClass());
		rightsSchemaAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsSchemaAttribute.setMultivalued(true);
		rightsSchemaAttribute.setClassType(String.class.getName());
		rightsSchemaAttribute.setReadable(true);
		rightsSchemaAttribute.setUpdateable(true);

		rightsSchemaAttribute = schemaAttributeService.save(rightsSchemaAttribute);

		SysSystemAttributeMappingDto rightsAttribute = new SysSystemAttributeMappingDto();
		rightsAttribute.setSchemaAttribute(rightsSchemaAttribute.getId());
		rightsAttribute.setSystemMapping(mapping.getId());
		rightsAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);
		rightsAttribute = attributeMappingService.save(rightsAttribute);

		SysRoleSystemAttributeDto roleAttributeOne = new SysRoleSystemAttributeDto();
		roleAttributeOne.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOne.setRoleSystem(roleSystemOne.getId());
		roleAttributeOne.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOne.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOne.setTransformToResourceScript("return '" + ONE_VALUE + "';");
		roleAttributeOne = roleSystemAttributeService.saveInternal(roleAttributeOne);

		List<Serializable> controlledAttributeValues = attributeMappingService
				.getControlledAttributeValues(system.getId(), mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		SysRoleSystemAttributeDto roleAttributeTwo = new SysRoleSystemAttributeDto();
		roleAttributeTwo.setName(RIGHTS_ATTRIBUTE);
		roleAttributeTwo.setRoleSystem(roleSystemTwo.getId());
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(rightsAttribute.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		List<Serializable> historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());

		// Change value definition on attribute TWO (should be changed in controlled
		// values
		// and old value appears in the history)
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "Changed';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE + "Changed"));

		// Search historic controlled values for that attribute
		historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(1, historicControlledValues.size());
		assertTrue(historicControlledValues.contains(TWO_VALUE));

	}

	@Test
	public void testDisableAttribteControlledValues() {
		SysSystemDto system = helper.createSystem("test_resource");
		SysSystemMappingDto mapping = helper.createMapping(system);
		IdmRoleDto roleOne = helper.createRole();
		IdmRoleDto roleTwo = helper.createRole();

		SysRoleSystemDto roleSystemOne = helper.createRoleSystem(roleOne, system);
		SysRoleSystemDto roleSystemTwo = helper.createRoleSystem(roleTwo, system);

		SysSchemaAttributeDto rightsSchemaAttribute = new SysSchemaAttributeDto();
		rightsSchemaAttribute.setObjectClass(mapping.getObjectClass());
		rightsSchemaAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsSchemaAttribute.setMultivalued(true);
		rightsSchemaAttribute.setClassType(String.class.getName());
		rightsSchemaAttribute.setReadable(true);
		rightsSchemaAttribute.setUpdateable(true);

		rightsSchemaAttribute = schemaAttributeService.save(rightsSchemaAttribute);

		SysSystemAttributeMappingDto rightsAttribute = new SysSystemAttributeMappingDto();
		rightsAttribute.setSchemaAttribute(rightsSchemaAttribute.getId());
		rightsAttribute.setSystemMapping(mapping.getId());
		rightsAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);
		rightsAttribute = attributeMappingService.save(rightsAttribute);

		SysRoleSystemAttributeDto roleAttributeOne = new SysRoleSystemAttributeDto();
		roleAttributeOne.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOne.setRoleSystem(roleSystemOne.getId());
		roleAttributeOne.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOne.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOne.setTransformToResourceScript("return '" + ONE_VALUE + "';");
		roleAttributeOne = roleSystemAttributeService.saveInternal(roleAttributeOne);

		List<Serializable> controlledAttributeValues = attributeMappingService
				.getControlledAttributeValues(system.getId(), mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		SysRoleSystemAttributeDto roleAttributeTwo = new SysRoleSystemAttributeDto();
		roleAttributeTwo.setName(RIGHTS_ATTRIBUTE);
		roleAttributeTwo.setRoleSystem(roleSystemTwo.getId());
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(rightsAttribute.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		List<Serializable> historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());

		// Set attribute TWO as disabled (should be disappears from controlled values
		// and appears in the history)
		roleAttributeTwo.setDisabledAttribute(true);
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(1, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));

		// Search historic controlled values for that attribute
		historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(1, historicControlledValues.size());
		assertTrue(historicControlledValues.contains(TWO_VALUE));

		// Set attribute TWO as enabled (should be appears in controlled values
		// and disappears from the history)
		roleAttributeTwo.setDisabledAttribute(false);
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		// Recalculation
		attributeMappingService.getCachedControlledAndHistoricAttributeValues(system.getId(), mapping.getEntityType(),
				RIGHTS_ATTRIBUTE);

		// Search historic controlled values for that attribute
		historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());
	}

	@Test
	public void testChangeStrategyAttribteControlledValues() {
		SysSystemDto system = helper.createSystem("test_resource");
		SysSystemMappingDto mapping = helper.createMapping(system);
		IdmRoleDto roleOne = helper.createRole();
		IdmRoleDto roleTwo = helper.createRole();

		SysRoleSystemDto roleSystemOne = helper.createRoleSystem(roleOne, system);
		SysRoleSystemDto roleSystemTwo = helper.createRoleSystem(roleTwo, system);

		SysSchemaObjectClassDto objectClass = schemaService.get(mapping.getObjectClass());
		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setObjectClassId(objectClass.getId());
		schemaAttributeService.find(schemaAttributeFilter, null);

		SysSchemaAttributeDto rightsSchemaAttribute = new SysSchemaAttributeDto();
		rightsSchemaAttribute.setObjectClass(objectClass.getId());
		rightsSchemaAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsSchemaAttribute.setMultivalued(true);
		rightsSchemaAttribute.setClassType(String.class.getName());
		rightsSchemaAttribute.setReadable(true);
		rightsSchemaAttribute.setUpdateable(true);

		rightsSchemaAttribute = schemaAttributeService.save(rightsSchemaAttribute);

		SysSystemAttributeMappingDto rightsAttribute = new SysSystemAttributeMappingDto();
		rightsAttribute.setSchemaAttribute(rightsSchemaAttribute.getId());
		rightsAttribute.setSystemMapping(mapping.getId());
		rightsAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);
		rightsAttribute = attributeMappingService.save(rightsAttribute);

		SysRoleSystemAttributeDto roleAttributeOne = new SysRoleSystemAttributeDto();
		roleAttributeOne.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOne.setRoleSystem(roleSystemOne.getId());
		roleAttributeOne.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOne.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOne.setTransformToResourceScript("return '" + ONE_VALUE + "';");
		roleAttributeOne = roleSystemAttributeService.saveInternal(roleAttributeOne);

		List<Serializable> controlledAttributeValues = attributeMappingService
				.getControlledAttributeValues(system.getId(), mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		SysRoleSystemAttributeDto roleAttributeTwo = new SysRoleSystemAttributeDto();
		roleAttributeTwo.setName(RIGHTS_ATTRIBUTE);
		roleAttributeTwo.setRoleSystem(roleSystemTwo.getId());
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(rightsAttribute.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		List<Serializable> historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());

		// Set attribute TWO as SET (should be disappears from controlled values
		// and appears in the history)
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.SET);
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(1, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));

		// Search historic controlled values for that attribute
		historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(1, historicControlledValues.size());
		assertTrue(historicControlledValues.contains(TWO_VALUE));

		// Set attribute TWO as MERGE (should be appears in controlled values
		// and disappears from the history)
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		// Manual recalculation (needed for deleting redundant historic value)
		attributeMappingService.recalculateAttributeControlledValues(system.getId(), mapping.getEntityType(),
				RIGHTS_ATTRIBUTE, rightsAttribute);

		// Search historic controlled values for that attribute
		historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());
	}

	@Test
	public void testCachedControlledAndHistoricValues() {
		SysSystemDto system = helper.createSystem("test_resource");
		SysSystemMappingDto mapping = helper.createMapping(system);
		IdmRoleDto roleOne = helper.createRole();
		IdmRoleDto roleTwo = helper.createRole();

		SysRoleSystemDto roleSystemOne = helper.createRoleSystem(roleOne, system);
		SysRoleSystemDto roleSystemTwo = helper.createRoleSystem(roleTwo, system);

		SysSchemaAttributeDto rightsSchemaAttribute = new SysSchemaAttributeDto();
		rightsSchemaAttribute.setObjectClass(mapping.getObjectClass());
		rightsSchemaAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsSchemaAttribute.setMultivalued(true);
		rightsSchemaAttribute.setClassType(String.class.getName());
		rightsSchemaAttribute.setReadable(true);
		rightsSchemaAttribute.setUpdateable(true);

		rightsSchemaAttribute = schemaAttributeService.save(rightsSchemaAttribute);

		SysSystemAttributeMappingDto rightsAttribute = new SysSystemAttributeMappingDto();
		rightsAttribute.setSchemaAttribute(rightsSchemaAttribute.getId());
		rightsAttribute.setSystemMapping(mapping.getId());
		rightsAttribute.setName(RIGHTS_ATTRIBUTE);
		rightsAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);
		rightsAttribute = attributeMappingService.save(rightsAttribute);

		SysRoleSystemAttributeDto roleAttributeOne = new SysRoleSystemAttributeDto();
		roleAttributeOne.setName(RIGHTS_ATTRIBUTE);
		roleAttributeOne.setRoleSystem(roleSystemOne.getId());
		roleAttributeOne.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeOne.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeOne.setTransformToResourceScript("return '" + ONE_VALUE + "';");
		roleAttributeOne = roleSystemAttributeService.saveInternal(roleAttributeOne);

		List<Serializable> controlledAttributeValues = attributeMappingService
				.getControlledAttributeValues(system.getId(), mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		SysRoleSystemAttributeDto roleAttributeTwo = new SysRoleSystemAttributeDto();
		roleAttributeTwo.setName(RIGHTS_ATTRIBUTE);
		roleAttributeTwo.setRoleSystem(roleSystemTwo.getId());
		roleAttributeTwo.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleAttributeTwo.setSystemAttributeMapping(rightsAttribute.getId());
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE));

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(rightsAttribute.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		List<Serializable> historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		assertNotNull(historicControlledValues);
		assertEquals(0, historicControlledValues.size());

		// Manual recalculation
		attributeMappingService.recalculateAttributeControlledValues(system.getId(), mapping.getEntityType(),
				RIGHTS_ATTRIBUTE, rightsAttribute);
		// Check cached controlled and historic values (results are controlled and
		// historic values in one lists)
		List<Serializable> cachedControlledAndHistoricAttributeValues = attributeMappingService
				.getCachedControlledAndHistoricAttributeValues(system.getId(), mapping.getEntityType(),
						RIGHTS_ATTRIBUTE);
		assertNotNull(cachedControlledAndHistoricAttributeValues);
		assertEquals(2, cachedControlledAndHistoricAttributeValues.size());
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(ONE_VALUE));
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(TWO_VALUE));

		// Change value definition on attribute TWO (should be changed in controlled
		// values
		// and old value appears in the history)
		roleAttributeTwo.setTransformToResourceScript("return '" + TWO_VALUE + "Changed';");
		roleAttributeTwo = roleSystemAttributeService.saveInternal(roleAttributeTwo);

		controlledAttributeValues = attributeMappingService.getControlledAttributeValues(system.getId(),
				mapping.getEntityType(), RIGHTS_ATTRIBUTE);

		assertNotNull(controlledAttributeValues);
		assertEquals(2, controlledAttributeValues.size());
		assertTrue(controlledAttributeValues.contains(ONE_VALUE));
		assertTrue(controlledAttributeValues.contains(TWO_VALUE + "Changed"));

		// Search historic controlled values for that attribute
		List<SysAttributeControlledValueDto> historicControlledValueDtos = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent();

		assertNotNull(historicControlledValueDtos);
		assertEquals(1, historicControlledValueDtos.size());
		assertTrue(historicControlledValueDtos.get(0).getValue().equals(TWO_VALUE));

		// Manual recalculation (needed for deleting redundant historic value)
		attributeMappingService.recalculateAttributeControlledValues(system.getId(), mapping.getEntityType(),
				RIGHTS_ATTRIBUTE, rightsAttribute);
		// Check cached controlled and historic values (results are controlled and
		// historic values in one lists)
		cachedControlledAndHistoricAttributeValues = attributeMappingService
				.getCachedControlledAndHistoricAttributeValues(system.getId(), mapping.getEntityType(),
						RIGHTS_ATTRIBUTE);
		assertNotNull(cachedControlledAndHistoricAttributeValues);
		assertEquals(3, cachedControlledAndHistoricAttributeValues.size());
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(ONE_VALUE));
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(TWO_VALUE));
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(TWO_VALUE + "Changed"));

		// Delete historic value TWO. Should be disappear from the cached values
		attributeControlledValueService.delete(historicControlledValueDtos.get(0));

		cachedControlledAndHistoricAttributeValues = attributeMappingService
				.getCachedControlledAndHistoricAttributeValues(system.getId(), mapping.getEntityType(),
						RIGHTS_ATTRIBUTE);
		assertNotNull(cachedControlledAndHistoricAttributeValues);
		assertEquals(2, cachedControlledAndHistoricAttributeValues.size());
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(ONE_VALUE));
		assertTrue(cachedControlledAndHistoricAttributeValues.contains(TWO_VALUE + "Changed"));

	}

}
