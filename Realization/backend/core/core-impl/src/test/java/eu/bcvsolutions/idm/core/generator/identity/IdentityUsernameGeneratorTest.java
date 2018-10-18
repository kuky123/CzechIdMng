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
import eu.bcvsolutions.idm.core.generator.identity.IdentityUsernameGenerator;

public class IdentityUsernameGeneratorTest extends AbstractGeneratorTest {

	@Autowired
	private IdmIdentityService identityService;
	
	@Test
	public void generateMissingFirstName() {
		String lastName = "lastName-" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setLastName(lastName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getUsername(), generatedDto.getUsername());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNull(generatedDto.getUsername());
	}

	@Test
	public void generateMissingLastName() {
		String firstName = "firstName-" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getUsername(), generatedDto.getUsername());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNull(generatedDto.getUsername());
	}

	@Test
	public void generateGreenLine() {
		String firstName = "firstName-" + System.currentTimeMillis();
		String lastName = "lastName-" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName);
		identityDto.setLastName(lastName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(firstName.toLowerCase() + lastName.toLowerCase(), generatedDto.getUsername());
	}

	@Test
	public void generateDiacriticAndUpper() {
		String firstName = "áčďéěžšǍÁ";
		String lastName = "óÉČŘčř-";
		
		String newFirstName = "acdeezsaa";
		String newLastName = "oecrcr-";

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())), 1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName);
		identityDto.setLastName(lastName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(newFirstName + newLastName, generatedDto.getUsername());
	}

	@Test
	public void generateShorterProperties() {
		String firstName = "firstName";
		String lastName = "lastName";
		
		String newFirstName = "first";
		String newLastName = "last";


		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "5",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "4",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName);
		identityDto.setLastName(lastName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(newFirstName + newLastName, generatedDto.getUsername());
	}

	@Test
	public void generateShorterProperties2() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();
		
		String newFirstName = "f";
		String newLastName = "l";

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "--",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName);
		identityDto.setLastName(lastName);
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(newFirstName + "--" + newLastName, generatedDto.getUsername());
	}

	@Test
	public void generateMoreThan255() {
		StringBuilder firstName = new StringBuilder();
		for (int index = 0; index < 130; index++) {
			firstName.append("F");
		}
		
		StringBuilder lastName = new StringBuilder();
		for (int index = 0; index < 130; index++) {
			lastName.append("L");
		}

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(255, generatedDto.getUsername().length());
	}

	@Test
	public void generateDurignSaveIdentity() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(firstName.toLowerCase() + lastName.toLowerCase(), generatedDto.getUsername());
	}

	@Test
	public void generateWithMoreGenerator() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		// first
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "--")),
				1, null);
		
		// second
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "5",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "-")),
				10, null);

		// the last - this generator will be generate username
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.CONNECTING_CHARACTER, ".",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				20, null);
		
		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(firstName.toLowerCase() + "." + lastName.toLowerCase(), generatedDto.getUsername());
	}

	@Test
	public void generateWithMoreGeneratorAndRegenerate() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();
		
		String newFirstName = "first";

		ValueGeneratorDto generator = getGenerator();

		// first
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "--")),
				1, null);
		
		// second - this generator will be generate username
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "5",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "-",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				10, null);

		// the last
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.CONNECTING_CHARACTER, ".")),
				20, Boolean.FALSE);
		
		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(newFirstName + "-" + lastName.toLowerCase(), generatedDto.getUsername());
	}

	@Test
	public void generateWithNoRegenerate() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();
		
		String username = "username---" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "--",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, Boolean.FALSE);
		
		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		identityDto.setUsername(username);
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(username, generatedDto.getUsername());
	}

	@Test
	public void generateWithMoreGeneratorAndDisable() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();
		
		String newFirstName = "f";
		String newLastName = "l";

		ValueGeneratorDto generator = getGenerator();

		// first - this generator will be generate username
		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.LAST_NAME_CHARACTERS_COUNT, "1",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "*",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				1, null);
		
		// second
		IdmGenerateValueDto createGenerator = this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.FIRST_NAME_CHARACTERS_COUNT, "5",
								IdentityUsernameGenerator.CONNECTING_CHARACTER, "-",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				10, null);
		createGenerator.setDisabled(true);
		this.generatedAttributeService.save(createGenerator);

		// the last
		createGenerator = this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(),
						ImmutableMap.of(
								IdentityUsernameGenerator.CONNECTING_CHARACTER, ".",
								IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.TRUE.toString())),
				20, null);
		createGenerator.setDisabled(true);
		this.generatedAttributeService.save(createGenerator);
		
		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(newFirstName + "*" + newLastName, generatedDto.getUsername());
	}

	@Test
	public void generateWithChangePosition() {
		String firstName = "firstName" + System.currentTimeMillis();
		String lastName = "lastName" + System.currentTimeMillis();

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityUsernameGenerator.FIRST_NAME_FIRST, Boolean.FALSE.toString())),
				1, null);

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(firstName.toString());
		identityDto.setLastName(lastName.toString());
		
		IdmIdentityDto generatedDto = identityService.save(identityDto);
		
		assertEquals(identityDto.getLastName(), generatedDto.getLastName());
		assertEquals(identityDto.getFirstName(), generatedDto.getFirstName());
		
		assertNotNull(generatedDto.getUsername());
		assertEquals(lastName.toLowerCase() + firstName.toLowerCase(), generatedDto.getUsername());
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		return IdmIdentityDto.class;
	}

	@Override
	protected String getGeneratorType() {
		return  IdentityUsernameGenerator.class.getCanonicalName();
	}
}
