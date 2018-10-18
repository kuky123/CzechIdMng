package eu.bcvsolutions.idm.core.eav.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.InitDemoData;
import eu.bcvsolutions.idm.core.api.config.domain.PrivateIdentityConfiguration;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.FormableDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormValueFilter;
import eu.bcvsolutions.idm.core.eav.api.service.AbstractFormableService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute_;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.ecm.service.DefaultAttachmentManagerIntegrationTest;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmIdentityFormValue;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmRoleFormValue;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmRoleFormValue_;
import eu.bcvsolutions.idm.core.model.repository.IdmRoleRepository;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.evaluator.eav.AbstractFormValueEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.eav.IdentityFormValueEvaluator;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Target system tests
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class DefaultFormServiceItegrationTest extends AbstractIntegrationTest {
	
	private final static String FORM_VALUE_ONE = "one";
	private final static String FORM_VALUE_TWO = "two";
	private final static String FORM_VALUE_THREE = "three";
	private final static String FORM_VALUE_FOUR = "four";
	
	@Autowired private ApplicationContext context;
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmFormDefinitionService formDefinitionService;	
	@Autowired private IdmRoleRepository roleRepository;
	@Autowired private LookupService lookupService;
	@Autowired private AttachmentManager attachmentManager;
	//
	private DefaultFormService formService;
	
	@Before
	public void init() {
		formService = context.getAutowireCapableBeanFactory().createBean(DefaultFormService.class);
	}
	
	@Test
	public void testFillFormValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition one		
		IdmFormAttributeDto attributeDefinitionOne = new IdmFormAttributeDto();
		attributeDefinitionOne.setCode("name_" + System.currentTimeMillis());
		attributeDefinitionOne.setName(attributeDefinitionOne.getCode());
		attributeDefinitionOne.setPersistentType(PersistentType.TEXT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v1", Lists.newArrayList(attributeDefinitionOne));
		attributeDefinitionOne = formDefinitionOne.getMappedAttributeByCode(attributeDefinitionOne.getCode());
		//
		// create definition two		
		IdmFormAttributeDto attributeDefinitionTwo = new IdmFormAttributeDto();
		attributeDefinitionTwo.setCode("name_" + System.currentTimeMillis());
		attributeDefinitionTwo.setName(attributeDefinitionTwo.getCode());
		attributeDefinitionTwo.setPersistentType(PersistentType.TEXT);		
		IdmFormDefinitionDto formDefinitionTwo = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v2", Lists.newArrayList(attributeDefinitionTwo));
		attributeDefinitionTwo = formDefinitionTwo.getMappedAttributeByCode(attributeDefinitionTwo.getCode());
		//		
		IdmFormValueDto value1 = new IdmFormValueDto(attributeDefinitionOne);
		value1.setValue(FORM_VALUE_ONE);
		
		IdmFormValueDto value2 = new IdmFormValueDto(attributeDefinitionTwo);
		value2.setValue(FORM_VALUE_TWO);
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1));
		formService.saveValues(owner, formDefinitionTwo, Lists.newArrayList(value2));
		
		assertEquals(FORM_VALUE_ONE, formService.getValues(owner, formDefinitionOne).get(0).getStringValue());
		assertEquals(FORM_VALUE_TWO, formService.getValues(owner, formDefinitionTwo).get(0).getStringValue());
		//
		// create second owner
		Identifiable owner2 = getHelper().createIdentity((GuardedString) null);
		
		assertEquals(0, formService.getValues(owner2, formDefinitionOne).size());
		assertEquals(1, formService.getValues(owner, formDefinitionOne).size());
		assertEquals(1, formService.getValues(owner, formDefinitionTwo).size());
		
		identityService.deleteById(owner2.getId());
		
		assertEquals(0, formService.getValues(new IdmIdentity((UUID)owner2.getId()), formDefinitionOne).size()); // TODO: remove this test? - not exists entity has to be given
		assertEquals(1, formService.getValues(owner, formDefinitionOne).size());
		assertEquals(1, formService.getValues(owner, formDefinitionTwo).size());
		
		formService.deleteValues(owner, formDefinitionOne);		
		assertEquals(0, formService.getValues(owner, formDefinitionOne).size());
		assertEquals(1, formService.getValues(owner, formDefinitionTwo).size());
		assertEquals(FORM_VALUE_TWO, formService.getValues(owner, formDefinitionTwo).get(0).getStringValue());
		
		identityService.deleteById(owner.getId());
		
		assertEquals(0, formService.getValues(new IdmIdentity((UUID)owner.getId()), formDefinitionOne).size());
		assertEquals(0, formService.getValues(new IdmIdentity((UUID)owner.getId()), formDefinitionTwo).size());		
	}
	
	/**
	 * Test multi values order and removal
	 */
	@Test
	public void testMultipleValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with multi parameter	
		IdmFormAttributeDto multiAttribite = new IdmFormAttributeDto();
		String multiAttributeName = "name_" + System.currentTimeMillis();
		multiAttribite.setCode(multiAttributeName);
		multiAttribite.setName(multiAttribite.getCode());
		multiAttribite.setPersistentType(PersistentType.TEXT);
		multiAttribite.setMultiple(true);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v3", Lists.newArrayList(multiAttribite));
		multiAttribite = formDefinitionOne.getMappedAttributeByCode(multiAttribite.getCode());
		//
		// fill values
		IdmFormValueDto value1 = new IdmFormValueDto(multiAttribite);
		value1.setValue(FORM_VALUE_ONE);
		value1.setSeq((short) 0);
		
		IdmFormValueDto value2 = new IdmFormValueDto(multiAttribite);
		value2.setValue(FORM_VALUE_TWO);
		value2.setSeq((short) 1);
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1, value2));
		
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check order
		assertEquals(2, m.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, (m.get(multiAttributeName).get(0)).getValue());
		assertEquals(FORM_VALUE_TWO, (m.get(multiAttributeName).get(1)).getValue());
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1));
		
		// check delete unsaved multiple values
		m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		assertEquals(1, m.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, m.get(multiAttributeName).get(0).getValue());
		
		// checks value map
		Map<String, ? extends List<Serializable>> v = formService.getFormInstance(owner, formDefinitionOne).toPersistentValueMap();
		assertEquals(1, v.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, v.get(multiAttributeName).get(0));
	}
	
	@Test
	public void testDefaultDefinitionType() {
		assertEquals(IdmIdentity.class.getCanonicalName(), formService.getDefaultDefinitionType(IdmIdentity.class));
	}
	
	@Test
	public void testReadDefaultDefinition() {		
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		
		Assert.assertNotNull(formDefinition);
		Assert.assertEquals(IdmFormDefinitionService.DEFAULT_DEFINITION_CODE, formDefinition.getCode());
		Assert.assertEquals(IdmFormDefinitionService.DEFAULT_DEFINITION_CODE, formDefinition.getName());
		Assert.assertEquals(PersistentType.TEXT, formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE).getPersistentType());
	}
	
	@Test
	public void testGetMainDefinition() {
		IdmFormDefinitionDto mainDefinition = formService.createDefinition(IdmIdentityDto.class, getHelper().createName(), null);
		mainDefinition.setMain(true);
		mainDefinition = formService.saveDefinition(mainDefinition);
		IdmFormDefinitionDto otherDefinition = formService.createDefinition(IdmIdentity.class, getHelper().createName(), null);
		//
		IdmFormDefinitionDto result = formService.getDefinition(IdmIdentity.class, (String) null);
		//
		Assert.assertEquals(mainDefinition, result);
		//
		result = formService.getDefinition(formService.getDefaultDefinitionType(IdmIdentity.class));
		//
		Assert.assertEquals(mainDefinition, result);
		//
		result = formService.getDefinition(formService.getDefaultDefinitionType(IdmIdentity.class), otherDefinition.getCode());
		//
		Assert.assertEquals(otherDefinition, result);
	}
	
	@Test
	public void testGetDefinitions() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		IdmFormDefinitionDto definitionOne = formService.createDefinition(IdmIdentity.class, getHelper().createName(), null);
		IdmFormDefinitionDto definitionTwo = formService.createDefinition(owner.getClass(), getHelper().createName(), null);
		IdmFormDefinitionDto definitionOther = formService.createDefinition(IdmRole.class, getHelper().createName(), null);
		//
		List<IdmFormDefinitionDto> results = formService.getDefinitions(IdmIdentity.class);
		//
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionOne.getId())));
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionTwo.getId())));
		Assert.assertFalse(results.stream().anyMatch(d -> d.getId().equals(definitionOther.getId())));
		//
		results = formService.getDefinitions(owner.getClass());
		//
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionOne.getId())));
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionTwo.getId())));
		Assert.assertFalse(results.stream().anyMatch(d -> d.getId().equals(definitionOther.getId())));
		//
		results = formService.getDefinitions(owner);
		//
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionOne.getId())));
		Assert.assertTrue(results.stream().anyMatch(d -> d.getId().equals(definitionTwo.getId())));
		Assert.assertFalse(results.stream().anyMatch(d -> d.getId().equals(definitionOther.getId())));
	}
	
	@Test
	public void testIsFormable() {
		Assert.assertTrue(formService.isFormable(IdmIdentity.class));
		Assert.assertTrue(formService.isFormable(IdmIdentityDto.class));
		Assert.assertTrue(formService.isFormable(IdmRoleDto.class));
		//
		Assert.assertFalse(formService.isFormable(IdmFormDefinitionDto.class));
	}
	
	@Test
	public void testGetOwnerTypes() {
		List<String> ownerTypes = formService.getOwnerTypes();
		//
		Assert.assertTrue(ownerTypes.stream().anyMatch(o -> o.equals(formService.getDefaultDefinitionType(IdmIdentity.class))));
		Assert.assertTrue(ownerTypes.stream().anyMatch(o -> o.equals(formService.getDefaultDefinitionType(IdmRoleDto.class))));
		//
		Assert.assertFalse(ownerTypes.stream().anyMatch(o -> o.equals(IdmFormDefinition.class.getCanonicalName())));
	}
	
	@Test
	public void testGetAttributes() {
		IdmFormAttributeDto attributeOne = new IdmFormAttributeDto(getHelper().createName(), getHelper().createName(), PersistentType.SHORTTEXT);
		IdmFormAttributeDto attributeTwo = new IdmFormAttributeDto(getHelper().createName(), getHelper().createName(), PersistentType.SHORTTEXT);
		IdmFormDefinitionDto definitionOne = formService.createDefinition(
				IdmIdentity.class, 
				getHelper().createName(), 
				Lists.newArrayList(attributeOne, attributeTwo));
		IdmFormAttributeDto attributeOther = new IdmFormAttributeDto(getHelper().createName(), getHelper().createName(), PersistentType.SHORTTEXT);
		IdmFormDefinitionDto definitionOther = formService.createDefinition(
				IdmIdentityDto.class, 
				getHelper().createName(), 
				Lists.newArrayList(attributeOther));
		//
		List<IdmFormAttributeDto> results = formService.getAttributes(definitionOne);
		//
		Assert.assertTrue(results.stream().anyMatch(a -> a.getCode().equals(attributeOne.getCode())));
		Assert.assertTrue(results.stream().anyMatch(a -> a.getCode().equals(attributeTwo.getCode())));
		Assert.assertFalse(results.stream().anyMatch(a -> a.getCode().equals(attributeOther.getCode())));
		//
		results = formService.getAttributes(definitionOther);
		//
		Assert.assertFalse(results.stream().anyMatch(a -> a.getCode().equals(attributeOne.getCode())));
		Assert.assertFalse(results.stream().anyMatch(a -> a.getCode().equals(attributeTwo.getCode())));
		Assert.assertTrue(results.stream().anyMatch(a -> a.getCode().equals(attributeOther.getCode())));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testOwnerWithoutId() {
		// unpersisted identity
		Identifiable owner = new IdmIdentity();
		formService.getValues(owner);
	}
	
	@Test
	public void testUnpersistedOwnerWithId() {
		// unpersisted identity
		Identifiable owner = new IdmIdentity(UUID.randomUUID());
		assertTrue(formService.getValues(owner).isEmpty());
	}
	
	@Test
	public void testReadDefaultDefinitionValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		// save value into default owner and default form definition
		IdmFormValueDto value1 = new IdmFormValueDto(formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE));
		value1.setValue(FORM_VALUE_ONE);		
		
		formService.saveValues(owner, formDefinition, Lists.newArrayList(value1));
		
		IdmFormInstanceDto savedValues = formService.getFormInstance(owner);
		assertEquals(1, savedValues.getValues().size());
		assertEquals(FORM_VALUE_ONE, savedValues.toSinglePersistentValue(InitDemoData.FORM_ATTRIBUTE_PHONE));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testReadDefaultDefinitionValueNotSingle() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		// save value into default owner and default form definition
		IdmFormValueDto value1 = new IdmFormValueDto(formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_WWW));
		value1.setValue(FORM_VALUE_ONE);
		IdmFormValueDto value2 = new IdmFormValueDto(formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_WWW));
		value2.setValue(FORM_VALUE_TWO);
		
		formService.saveValues(owner, formDefinition, Lists.newArrayList(value1, value2));
		
		IdmFormInstanceDto savedValues = formService.getFormInstance(owner);
		assertEquals(2, savedValues.getValues().size());
		savedValues.toSinglePersistentValue(InitDemoData.FORM_ATTRIBUTE_WWW);
	}
	
	@Test
	public void testReadConfidentialFormValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		// save password
		IdmFormValueDto value1 = new IdmFormValueDto(formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PASSWORD));
		value1.setValue(FORM_VALUE_ONE);
		
		formService.saveValues(owner, formDefinition, Lists.newArrayList(value1));
		
		IdmFormInstanceDto savedValues = formService.getFormInstance(owner);
		assertEquals(1, savedValues.getValues().size());
		assertEquals(GuardedString.SECRED_PROXY_STRING, savedValues.toSinglePersistentValue(InitDemoData.FORM_ATTRIBUTE_PASSWORD));
	}
	
	@Test(expected = ResultCodeException.class)
	public void testDeleteDefinitionWithFormValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		
		// save password
		IdmFormValueDto value1 = new IdmFormValueDto(formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PASSWORD));
		value1.setValue(FORM_VALUE_ONE);
		
		formService.saveValues(owner, formDefinition, Lists.newArrayList(value1));
		
		List<IdmFormValueDto> savedValues = formService.getValues(owner);
		assertEquals(1, savedValues.size());
		
		formDefinitionService.delete(formDefinition);
	}
	
	@Test
	public void testSaveSingleAttributeValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE);
		// save value
		List<IdmFormValueDto> attributeValues = formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		
		assertEquals(1, attributeValues.size());
		assertEquals(FORM_VALUE_ONE, attributeValues.get(0).getValue());
		
		attributeValues = formService.getValues(owner, attribute);
		
		assertEquals(1, attributeValues.size());
		assertEquals(FORM_VALUE_ONE, attributeValues.get(0).getValue());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSaveMultipleAttributeValuesToSingleAttribute() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE);
		// save value
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE, FORM_VALUE_TWO));
	}
	
	@Test
	public void testDeleteSingleAttributeValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE);
		IdmFormAttributeDto attributeWWW = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_WWW);
		// save value
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		formService.saveValues(owner, attributeWWW, Lists.newArrayList(FORM_VALUE_ONE, FORM_VALUE_TWO));
		//
		List<IdmFormValueDto> attributeValues = formService.getValues(owner, attribute);		
		assertEquals(1, attributeValues.size());
		assertEquals(FORM_VALUE_ONE, attributeValues.get(0).getValue());
		List<IdmFormValueDto> attributeWWWValues = formService.getValues(owner, formDefinition, InitDemoData.FORM_ATTRIBUTE_WWW);		
		assertEquals(2, attributeWWWValues.size());
		assertEquals(FORM_VALUE_ONE, attributeWWWValues.get(0).getValue());
		//
		formService.deleteValues(owner, attribute);
		//
		attributeValues = formService.getValues(owner, attribute);		
		assertEquals(0, attributeValues.size());
		attributeWWWValues = formService.getValues(owner, attributeWWW);		
		assertEquals(2, attributeWWWValues.size());
		assertEquals(FORM_VALUE_ONE, attributeWWWValues.get(0).getValue());
	}
	
	@Test
	public void testEditMultipleAttributeValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		// save value
		formService.saveValues(owner, InitDemoData.FORM_ATTRIBUTE_WWW, Lists.newArrayList(FORM_VALUE_ONE, FORM_VALUE_TWO));
		//
		List<IdmFormValueDto> attributeWWWValues = formService.getValues(owner, InitDemoData.FORM_ATTRIBUTE_WWW);
		assertEquals(2, attributeWWWValues.size());
		assertEquals(FORM_VALUE_ONE, attributeWWWValues.get(0).getValue());
		// update
		formService.saveValues(owner, InitDemoData.FORM_ATTRIBUTE_WWW, Lists.newArrayList(FORM_VALUE_TWO));
		//
		attributeWWWValues = formService.getValues(owner, InitDemoData.FORM_ATTRIBUTE_WWW);		
		assertEquals(1, attributeWWWValues.size());
		assertEquals(FORM_VALUE_TWO, attributeWWWValues.get(0).getValue());
	}
	
	@Test
	public void testFindOwnersByMultiStringAttributeValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		Identifiable ownerTwo = getHelper().createIdentity((GuardedString) null);
		Identifiable ownerThree = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(owner.getClass());
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_WWW);
		// save values
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE, FORM_VALUE_TWO));
		formService.saveValues(ownerTwo, attribute, Lists.newArrayList(FORM_VALUE_THREE, FORM_VALUE_TWO));
		formService.saveValues(ownerThree, attribute, Lists.newArrayList(FORM_VALUE_FOUR, FORM_VALUE_FOUR));
		//
		Page<? extends Identifiable> owners = formService.findOwners(owner.getClass(), attribute, FORM_VALUE_ONE, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(owner.getClass(), attribute.getCode(), FORM_VALUE_TWO, null);
		assertEquals(2, owners.getTotalElements());
		//
		owners = formService.findOwners(owner.getClass(), attribute, FORM_VALUE_FOUR, null);
		assertEquals(1, owners.getTotalElements());
	}
	
	@Test
	public void testFindOwnersByStringAttributeValue() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto ownerTwo = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto ownerThree = getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE);
		// save values
		formService.saveValues(owner.getId(), IdmIdentity.class, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		formService.saveValues(ownerTwo.getId(), IdmIdentity.class, attribute, Lists.newArrayList(FORM_VALUE_TWO));
		formService.saveValues(ownerThree.getId(), IdmIdentity.class, attribute, Lists.newArrayList(FORM_VALUE_FOUR));
		//
		Page<? extends Identifiable> owners = formService.findOwners(IdmIdentity.class, attribute, FORM_VALUE_ONE, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(IdmIdentity.class, attribute.getCode(), FORM_VALUE_TWO, null);
		assertEquals(1, owners.getTotalElements());
		//
		owners = formService.findOwners(IdmIdentity.class, attribute, FORM_VALUE_FOUR, null);
		assertEquals(1, owners.getTotalElements());
	}
	
	@Test
	public void testFindTreeNodesByNullAttributeValue() {
		IdmTreeNodeDto owner = getHelper().createTreeNode();
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmTreeNode.class);
		IdmFormAttributeDto attribute = formDefinition.getFormAttributes().get(0);
		// save values
		formService.saveValues(owner.getId(), IdmTreeNode.class, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		//
		Page<? extends Identifiable> owners = formService.findOwners(IdmTreeNode.class, attribute, FORM_VALUE_ONE, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(IdmTreeNode.class, attribute.getCode(), null, null);
		assertEquals(0, owners.getTotalElements());
	}
	
	@Test
	public void testFindOwnersByDateAttributeValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		Identifiable ownerTwo = getHelper().createIdentity((GuardedString) null);
		
		IdmFormDefinitionDto formDefinition = formService.getDefinition(owner.getClass());
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_DATETIME);
		// save values
		DateTime now = new DateTime();
		DateTime tomorrow =  now.plusDays(1);
		formService.saveValues(owner, attribute, Lists.newArrayList(now));
		formService.saveValues(ownerTwo, attribute, Lists.newArrayList(tomorrow));
		//
		Page<? extends Identifiable> owners = formService.findOwners(owner.getClass(), InitDemoData.FORM_ATTRIBUTE_DATETIME, now, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(owner.getClass(), InitDemoData.FORM_ATTRIBUTE_DATETIME, tomorrow, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(ownerTwo.getId(), owners.getContent().get(0).getId());
		//
		identityService.deleteById(owner.getId());
		identityService.deleteById(ownerTwo.getId());
	}
	
	@Test
	public void testFindOwnersByUuidAttributeValue() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto ownerTwo = getHelper().createIdentity((GuardedString) null);
		
		IdmFormDefinitionDto formDefinition = formService.getDefinition(owner.getClass());
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_UUID);
		// save values
		UUID one = UUID.randomUUID();
		UUID two = UUID.randomUUID();
		formService.saveValues(owner, attribute, Lists.newArrayList(one));
		formService.saveValues(ownerTwo, attribute, Lists.newArrayList(two));
		//
		Page<? extends Identifiable> owners = formService.findOwners(owner.getClass(), InitDemoData.FORM_ATTRIBUTE_UUID, one, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(owner.getClass(), InitDemoData.FORM_ATTRIBUTE_UUID, two, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(ownerTwo.getId(), owners.getContent().get(0).getId());
		//
		identityService.deleteById(owner.getId());
		identityService.deleteById(ownerTwo.getId());
	}
	
	@Test
	public void testFindOwnersByShortTextAttributeValue() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto ownerTwo = getHelper().createIdentity((GuardedString) null);
		
		IdmFormAttributeDto shortTextAttr = new IdmFormAttributeDto();
		String shortTextAttrName = getHelper().createName();
		shortTextAttr.setCode(shortTextAttrName);
		shortTextAttr.setName(shortTextAttrName);
		shortTextAttr.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormAttributeDto attribute = formService.saveAttribute(owner.getClass(), shortTextAttr);
		// save values
		String one = "one";
		String two = "two";
		formService.saveValues(owner, attribute, Lists.newArrayList(one));
		formService.saveValues(ownerTwo, attribute, Lists.newArrayList(two));
		//
		Page<? extends Identifiable> owners = formService.findOwners(owner.getClass(), shortTextAttrName, one, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(owner.getId(), owners.getContent().get(0).getId());
		//
		owners = formService.findOwners(owner.getClass(), shortTextAttrName, two, null);
		//
		assertEquals(1, owners.getTotalElements());
		assertEquals(ownerTwo.getId(), owners.getContent().get(0).getId());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testFindOwnersByConfidentialAttribute() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		IdmFormAttributeDto confidentialAttr = new IdmFormAttributeDto(getHelper().createName());
		confidentialAttr.setConfidential(true);
		confidentialAttr = formService.saveAttribute(owner.getClass(), confidentialAttr);
		//
		formService.findOwners(owner.getClass(), confidentialAttr, "test", null);
	}	
	
	@Test
	public void testFindAttribute() {
		IdmFormAttributeDto attribute = formService.getAttribute(IdmIdentity.class, InitDemoData.FORM_ATTRIBUTE_DATETIME);
		//
		assertNotNull(attribute);
		assertEquals(PersistentType.DATETIME, attribute.getPersistentType());
	}
	
	@Test
	public void testFindAttributeNotExist() {
		IdmFormAttributeDto attribute = formService.getAttribute(IdmIdentity.class, "notExist_test");
		//
		assertNull(attribute);
	}
	
	@Test
	public void testSaveAttribute() {
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = "name_" + System.currentTimeMillis();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.TEXT);
		//
		attribute = formService.saveAttribute(IdmIdentity.class, attribute);
		//
		IdmFormAttributeDto savedAttr = formService.getAttribute(IdmIdentity.class, attribute.getCode());
		//
		assertNotNull(savedAttr);
		assertEquals(PersistentType.TEXT, savedAttr.getPersistentType());
		assertEquals(formService.getDefinition(IdmIdentity.class).getId(), savedAttr.getFormDefinition());
		//
		formService.deleteAttribute(attribute);
		//
		savedAttr = formService.getAttribute(IdmIdentity.class, attribute.getCode());
		//
		assertNull(savedAttr);
	}
	
	@Test
	public void findOwnerByCriteria() {
		IdmRoleDto owner = getHelper().createRole();
		IdmRoleDto ownerTwo = getHelper().createRole();
		
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmRole.class);
		IdmFormAttributeDto attribute = formDefinition.getFormAttributes().get(0);
		//
		formService.saveValues(owner.getId(), IdmRole.class, attribute, Lists.newArrayList("test"));
		formService.saveValues(ownerTwo.getId(), IdmRole.class, attribute, Lists.newArrayList("test2"));
		
		Specification<IdmRole> criteria = new Specification<IdmRole>() {
			public Predicate toPredicate(Root<IdmRole> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				Subquery<IdmRoleFormValue> subquery = query.subquery(IdmRoleFormValue.class);
				Root<IdmRoleFormValue> subRoot = subquery.from(IdmRoleFormValue.class);
				subquery.select(subRoot);
				
				Predicate predicate = builder.and(
						builder.equal(subRoot.get(IdmRoleFormValue_.owner), root),
						builder.equal(subRoot.get(IdmRoleFormValue_.formAttribute).get(IdmFormAttribute_.id), attribute.getId()),
						builder.equal(subRoot.get(IdmRoleFormValue_.stringValue), "test"));				
				subquery.where(predicate);
				//
				return query.where(builder.exists(subquery)).getRestriction();
			}
		};
		List<IdmRole> roles = roleRepository.findAll(criteria, (Pageable) null).getContent();
		assertEquals(1, roles.size());
		assertEquals(owner.getId(), roles.get(0).getId());
	}
	
	@Test
	public void testSaveValuesByOwnerId() {
		Identifiable owner =getHelper().createIdentity((GuardedString) null);
		IdmFormDefinitionDto formDefinition = formService.getDefinition(IdmIdentity.class);
		IdmFormAttributeDto attribute = formDefinition.getMappedAttributeByCode(InitDemoData.FORM_ATTRIBUTE_PHONE);
		// save value
		List<IdmFormValueDto> attributeValues = formService.saveValues(
				(UUID) owner.getId(), owner.getClass(), attribute, Lists.newArrayList(FORM_VALUE_ONE));
		
		assertEquals(1, attributeValues.size());
		assertEquals(FORM_VALUE_ONE, attributeValues.get(0).getValue());
		
		List<IdmFormValueDto> getValues = formService.getValues((UUID) owner.getId(), owner.getClass(), attribute);
		
		assertEquals(1, getValues.size());
		assertEquals(FORM_VALUE_ONE, ((IdmFormValueDto) getValues.get(0)).getValue());
		//
		identityService.deleteById(owner.getId());
	}
	
	@Test
	public void testFindDefinitionByDtoClass() {
		IdmFormDefinitionDto definitionByEntityClass = formService.getDefinition(IdmIdentity.class);
		IdmFormDefinitionDto definitionByDtoClass = formService.getDefinition(IdmIdentityDto.class);
		//
		Assert.assertNotNull(definitionByEntityClass);
		Assert.assertEquals(definitionByEntityClass, definitionByDtoClass);
	}
	
	/**
	 * #1051 - Change value persistent type, when single attribute is saved and attribute persistent type changed
	 */
	@Test
	public void testChangeAttributePersistentType() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with attribute
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = getHelper().createName();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), getHelper().createName(), Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// fill values		
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check value and persistent type
		assertEquals(1, m.get(attributeName).size());
		assertEquals(FORM_VALUE_ONE, (m.get(attributeName).get(0)).getValue());
		assertEquals(attribute.getPersistentType(), (m.get(attributeName).get(0)).getPersistentType());
		//
		// change attribute persistentn type
		attribute.setPersistentType(PersistentType.TEXT);
		attribute = formService.saveAttribute(attribute);
		// update value
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_TWO));
		m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		//
		// check value and persistent type
		assertEquals(1, m.get(attributeName).size());
		assertEquals(FORM_VALUE_TWO, (m.get(attributeName).get(0)).getValue());
		assertEquals(attribute.getPersistentType(), (m.get(attributeName).get(0)).getPersistentType());
	}
	
	@Test
	public void testDeleteEmptyValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with attribute
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = getHelper().createName();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), getHelper().createName(), Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// fill values		
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check value and persistent type
		assertEquals(1, m.get(attributeName).size());
		assertEquals(FORM_VALUE_ONE, (m.get(attributeName).get(0)).getValue());
		assertEquals(attribute.getPersistentType(), (m.get(attributeName).get(0)).getPersistentType());
		
		// update value
		formService.saveValues(owner, attribute, Lists.newArrayList("")); // empty string
		m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		//
		assertEquals(1, m.get(attributeName).size());
		assertEquals("", (m.get(attributeName).get(0)).getValue());
		assertEquals(attribute.getPersistentType(), (m.get(attributeName).get(0)).getPersistentType());
	}
	
	@Test
	public void testPreserveNotChangedValues() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with multi parameter	
		IdmFormAttributeDto multiAttribite = new IdmFormAttributeDto();
		String multiAttributeName = "name_" + System.currentTimeMillis();
		multiAttribite.setCode(multiAttributeName);
		multiAttribite.setName(multiAttribite.getCode());
		multiAttribite.setPersistentType(PersistentType.TEXT);
		multiAttribite.setMultiple(true);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v3", Lists.newArrayList(multiAttribite));
		multiAttribite = formDefinitionOne.getMappedAttributeByCode(multiAttribite.getCode());
		//
		// fill values
		IdmFormValueDto value1 = new IdmFormValueDto(multiAttribite);
		value1.setValue(FORM_VALUE_ONE);
		value1.setSeq((short) 0);
		
		IdmFormValueDto value2 = new IdmFormValueDto(multiAttribite);
		value2.setValue(FORM_VALUE_TWO);
		value2.setSeq((short) 1);
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1, value2));
		
		Map<String, List<IdmFormValueDto>> created = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check order
		assertEquals(2, created.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, created.get(multiAttributeName).get(0).getValue());
		assertEquals(FORM_VALUE_TWO, created.get(multiAttributeName).get(1).getValue());
		//
		// update
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(created.get(multiAttributeName).get(0), created.get(multiAttributeName).get(1)));
		//
		// check after update
		// values uuid should be the same 
		Map<String, List<IdmFormValueDto>> updated = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		assertEquals(2, updated.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, updated.get(multiAttributeName).get(0).getValue());
		assertEquals(FORM_VALUE_TWO, updated.get(multiAttributeName).get(1).getValue());
		assertEquals(created.get(multiAttributeName).get(0).getId(), updated.get(multiAttributeName).get(0).getId());
		assertEquals(created.get(multiAttributeName).get(1).getId(), updated.get(multiAttributeName).get(1).getId());
	}
	
	@Test
	public void testDeleteNullValue() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with attribute
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = getHelper().createName();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), getHelper().createName(), Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// fill values		
		formService.saveValues(owner, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check value and persistent type
		assertEquals(1, m.get(attributeName).size());
		assertEquals(FORM_VALUE_ONE, (m.get(attributeName).get(0)).getValue());
		assertEquals(attribute.getPersistentType(), (m.get(attributeName).get(0)).getPersistentType());
		
		// update value
		formService.saveValues(owner, attribute, Lists.newArrayList());
		m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		//
		assertNull(m.get(attributeName));
	}
	
	@Test
	public void testMultipleValuesWithNull() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with multi parameter	
		IdmFormAttributeDto multiAttribite = new IdmFormAttributeDto();
		String multiAttributeName = "name_" + System.currentTimeMillis();
		multiAttribite.setCode(multiAttributeName);
		multiAttribite.setName(multiAttribite.getCode());
		multiAttribite.setPersistentType(PersistentType.TEXT);
		multiAttribite.setMultiple(true);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v3", Lists.newArrayList(multiAttribite));
		multiAttribite = formDefinitionOne.getMappedAttributeByCode(multiAttribite.getCode());
		//
		// fill values
		IdmFormValueDto value1 = new IdmFormValueDto(multiAttribite);
		value1.setValue(FORM_VALUE_ONE);
		value1.setSeq((short) 0);
		
		IdmFormValueDto value2 = new IdmFormValueDto(multiAttribite);
		value2.setValue(FORM_VALUE_TWO);
		value2.setSeq((short) 1);
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1, value2));
		
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		
		// check order
		assertEquals(2, m.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, (m.get(multiAttributeName).get(0)).getValue());
		assertEquals(FORM_VALUE_TWO, (m.get(multiAttributeName).get(1)).getValue());
		
		value2.setValue(null);
		
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1, value2));
		
		// check delete unsaved multiple values
		m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		assertEquals(1, m.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, m.get(multiAttributeName).get(0).getValue());
		
		// checks value map
		Map<String, ? extends List<Serializable>> v = formService.getFormInstance(owner, formDefinitionOne).toPersistentValueMap();
		assertEquals(1, v.get(multiAttributeName).size());
		assertEquals(FORM_VALUE_ONE, v.get(multiAttributeName).get(0));
	}
	
	@Test
	public void testUpdateConfidentialProperty() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with confidential parameter	
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = "name_" + System.currentTimeMillis();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		attribute.setConfidential(true);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v3", Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// fill values
		IdmFormValueDto value1 = new IdmFormValueDto(attribute);
		value1.setValue(FORM_VALUE_ONE);
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(value1));
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		//
		// check 
		Assert.assertEquals(1, m.get(attributeName).size());
		Assert.assertEquals(GuardedString.SECRED_PROXY_STRING, (m.get(attributeName).get(0)).getValue());
		Assert.assertTrue(m.get(attributeName).get(0).isConfidential());
		//
		// check confidential value
		Assert.assertEquals(FORM_VALUE_ONE, formService.getConfidentialPersistentValue(m.get(attributeName).get(0)));
		//
		// save other values - confidential will not be included
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList());
		Assert.assertEquals(FORM_VALUE_ONE, formService.getConfidentialPersistentValue(m.get(attributeName).get(0)));
		//
		// update
		IdmFormValueDto confidentialValue = m.get(attributeName).get(0);
		confidentialValue.setValue("");
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(confidentialValue));
		Assert.assertEquals("", formService.getConfidentialPersistentValue(m.get(attributeName).get(0)));
		//
		// update 2
		confidentialValue = m.get(attributeName).get(0);
		confidentialValue.setValue(FORM_VALUE_ONE);
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(confidentialValue));
		Assert.assertEquals(FORM_VALUE_ONE, formService.getConfidentialPersistentValue(m.get(attributeName).get(0)));
		//
		confidentialValue.setValue(null);
		formService.saveValues(owner, formDefinitionOne, Lists.newArrayList(confidentialValue));
		Assert.assertNull(formService.getConfidentialPersistentValue(m.get(attributeName).get(0)));
	}
	
	@Test
	public void testSaveMultipleConfidentialProperties() {
		Identifiable owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with multi confidential parameter	
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = "name_" + System.currentTimeMillis();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		attribute.setConfidential(true);
		attribute.setMultiple(true);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(IdmIdentity.class.getCanonicalName(), "t_v3", Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// save three values
		IdmFormValueDto valueOne = new IdmFormValueDto(attribute);
		valueOne.setValue(FORM_VALUE_ONE);
		IdmFormValueDto valueTwo = new IdmFormValueDto(attribute);
		valueTwo.setValue(FORM_VALUE_TWO);
		IdmFormValueDto valueThree = new IdmFormValueDto(attribute);
		valueThree.setValue(FORM_VALUE_THREE);
		formService.saveValues(owner, formDefinitionOne.getId(), Lists.newArrayList(valueOne, valueTwo, valueThree));
		List<IdmFormValueDto> values = formService.getValues(owner, attribute);
		//
		Assert.assertEquals(3, values.size());
		valueOne = values.get(0);
		valueTwo = values.get(1);
		valueThree = values.get(2);
		Assert.assertEquals(FORM_VALUE_ONE, formService.getConfidentialPersistentValue(valueOne));
		Assert.assertEquals(FORM_VALUE_TWO, formService.getConfidentialPersistentValue(valueTwo));
		Assert.assertEquals(FORM_VALUE_THREE, formService.getConfidentialPersistentValue(valueThree));
		//
		// remove value two - we need to save remaining (one, three)
		valueOne.setValue(FORM_VALUE_ONE);
		valueThree.setValue(FORM_VALUE_THREE);
		formService.saveValues(owner, formDefinitionOne.getId(), Lists.newArrayList(valueOne, valueThree));
		//
		values = formService.getValues(owner, attribute);
		Assert.assertEquals(2, values.size());
		Assert.assertEquals(FORM_VALUE_ONE, formService.getConfidentialPersistentValue(valueOne));
		Assert.assertEquals(FORM_VALUE_THREE, formService.getConfidentialPersistentValue(valueThree));
		Assert.assertEquals(valueOne.getId(), values.get(0).getId());
		Assert.assertEquals(valueThree.getId(), values.get(1).getId());
		Assert.assertNull(formService.getConfidentialPersistentValue(valueTwo));
	}

	@Test
	public void testFindValues() {
		IdmIdentityDto ownerIdentity = getHelper().createIdentity((GuardedString) null);
		IdmRoleDto ownerRole = getHelper().createRole();
		IdmTreeNodeDto ownerTreeNode = getHelper().createTreeNode();
		IdmIdentityContractDto ownerIdentityContract = getHelper().createIdentityContact(ownerIdentity);
		//
		Assert.assertEquals(1, prepareDataAndFind(IdmIdentity.class, ownerIdentity));
		Assert.assertEquals(1, prepareDataAndFind(IdmRole.class, ownerRole));
		Assert.assertEquals(1, prepareDataAndFind(IdmTreeNode.class, ownerTreeNode));
		Assert.assertEquals(1, prepareDataAndFind(IdmIdentityContract.class, ownerIdentityContract));
	}
	
	@Test
	public void testSaveEavWithIdentityTogether() {
		// create owner
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		// create definition with parameter
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = getHelper().createName();
		attribute.setCode(attributeName);
		attribute.setName(attributeName);
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(
				IdmIdentity.class.getCanonicalName(),
				getHelper().createName(),
				Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// fill values
		IdmFormValueDto value = new IdmFormValueDto(attribute);
		value.setValue(FORM_VALUE_ONE);
		// + change owner
		owner.setFirstName(FORM_VALUE_ONE);
		owner.getEavs().add(new IdmFormInstanceDto(owner, formDefinitionOne, Lists.newArrayList(value)));
		identityService.save(owner);
		//
		// load saved
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
		Assert.assertEquals(1, m.get(attributeName).size());
		Assert.assertEquals(FORM_VALUE_ONE, (m.get(attributeName).get(0)).getValue());
		//
		// load owner
		Assert.assertEquals(FORM_VALUE_ONE, identityService.get(owner).getFirstName());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testSaveEavWithOwner() {
		List<FormableDto> owners = new ArrayList<>();
		owners.add(getHelper().createIdentity((GuardedString) null)); // implemented authorization policies for eav attributes
		owners.add(getHelper().createRole()); // without authorization policies for eav attributes
		//
		owners.forEach(owner -> {
			//
			// create definition with parameter
			IdmFormAttributeDto attribute = new IdmFormAttributeDto();
			String attributeName = getHelper().createName();
			attribute.setCode(attributeName);
			attribute.setName(attributeName);
			attribute.setPersistentType(PersistentType.SHORTTEXT);
			IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(
					owner.getClass().getCanonicalName(),
					getHelper().createName(),
					Lists.newArrayList(attribute));
			attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
			//
			// fill values
			IdmFormValueDto value = new IdmFormValueDto(attribute);
			value.setValue(FORM_VALUE_ONE);
			owner.getEavs().add(new IdmFormInstanceDto(owner, formDefinitionOne, Lists.newArrayList(value)));
			((AbstractFormableService) lookupService.getDtoService(owner.getClass())).save(owner);
			//
			// load saved
			Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinitionOne).toValueMap();
			Assert.assertEquals(1, m.get(attributeName).size());
			Assert.assertEquals(FORM_VALUE_ONE, (m.get(attributeName).get(0)).getValue());
		});
	}
	
	@Test
	public void testSaveEavByOwnerSecured() {
		// create owner
		IdmIdentityDto owner = getHelper().createIdentity();
		//
		// create definition with parameter
		IdmFormAttributeDto attributeNotSecured = new IdmFormAttributeDto();
		String attributeNotSecuredName = getHelper().createName();
		attributeNotSecured.setCode(attributeNotSecuredName);
		attributeNotSecured.setName(attributeNotSecuredName);
		attributeNotSecured.setPersistentType(PersistentType.SHORTTEXT);
		IdmFormAttributeDto attributeSecured = new IdmFormAttributeDto();
		String attributeSecuredName = getHelper().createName();
		attributeSecured.setCode(attributeSecuredName);
		attributeSecured.setName(attributeSecuredName);
		attributeSecured.setPersistentType(PersistentType.SHORTTEXT);
		//
		IdmFormDefinitionDto formDefinition = formService.createDefinition(
				IdmIdentity.class.getCanonicalName(),
				getHelper().createName(),
				Lists.newArrayList(attributeNotSecured, attributeSecured));
		attributeNotSecured = formDefinition.getMappedAttributeByCode(attributeNotSecured.getCode());
		attributeSecured = formDefinition.getMappedAttributeByCode(attributeSecured.getCode());
		//
		// fill values
		IdmFormValueDto valueNotSecured = new IdmFormValueDto(attributeNotSecured);
		valueNotSecured.setValue(FORM_VALUE_ONE);
		IdmFormValueDto valueSecured = new IdmFormValueDto(attributeSecured);
		valueSecured.setValue(FORM_VALUE_TWO);
		owner.getEavs().add(new IdmFormInstanceDto(owner, formDefinition, Lists.newArrayList(valueNotSecured, valueSecured)));
		identityService.save(owner);
		//
		// load saved
		Map<String, List<IdmFormValueDto>> m = formService.getFormInstance(owner, formDefinition).toValueMap();
		Assert.assertEquals(FORM_VALUE_ONE, (m.get(attributeNotSecuredName).get(0)).getValue());
		Assert.assertEquals(FORM_VALUE_TWO, (m.get(attributeSecuredName).get(0)).getValue());
		//
		// set authorization policies for owner - login + change
		IdmRoleDto role = getHelper().createRole();
		getHelper().createIdentityRole(owner, role);
		ConfigurationMap properties = new ConfigurationMap();
		properties.put(AbstractFormValueEvaluator.PARAMETER_FORM_DEFINITION, formDefinition.getId());
		properties.put(AbstractFormValueEvaluator.PARAMETER_FORM_ATTRIBUTES, attributeNotSecuredName);
		getHelper().createAuthorizationPolicy(
				role.getId(),
				CoreGroupPermission.FORMVALUE,
				IdmIdentityFormValue.class,
				IdentityFormValueEvaluator.class,
				properties,
				IdmBasePermission.READ, IdmBasePermission.UPDATE);
		getHelper().createBasePolicy(role.getId(), CoreGroupPermission.IDENTITY, IdmIdentity.class, IdmBasePermission.READ, IdmBasePermission.UPDATE);
		getHelper().createBasePolicy(role.getId(), CoreGroupPermission.FORMDEFINITION, IdmFormDefinition.class, IdmBasePermission.READ);
		getHelper().createBasePolicy(role.getId(), CoreGroupPermission.FORMATTRIBUTE, IdmFormAttribute.class, IdmBasePermission.READ);
		//
		String updatedValue = "updated";
		try {
			getHelper().setConfigurationValue(PrivateIdentityConfiguration.PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED, true);
			getHelper().login(owner);
			
			IdmIdentityDto updateOwner = identityService.get(owner, IdmBasePermission.READ);
			
			IdmFormValueDto changeValue = new IdmFormValueDto(attributeNotSecured);
			changeValue.setValue(updatedValue);
			updateOwner.getEavs().add(new IdmFormInstanceDto(updateOwner, formDefinition, Lists.newArrayList( changeValue)));
			identityService.save(updateOwner, IdmBasePermission.UPDATE);
			//
			// check
			m =  formService.getFormInstance(owner, formDefinition, IdmBasePermission.READ).toValueMap();
			Assert.assertEquals(updatedValue, (m.get(attributeNotSecuredName).get(0)).getValue());
			Assert.assertNull(FORM_VALUE_TWO, m.get(attributeSecuredName)); // not readable
		} finally {
			logout();
			getHelper().setConfigurationValue(PrivateIdentityConfiguration.PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED, false);
		}
		//
		// check secured attribute is untouched as admin
		m = formService.getFormInstance(owner, formDefinition).toValueMap();
		Assert.assertEquals(updatedValue, (m.get(attributeNotSecuredName).get(0)).getValue());
		Assert.assertEquals(FORM_VALUE_TWO, (m.get(attributeSecuredName).get(0)).getValue());
	}
	
	@Test
	public void testSaveAttachment() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		// create attachment ... upload attachment is solved by @IdmAttachmentControllerRestTest
		IdmAttachmentDto tempAttachment = createTempAttachment();
		//
		// create definition with attachment parameter
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = getHelper().createName();
		attribute.setCode(attributeName);
		attribute.setName(attributeName);
		attribute.setPersistentType(PersistentType.ATTACHMENT);
		IdmFormDefinitionDto formDefinitionOne = formService.createDefinition(
				IdmIdentity.class.getCanonicalName(),
				getHelper().createName(),
				Lists.newArrayList(attribute));
		attribute = formDefinitionOne.getMappedAttributeByCode(attribute.getCode());
		//
		// save new form value
		IdmFormValueDto value = new IdmFormValueDto(attribute);
		value.setShortTextValue(tempAttachment.getName()); // filename
		value.setValue(tempAttachment.getId());
		//
		List<IdmFormValueDto> saveValues = formService.saveValues(owner, formDefinitionOne.getId(), Lists.newArrayList(value));
		//
		Assert.assertEquals(1, saveValues.size());
		Assert.assertEquals(tempAttachment.getName(), saveValues.get(0).getShortTextValue()); // filename
		Assert.assertEquals(tempAttachment.getId(), saveValues.get(0).getUuidValue());
		//
		IdmAttachmentDto attachment = attachmentManager.get(saveValues.get(0).getUuidValue());
		Assert.assertEquals(saveValues.get(0).getId(), attachment.getOwnerId());
		Assert.assertNotEquals(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE, attachment.getOwnerType());
		//
		// update form value
		tempAttachment = createTempAttachment();
		value = new IdmFormValueDto(attribute);
		value.setShortTextValue(tempAttachment.getName()); // filename
		value.setValue(tempAttachment.getId());
		//
		saveValues = formService.saveValues(owner, formDefinitionOne.getId(), Lists.newArrayList(value));
		//
		Assert.assertEquals(1, saveValues.size());
		Assert.assertEquals(tempAttachment.getName(), saveValues.get(0).getShortTextValue()); // filename
		Assert.assertEquals(tempAttachment.getId(), saveValues.get(0).getUuidValue());
		//
		attachment = attachmentManager.get(attachment.getId());
		IdmAttachmentDto newAttachmentVersion = attachmentManager.get(saveValues.get(0).getUuidValue());
		Assert.assertEquals(saveValues.get(0).getId(), newAttachmentVersion.getOwnerId());
		Assert.assertNotEquals(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE, attachment.getOwnerType());
		Assert.assertEquals(attachment.getId(), newAttachmentVersion.getParent());
		Assert.assertEquals(newAttachmentVersion.getId(), attachment.getNextVersion());
		//
		// delete form value
		value = new IdmFormValueDto(attribute);
		saveValues = formService.saveValues(owner, formDefinitionOne.getId(), Lists.newArrayList(value));
		//
		Assert.assertTrue(saveValues.isEmpty());
		Assert.assertNull(attachmentManager.get(newAttachmentVersion.getId()));
		Assert.assertNull(attachmentManager.get(attachment.getId()));
	}
	
	private long prepareDataAndFind(Class<? extends AbstractEntity> type, AbstractDto owner) {
		//
		//create attribute
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		String attributeName = "name_" + System.currentTimeMillis();
		attribute.setCode(attributeName);
		attribute.setName(attribute.getCode());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		//
		//create definition
		IdmFormDefinitionDto definition = formService.createDefinition(type, getHelper().createName(), Lists.newArrayList(attribute));
		attribute = definition.getMappedAttributeByCode(attribute.getCode());
		//
		//save value
		formService.saveValues(owner.getId(), type, attribute, Lists.newArrayList(FORM_VALUE_ONE));
		//
		//find
		IdmFormValueFilter<?> filter = new IdmFormValueFilter<>();
		filter.setDefinitionId(definition.getId());
		Page<IdmFormValueDto> result = formService.findValues(filter, new PageRequest(0, Integer.MAX_VALUE));
		return result.getTotalElements();
	}
	
	private IdmAttachmentDto createTempAttachment() {
		IdmAttachmentDto dto = DefaultAttachmentManagerIntegrationTest.prepareDto();
		//
		return attachmentManager.save(dto);
	}
}
