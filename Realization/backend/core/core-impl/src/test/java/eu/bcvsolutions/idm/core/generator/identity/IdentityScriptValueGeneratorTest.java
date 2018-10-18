package eu.bcvsolutions.idm.core.generator.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.ScriptAuthorityType;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmScriptAuthorityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmScriptDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.generator.AbstractGeneratorTest;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmScriptAuthorityService;
import eu.bcvsolutions.idm.core.api.service.IdmScriptService;
import eu.bcvsolutions.idm.core.generator.identity.IdentityScriptValueGenerator;

/**
 * Tests for {@link IdentityScriptValueGenerator}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class IdentityScriptValueGeneratorTest extends AbstractGeneratorTest {

	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private IdmScriptService scriptService;
	@Autowired
	private IdmScriptAuthorityService scriptAuthorityService;
	
	@Test
	public void testGreenLine() {
		IdmScriptDto script = createScript(
				"import " + StringUtils.class.getCanonicalName() + ";" + System.lineSeparator() +
				"if (!valueGenerator.isRegenerateValue() && StringUtils.isNotEmpty(entity.getDescription())) {" + System.lineSeparator() +
				"	return entity;" + System.lineSeparator() +
				"}" + System.lineSeparator() +
				"" + System.lineSeparator() +
				"entity.setDescription(entity.getUsername() + 123);" + System.lineSeparator());
		
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityScriptValueGenerator.SCRIPT_CODE, script.getCode())), 1, null);

		IdmIdentityDto generatedDto = valueGeneratorManager.generate(identityDto);
		assertNotNull(generatedDto.getDescription());
		assertEquals(username + 123, generatedDto.getDescription());
	}

	@Test
	public void testGreenLineWithSave() {
		IdmScriptDto script = createScript(
				"import " + StringUtils.class.getCanonicalName() + ";" + System.lineSeparator() +
				"if (!valueGenerator.isRegenerateValue() && StringUtils.isNotEmpty(entity.getDescription())) {" + System.lineSeparator() +
				"	return entity;" + System.lineSeparator() +
				"}" + System.lineSeparator() +
				"" + System.lineSeparator() +
				"entity.setDescription(entity.getUsername() + 666);" + System.lineSeparator());
		
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityScriptValueGenerator.SCRIPT_CODE, script.getCode())), 1, null);

		IdmIdentityDto generatedDto = identityService.save(identityDto);
		assertNotNull(generatedDto.getDescription());
		assertEquals(username + 666, generatedDto.getDescription());
	}

	@Test(expected = ResultCodeException.class)
	public void testMissingScript() {
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityScriptValueGenerator.SCRIPT_CODE, this.getHelper().createName())), 1, null);

		identityService.save(identityDto);
	}

	@Test(expected = ResultCodeException.class)
	public void testScriptReturnNull() {
		IdmScriptDto script = createScript("");
		script.setScript("return null;");
		scriptService.save(script);
		
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityScriptValueGenerator.SCRIPT_CODE, script.getCode())), 1, null);

		identityService.save(identityDto);
	}

	@Test(expected = ResultCodeException.class)
	public void testScriptReturnTreeNode() {
		IdmScriptDto script = createScript("");
		script.setScript("import " + IdmTreeNodeDto.class.getCanonicalName() + "; " + System.lineSeparator()
				+ "return new IdmTreeNode();");
		scriptService.save(script);
		
		String username = this.getHelper().createName();

		IdmIdentityDto identityDto = new IdmIdentityDto();
		identityDto.setFirstName(this.getHelper().createName());
		identityDto.setLastName(this.getHelper().createName());
		identityDto.setUsername(username);

		ValueGeneratorDto generator = getGenerator();

		this.createGenerator(getDtoType(), getGeneratorType(),
				this.createConfiguration(generator.getFormDefinition(), ImmutableMap.of(
						IdentityScriptValueGenerator.SCRIPT_CODE, script.getCode())), 1, null);

		identityService.save(identityDto);
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		return IdmIdentityDto.class;
	}

	@Override
	protected String getGeneratorType() {
		return IdentityScriptValueGenerator.class.getCanonicalName();
	}

	/**
	 * Create script with authority for {@link IdmIdentityDto} and return statment for entity.
	 *
	 * @param scriptBodyCode
	 * @return
	 */
	private IdmScriptDto createScript(String scriptBodyCode) {
		IdmScriptDto script = new IdmScriptDto();
		script.setCode(getHelper().createName());
		script.setName(getHelper().createName());

		script.setScript(scriptBodyCode + System.lineSeparator() + "return entity;" + System.lineSeparator());

		script = scriptService.save(script);
		
		IdmScriptAuthorityDto auth = new IdmScriptAuthorityDto();
		auth.setClassName(IdmIdentityDto.class.getCanonicalName());
		auth.setType(ScriptAuthorityType.CLASS_NAME);
		auth.setScript(script.getId());
		scriptAuthorityService.save(auth);
		
		auth = new IdmScriptAuthorityDto();
		auth.setClassName(StringUtils.class.getCanonicalName());
		auth.setType(ScriptAuthorityType.CLASS_NAME);
		auth.setScript(script.getId());
		scriptAuthorityService.save(auth);
		
		return script;
	}
}
