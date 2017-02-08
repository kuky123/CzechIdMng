package eu.bcvsolutions.idm.acc.service.api;

import eu.bcvsolutions.idm.acc.domain.ProvisioningOperation;
import eu.bcvsolutions.idm.acc.dto.filter.ProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningArchive;
import eu.bcvsolutions.idm.core.api.service.ReadWriteEntityService;

/**
 * Archived provisioning operation
 * 
 * @author Radek Tomiška
 *
 */
public interface SysProvisioningArchiveService extends ReadWriteEntityService<SysProvisioningArchive, ProvisioningOperationFilter> {

	/**
	 * Archives provisioning operation
	 * 
	 * @param provisioningOperation
	 * @return
	 */
	SysProvisioningArchive archive(ProvisioningOperation provisioningOperation);
}