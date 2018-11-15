package eu.bcvsolutions.idm.acc.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;

/**
 * Schema attributes handling repository
 * 
 * @author Svanda
 *
 */
public interface SysSystemAttributeMappingRepository extends AbstractEntityRepository<SysSystemAttributeMapping> {
	
	@Query(value = "select e from SysSystemAttributeMapping e" +
			" where"
			+ " (?#{[0].text} is null or lower(e.name) like ?#{[0].text == null ? '%' : '%'.concat([0].text.toLowerCase()).concat('%')})"
	        + " and"
	        + " (?#{[0].systemMappingId} is null or e.systemMapping.id = ?#{[0].systemMappingId})"
	        + " and"
	        + " (?#{[0].schemaAttributeId} is null or e.schemaAttribute.id = ?#{[0].schemaAttributeId})"
	        +  "and"
	        + " (?#{[0].schemaAttributeName} is null or e.schemaAttribute.name = ?#{[0].schemaAttributeName})"
	        + " and"
	        + " (?#{[0].systemId} is null or e.systemMapping.objectClass.system.id = ?#{[0].systemId})"
	        + " and"
	        + " (?#{[0].isUid} is null or e.uid = ?#{[0].isUid})"
	        + " and"
			+ " (?#{[0].idmPropertyName} is null or lower(e.idmPropertyName) like ?#{[0].idmPropertyName == null ? '%' : '%'.concat([0].idmPropertyName.toLowerCase()).concat('%')})"
			+ " and"
	        + " (?#{[0].sendOnPasswordChange} is null or e.sendOnPasswordChange = ?#{[0].sendOnPasswordChange})"
	        + " and"
	        + " (?#{[0].passwordAttribute} is null or e.passwordAttribute = ?#{[0].passwordAttribute})"
			)
	Page<SysSystemAttributeMapping> find(SysSystemAttributeMappingFilter filter, Pageable pageable);
	
	@Query("SELECT e FROM SysSystemAttributeMapping e WHERE "
			+ "e.authenticationAttribute = true "
			+ "AND "
			+ "e.systemMapping.operationType = :operationType "
			+ "AND "
			+ "e.systemMapping.objectClass.system.id = :systemId "
			+ "AND "
			+ "e.systemMapping.entityType = :entityType")
	SysSystemAttributeMapping findAuthenticationAttribute(
			@Param("systemId") UUID systemId,
			@Param("operationType") SystemOperationType operationType, 
			@Param("entityType") SystemEntityType entityType);
	
	@Query("SELECT e FROM SysSystemAttributeMapping e WHERE "
			+ "e.uid = true "
			+ "AND "
			+ "e.systemMapping.operationType = :operationType "
			+ "AND "
			+ "e.systemMapping.objectClass.system.id = :systemId "
			+ "AND "
			+ "e.systemMapping.entityType = :entityType")
	SysSystemAttributeMapping findUidAttribute(@Param("systemId") UUID systemId,
			@Param("operationType") SystemOperationType operationType, @Param("entityType")  SystemEntityType entityType);
	
	/**
	 * Single mapped attribute in given mapping by given name
	 * 
	 * @param systemMappingId
	 * @param name
	 * @return
	 */
	SysSystemAttributeMapping findBySystemMapping_IdAndName(@Param("systemMappingId") UUID systemMappingId, @Param("name") String name);
	
	/**
	 * All mapped attributes in given mapping
	 * 
	 * @param systemMappingId
	 * @return
	 */
	List<SysSystemAttributeMapping> findAllBySystemMapping_Id(@Param("systemMappingId") UUID systemMappingId);
	
	/**
	 * Remove the primary attribute tag (isUid) for all attributes in that system-mapping except the given updatedEntityId
	 * 
	 * @param updatedEntityId
	 * @param systemMappingId
	 * @deprecated - skips audit, will be removed
	 */
	@Deprecated
	@Modifying
	@Query("update #{#entityName} e set e.uid = false where e.systemMapping.id = :systemMappingId and (:updatedEntityId is null or e.id != :updatedEntityId)")
	void clearIsUidAttribute(@Param("systemMappingId") UUID systemMappingId, @Param("updatedEntityId") UUID updatedEntityId);
}
