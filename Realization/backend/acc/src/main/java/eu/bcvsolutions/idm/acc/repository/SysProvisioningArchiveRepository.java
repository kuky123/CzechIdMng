package eu.bcvsolutions.idm.acc.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningArchive;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;

/**
 * Provisioning log
 * 
 * @author Radek Tomiška
 *
 */
public interface SysProvisioningArchiveRepository extends AbstractEntityRepository<SysProvisioningArchive> {

	@Query(value = "select e from #{#entityName} e"
			+ " where"
			+ " ("
	        	+ " ?#{[0].systemId} is null or e.system.id = ?#{[0].systemId}"
	    	+ " ) "
	    	+ " and "
        	+ " (?#{[0].from == null ? 'null' : ''} = 'null' or e.created >= ?#{[0].from}) "
        	+ " and "
        	+ " (?#{[0].till == null ? 'null' : ''} = 'null' or e.created <= ?#{[0].till})"
        	+ " and "
        	+ " (?#{[0].operationType} is null or e.operationType = ?#{[0].operationType})"
        	+ " and "
        	+ " (?#{[0].entityType} is null or e.entityType = ?#{[0].entityType})"
        	+ " and "
        	+ " (?#{[0].entityIdentifier} is null or e.entityIdentifier = ?#{[0].entityIdentifier})"
        	+ " and "
        	+ " (?#{[0].systemEntityUid} is null or e.systemEntityUid = ?#{[0].systemEntityUid})"
        	+ " and "
        	+ " (?#{[0].resultState} is null or e.result.state = ?#{[0].resultState})")
	Page<SysProvisioningArchive> find(SysProvisioningOperationFilter filter, Pageable pageable);
	
	/**
	 * Delete all archived provisioning logs
	 * 
	 * @param system
	 * @return
	 * @deprecated use {@link #deleteBySystem_Id(UUID)}
	 */
	@Deprecated
	int deleteBySystem(SysSystem system);
	
	/**
	 * Delete all archived provisioning logs
	 * 
	 * @param systemId
	 * @return
	 */
	int deleteBySystem_Id(UUID systemId);
}
