package eu.bcvsolutions.idm.core.generator.identity;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;

/**
 * Default core implementation generating username. Generator takes lower firstname,
 * and unite with lower lastname without diacritics.
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.2.0
 */
@Component(IdentityUsernameGenerator.GENERATOR_NAME)
@Description("Generate idenity username from firstName and lastName.")
public class IdentityUsernameGenerator extends AbstractIdentityValueGenerator {

	public static final String GENERATOR_NAME = "core-identity-username-value-generator";
	public static String SEARCH_UNIQUE_USERNAME = "searchUniqueUsername";
	//
	private static int MAXIMUM_SEARCH_FOR_UNIQUE_USERNAME = 100;

	@Autowired
	private IdmIdentityService identityService;
	
	@Override
	public String getName() {
		return GENERATOR_NAME;
	}

	@Override
	public List<String> getPropertyNames() {
		List<String> properties = super.getPropertyNames();
		properties.add(SEARCH_UNIQUE_USERNAME);
		return properties;
	}

	@Override
	public IdmIdentityDto generate(IdmIdentityDto dto, IdmGenerateValueDto valueGenerator) {
		Assert.notNull(dto);
		Assert.notNull(valueGenerator);
		//
		// if exists username and configuration doesn't allow regenerate return dto
		if (!valueGenerator.isRegenerateValue() && StringUtils.isNotEmpty(dto.getUsername())) {
			return dto;
		}
		//
		String resultUsername = generateUsername(dto, valueGenerator);
		if (StringUtils.isEmpty(resultUsername)) {
			return dto;
		}
		//
		if (isSearchUniqueUsername(valueGenerator)) {
			dto.setUsername(getUniqueUsername(resultUsername));
		} else {
			dto.setUsername(resultUsername);
		}
		//
		return dto;
	}

	/**
	 * Method returns unique username. Try found this username and increment it.
	 *
	 * @param username
	 * @return
	 */
	private String getUniqueUsername(String username) {
		IdmIdentityFilter filter = new IdmIdentityFilter();
		
		filter.setUsername(username);
		long count = identityService.count(filter);
		if (count > 0) {
			for (int index = 1; index < MAXIMUM_SEARCH_FOR_UNIQUE_USERNAME; index++) {
				String newUsername = username + index;
				filter.setUsername(newUsername);
				count = identityService.count(filter);
				if (count == 0) {
					// founded free
					return newUsername;
				}
			}
		}
		// not founded return old
		return username;
	}

	/**
	 * Is search unique username
	 *
	 * @return
	 */
	protected boolean isSearchUniqueUsername(IdmGenerateValueDto valueGenerator) {
		return valueGenerator.getGeneratorProperties().getBoolean(SEARCH_UNIQUE_USERNAME);
	}

	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		List<IdmFormAttributeDto> attributes = super.getFormAttributes();
		attributes.forEach(attribute -> {
			if (attribute.getName().equals(SEARCH_UNIQUE_USERNAME)) {
				attribute.setPersistentType(PersistentType.BOOLEAN);
				attribute.setDefaultValue(Boolean.TRUE.toString());
			}
		});
		return attributes;
	}
}
