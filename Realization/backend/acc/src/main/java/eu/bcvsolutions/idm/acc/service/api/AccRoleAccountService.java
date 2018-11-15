package eu.bcvsolutions.idm.acc.service.api;

import java.util.UUID;

import eu.bcvsolutions.idm.acc.dto.AccRoleAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccRoleAccountFilter;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Role accounts on target system
 * 
 * @author Svanda
 *
 */
public interface AccRoleAccountService extends 
	ReadWriteDtoService<AccRoleAccountDto, AccRoleAccountFilter>,
	AuthorizableService<AccRoleAccountDto> {

	/**
	 * Delete role account
	 * @param entity
	 * @param deleteAccount If true, then the account on the target system will be deleted (call provisioning). 
	 */
	void delete(AccRoleAccountDto entity, boolean deleteAccount, BasePermission... permission);
	
	/**
	 * Method return roles id based on accountId.
	 * 
	 * @param account
	 * @return
	 */
	UUID getRoleId(UUID account);
}
	
