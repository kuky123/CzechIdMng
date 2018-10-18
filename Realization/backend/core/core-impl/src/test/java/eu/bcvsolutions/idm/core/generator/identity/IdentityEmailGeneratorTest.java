package eu.bcvsolutions.idm.core.generator.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.generator.AbstractGeneratorTest;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.generator.identity.IdentityEmailGenerator;

/**
 * Test for {@link IdentityEmailGenerator}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class IdentityEmailGeneratorTest extends AbstractGeneratorTest {

	@Autowired
	private IdmIdentityService identityService;

	@Test
	public void testWithoutUsername() {
		String emailSuffix = "@example.tld";
		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNull(generatedDto.getEmail());
	}

	@Test
	public void testGreenLine() {
		String emailSuffix = "@example.tld";
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(username + emailSuffix, generatedDto.getEmail());
	}
	
	@Test
	public void testGreenLineWithIdentityService() {
		String emailSuffix = "@example.tld";
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto generatedDto = identityService.save(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(username + emailSuffix, generatedDto.getEmail());
	}

	@Test
	public void testGreenLineWithoutAt() {
		String emailSuffix = "example.tld";
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(username + "@" + emailSuffix, generatedDto.getEmail());
	}

	@Test
	public void testUsernameContainsAt() {
		String emailSuffix = "example.tld";
		String username = "test@username";

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNull(generatedDto.getEmail());
	}
	
	@Test
	public void testOrder() {
		String emailSuffix = "ex.tld";
		String emailSuffix1 = "@test.tld";
		String emailSuffix2 = "bcvsolutions.tld";
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		// first
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 10, null);

		// second
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix1,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 20, null);

		// the last - this generator will be generate email
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix2,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 30, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(username + "@" + emailSuffix2, generatedDto.getEmail());
	}

	@Test
	public void testRegenerate() {
		String emailSuffix = "ex.tld";
		String emailSuffix1 = "@test.tld";
		String emailSuffix2 = "bcvsolutions.tld";
		String username = this.getHelper().createName();
		String email = "email@example.tdl";

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		identityDto.setEmail(email);
		
		ValueGeneratorDto generator = getGenerator();

		// first
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 10, Boolean.FALSE);

		// second
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix1,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 20, Boolean.FALSE);

		// the last
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix2,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 30, Boolean.FALSE);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(email, generatedDto.getEmail());
	}

	@Test
	public void testDisable() {
		String emailSuffix = "ex.tld";
		String emailSuffix1 = "@test.tld";
		String emailSuffix2 = "bcvsolutions.tld";
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);
		
		ValueGeneratorDto generator = getGenerator();

		// first - this generator will be generate email
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 10, null);

		// second
		IdmGenerateValueDto createGenerator = this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix1,
						IdentityEmailGenerator.GENERATE_FROM_USERNAME, Boolean.TRUE.toString())), 20, null);
		createGenerator.setDisabled(true);
		this.generatedAttributeService.save(createGenerator);

		// the last
		createGenerator = this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityEmailGenerator.EMAIL_SUFFIX, emailSuffix2)), 30, null);
		createGenerator.setDisabled(true);
		this.generatedAttributeService.save(createGenerator);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getEmail());
		assertEquals(username + "@" + emailSuffix, generatedDto.getEmail());
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		return IdmIdentityDto.class;
	}

	@Override
	protected String getGeneratorType() {
		return IdentityEmailGenerator.class.getCanonicalName();
	}
}
