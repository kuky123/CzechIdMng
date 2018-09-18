package eu.bcvsolutions.idm.core.model.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.model.entity.IdmEntityState;

/**
 * Entity states
 * 
 * @author Radek Tomiška
 *
 */
public interface IdmEntityStateRepository extends AbstractEntityRepository<IdmEntityState> {

	/**
	 * Delete all state, which are related to entity events
	 */
	@Modifying
	@Query("delete from #{#entityName} e where event is not null")
	void deleteByEventIsNotNull();
}
