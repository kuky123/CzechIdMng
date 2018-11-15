package eu.bcvsolutions.idm.acc.service.api;

import eu.bcvsolutions.idm.acc.dto.AccRoleCatalogueAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccRoleCatalogueAccountFilter;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Role catalogue accounts on target system
 * 
 * @author Svanda
 *
 */
public interface AccRoleCatalogueAccountService extends 
		ReadWriteDtoService<AccRoleCatalogueAccountDto, AccRoleCatalogueAccountFilter>,
		AuthorizableService<AccRoleCatalogueAccountDto> {

	/**
	 * Delete role catalogue account
	 * @param entity
	 * @param deleteAccount If true, then the account on the target system will be deleted (call provisioning). 
	 */
	void delete(AccRoleCatalogueAccountDto entity, boolean deleteAccount, BasePermission... permission);
}
	
