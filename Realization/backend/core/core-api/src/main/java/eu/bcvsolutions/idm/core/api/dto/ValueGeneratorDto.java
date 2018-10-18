package eu.bcvsolutions.idm.core.api.dto;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;

/**
 * Value generator definition
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.2.0
 */
@JsonInclude(Include.NON_NULL)
@Relation(collectionRelation = "valueGenerators")
public class ValueGeneratorDto extends AbstractComponentDto {

	private static final long serialVersionUID = 1L;

	@NotEmpty
	private String dtoType;
	@NotEmpty
	private String generatorType;
	private IdmFormDefinitionDto formDefinition;

	public String getDtoType() {
		return dtoType;
	}

	public void setDtoType(String dtoType) {
		this.dtoType = dtoType;
	}

	public IdmFormDefinitionDto getFormDefinition() {
		return formDefinition;
	}

	public void setFormDefinition(IdmFormDefinitionDto formDefinition) {
		this.formDefinition = formDefinition;
	}

	public String getGeneratorType() {
		return generatorType;
	}

	public void setGeneratorType(String generatorType) {
		this.generatorType = generatorType;
	}
}
