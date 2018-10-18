package eu.bcvsolutions.idm.acc.bulk.action.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.MessageFormat;

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
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractBulkActionTest;

/**
 * Integration tests for {@link SystemDuplicateBulkAction}
 *
 * @author svandav
 *
 */

public class SystemDuplicateBulkActionTest extends AbstractBulkActionTest {

	@Autowired
	private TestHelper helper;
	@Autowired
	private SysSystemService systemService;

	@Before
	public void login() {
		IdmIdentityDto adminIdentity = this.createUserWithAuthorities(IdmBasePermission.UPDATE, IdmBasePermission.READ);
		loginAsNoAdmin(adminIdentity.getUsername());
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void processBulkActionByIds() {

		SysSystemDto system = helper.createSystem(getHelper().createName());

		IdmBulkActionDto bulkAction = this.findBulkAction(SysSystem.class, SystemDuplicateBulkAction.NAME);
		bulkAction.setIdentifiers(ImmutableSet.of(system.getId()));

		String name = MessageFormat.format("{0}{1}", "Copy-of-", system.getName());
		assertNull(systemService.getByCode(name));

		IdmBulkActionDto processAction = bulkActionManager.processAction(bulkAction);
		checkResultLrt(processAction, 1l, null, null);
		assertNotNull(systemService.getByCode(name));
	}
}
