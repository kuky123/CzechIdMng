package eu.bcvsolutions.idm.acc.bulk.action.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableSet;

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractBulkActionTest;

/**
 * Integration tests for {@link SystemDeleteBulkAction}
 *
 * @author svandav
 *
 */

public class SystemDeleteBulkActionTest extends AbstractBulkActionTest {

	@Autowired
	private TestHelper helper;
	@Autowired
	private SysSystemService systemService;

	@Before
	public void login() {
		IdmIdentityDto adminIdentity = this.createUserWithAuthorities(IdmBasePermission.DELETE, IdmBasePermission.READ);
		loginAsNoAdmin(adminIdentity.getUsername());
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void processBulkActionByIds() {

		SysSystemDto system = helper.createSystem(getHelper().createName());

		IdmBulkActionDto bulkAction = this.findBulkAction(SysSystem.class, SystemDeleteBulkAction.NAME);
		bulkAction.setIdentifiers(ImmutableSet.of(system.getId()));
		IdmBulkActionDto processAction = bulkActionManager.processAction(bulkAction);

		checkResultLrt(processAction, 1l, null, null);

		assertNull(systemService.get(system.getId()));
	}

	@Test
	public void prevalidationBulkActionByIds() {
		SysSystemDto system = helper.createSystem(getHelper().createName());

		IdmBulkActionDto bulkAction = this.findBulkAction(SysSystem.class, SystemDeleteBulkAction.NAME);
		bulkAction.getIdentifiers().add(system.getId());

		// None info results
		ResultModels resultModels = bulkActionManager.prevalidate(bulkAction);
		assertEquals(0, resultModels.getInfos().size());

		// Assign identity to system
		helper.createIdentityAccount(system, getHelper().createIdentity());

		// Warning message, role has identity
		resultModels = bulkActionManager.prevalidate(bulkAction);
		assertEquals(1, resultModels.getInfos().size());
	}
}
