package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.UnmodifiableEntity;
import io.swagger.annotations.ApiModel;

/**
 * Base DTO for generated values
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.2.0
 */
@Relation(collectionRelation = "generateValues")
@ApiModel(description = "Definition for configured generator")
public class IdmGenerateValueDto extends AbstractDto implements UnmodifiableEntity {

	private static final long serialVersionUID = 1L;

	@Size(max = DefaultFieldLengths.DESCRIPTION)
	private String description;
	@Size(max = DefaultFieldLengths.NAME)
	private String dtoType;
	@Size(max = DefaultFieldLengths.NAME)
	private String generatorType;
	@Min(0)
	@Max(99999)
	private short seq;
	private ConfigurationMap generatorProperties;
	@NotNull
	private boolean disabled = false;
	@NotNull
	private boolean regenerateValue = false;
	private boolean unmodifiable = false;

	public IdmGenerateValueDto() {
	}
	
	public IdmGenerateValueDto(UUID id) {
		super(id);
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDtoType() {
		return dtoType;
	}

	public void setDtoType(String dtoType) {
		this.dtoType = dtoType;
	}

	public String getGeneratorType() {
		return generatorType;
	}

	public void setGeneratorType(String generatorType) {
		this.generatorType = generatorType;
	}

	public short getSeq() {
		return seq;
	}

	public void setSeq(short seq) {
		this.seq = seq;
	}

	public void setGeneratorProperties(ConfigurationMap generatorProperties) {
		this.generatorProperties = generatorProperties;
	}

	public ConfigurationMap getGeneratorProperties() {
		if (generatorProperties == null) {
			generatorProperties = new ConfigurationMap();
		}
		return generatorProperties;
	}

	public boolean isRegenerateValue() {
		return regenerateValue;
	}

	public void setRegenerateValue(boolean regenerateValue) {
		this.regenerateValue = regenerateValue;
	}
	
	@Override
	public boolean isUnmodifiable() {
		return unmodifiable;
	}

	@Override
	public void setUnmodifiable(boolean unmodifiable) {
		this.unmodifiable = unmodifiable;
	}
}
