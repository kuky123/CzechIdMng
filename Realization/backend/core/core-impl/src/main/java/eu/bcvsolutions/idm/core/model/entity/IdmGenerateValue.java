package eu.bcvsolutions.idm.core.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.UnmodifiableEntity;

/**
 * Generate value
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @since 9.2.0
 */
@Entity
@Table(name = "idm_generate_value", indexes = {
		@Index(name = "idx_idm_generate_val_dto_type", columnList = "dto_type"),
})
public class IdmGenerateValue extends AbstractEntity implements UnmodifiableEntity {

	private static final long serialVersionUID = 1L;

	@Audited
	@Column(name = "description")
	private String description;

	@Audited
	@NotNull
	@Column(name = "dto_type", nullable = false)
	private String dtoType;

	@Audited
	@NotNull
	@Column(name = "generator_type", nullable = false)
	private String generatorType;

	@Audited
	@Min(0)
	@Max(99999)
	@Column(name = "seq", nullable = false)
	private short seq = 0;

	@Audited
	@Column(name = "generator_properties", length = Integer.MAX_VALUE)
	private ConfigurationMap generatorProperties;
	
	@Audited
	@NotNull
	@Column(name = "disabled", nullable = false)
	private boolean disabled = false;
	
	@Audited
	@NotNull
	@Column(name = "regenerate_value", nullable = false)
	private boolean regenerateValue = false;
	
	@NotNull
	@Column(name = "unmodifiable", nullable = false)
	private boolean unmodifiable = false;

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
		return this.unmodifiable;
	}

	@Override
	public void setUnmodifiable(boolean unmodifiable) {
		this.unmodifiable = unmodifiable;
	}
}
