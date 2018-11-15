package eu.bcvsolutions.idm.acc.dto;

import java.io.Serializable;
import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;

/**
 * Controlled value for attribute DTO. Is using in the provisioning merge.
 * 
 * @author Vít Švanda
 *
 */
@Relation(collectionRelation = "controlledValues")
public class SysAttributeControlledValueDto extends AbstractDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = SysSystemAttributeMappingDto.class)
	private UUID attributeMapping;
	private boolean historicValue = false;
	private Serializable value;

	public UUID getAttributeMapping() {
		return attributeMapping;
	}

	public void setAttributeMapping(UUID attributeMapping) {
		this.attributeMapping = attributeMapping;
	}

	public boolean isHistoricValue() {
		return historicValue;
	}

	public void setHistoricValue(boolean historicValue) {
		this.historicValue = historicValue;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

}
