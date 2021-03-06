package eu.bcvsolutions.idm.acc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;

/**
 * Role could assign identity account on target system.
 * 
 * @author Radek Tomiška
 *
 */
public interface SysRoleSystemRepository extends AbstractEntityRepository<SysRoleSystem> {
	
	@Query(value = "select e from SysRoleSystem e" +
	        " where" +
	        " (?#{[0].roleId} is null or e.role.id = ?#{[0].roleId})" +
	        " and" +
	        " (?#{[0].systemId} is null or e.system.id = ?#{[0].systemId})" +
	        " and" +
	        " (?#{[0].systemMappingId} is null or e.systemMapping.id = ?#{[0].systemMappingId})")
	Page<SysRoleSystem> find(SysRoleSystemFilter filter, Pageable pageable);
}
