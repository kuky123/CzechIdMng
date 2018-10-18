package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.generator.AbstractGeneratorTest;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;

/**
 * Integration tests for default implementation of generator manager
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class DefaultValueGeneratorManagerTest extends AbstractGeneratorTest {

	@Before
	public void init() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		cleanAllGenerator();
		super.logout();
	}

	@Test
	public void testCreateGenerator() {
		Set<Class<? extends AbstractDto>> supportedTypes = valueGeneratorManager.getSupportedTypes();
		//
		supportedTypes.forEach(supportedType -> {
			List<ValueGeneratorDto> availableGenerators = valueGeneratorManager
					.getAvailableGenerators(supportedType);
			availableGenerators.forEach(generator -> {
				IdmFormDefinitionDto formDefinition = generator.getFormDefinition();
				//
				createGenerator(supportedType, generator.getGeneratorType(),
						createConfiguration(formDefinition, ImmutableMap.of()), null, null);
			});
		});
	}

	@Test
	public void testGeneratorTypes() {
		List<ValueGeneratorDto> generators = valueGeneratorManager.getAvailableGenerators(null);
		
		for (ValueGeneratorDto generator : generators) {
			try {
				Class.forName(generator.getDtoType());
				Class.forName(generator.getGeneratorType());
				} catch (ClassNotFoundException e) {
				fail(e.getMessage());
			}
		}
	}

	@Test
	public void testGeneratorTypesForIdentity() {
		List<ValueGeneratorDto> generators = valueGeneratorManager
				.getAvailableGenerators(IdmIdentityDto.class);

		for (ValueGeneratorDto generator : generators) {
			try {
				Class.forName(generator.getDtoType());
				Class.forName(generator.getGeneratorType());
				assertEquals(IdmIdentityDto.class.getCanonicalName(), generator.getDtoType());
			} catch (ClassNotFoundException e) {
				fail(e.getMessage());
			}
		}
	}

	@Override
	protected Class<? extends AbstractDto> getDtoType() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getGeneratorType() {
		throw new UnsupportedOperationException();
	}
}
