package eu.bcvsolutions.idm.core.bulk.action.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;

/**
 * Mock bulk action for testing action setting:
 * - showWithSelection
 * - showWithoutSelection
 * 
 * @author Radek Tomiška
 *
 */
@Component
public class MockBulkActionShowWithoutSelection extends MockBulkAction {
	
	@Override
	public boolean showWithoutSelection() {
		return true;
	}
	
	@Override
	protected List<UUID> getAllEntities(IdmBulkActionDto action, StringBuilder description) {
		return new ArrayList<>();
	}
}