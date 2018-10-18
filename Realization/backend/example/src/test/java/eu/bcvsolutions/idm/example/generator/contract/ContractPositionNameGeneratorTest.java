package eu.bcvsolutions.idm.example.generator.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.generator.AbstractGeneratorTest;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;

/**
 * Tests for {@link ContractPositionNameGenerator}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class ContractPositionNameGeneratorTest extends AbstractGeneratorTest {

	@Autowired
	private IdmIdentityContractService identityContractService;
	
	@Test
	public void testGreenLine() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		String prefix = "prefix" + System.currentTimeMillis();
		String suffix = "suffix" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						ContractPositionNameGenerator.POSITION_NAME_PREFIX, prefix,
						ContractPositionNameGenerator.POSITION_NAME_SUFFIX, suffix)), 1, null);

		IdmIdentityContractDto generated = valueGeneratorManager.generate(contractDto);
		assertNotNull(generated);
		assertEquals(prefix + identityDto.getUsername() + suffix, generated.getPosition());
	}

	@Test
	public void testRegenerateOff() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		String positionName = "positionName" + System.currentTimeMillis();
		String prefix = "prefix" + System.currentTimeMillis();
		String suffix = "suffix" + System.currentTimeMillis();
		contractDto.setPosition(positionName);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						ContractPositionNameGenerator.POSITION_NAME_PREFIX, prefix,
						ContractPositionNameGenerator.POSITION_NAME_SUFFIX, suffix)), 1, Boolean.FALSE);

		IdmIdentityContractDto generated = valueGeneratorManager.generate(contractDto);
		assertNotNull(generated);
		assertEquals(positionName, generated.getPosition());
	}

	@Test
	public void testGreenLineWithSave() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		String prefix = "prefix" + System.currentTimeMillis();
		String suffix = "suffix" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						ContractPositionNameGenerator.POSITION_NAME_PREFIX, prefix,
						ContractPositionNameGenerator.POSITION_NAME_SUFFIX, suffix)), 1, null);

		IdmIdentityContractDto generated = identityContractService.save(contractDto);
		assertNotNull(generated);
		assertEquals(prefix + identityDto.getUsername() + suffix, generated.getPosition());
	}

	@Test
	public void testWithoutSuffix() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		String prefix = "prefix" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						ContractPositionNameGenerator.POSITION_NAME_PREFIX, prefix)), 1, null);

		IdmIdentityContractDto generated = valueGeneratorManager.generate(contractDto);
		assertNotNull(generated);
		assertEquals(prefix + identityDto.getUsername(), generated.getPosition());
	}

	@Test
	public void testWithoutPrefix() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		String suffix = "suffix" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						ContractPositionNameGenerator.POSITION_NAME_SUFFIX, suffix)), 1, null);

		IdmIdentityContractDto generated = valueGeneratorManager.generate(contractDto);
		assertNotNull(generated);
		assertEquals(identityDto.getUsername() + suffix, generated.getPosition());
	}

	@Test
	public void testWithoutPrefixAndSuffix() {
		IdmIdentityDto identityDto = getHelper().createIdentity();
		IdmIdentityContractDto contractDto = new IdmIdentityContractDto();
		contractDto.setIdentity(identityDto.getId());

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), null), 1, null);

		IdmIdentityContractDto generated = valueGeneratorManager.generate(contractDto);
		assertNotNull(generated);
		assertEquals(identityDto.getUsername(), generated.getPosition());
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		return IdmIdentityContractDto.class;
	}

	@Override
	protected String getGeneratorType() {
		return ContractPositionNameGenerator.class.getCanonicalName();
	}

}
