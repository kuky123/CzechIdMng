package eu.bcvsolutions.idm.core.eav.api.dto;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.test.api.AbstractUnitTest;

/**
 * Form value conversion
 * 
 * TODO: all persistent types
 * 
 * @author Radek Tomiška
 *
 */
public class IdmFormValueDtoUnitTest extends AbstractUnitTest {
	
	@Test
	public void testNullValues() {
		for (PersistentType persistentType : PersistentType.values()) {
			IdmFormValueDto formValue = new IdmFormValueDto();
			formValue.setPersistentType(persistentType);
			formValue.setValue(null);
			//
			Assert.assertNull(formValue.getValue());
			Assert.assertTrue(formValue.isEmpty());
			Assert.assertTrue(formValue.isNull());
		}
	}
	
	@Test
	public void testConstructByAttribute() {
		IdmFormAttributeDto attribute = new IdmFormAttributeDto();
		attribute.setId(UUID.randomUUID());
		attribute.setPersistentType(PersistentType.SHORTTEXT);
		attribute.setConfidential(true);
		//
		IdmFormValueDto formValue = new IdmFormValueDto(attribute);
		//
		Assert.assertEquals(attribute.getId(), formValue.getFormAttribute());
		Assert.assertEquals(attribute.getPersistentType(), formValue.getPersistentType());
		Assert.assertEquals(attribute.isConfidential(), formValue.isConfidential());
		Assert.assertEquals(attribute, DtoUtils.getEmbedded(formValue, IdmFormValueDto.PROPERTY_FORM_ATTRIBUTE));
	}

	@Test
	public void testDateTimeValueAsDateTime() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		DateTime current = new DateTime();
		
		formValue.setValue(current);
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testDateTimeValueAsDate() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		DateTime current = new DateTime();
		
		formValue.setValue(current.toDate());
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testDateTimeValueAsLong() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		DateTime current = new DateTime();
		
		formValue.setValue(current.toDate().getTime());
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testDateTimeValueAsLocalDate() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		DateTime current = new DateTime().withTimeAtStartOfDay();
		
		formValue.setValue(current.toLocalDate());
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testDateTimeValueAsStringWithTimeZone() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		//
		DateTime current = new DateTime();		
		
		formValue.setValue(current.toString());
		// time zone default vs. constructed
		Assert.assertTrue(current.isEqual((DateTime) formValue.getValue()));
	}
	
	@Test
	public void testDateTimeValueAsStringWithUtc() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		//
		DateTime current = new DateTime(DateTimeZone.UTC);		
		
		formValue.setValue(current.toString());
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testDateValueAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATE);
		//
		LocalDate current = new LocalDate();		
		
		formValue.setValue(current.toString());
		
		Assert.assertEquals(current, ((DateTime) formValue.getValue()).toLocalDate());
	}
	
	@Test(expected = ResultCodeException.class)
	public void testWrongDate() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		formValue.setValue("wrong");
	}
	
	@Test(expected = ResultCodeException.class)
	public void testWrongDateObject() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.DATETIME);
		
		formValue.setValue(new IdmFormValueDto());
	}
	
	@Test
	public void uuidAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.UUID);
		UUID uuid = UUID.randomUUID();
		
		formValue.setValue(uuid.toString());
		
		Assert.assertEquals(uuid, formValue.getValue());
	}
	
	@Test
	public void attachmentAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.ATTACHMENT);
		UUID uuid = UUID.randomUUID();
		
		formValue.setValue(uuid.toString());
		
		Assert.assertEquals(uuid, formValue.getValue());
	}
	
	@Test
	public void testLongValueAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.LONG);
		//
		Long current = 14896357892248L;		
		
		formValue.setValue(current.toString());
		
		Assert.assertEquals(current, formValue.getValue());
	}
	
	@Test
	public void testIntValueAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.INT);
		//
		Long current = 14896357892248L;		
		
		formValue.setValue(current.toString());
		
		Assert.assertEquals(current.intValue(), formValue.getValue());
	}
	
	@Test
	public void testBooleanValueAsString() {
		IdmFormValueDto formValue = new IdmFormValueDto();
		formValue.setPersistentType(PersistentType.BOOLEAN);
		//
		Boolean current = Boolean.TRUE;		
		
		formValue.setValue(current.toString());
		Assert.assertEquals(current, formValue.getValue());
		formValue.setValue("ff");
		Assert.assertFalse((Boolean) formValue.getValue());
		formValue.setValue("false");
		Assert.assertFalse((Boolean) formValue.getValue());
		formValue.setValue("TRUE");
		Assert.assertTrue((Boolean) formValue.getValue());
	}
}
