package eu.bcvsolutions.idm.core.rest.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.config.domain.PrivateIdentityConfiguration;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmProfileDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmProfileService;
import eu.bcvsolutions.idm.core.bulk.action.impl.IdentityDisableBulkAction;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmIdentityFormValue;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.evaluator.eav.IdentityFormValueEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.SelfIdentityEvaluator;
import eu.bcvsolutions.idm.test.api.TestHelper;

/**
 * Identity controller tests
 * - CRUD
 * - bulk actions
 * - eav attributes with authorization policies
 * - profile CRUD
 * 
 * - TODO: move filters here
 * 
 * @author Radek Tomiška
 *
 */
public class IdmIdentityControllerRestTest extends AbstractReadWriteDtoControllerRestTest<IdmIdentityDto> {

	@Autowired private IdmIdentityController controller;
	@Autowired private PrivateIdentityConfiguration identityConfiguration;
	@Autowired private FormService formService;
	@Autowired private IdmIdentityService identityService;
	@Autowired private AttachmentManager attachmentManager;
	@Autowired private IdmProfileService profileService;
	
	@Override
	protected AbstractReadWriteDtoController<IdmIdentityDto, ?> getController() {
		return controller;
	}

	@Override
	protected IdmIdentityDto prepareDto() {
		IdmIdentityDto dto = new IdmIdentityDto();
		dto.setUsername(getHelper().createName());
		return dto;
	}
	
	@Test
    public void userNotFound() throws Exception {
		getMockMvc().perform(get(getDetailUrl("n_a_user"))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isNotFound());
    }
	
