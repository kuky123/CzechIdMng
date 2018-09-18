package eu.bcvsolutions.idm.core.model.repository;

import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.model.entity.IdmEntityEvent;

/**
 * Entity was changed
 * 
 * @author Radek Tomiška
 * @since 8.0.0
 */
public interface IdmEntityEventRepository extends AbstractEntityRepository<IdmEntityEvent> {

	/**
	 * Finds changes by state
	 * 
	 * @param state
	 * @return
	 */
	List<IdmEntityEvent> findByInstanceIdAndResult_StateOrderByCreatedAsc(String instanceId, OperationState state);
	
	/**
	 * Find event ready to be executed
	 * 
	 * @param instanceId
	 * @param state
	 * @param executeDate
	 * @param priority
	 * @param pageable
	 * @return
	 */
	@Query(value = "SELECT e FROM #{#entityName} e WHERE"
			+ " instanceId = :instanceId"
			+ " AND"
			+ " (e.executeDate is null or e.executeDate <= :executeDate)"
			+ " AND"
			+ " (:priority is null or e.priority = :priority)"
			+ " AND"
			+ " e.result.state = :state")
	Page<IdmEntityEvent> findToExecute(
			@Param("instanceId") String instanceId, 
			@Param("state") OperationState state,
			@Param("executeDate") DateTime executeDate,
			@Param("priority") PriorityType priority,
			Pageable pageable);
	
	/**
	 * Returns children count for given parent
	 * 
	 * @param parentId
	 * @return
	 */
	int countByParentId(UUID parentId);
	
	/**
	 * Delete all events
	 */
	@Modifying
	@Query("delete from #{#entityName}")
	void deleteAll();
	
}
