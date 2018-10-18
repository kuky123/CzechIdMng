package eu.bcvsolutions.idm.core.rest.impl;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmProfileDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmProfileFilter;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;

/**
 * Controller tests
 * - crud
 * - filter
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class IdmProfileControllerRestTest extends AbstractReadWriteDtoControllerRestTest<IdmProfileDto> {

	@Autowired private IdmProfileController controller;
	
	@Override
	protected AbstractReadWriteDtoController<IdmProfileDto, ?> getController() {
		return controller;
	}

	@Override
	protected IdmProfileDto prepareDto() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		IdmProfileDto dto = new IdmProfileDto();
		dto.setIdentity(owner.getId());
		return dto;
	}
	
	@Test
	public void testFindByIdentity() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		IdmProfileDto profileOne =  prepareDto();
		profileOne.setIdentity(owner.getId());
		profileOne = createDto(profileOne);
		createDto(); // other
		createDto(); // other
		
		IdmProfileFilter filter = new IdmProfileFilter();
		filter.setIdentityId(owner.getId());
		
		List<IdmProfileDto> results = find(filter);
		//
		Assert.assertEquals(1, results.size());
		Assert.assertEquals(profileOne, results.get(0));
	}
}
