package eu.bcvsolutions.idm.core.bulk.action.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;

/**
 * Mock bulk action for testing action setting:
 * - showWithSelection
 * - showWithoutSelection
 * 
 * @author Radek Tomiška
 *
 */
@Component
public class MockBulkAction extends AbstractBulkAction<IdmIdentityDto, IdmIdentityFilter> {

	@Autowired private IdmIdentityService identityService;

	@Override
	public ReadWriteDtoService<IdmIdentityDto, IdmIdentityFilter> getService() {
		return identityService;
	}

	@Override
	protected OperationResult processDto(IdmIdentityDto dto) {
		// nothing
		return null;
	}
}