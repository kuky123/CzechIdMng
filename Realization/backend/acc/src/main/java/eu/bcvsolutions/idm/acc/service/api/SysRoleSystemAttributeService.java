package eu.bcvsolutions.idm.acc.service.api;

import java.util.UUID;

import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;

/**
 * Mapping attribute to system for role
 * 
 * @author svandav
 *
 */
public interface SysRoleSystemAttributeService extends ReadWriteDtoService<SysRoleSystemAttributeDto, SysRoleSystemAttributeFilter> {
	
	/**
	 * Method creates provisioning of attribute on role to system. 
	 * 
	 * @param systemId
	 * @param roleId
	 * @param attributeName
	 * @param transformationScript
	 * @param objectClassName
	 * @param attribute
	 */
	public void addRoleMappingAttribute(UUID systemId, UUID roleId, String attributeName, String transformationScript,
			String objectClassName, SysRoleSystemAttributeDto attribute);
	
	/**
	 * Method changes transformation script and returns Boolean.TRUE if script is changed,
	 * Boolean.FALSE if script is not changed and null if it doesn't find system mapping on this role.
	 * 
	 * @param systemId
	 * @param roleId
	 * @param attributeName
	 * @param transformationScript
	 * @return
	 */
	public Boolean isChangedRoleMappingAttribute(UUID systemId, UUID roleId, String attributeName,
			String transformationScript);
	
}
