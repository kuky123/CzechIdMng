package eu.bcvsolutions.idm.rpt.ldap;

import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Create test helper for working with ldap
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public interface LdapTestHelper {

	String ATTRIBUTE_MAPPING_NAME = "__NAME__";
	String ATTRIBUTE_MAPPING_PASSWORD = IcConnectorFacade.PASSWORD_ATTRIBUTE_NAME;
	String ATTRIBUTE_MAPPING_FIRSTNAME = "givenName";
	String ATTRIBUTE_MAPPING_LASTNAME = "sn";
	String ATTRIBUTE_MAPPING_CN = "cn";
	String ATTRIBUTE_MAPPING_MEMBER_OF = "memberOf";
	String ATTRIBUTE_MAPPING_EMAIL = "mail";

	/**
	 * Create ldap system without mapping
	 *
	 * @param systemName
	 * @return
	 */
	SysSystemDto createSystem(String systemName);

	/**
	 * Create ldap system with or without mapping
	 *
	 * @param withMapping
	 * @param systemName
	 * @return
	 */
	SysSystemDto createTestResourceSystem(boolean withMapping, String systemName);

	/**
	 * Create new merged attributes for memberOf attribute with given value
	 *
	 * @param system
	 * @param roleSystem
	 * @param value
	 */
	void createMergeAttributeForRole(SysSystemDto system, SysRoleSystemDto roleSystem, String value);

	/**
	 * Return member of attribute mapping
	 *
	 * @param system
	 * @return
	 */
	SysSystemAttributeMappingDto getAttributeMappingMemberOfAttributeForSystem(SysSystemDto system,
			SysSchemaAttributeDto memberOfAttributeForSystem);

	/**
	 * Return member of schema attribute
	 *
	 * @param system
	 * @return
	 */
	SysSchemaAttributeDto getSchemaAttributeMemberOfAttributeForSystem(SysSystemDto system);
}
