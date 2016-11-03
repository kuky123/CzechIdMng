package eu.bcvsolutions.idm.core.api.service;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.entity.BaseEntity;

/**
 * Entity is identifiable by id and name
 * 
 * @author Radek Tomiška
 *
 * @param <E>
 */
public interface IdentifiableByNameEntityService<E extends BaseEntity> {

	/**
	 * Returns entity by given id
	 * 
	 * @param id
	 * @return
	 */
	E get(UUID id);
	
	/**
	 * Return entity by given name
	 * 
	 * @param name
	 * @return
	 */
	E getByName(String name);
	
}