	@Test
    public void userFoundByUsername() throws Exception {
		getMockMvc().perform(get(getDetailUrl(TestHelper.ADMIN_USERNAME))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(jsonPath("$.username", equalTo(TestHelper.ADMIN_USERNAME)));
    }
	
	@Test
    public void testUsernameWithDotCharacter() throws Exception {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("admin.com");
		identity.setFirstName("test");
		identity.setLastName("test");
		identity = getHelper().getService(IdmIdentityService.class).save(identity);
		//
		getMockMvc().perform(get(getDetailUrl(URLEncoder.encode(identity.getUsername(), "UTF-8")))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(jsonPath("$.username", equalTo(identity.getUsername())));
    }
	
	@Test
	@Ignore // TODO: surefire parameters are ignored ... why?
    public void testUsernameWithSlashCharacter() throws Exception {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("admin/com");
		identity.setFirstName("test");
		identity.setLastName("test");
		identity = getHelper().getService(IdmIdentityService.class).save(identity);
		//
		getMockMvc().perform(get(getDetailUrl(URLEncoder.encode(identity.getUsername(), "UTF-8")))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(jsonPath("$.username", equalTo(identity.getUsername())));
    }
	
	@Test
	public void testDisableEnable() throws Exception {
		IdmIdentityDto dto = createDto();
		Assert.assertFalse(dto.isDisabled());
		//
		getMockMvc().perform(patch(String.format("%s/disable", getDetailUrl(dto.getId())))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
				.andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(jsonPath("$.disabled", equalTo(true)));
		Assert.assertTrue(getDto(dto.getId()).isDisabled());	
		//
		getMockMvc().perform(patch(String.format("%s/enable", getDetailUrl(dto.getId())))
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
				.andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(jsonPath("$.disabled", equalTo(false)));
		//
		Assert.assertFalse(getDto(dto.getId()).isDisabled());
	}
	
	@Test
	public void testBulkDisable() {
		IdmIdentityDto identity = createDto();
		Assert.assertFalse(identity.isDisabled());
		// check disable operation is available
		List<IdmBulkActionDto> availableBulkActions = getAvailableBulkActions();
		//
		IdmBulkActionDto disableAction = availableBulkActions
				.stream()
				.filter(action -> action.getName().equals(IdentityDisableBulkAction.NAME))
				.findFirst()
				.orElse(null);
		Assert.assertNotNull(disableAction);
		//
		disableAction.getIdentifiers().add(identity.getId());
		//
		IdmBulkActionDto result = bulkAction(disableAction);
		//
		Assert.assertNotNull(result);
		Assert.assertTrue(getDto(identity.getId()).isDisabled());
	}
	
	@Test
	public void testSaveFormValuesWithDisabledAuthorizationPoliciesSupport() throws Exception {
		// disabled by default - legacy support
		Assert.assertFalse(identityConfiguration.isFormAttributesSecured());
		//
		// create some form definition
		IdmFormAttributeDto formAttribute = new IdmFormAttributeDto(getHelper().createName());
		IdmFormDefinitionDto formDefinition = formService.createDefinition(prepareDto().getClass(), getHelper().createName(), Lists.newArrayList(formAttribute));
		formAttribute = formDefinition.getFormAttributes().get(0);
		//
		IdmIdentityDto identity = getHelper().createIdentity(); // password is needed
		//
		// 403 by default
		getMockMvc().perform(get(getDetailUrl(identity.getUsername()))
        		.with(authentication(getAuthentication(identity.getUsername())))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isForbidden());
		getMockMvc().perform(get(getFormDefinitionsUrl(identity.getUsername()))
        		.with(authentication(getAuthentication(identity.getUsername())))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isForbidden());
		getMockMvc().perform(get(getFormValuesUrl(identity.getUsername()))
        		.with(authentication(getAuthentication(identity.getUsername())))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isForbidden());
		//
		// assign self identity authorization policy
		IdmRoleDto role = getHelper().createRole();		
		getHelper().createAuthorizationPolicy(
				role.getId(), 
				CoreGroupPermission.IDENTITY, 
				IdmIdentity.class, 
				SelfIdentityEvaluator.class, 
				IdmBasePermission.AUTOCOMPLETE, IdmBasePermission.READ);
		getHelper().createIdentityRole(identity, role);
		//
		// form definition is available automatically for identity witf self read permission
		List<IdmFormDefinitionDto> formDefinitions = getFormDefinitions(identity.getId(), identity.getUsername());
		//
		// form definition was found
		Assert.assertTrue(formDefinitions.stream().anyMatch(d -> d.getId().equals(formDefinition.getId())));
		//
		// test get values - empty
		IdmFormInstanceDto formInstance = getFormInstance(identity.getId(), identity.getUsername(), formDefinition.getCode());
		Assert.assertEquals(identity.getId().toString(), formInstance.getOwnerId());
		Assert.assertEquals(formDefinition.getId().toString(), formInstance.getFormDefinition().getId().toString());
		Assert.assertEquals(formDefinition.getFormAttributes().get(0).getId().toString(), formInstance.getFormDefinition().getFormAttributes().get(0).getId().toString());
		Assert.assertTrue(formInstance.getValues().isEmpty());
		//
		// save value - expect forbidden - self policy does not contain UPDATE
		IdmFormValueDto formValue = new IdmFormValueDto(formAttribute);
		formValue.setValue(getHelper().createName());
		List<IdmFormValueDto> formValues = Lists.newArrayList(formValue);
		getMockMvc().perform(patch(getFormValuesUrl(identity.getUsername()))
        		.with(authentication(getAuthentication(identity.getUsername())))
        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
        		.content(getMapper().writeValueAsString(formValues))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isForbidden());
		//
		// add UPDATE permission
		getHelper().createAuthorizationPolicy(
				role.getId(), 
				CoreGroupPermission.IDENTITY, 
				IdmIdentity.class, 
				SelfIdentityEvaluator.class, 
				IdmBasePermission.UPDATE);
		//
		// save values
		saveFormValues(identity.getId(), identity.getUsername(), formDefinition.getCode(), formValues);
		//
		// get saved value
		formInstance = getFormInstance(identity.getId(), identity.getUsername(), formDefinition.getCode());
		Assert.assertEquals(identity.getId().toString(), formInstance.getOwnerId());
		Assert.assertEquals(formDefinition.getId().toString(), formInstance.getFormDefinition().getId().toString());
		Assert.assertEquals(formDefinition.getFormAttributes().get(0).getId().toString(), formInstance.getFormDefinition().getFormAttributes().get(0).getId().toString());
		Assert.assertEquals(1, formInstance.getValues().size());
		Assert.assertEquals(formValue.getShortTextValue(), formInstance.getValues().get(0).getShortTextValue());
	}
	
	@Test
	public void testSaveFormValuesWithAuthorizationPoliciesSupport() throws Exception {
		getHelper().setConfigurationValue(PrivateIdentityConfiguration.PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED, true);
		try {
			Assert.assertTrue(identityConfiguration.isFormAttributesSecured());
			//
			// create definition with two attributes
			IdmFormAttributeDto formAttributeOne = new IdmFormAttributeDto("one");
			IdmFormAttributeDto formAttributeTwo = new IdmFormAttributeDto("two");
			IdmFormDefinitionDto formDefinition = formService.createDefinition(
					prepareDto().getClass(), 
					getHelper().createName(), 
					Lists.newArrayList(formAttributeOne, formAttributeTwo));
			formAttributeOne = formDefinition.getMappedAttributeByCode(formAttributeOne.getCode());
			formAttributeTwo = formDefinition.getMappedAttributeByCode(formAttributeTwo.getCode());
			//
			IdmIdentityDto identityOne = getHelper().createIdentity(); // password is needed
			IdmIdentityDto identityTwo = getHelper().createIdentity(); // password is needed
			IdmIdentityDto identityOther = getHelper().createIdentity((GuardedString) null);
			//
			// assign self identity authorization policy - READ - to identityOne
			IdmRoleDto roleReadIdentity = getHelper().createRole();		
			getHelper().createAuthorizationPolicy(
					roleReadIdentity.getId(), 
					CoreGroupPermission.IDENTITY, 
					IdmIdentity.class, 
					SelfIdentityEvaluator.class, 
					IdmBasePermission.AUTOCOMPLETE, IdmBasePermission.READ);
			getHelper().createUuidPolicy( // and other
					roleReadIdentity.getId(), 
					identityOther.getId(), 
					IdmBasePermission.AUTOCOMPLETE, IdmBasePermission.READ);
			getHelper().createIdentityRole(identityOne, roleReadIdentity);
			//
			// assign self identity authorization policy - UPDATE - to identityOne
			IdmRoleDto roleUpdateIdentity = getHelper().createRole();		
			getHelper().createAuthorizationPolicy(
					roleUpdateIdentity.getId(), 
					CoreGroupPermission.IDENTITY, 
					IdmIdentity.class, 
					SelfIdentityEvaluator.class, // self
					IdmBasePermission.AUTOCOMPLETE, IdmBasePermission.READ, IdmBasePermission.UPDATE);
			getHelper().createUuidPolicy( // and other
					roleUpdateIdentity.getId(), 
					identityOther.getId(), 
					IdmBasePermission.AUTOCOMPLETE, IdmBasePermission.READ, IdmBasePermission.UPDATE);
			getHelper().createIdentityRole(identityTwo, roleUpdateIdentity);
			//
			// form definition cannot be still read - authorization policies are supported
			List<IdmFormDefinitionDto> formDefinitions = getFormDefinitions(identityOne.getId(), identityOne.getUsername());
			Assert.assertTrue(formDefinitions.isEmpty());
			formDefinitions = getFormDefinitions(identityTwo.getId(), identityTwo.getUsername());
			Assert.assertTrue(formDefinitions.isEmpty());
			//
			// assign autocomplete to form definition 
			getHelper().createUuidPolicy( 
					roleReadIdentity.getId(), 
					formDefinition.getId(), 
					IdmBasePermission.AUTOCOMPLETE);
			getHelper().createUuidPolicy( // and other
					roleUpdateIdentity.getId(), 
					formDefinition.getId(), 
					IdmBasePermission.AUTOCOMPLETE);
			//
			// form definition can be read - look out form definitions in list are trimmed
			formDefinitions = getFormDefinitions(identityOne.getId(), identityOne.getUsername());
			Assert.assertTrue(formDefinitions.stream().anyMatch(d -> d.getCode().equals(formDefinition.getCode())));
			formDefinitions = getFormDefinitions(identityTwo.getId(), identityTwo.getUsername());
			Assert.assertTrue(formDefinitions.stream().anyMatch(d -> d.getCode().equals(formDefinition.getCode())));
			//
			// save some values as admin to identity one
			IdmFormValueDto formValueOne = new IdmFormValueDto(formAttributeOne);
			formValueOne.setValue(getHelper().createName());
			IdmFormValueDto formValueTwo = new IdmFormValueDto(formAttributeTwo);
			formValueTwo.setValue(getHelper().createName());
			List<IdmFormValueDto> formValues = Lists.newArrayList(formValueOne, formValueTwo);
			saveFormValues(identityOne.getId(), TestHelper.ADMIN_USERNAME, formDefinition.getCode(), formValues);
			//
			// values cannot be read as identity one 
			IdmFormInstanceDto formInstance = getFormInstance(identityOne.getId(), identityOne.getUsername(), formDefinition.getCode());
			Assert.assertTrue(formInstance.getValues().isEmpty());
			Assert.assertEquals(0, formInstance.getFormDefinition().getFormAttributes().size());
			formInstance = getFormInstance(identityOther.getId(), identityTwo.getUsername(), formDefinition.getCode());
			Assert.assertTrue(formInstance.getValues().isEmpty());
			Assert.assertEquals(0, formInstance.getFormDefinition().getFormAttributes().size());
			//
			// configure authorization policy to read attribute one and edit attribute two - for self
			ConfigurationMap properties = new ConfigurationMap();
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_DEFINITION, formDefinition.getId());
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_ATTRIBUTES, formAttributeOne.getCode());
			properties.put(IdentityFormValueEvaluator.PARAMETER_SELF_ONLY, true);
			getHelper().createAuthorizationPolicy(
					roleReadIdentity.getId(), 
					CoreGroupPermission.FORMVALUE, 
					IdmIdentityFormValue.class, 
					IdentityFormValueEvaluator.class,
					properties,
					IdmBasePermission.READ);
			//
			// read self attribute one
			formInstance = getFormInstance(identityOne.getId(), identityOne.getUsername(), formDefinition.getCode());
			Assert.assertEquals(1, formInstance.getValues().size());
			Assert.assertEquals(formValueOne.getShortTextValue(), formInstance.getValues().get(0).getShortTextValue());
			Assert.assertEquals(1, formInstance.getFormDefinition().getFormAttributes().size());
			Assert.assertEquals(formAttributeOne.getCode(), formInstance.getFormDefinition().getFormAttributes().get(0).getCode());
			//
			// update is forbidden
			getMockMvc().perform(patch(getFormValuesUrl(identityOne.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueOne)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			getMockMvc().perform(patch(getFormValuesUrl(identityTwo.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueOne)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			//
			// add policy to edit attribute two for identity one
			properties = new ConfigurationMap();
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_DEFINITION, formDefinition.getId());
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_ATTRIBUTES, formAttributeTwo.getCode());
			properties.put(IdentityFormValueEvaluator.PARAMETER_SELF_ONLY, true);
			getHelper().createAuthorizationPolicy(
					roleReadIdentity.getId(), 
					CoreGroupPermission.FORMVALUE, 
					IdmIdentityFormValue.class, 
					IdentityFormValueEvaluator.class,
					properties,
					IdmBasePermission.READ, IdmBasePermission.UPDATE);
			//
			String updatedValue = getHelper().createName();
			formValueTwo.setValue(updatedValue);
			saveFormValues(identityOne.getId(), identityOne.getUsername(), formDefinition.getCode(), Lists.newArrayList(formValueTwo));
			formInstance = getFormInstance(identityOne.getId(), identityOne.getUsername(), formDefinition.getCode());
			Assert.assertEquals(2, formInstance.getValues().size());
			Assert.assertEquals(formValueOne.getShortTextValue(), formInstance.toSinglePersistentValue(formAttributeOne.getCode()));
			Assert.assertEquals(updatedValue, formInstance.toSinglePersistentValue(formAttributeTwo.getCode()));
			getMockMvc().perform(patch(getFormValuesUrl(identityTwo.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueTwo)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			formValueOne.setValue(updatedValue);
			getMockMvc().perform(patch(getFormValuesUrl(identityOne.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueOne, formValueTwo)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			getMockMvc().perform(patch(getFormValuesUrl(identityOther.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueTwo)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			//
			// add policy to edit attribute two for identity two
			properties = new ConfigurationMap();
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_DEFINITION, formDefinition.getId());
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_ATTRIBUTES, formAttributeTwo.getCode());
			properties.put(IdentityFormValueEvaluator.PARAMETER_OWNER_UPDATE, true);
			getHelper().createAuthorizationPolicy(
					roleUpdateIdentity.getId(), 
					CoreGroupPermission.FORMVALUE, 
					IdmIdentityFormValue.class, 
					IdentityFormValueEvaluator.class,
					properties,
					IdmBasePermission.READ, IdmBasePermission.UPDATE);
			//
			// create new values under identity two
			// identity two can now update values of attribute two for self and other (no for identity one)
			updatedValue = getHelper().createName();
			formValueOne.setValue(updatedValue);
			formValueTwo.setValue(updatedValue);
			getMockMvc().perform(patch(getFormValuesUrl(identityOne.getId()))
	        		.with(authentication(getAuthentication(identityTwo.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueTwo)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			saveFormValues(identityTwo.getId(), identityTwo.getUsername(), formDefinition.getCode(), Lists.newArrayList(formValueTwo));
			formInstance = getFormInstance(identityTwo.getId(), identityTwo.getUsername(), formDefinition.getCode());
			Assert.assertEquals(updatedValue, formInstance.toSinglePersistentValue(formAttributeTwo.getCode()));
			saveFormValues(identityOther.getId(), identityTwo.getUsername(), formDefinition.getCode(), Lists.newArrayList(formValueTwo));
			formInstance = getFormInstance(identityOther.getId(), identityTwo.getUsername(), formDefinition.getCode());
			Assert.assertEquals(updatedValue, formInstance.toSinglePersistentValue(formAttributeTwo.getCode()));
			getMockMvc().perform(patch(getFormValuesUrl(identityOther.getId()))
	        		.with(authentication(getAuthentication(identityTwo.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueOne)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
			//
			// add permission to update attributes of other identity
			properties = new ConfigurationMap();
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_DEFINITION, formDefinition.getId());
			properties.put(IdentityFormValueEvaluator.PARAMETER_FORM_ATTRIBUTES, formAttributeTwo.getCode());
			properties.put(IdentityFormValueEvaluator.PARAMETER_OWNER_READ, true);
			getHelper().createAuthorizationPolicy(
					roleReadIdentity.getId(), 
					CoreGroupPermission.FORMVALUE, 
					IdmIdentityFormValue.class, 
					IdentityFormValueEvaluator.class,
					properties,
					IdmBasePermission.READ, IdmBasePermission.UPDATE);
			//
			updatedValue = getHelper().createName();
			formValueOne.setValue(updatedValue);
			formValueTwo.setValue(updatedValue);
			saveFormValues(identityOther.getId(), identityOne.getUsername(), formDefinition.getCode(), Lists.newArrayList(formValueTwo));
			formInstance = getFormInstance(identityOther.getId(), identityTwo.getUsername(), formDefinition.getCode());
			Assert.assertEquals(updatedValue, formInstance.toSinglePersistentValue(formAttributeTwo.getCode()));
			getMockMvc().perform(patch(getFormValuesUrl(identityOther.getId()))
	        		.with(authentication(getAuthentication(identityOne.getUsername())))
	        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
	        		.content(getMapper().writeValueAsString(Lists.newArrayList(formValueOne)))
	                .contentType(TestHelper.HAL_CONTENT_TYPE))
	                .andExpect(status().isForbidden());
		} finally {
			getHelper().setConfigurationValue(PrivateIdentityConfiguration.PROPERTY_IDENTITY_FORM_ATTRIBUTES_SECURED, false);
			Assert.assertFalse(identityConfiguration.isFormAttributesSecured());
		}
	}
	
	@Test
	public void testSaveFormValueWithAttributeIdOnly() throws Exception {
		IdmFormAttributeDto formAttribute = new IdmFormAttributeDto(getHelper().createName());
		IdmFormDefinitionDto formDefinition = formService.createDefinition(prepareDto().getClass(), getHelper().createName(), Lists.newArrayList(formAttribute));
		formAttribute = formDefinition.getFormAttributes().get(0);
		//
		IdmIdentityDto owner = createDto();
		//
		String value = getHelper().createName();
		//
		// save values		
		getMockMvc().perform(patch(getFormValuesUrl(owner))
        		.with(authentication(getAuthentication(TestHelper.ADMIN_USERNAME)))
        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
        		.content("[{ \"formAttribute\": \"" + formAttribute.getId() + "\", \"shortTextValue\": \"" + value + "\" }]")
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isOk());
		
		// get saved values
		IdmFormInstanceDto formInstance = getFormInstance(owner.getId(), TestHelper.ADMIN_USERNAME, formDefinition.getCode());
		Assert.assertEquals(owner.getId().toString(), formInstance.getOwnerId());
		Assert.assertEquals(1, formInstance.getValues().size());
		Assert.assertEquals(value, formInstance.getValues().get(0).getShortTextValue());
		Assert.assertEquals(formAttribute.getId(), formInstance.getValues().get(0).getFormAttribute());
	}
	
	@Test
	public void testSaveFormValueWithValueOnlyIsNotPossible() throws Exception {
		IdmFormAttributeDto formAttribute = new IdmFormAttributeDto(getHelper().createName());
		IdmFormDefinitionDto formDefinition = formService.createDefinition(prepareDto().getClass(), getHelper().createName(), Lists.newArrayList(formAttribute));
		formAttribute = formDefinition.getFormAttributes().get(0);
		//
		IdmIdentityDto owner = createDto();
		//
		String value = getHelper().createName();
		//
		// save values
		// if value is given only, then no value is saved => value is null => shortTextValue has to be given
		getMockMvc().perform(patch(getFormValuesUrl(owner))
        		.with(authentication(getAuthentication(TestHelper.ADMIN_USERNAME)))
        		.param(IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, formDefinition.getCode())
        		.content("[{ \"formAttribute\": \"" + formAttribute.getId() + "\", \"value\": \"" + value + "\" }]")
                .contentType(TestHelper.HAL_CONTENT_TYPE))
                .andExpect(status().isOk());
		
		// get saved values
		IdmFormInstanceDto formInstance = getFormInstance(owner.getId(), TestHelper.ADMIN_USERNAME, formDefinition.getCode());
		Assert.assertEquals(owner.getId().toString(), formInstance.getOwnerId());
		Assert.assertEquals(0, formInstance.getValues().size());
	}
	
	@Test
	public void testProfile() throws UnsupportedEncodingException, IOException, Exception {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		// profile image
		getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isNotFound());
		getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile/image")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isNotFound());
		//
		String fileName = "file.png";
		String content = "some image";
		String response = getMockMvc().perform(MockMvcRequestBuilders.fileUpload(getDetailUrl(owner.getId()) + "/profile/image")
				.file("data", IOUtils.toByteArray(IOUtils.toInputStream(content)))
        		.param("fileName", fileName)
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andReturn()
                .getResponse()
                .getContentAsString();
		IdmProfileDto createdProfile = (IdmProfileDto) getMapper().readValue(response, IdmProfileDto.class);
		//
		Assert.assertNotNull(createdProfile);
		Assert.assertNotNull(createdProfile.getId());
		Assert.assertNotNull(createdProfile.getImage());
		IdmAttachmentDto image = attachmentManager.get(createdProfile.getImage());
		Assert.assertEquals(content.length(), image.getFilesize().intValue());
		Assert.assertEquals(createdProfile.getId(), image.getOwnerId());
		Assert.assertEquals(attachmentManager.getOwnerType(createdProfile), image.getOwnerType());
		Assert.assertEquals(fileName, image.getName());
		Assert.assertEquals(content, IOUtils.toString(attachmentManager.getAttachmentData(image.getId())));
		//
		// get profile
		response = getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andReturn()
                .getResponse()
                .getContentAsString();
		createdProfile = (IdmProfileDto) getMapper().readValue(response, IdmProfileDto.class);
		Assert.assertEquals(image.getId(), createdProfile.getImage());
		//
		// get profile image
		response = getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile/image")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
		Assert.assertEquals(content, response);
		//
		// get profile permissions
		response = getMockMvc().perform(get(getDetailUrl(owner.getId()) + "/profile/permissions")
        		.with(authentication(getAdminAuthentication()))
                .contentType(TestHelper.HAL_CONTENT_TYPE))
				.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
		//
		// convert embedded object to list of strings
		List<String> permissions = getMapper().readValue(response, new TypeReference<List<String>>(){});
		Assert.assertNotNull(permissions);
		Assert.assertFalse(permissions.isEmpty());
		Assert.assertTrue(permissions.stream().anyMatch(p -> p.equals(IdmBasePermission.ADMIN.getName())));
		//
		// delete image		
		response = getMockMvc().perform(MockMvcRequestBuilders.delete(getDetailUrl(owner.getId()) + "/profile/image")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isOk())
				.andReturn()
                .getResponse()
                .getContentAsString();
		createdProfile = (IdmProfileDto) getMapper().readValue(response, IdmProfileDto.class);
		Assert.assertNull(createdProfile.getImage());
		//
		// get profile without image
		getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile/image")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isNotFound());
		//
		identityService.delete(owner);
		//
		// profile is deleted
		getMockMvc().perform(MockMvcRequestBuilders.get(getDetailUrl(owner.getId()) + "/profile/image")
        		.with(authentication(getAdminAuthentication())))
				.andExpect(status().isNotFound());
		// attachment is deleted
		Assert.assertNull(attachmentManager.get(image));
		Assert.assertNull(profileService.get(createdProfile));
	}
	
	@Test
	public void testPatchProfile() throws UnsupportedEncodingException, IOException, Exception {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		// create + patch profile
		String response = getMockMvc().perform(MockMvcRequestBuilders.patch(getDetailUrl(owner.getId()) + "/profile")
        		.with(authentication(getAdminAuthentication()))
				.content("{ \"preferredLanguage\": \"en\" }") // PATCH => lookout, mappar cannot be used (maps even null attrs).
                .contentType(TestHelper.HAL_CONTENT_TYPE))
				.andExpect(status().isOk())
                .andExpect(content().contentType(TestHelper.HAL_CONTENT_TYPE))
                .andReturn()
                .getResponse()
                .getContentAsString();
		IdmProfileDto createdProfile = (IdmProfileDto) getMapper().readValue(response, IdmProfileDto.class);
		Assert.assertEquals(owner.getId(), createdProfile.getIdentity());
		Assert.assertEquals("en", createdProfile.getPreferredLanguage());
	}
}
