package eu.bcvsolutions.idm.acc.service.api;

import eu.bcvsolutions.idm.acc.dto.AccContractAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccContractAccountFilter;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Contract-account on target system
 * 
 * @author Svanda
 *
 */
public interface AccContractAccountService extends 
	ReadWriteDtoService<AccContractAccountDto, AccContractAccountFilter>,
	AuthorizableService<AccContractAccountDto> {

	/**
	 * Delete contract-account
	 * @param entity
	 * @param deleteAccount  If true, then the account on the target system will be deleted (call provisioning).
	 */
	void delete(AccContractAccountDto entity, boolean deleteAccount, BasePermission... permission);
}
	
