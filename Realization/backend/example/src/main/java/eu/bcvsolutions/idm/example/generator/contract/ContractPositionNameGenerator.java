package eu.bcvsolutions.idm.example.generator.contract;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.generator.AbstractValueGenerator;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.example.ExampleModuleDescriptor;

/**
 * Example generator for generates position name form username and prefix suffix.
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Enabled(ExampleModuleDescriptor.MODULE_ID)
@Component(ContractPositionNameGenerator.GENERATOR_NAME)
@Description("Generate position name from identity name and some prefix and suffix.")
public class ContractPositionNameGenerator extends AbstractValueGenerator<IdmIdentityContractDto> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContractPositionNameGenerator.class);

	public static final String GENERATOR_NAME = "example-contract-position-name-value-generator";
	public static String POSITION_NAME_SUFFIX = "positionNameSuffix";
	public static String POSITION_NAME_PREFIX = "positionNamePrefix";

	@Autowired
	private IdmIdentityService identityService;

	@Override
	public String getName() {
		return GENERATOR_NAME;
	}

	@Override
	public List<String> getPropertyNames() {
		List<String> properties = super.getPropertyNames();
		properties.add(POSITION_NAME_PREFIX);
		properties.add(POSITION_NAME_SUFFIX);
		return properties;
	}

	@Override
	public IdmIdentityContractDto generate(IdmIdentityContractDto dto, IdmGenerateValueDto generatorConfiguration) {
		// if exists email and configuration doesn't allow regenerate return dto
		if (!generatorConfiguration.isRegenerateValue() && StringUtils.isNotEmpty(dto.getPosition())) {
			return dto;
		}

		IdmIdentityDto identityDto = DtoUtils.getEmbedded(dto, IdmIdentityContract_.identity, IdmIdentityDto.class, null);
		if (identityDto == null) {
			// embedded is not filled
			identityDto = identityService.get(dto.getIdentity());
		}

		if (identityDto == null) {
			// this is possible, when create identity contract by rest
			LOG.error("New contract hasn't correctly filed identity ID [{}]", dto.getIdentity());
			return dto;
		}

		String username = identityDto.getUsername();
		
		StringBuilder result = new StringBuilder();
		
		String prefix = this.getPrefix(generatorConfiguration);
		String suffix = this.getSuffix(generatorConfiguration);
		
		if (StringUtils.isNotEmpty(prefix)) {
			result.append(prefix);
		}
		result.append(username);
		if (StringUtils.isNotEmpty(suffix)) {
			result.append(suffix);
		}

		dto.setPosition(result.toString());
		return dto;
	}

	/**
	 * Get suffix
	 *
	 * @return
	 */
	private String getSuffix(IdmGenerateValueDto generatorConfiguration) {
		return generatorConfiguration.getGeneratorProperties().getString(POSITION_NAME_SUFFIX);
	}

	/**
	 * Get prefix
	 *
	 * @return
	 */
	private String getPrefix(IdmGenerateValueDto generatorConfiguration) {
		return generatorConfiguration.getGeneratorProperties().getString(POSITION_NAME_PREFIX);
	}
}
