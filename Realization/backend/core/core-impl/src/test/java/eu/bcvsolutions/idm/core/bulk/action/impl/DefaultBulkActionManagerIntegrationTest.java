package eu.bcvsolutions.idm.core.bulk.action.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.test.api.AbstractBulkActionTest;


/**
 * Bulk action manager integration test:
 * - get entities
 * TODO: other manager methods (+ add method for get bulk action by simple name to api)
 * 
 * @author Radek Tomiška
 */
@Transactional
public class DefaultBulkActionManagerIntegrationTest extends AbstractBulkActionTest {

	@Autowired private ApplicationContext context;
	//
	private DefaultBulkActionManager manager;

	@Before
	public void init() {
		manager = context.getAutowireCapableBeanFactory().createBean(DefaultBulkActionManager.class);
	}
	
	@Test(expected = ResultCodeException.class)
	public void testActionWithoutFilterAndWithoutSelection() {
		IdmBulkActionDto dto = findBulkAction(IdmIdentity.class, MockBulkAction.class.getCanonicalName());
		//
		manager.processAction(dto);
	}
	
	@Test
	public void testActionWithoutFilterAndWithSelectionAllUnimlemented() {
		IdmBulkActionDto dto = findBulkAction(IdmIdentity.class, MockBulkActionShowWithoutSelectionUnimplemented.class.getCanonicalName());
		//
		IdmBulkActionDto processAction = manager.processAction(dto);
		//
		IdmLongRunningTaskDto lrt = checkResultLrt(processAction, null, null, null);
		//
		Assert.assertEquals(CoreResultCode.BULK_ACTION_ENTITIES_ARE_NOT_SPECIFIED.getCode(), lrt.getResult().getCode());
	}
	
	@Test
	public void testActionWithoutFilterAndWithSelectionAllImlemented() {
		IdmBulkActionDto dto = findBulkAction(IdmIdentity.class, MockBulkActionShowWithoutSelection.class.getCanonicalName());
		//
		manager.processAction(dto);
	}
}
