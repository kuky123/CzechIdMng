package eu.bcvsolutions.idm.rpt.ldap;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.SysConnectorKeyDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.repository.SysSystemRepository;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.ic.impl.IcConnectorKeyImpl;

/**
 * Create test helper for working with ldap
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Component("ldapTestHelper")
public class DefaultLdapTestHelper implements LdapTestHelper {

	@Autowired
	private SysSystemService systemService;
	@Autowired
	private SysSystemMappingService systemMappingService;
	@Autowired
	private SysSystemAttributeMappingService systemAttributeMappingService;
	@Autowired
	private SysSchemaAttributeService schemaAttributeService;
	@Autowired
	private FormService formService;
	@Autowired
	private SysSystemRepository systemRepository;
	@Autowired
	private SysRoleSystemAttributeService roleSystemAttributeService;
	@Autowired
	private SysSystemAttributeMappingService attributeMappingService;
	
	public SysSystemDto createSystem(String systemName) {
		SysSystemDto system = new SysSystemDto();
		system.setName(systemName == null ? "ldap-test-system" + "_" + System.currentTimeMillis() : systemName);
		system.setConnectorKey(new SysConnectorKeyDto(getLdapConnectorKey()));
		system = systemService.save(system);

		IdmFormDefinitionDto savedFormDefinition = systemService.getConnectorFormDefinition(system.getConnectorInstance());

		List<IdmFormValueDto> values = new ArrayList<>();

		IdmFormValueDto host = new IdmFormValueDto(
				savedFormDefinition.getMappedAttributeByCode("host"));
		host.setValue("localhost");
		values.add(host);

		IdmFormValueDto port = new IdmFormValueDto(
				savedFormDefinition.getMappedAttributeByCode("port"));
		port.setValue(LdapServer.DEFAULT_PORT);
		values.add(port);

		IdmFormValueDto principal = new IdmFormValueDto(
				savedFormDefinition.getMappedAttributeByCode("principal"));
		principal.setValue(LdapServer.ADMIN_USERNAME);
		values.add(principal);
		
		IdmFormValueDto credentials = new IdmFormValueDto(savedFormDefinition.getMappedAttributeByCode("credentials"));
		credentials.setValue(LdapServer.ADMIN_PASSWORD);
		values.add(credentials);
		
		IdmFormValueDto baseContexts = new IdmFormValueDto(savedFormDefinition.getMappedAttributeByCode("baseContexts"));
		baseContexts.setValue(LdapServer.DEFAULT_OU + "," + LdapServer.DEFAULT_ROOT);
		values.add(baseContexts);

		IdmFormValueDto passwordAttribute = new IdmFormValueDto(savedFormDefinition.getMappedAttributeByCode("passwordAttribute"));
		passwordAttribute.setValue("userPassword");
		values.add(passwordAttribute);

		IdmFormValueDto uidAttribute = new IdmFormValueDto(savedFormDefinition.getMappedAttributeByCode("uidAttribute"));
		uidAttribute.setValue("dn");
		values.add(uidAttribute);

		IdmFormValueDto dnAttribute = new IdmFormValueDto(savedFormDefinition.getMappedAttributeByCode("dnAttribute"));
		dnAttribute.setValue("dn");
		values.add(dnAttribute);

		SysSystem systemEntity = systemRepository.findOne(system.getId());
		
		formService.saveValues(systemEntity, savedFormDefinition, values);

		return system;
	}

	@Override
	public SysSystemDto createTestResourceSystem(boolean withMapping, String systemName) {
		SysSystemDto system = this.createSystem(systemName);

		if (!withMapping) {
			return system;
		}

		//
		// generate schema for system
		List<SysSchemaObjectClassDto> objectClasses = systemService.generateSchema(system);
		
		SysSchemaObjectClassDto objectClass = objectClasses.stream().filter(oc -> oc.getObjectClassName().equals("__ACCOUNT__")).findFirst().orElse(null); // TODO use constatn
		assertNotNull(objectClass);
		//
		SysSystemMappingDto systemMapping = new SysSystemMappingDto();
		systemMapping.setName("default_" + System.currentTimeMillis());
		systemMapping.setEntityType(SystemEntityType.IDENTITY);
		systemMapping.setOperationType(SystemOperationType.PROVISIONING);
		systemMapping.setObjectClass(objectClass.getId());
		systemMapping = systemMappingService.save(systemMapping);

		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());

		SysSchemaAttributeDto memberOf = new SysSchemaAttributeDto();
		memberOf.setClassType("java.lang.String");
		memberOf.setCreateable(true);
		memberOf.setMultivalued(true);
		memberOf.setUpdateable(true);
		memberOf.setReturnedByDefault(true);
		memberOf.setReadable(true);
		memberOf.setObjectClass(objectClass.getId());
		memberOf.setNativeName(ATTRIBUTE_MAPPING_MEMBER_OF);
		memberOf.setName(ATTRIBUTE_MAPPING_MEMBER_OF);
		memberOf = schemaAttributeService.save(memberOf);

		// From some reason contains schema object class for account two __NAME__ just skip second
		Page<SysSchemaAttributeDto> schemaAttributesPage = schemaAttributeService.find(schemaAttributeFilter, null);
		for(SysSchemaAttributeDto schemaAttr : schemaAttributesPage) {

			// Test ldap has some duplicates attributes for example __NAME__ and cn
			SysSystemAttributeMappingDto founded = systemAttributeMappingService.findBySystemMappingAndName(systemMapping.getId(), schemaAttr.getName());
			if (founded != null) {
				continue;
			}

			if (ATTRIBUTE_MAPPING_NAME.equals(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setUid(true);
				attributeMapping.setEntityAttribute(true);
				attributeMapping.setIdmPropertyName(IdmIdentity_.username.getName());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(systemMapping.getId());
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_PASSWORD.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName("password");
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSystemMapping(systemMapping.getId());
				attributeMapping.setPasswordAttribute(true);
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_FIRSTNAME.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName(IdmIdentity_.firstName.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSystemMapping(systemMapping.getId());
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_CN.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName(IdmIdentity_.description.getName()); // TODO: map as script (combination last and first name)
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSystemMapping(systemMapping.getId());
				attributeMapping.setTransformToResourceScript("" + System.lineSeparator()
						+ "if (attributeValue) {" + System.lineSeparator()
						+ "	return attributeValue;" + System.lineSeparator()
						+ "}" + System.lineSeparator()
						+ "return entity.getFirstName() + ' ' + entity.getLastName();"  + System.lineSeparator()); // we must compose cn
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_LASTNAME.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName(IdmIdentity_.lastName.getName());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(systemMapping.getId());
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_EMAIL.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName(IdmIdentity_.email.getName());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(systemMapping.getId());
				systemAttributeMappingService.save(attributeMapping);
			} else if (ATTRIBUTE_MAPPING_MEMBER_OF.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setStrategyType(AttributeMappingStrategyType.MERGE);
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(systemMapping.getId());
				attributeMapping.setEntityAttribute(false);
				attributeMapping.setExtendedAttribute(false);
				systemAttributeMappingService.save(attributeMapping);
			}
		}
		return system;
 	}

	@Override
	public void createMergeAttributeForRole(SysSystemDto system, SysRoleSystemDto roleSystem, String value) {
		SysSchemaAttributeDto memberOfAttributeForSystem = getSchemaAttributeMemberOfAttributeForSystem(system);
		SysSystemAttributeMappingDto attributeMappingMemberOfAttributeForSystem = getAttributeMappingMemberOfAttributeForSystem(system, memberOfAttributeForSystem);
		
		SysRoleSystemAttributeDto roleSystemAttributeDto = new SysRoleSystemAttributeDto();
		roleSystemAttributeDto.setStrategyType(AttributeMappingStrategyType.MERGE);
		roleSystemAttributeDto.setSystemAttributeMapping(attributeMappingMemberOfAttributeForSystem.getId());
		roleSystemAttributeDto.setRoleSystem(roleSystem.getId());
		roleSystemAttributeDto.setSchemaAttribute(memberOfAttributeForSystem.getId());
		roleSystemAttributeDto.setName(memberOfAttributeForSystem.getName());
		roleSystemAttributeDto.setEntityAttribute(false);
		roleSystemAttributeDto.setExtendedAttribute(false);
		roleSystemAttributeDto.setTransformToResourceScript("return '" + value + "';" + System.lineSeparator());
		roleSystemAttributeDto = roleSystemAttributeService.save(roleSystemAttributeDto);
	}

	@Override
	public SysSchemaAttributeDto getSchemaAttributeMemberOfAttributeForSystem(SysSystemDto system) {
		SysSchemaAttributeFilter filter = new SysSchemaAttributeFilter();
		filter.setSystemId(system.getId());
		filter.setName(LdapTestHelper.ATTRIBUTE_MAPPING_MEMBER_OF);
		List<SysSchemaAttributeDto> schemaAttributes = schemaAttributeService.find(filter, null).getContent();
		return schemaAttributes.get(0);
	}

	@Override
	public SysSystemAttributeMappingDto getAttributeMappingMemberOfAttributeForSystem(SysSystemDto system, SysSchemaAttributeDto memberOfAttributeForSystem) {
		SysSystemAttributeMappingFilter filter = new SysSystemAttributeMappingFilter();
		filter.setSystemId(system.getId());
		filter.setSchemaAttributeId(memberOfAttributeForSystem.getId());
		List<SysSystemAttributeMappingDto> attributes = attributeMappingService.find(filter, null).getContent();
		return attributes.get(0);

	}

	private IcConnectorKeyImpl getLdapConnectorKey() {
		IcConnectorKeyImpl key = new IcConnectorKeyImpl();
		key.setFramework("connId");
		key.setConnectorName("net.tirasa.connid.bundles.ldap.LdapConnector");
		key.setBundleName("net.tirasa.connid.bundles.ldap");
		key.setBundleVersion("1.5.1");
		return key;
	}
}
