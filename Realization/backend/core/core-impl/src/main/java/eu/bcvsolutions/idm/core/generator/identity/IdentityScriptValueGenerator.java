package eu.bcvsolutions.idm.core.generator.identity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.generator.AbstractValueGenerator;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.script.evaluator.AbstractScriptEvaluator;
import eu.bcvsolutions.idm.core.script.evaluator.DefaultSystemScriptEvaluator;

/**
 * Identity script generator that is useed for generating values with scripts.
 * Script must implement behavior with regenerate.
 * 
 * TODO: abstract this generators
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.2.0
 */
@Component(IdentityScriptValueGenerator.GENERATOR_NAME)
@Description("Generate idenity value by script defined in parameters.")
public class IdentityScriptValueGenerator extends AbstractValueGenerator<IdmIdentityDto> {

	public static final String GENERATOR_NAME = "core-identity-script-value-generator";
	//
	public static String SCRIPT_CODE = "scriptCode";
	public static String ENTITY_KEY = "entity";
	public static String VALUE_GENERATOR_KEY = "valueGenerator";

	@Autowired
	private DefaultSystemScriptEvaluator systemScriptEvaluator;
	
	@Override
	public String getName() {
		return GENERATOR_NAME;
	}

	@Override
	public IdmIdentityDto generate(IdmIdentityDto dto, IdmGenerateValueDto valueGenerator) {
		Assert.notNull(dto);
		Assert.notNull(valueGenerator);
		//
		String scriptCode = getScriptCode(valueGenerator);
		Object returnValue = systemScriptEvaluator.evaluate(new AbstractScriptEvaluator.Builder()
				.setScriptCode(scriptCode)
				.addParameter(ENTITY_KEY, dto)
				.addParameter(VALUE_GENERATOR_KEY, valueGenerator));
		//
		if (returnValue == null || !(returnValue instanceof IdmIdentityDto)) {
			throw new ResultCodeException(CoreResultCode.GENERATOR_SCRIPT_RETURN_NULL_OR_BAD_DTO_TYPE,
					ImmutableMap.of(
							"scriptCode", scriptCode == null ? "null" : scriptCode,
							"returnedValue", returnValue == null ? "null" : returnValue));
		}
		//
		return (IdmIdentityDto) returnValue;

	}
	
	@Override
	public List<String> getPropertyNames() {
		List<String> properties = new ArrayList<>();
		properties.add(SCRIPT_CODE);
		return properties;
	}

	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		List<IdmFormAttributeDto> attributes = super.getFormAttributes();
		attributes.forEach(attribute -> {
			if (attribute.getName().equals(SCRIPT_CODE)) {
				attribute.setRequired(true);
			}
		});
		return attributes;
	}

	/**
	 * Get script code
	 *
	 * @param valueGenerator
	 * @return
	 */
	private String getScriptCode(IdmGenerateValueDto valueGenerator) {
		return valueGenerator.getGeneratorProperties().getString(SCRIPT_CODE);
	}
}
