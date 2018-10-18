package eu.bcvsolutions.idm.core.api.generator;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.service.Configurable;

/**
 * Interface for all generators
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 * @since 9.2.0
 * @param <DTO> generator is designed for only one dto type
 */
public interface ValueGenerator<DTO extends AbstractDto> extends Configurable {

	@Override
	default String getConfigurableType() {
		return "value-generator";
	}
	
	/**
	 *  bean name / unique identifier (spring bean name)
	 *  
	 * @return
	 */
	String getId();

	/**
	 * Returns dto class, which supports this generator
	 * 
	 * @return
	 */
	Class<DTO> getDtoClass();

	/**
	 * Generate values by given value generator configuration.
	 *
	 * @param dto
	 * @param generatorConfiguration 
	 * @return
	 */
	DTO generate(DTO dto, IdmGenerateValueDto generatorConfiguration);
	
	/**
	 * Returns true, when generator supports given entity type
	 * 
	 * @param dtoType
	 * @return
	 */
	boolean supports(Class<?> dtoType);
}
