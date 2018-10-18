package eu.bcvsolutions.idm.acc.bulk.action.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemFilter;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Bulk operation for duplication of the system
 * 
 * @author svandav
 *
 */

@Enabled(AccModuleDescriptor.MODULE_ID)
@Component("systemDuplicateBulkAction")
@Description("Bulk operation to duplicate the system.")
public class SystemDuplicateBulkAction extends AbstractBulkAction<SysSystemDto, SysSystemFilter> {

	public static final String NAME = "system-duplicate-bulk-action";

	@Autowired
	private SysSystemService systemService;

	@Override
	protected OperationResult processDto(SysSystemDto dto) {
		Assert.notNull(dto, "Role is required!");
		Assert.notNull(dto.getId(), "Id of system is required!");
		// Check rights
		systemService.checkAccess(systemService.get(dto.getId(), IdmBasePermission.READ), IdmBasePermission.UPDATE);
		// Duplicate the system
		systemService.duplicate(dto.getId());

		return new OperationResult(OperationState.EXECUTED);
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(AccGroupPermission.SYSTEM_READ, AccGroupPermission.SYSTEM_UPDATE);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 100;
	}

	@Override
	public ReadWriteDtoService<SysSystemDto, SysSystemFilter> getService() {
		return systemService;
	}

}
