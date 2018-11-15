package eu.bcvsolutions.idm.acc.service.api;

import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;

public interface AccAccountManagementService {

	/**
	 * Create or delete accounts for this identity according their roles
	 * @param identity
	 * @return
	 */
	boolean resolveIdentityAccounts(IdmIdentityDto identity);

	/**
	 * Identity role is deleting, we have to delete linked identity accounts
	 * 
	 * @param entity
	 */
	void deleteIdentityAccount(IdmIdentityRoleDto identityRole);
	
	/**
	 * Identity role is deleting, we have to delete linked identity accounts, or mark them for delete
	 * 
	 * @param event
	 */
	void deleteIdentityAccount(EntityEvent<IdmIdentityRoleDto> event);
	
	/**
	 * Return UID for this dto and roleSystem. First, the transform script
	 * from the roleSystem attribute is found and used. If UID attribute
	 * for the roleSystem is not defined, then default UID attribute handling
	 * will be used.
	 * 
	 * @param dto
	 * @param roleSystem
	 * @return
	 */
	String generateUID(AbstractDto dto, SysRoleSystemDto roleSystem);
}
