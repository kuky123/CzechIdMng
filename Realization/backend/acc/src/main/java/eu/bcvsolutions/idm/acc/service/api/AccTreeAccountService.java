package eu.bcvsolutions.idm.acc.service.api;

import eu.bcvsolutions.idm.acc.dto.AccTreeAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccTreeAccountFilter;
import eu.bcvsolutions.idm.core.api.script.ScriptEnabled;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Tree accounts on target system
 * 
 * @author Svanda
 *
 */
public interface AccTreeAccountService extends ReadWriteDtoService<AccTreeAccountDto, AccTreeAccountFilter>, ScriptEnabled,
		AuthorizableService<AccTreeAccountDto> {

	/**
	 * Delete tree node account
	 * @param entity
	 * @param deleteAccount  If true, then the account on the target system will be deleted (call provisioning).
	 */
	void delete(AccTreeAccountDto entity, boolean deleteAccount, BasePermission... permission);
}
	
