package eu.bcvsolutions.idm.core.generator.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.generator.AbstractGeneratorTest;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.generator.identity.IdentityFormDefaultValueGenerator;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;

/**
 * Tests for {@link IdentityFormDefaultValueGenerator}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class IdentityFormDefaultValueGeneratorTest extends AbstractGeneratorTest {

	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private IdmFormDefinitionService formDefinitionService;
	@Autowired
	private IdmFormAttributeService formAttributeService;
	
	@Test
	public void testGreenLine() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition();
		
		// prepare form attribute 1
		String attrCode1 = getHelper().createName();
		String attrDefaultValue1 = "100200";
		IdmFormAttributeDto att1 = createAttribute(attrCode1, attrDefaultValue1, PersistentType.LONG, formDefinition.getId());

		// prepare form attribute 2
		String attrCode2 = getHelper().createName();
		String attrDefaultValue2 = getHelper().createName() + getHelper().createName();
		IdmFormAttributeDto att2 = createAttribute(attrCode2, attrDefaultValue2, PersistentType.SHORTTEXT, formDefinition.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, null);

		// generate and check values after
		identity = this.valueGeneratorManager.generate(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		assertEquals(attrDefaultValue1, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		assertEquals(attrDefaultValue2, value.getValue().toString());
	}

	@Test
	public void testGreenLineWithSave() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition();
		
		// prepare form attribute 1
		String attrCode1 = getHelper().createName();
		String attrDefaultValue1 = "true";
		IdmFormAttributeDto att1 = createAttribute(attrCode1, attrDefaultValue1, PersistentType.BOOLEAN, formDefinition.getId());

		// prepare form attribute 2
		String attrCode2 = getHelper().createName();
		String attrDefaultValue2 = "A";
		IdmFormAttributeDto att2 = createAttribute(attrCode2, attrDefaultValue2, PersistentType.CHAR, formDefinition.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, null);

		// generate and check values after
		identity = identityService.save(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		assertEquals(attrDefaultValue1, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		assertEquals(attrDefaultValue2, value.getValue().toString());
	}

	@Test
	public void testRegenerateOff() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition();
		
		// prepare form attribute 1
		String attrCode1 = getHelper().createName();
		String attrDefaultValue1 = "true";
		IdmFormAttributeDto att1 = createAttribute(attrCode1, attrDefaultValue1, PersistentType.BOOLEAN, formDefinition.getId());

		// prepare form attribute 2
		String attrCode2 = getHelper().createName();
		String attrDefaultValue2 = "A";
		IdmFormAttributeDto att2 = createAttribute(attrCode2, attrDefaultValue2, PersistentType.CHAR, formDefinition.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, Boolean.FALSE);

		IdmFormInstanceDto formInstance = new IdmFormInstanceDto();
		formInstance.setFormDefinition(formDefinition);
		
		// create identity with given one EAV
		String givenNewValue = "false";
		List<IdmFormValueDto> givenValues = new ArrayList<>();
		IdmFormValueDto givenValue = new IdmFormValueDto(att1);
		givenValue.setValue(givenNewValue);
		givenValues.add(givenValue);
		formInstance.setValues(givenValues);
		eavs.add(formInstance);
		identity.setEavs(eavs);

		// generate and check values after
		identity = identityService.save(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		
		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		// we check manualy given
		assertEquals(givenNewValue, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		assertEquals(attrDefaultValue2, value.getValue().toString());
	}

	@Test
	public void testRegenerateOn() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition();
		
		// prepare form attribute 1
		String attrCode1 = getHelper().createName();
		String attrDefaultValue1 = UUID.randomUUID().toString();
		IdmFormAttributeDto att1 = createAttribute(attrCode1, attrDefaultValue1, PersistentType.UUID, formDefinition.getId());

		// prepare form attribute 2
		String attrCode2 = getHelper().createName();
		String attrDefaultValue2 = "100";
		IdmFormAttributeDto att2 = createAttribute(attrCode2, attrDefaultValue2, PersistentType.INT, formDefinition.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, null);

		IdmFormInstanceDto formInstance = new IdmFormInstanceDto();
		formInstance.setFormDefinition(formDefinition);
		
		// create identity with given one EAV
		String givenNewValue = UUID.randomUUID().toString();
		List<IdmFormValueDto> givenValues = new ArrayList<>();
		IdmFormValueDto givenValue = new IdmFormValueDto(att1);
		givenValue.setValue(givenNewValue);
		givenValues.add(givenValue);
		formInstance.setValues(givenValues);
		eavs.add(formInstance);
		identity.setEavs(eavs);

		// generate and check values after
		identity = identityService.save(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		
		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		assertEquals(attrDefaultValue1, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		assertEquals(attrDefaultValue2, value.getValue().toString());
	}

	@Test
	public void testNoDefaultValues() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition();
		
		// prepare form attribute 1
		String attrCode1 = getHelper().createName();
		IdmFormAttributeDto att1 = createAttribute(attrCode1, null, PersistentType.UUID, formDefinition.getId());

		// prepare form attribute 2
		String attrCode2 = getHelper().createName();
		IdmFormAttributeDto att2 = createAttribute(attrCode2, null, PersistentType.INT, formDefinition.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, null);

		IdmFormInstanceDto formInstance = new IdmFormInstanceDto();
		formInstance.setFormDefinition(formDefinition);
		
		// create identity with given one EAV
		String givenNewValue = UUID.randomUUID().toString();
		List<IdmFormValueDto> givenValues = new ArrayList<>();
		IdmFormValueDto givenValue = new IdmFormValueDto(att1);
		givenValue.setValue(givenNewValue);
		givenValues.add(givenValue);
		formInstance.setValues(givenValues);
		eavs.add(formInstance);
		identity.setEavs(eavs);

		// generate and check values after
		identity = identityService.save(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		
		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		// we check manually given
		assertEquals(givenNewValue, value.getValue().toString());

		// for second value doesn't exist default value
		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(0, values.size());
	}

	@Test
	public void testMoreDefinition() {
		// prepare identity
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(getHelper().createName());
		
		// prepare new form definition
		IdmFormDefinitionDto formDefinition = createFormDefinition("defOne");
		IdmFormDefinitionDto formDefinition2 = createFormDefinition("defTwo");
		
		// prepare form attribute 1 for first definition
		String attrCode1 = "defOneAttOne";
		String attrDefaultValue1 = UUID.randomUUID().toString();
		IdmFormAttributeDto att1 = createAttribute(attrCode1, attrDefaultValue1, PersistentType.UUID, formDefinition.getId());

		// prepare form attribute 2 for first definition
		String attrCode2 = "defOneAttTwo";
		String attrDefaultValue2 = "100";
		IdmFormAttributeDto att2 = createAttribute(attrCode2, attrDefaultValue2, PersistentType.INT, formDefinition.getId());

		// prepare form attribute 1 for second definition
		String attrCode3 = "defTwoAttThree";
		String attrDefaultValue3 = UUID.randomUUID().toString();
		IdmFormAttributeDto att3 = createAttribute(attrCode3, attrDefaultValue3, PersistentType.SHORTTEXT, formDefinition2.getId());

		// prepare form attribute 2 for second definition
		String attrCode4 = "defTwoAttFour";
		String attrDefaultValue4 = getHelper().createName() + getHelper().createName() + getHelper().createName();
		IdmFormAttributeDto att4 = createAttribute(attrCode4, attrDefaultValue4, PersistentType.TEXT, formDefinition2.getId());

		// check eav before
		List<IdmFormInstanceDto> eavs = identity.getEavs();
		assertTrue(eavs.isEmpty());

		// create generator
		ValueGeneratorDto generator = getGenerator();
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, Boolean.TRUE);

		IdmFormInstanceDto formInstance = new IdmFormInstanceDto();
		formInstance.setFormDefinition(formDefinition);
		
		// create identity with given one EAV
		String givenNewValue = UUID.randomUUID().toString();
		List<IdmFormValueDto> givenValues = new ArrayList<>();
		IdmFormValueDto givenValue = new IdmFormValueDto(att1);
		givenValue.setValue(givenNewValue);
		givenValues.add(givenValue);
		formInstance.setValues(givenValues);
		eavs.add(formInstance);
		
		// create second value from second form definition
		IdmFormInstanceDto formInstance2 = new IdmFormInstanceDto();
		formInstance2.setFormDefinition(formDefinition2);
		
		// create identity with given one EAV
		String givenNewValue2 = String.valueOf(System.currentTimeMillis());
		List<IdmFormValueDto> givenValues2 = new ArrayList<>();
		IdmFormValueDto givenValue2 = new IdmFormValueDto(att4);
		givenValue2.setValue(givenNewValue2);
		givenValues2.add(givenValue2);
		formInstance2.setValues(givenValues2);
		eavs.add(formInstance2);
		
		
		identity.setEavs(eavs);

		// generate and check values after
		identity = valueGeneratorManager.generate(identity);
		eavs = identity.getEavs();
		assertFalse(eavs.isEmpty());

		
		// get newly generated eav only for given form definition
		IdmFormInstanceDto generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);

		// check values
		List<IdmFormValueDto> values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att1.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		IdmFormValueDto value = values.get(0);
		// ///////////////////////////////////assertEquals(givenNewValue, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att2.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		// assertEquals(attrDefaultValue2, value.getValue().toString());
		
		// check second form
		generatedEav = eavs.stream().filter(eav -> eav.getFormDefinition().getCode().equals(formDefinition2.getCode())).findFirst().orElse(null);
		assertNotNull(generatedEav);
		
		// check values
		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att3.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		// assertEquals(attrDefaultValue3, value.getValue().toString());

		values = generatedEav.getValues().stream().filter(val -> val.getFormAttribute().equals(att4.getId())).collect(Collectors.toList());
		assertEquals(1, values.size());
		value = values.get(0);
		// assertEquals(givenNewValue2, value.getValue().toString());
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		return IdmIdentityDto.class;
	}

	@Override
	protected String getGeneratorType() {
		return IdentityFormDefaultValueGenerator.class.getCanonicalName();
	}

	/**
	 * Create new attribute with default value and given code.
	 *
	 * @param attCode
	 * @param attDefaultValue
	 * @param type
	 * @param formDefinition
	 * @return
	 */
	private IdmFormAttributeDto createAttribute(String attCode, String attDefaultValue, PersistentType type, UUID formDefinition) {
		IdmFormAttributeDto attributeDto = new IdmFormAttributeDto(attCode, attCode, type);
		attributeDto.setDefaultValue(attDefaultValue);
		attributeDto.setFormDefinition(formDefinition);
		return formAttributeService.save(attributeDto);
	}

	/**
	 * Create new definition for identity
	 *
	 * @return
	 */
	private IdmFormDefinitionDto createFormDefinition() {
		return createFormDefinition(getHelper().createName());
	}
	
	/**
	 * Create new definition for identity with name
	 *
	 * @return
	 */
	private IdmFormDefinitionDto createFormDefinition(String name) {
		IdmFormDefinitionDto formDefinition = new IdmFormDefinitionDto();
		formDefinition.setCode(name);
		formDefinition.setName(name);
		formDefinition.setType(IdmIdentity.class.getCanonicalName());
		return formDefinitionService.save(formDefinition);
	}
}
