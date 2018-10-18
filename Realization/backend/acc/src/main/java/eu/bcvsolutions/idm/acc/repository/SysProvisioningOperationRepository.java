package eu.bcvsolutions.idm.acc.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningOperation;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;

/**
 * Provisioning log
 * 
 * @author Radek Tomiška
 *
 */
public interface SysProvisioningOperationRepository extends AbstractEntityRepository<SysProvisioningOperation> {

	@Query(value = "select e from #{#entityName} e "
			+ " where"
			+ " (?#{[0].systemId} is null or e.system.id = ?#{[0].systemId}) "
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
        	+ " (?#{[0].systemEntity} is null or e.systemEntity.id = ?#{[0].systemEntity})"
        	+ " and "
        	+ " (?#{[0].systemEntityUid} is null or e.systemEntity.uid = ?#{[0].systemEntityUid})"
        	+ " and "
        	+ " (?#{[0].resultState} is null or e.result.state = ?#{[0].resultState})"
        	+ " and "
    		+ " (?#{[0].batchId} is null or e.batch.id = ?#{[0].batchId})")
	Page<SysProvisioningOperation> find(SysProvisioningOperationFilter filter, Pageable pageable);
	
	/**
	 * Delete operations by given system id
	 * 
	 * @param systemId
	 * @return
	 * @deprecated @since 9.2.1 this method calls select + delete for each row => use {@link #deleteBySystem(UUID)} which calls delete directly.
	 */
	long deleteBySystem_Id(UUID systemId);
	
	/**
	 * Delete operations by given system id
	 * 
	 * @param systemId @Modifying has to return int or Integer only
	 */
	@Modifying
	@Query("delete from #{#entityName} e where e.system.id = :systemId")
	int deleteBySystem(@Param("systemId") UUID systemId);
}
