package eu.bcvsolutions.idm.core.model.service.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ModuleDescriptorDto;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Default lookup service test:
 * - get dto by uuid / code
 * - get entity by uuid / code
 * - deg default owner type / id
 *
 * @author Radek Tomiška
 *
 */
@Transactional
public class DefaultLookupServiceIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ApplicationContext context;
	//
	private DefaultLookupService lookupService;

	@Before
	public void init() {
		lookupService = context.getAutowireCapableBeanFactory().createBean(DefaultLookupService.class);
	}

	@Test
	public void testIdentityLookupByUuid() {
		IdmIdentityDto dto = getHelper().createIdentity((GuardedString) null);
		//
		// by dto class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class, dto.getId()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentityDto.class, dto.getId()).getId());
		// by entity class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class, dto.getId()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentity.class, dto.getId()).getId());
		// by string class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class.getCanonicalName(), dto.getId()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class.getCanonicalName(), dto.getId()).getId());
	}

	@Test
	public void testIdentityLookupByCode() {
		IdmIdentityDto dto = getHelper().createIdentity((GuardedString) null);
		//
		// by dto class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class, dto.getCode()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentityDto.class, dto.getCode()).getId());
		// by entity class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class, dto.getCode()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentity.class, dto.getCode()).getId());
		// by string class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class.getCanonicalName(), dto.getCode()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class.getCanonicalName(), dto.getCode()).getId());
	}

	@Test
	public void testIdentityLookupByStringUuid() {
		IdmIdentityDto dto = getHelper().createIdentity((GuardedString) null);
		//
		// by dto class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class, dto.getId().toString()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentityDto.class, dto.getId().toString()).getId());
		// by entity class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class, dto.getId().toString()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupEntity(IdmIdentity.class, dto.getId().toString()).getId());
		// by string class
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentity.class.getCanonicalName(), dto.getId().toString()).getId());
		Assert.assertEquals(dto.getId(), lookupService.lookupDto(IdmIdentityDto.class.getCanonicalName(), dto.getId().toString()).getId());
	}

	@Test
	public void getOwnerId() {
		IdmIdentityDto dto = getHelper().createIdentity((GuardedString) null);
		//
		Assert.assertEquals(dto.getId(), lookupService.getOwnerId(dto));
		Assert.assertNull(lookupService.getOwnerId(new IdmIdentityDto()));
	}

	@Test
	public void getOwnerType() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		Assert.assertEquals(IdmIdentity.class.getCanonicalName(), lookupService.getOwnerType(IdmIdentity.class));
		Assert.assertEquals(IdmIdentity.class.getCanonicalName(), lookupService.getOwnerType(IdmIdentityDto.class));
		//
		Assert.assertEquals(IdmIdentity.class.getCanonicalName(), lookupService.getOwnerType(owner));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getWrongOwnerType() {
		lookupService.getOwnerType(ModuleDescriptorDto.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongIdentifiableType() {
		IdmIdentityDto dto = getHelper().createIdentity((GuardedString) null);
		//
		lookupService.lookupDto("wrongType", dto.getId());
	}
}
