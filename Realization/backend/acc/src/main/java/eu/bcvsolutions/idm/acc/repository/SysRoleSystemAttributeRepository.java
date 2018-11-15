package eu.bcvsolutions.idm.acc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;

/**
 * Mapping attribute to system for role
 * 
 * @author svandav
 *
 */
public interface SysRoleSystemAttributeRepository extends AbstractEntityRepository<SysRoleSystemAttribute> {

	@Query(value = "select e from SysRoleSystemAttribute e" + " where"
			+ " (?#{[0].roleSystemId} is null or e.roleSystem.id = ?#{[0].roleSystemId})"
			+ " and"
		    + " (?#{[0].systemMappingId} is null or e.roleSystem.systemMapping.id = ?#{[0].systemMappingId})"
			+ " and"
		    + " (?#{[0].schemaAttributeName} is null or e.systemAttributeMapping.schemaAttribute.name = ?#{[0].schemaAttributeName})")
	Page<SysRoleSystemAttribute> find(SysRoleSystemAttributeFilter filter, Pageable pageable);
	
	/**
	 * Delete attributes of given roleSystem
	 * 
	 * @param roleSystem
	 * @return
	 */
	int deleteByRoleSystem(@Param("roleSystem") SysRoleSystem roleSystem);
	
	/**
	 * Delete attributes of given systemAttributeMapping
	 * 
	 * @param systemAttributeMapping
	 * @return
	 */
	int deleteBySystemAttributeMapping(@Param("systemAttributeMapping") SysSystemAttributeMapping systemAttributeMapping);
}
