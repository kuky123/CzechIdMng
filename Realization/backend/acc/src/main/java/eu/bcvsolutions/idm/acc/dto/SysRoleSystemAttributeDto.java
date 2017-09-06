package eu.bcvsolutions.idm.acc.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;

/**
 * DTO for {@link SysRoleSystemAttribute}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

@Relation(collectionRelation = "roleSystemAttributes")
public class SysRoleSystemAttributeDto extends AbstractDto implements AttributeMapping {

	private static final long serialVersionUID = -3340543770861555491L;
	
	private String name;
	private String idmPropertyName;
	@Embedded(dtoClass = SysRoleSystemDto.class)
	private UUID roleSystem;
	@Embedded(dtoClass = SysSystemAttributeMappingDto.class)
	private UUID systemAttributeMapping;
	private boolean extendedAttribute = false;
	private boolean entityAttribute = true;
	private boolean confidentialAttribute = false;
	private boolean disabledDefaultAttribute = false;
	private boolean uid = false;
	private String transformScript;
	private AttributeMappingStrategyType strategyType = AttributeMappingStrategyType.SET;
	private boolean sendAlways = false;
	private boolean sendOnlyIfNotNull = false;
	@JsonIgnore
	private UUID schemaAttribute = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIdmPropertyName() {
		return idmPropertyName;
	}

	public void setIdmPropertyName(String idmPropertyName) {
		this.idmPropertyName = idmPropertyName;
	}

	public UUID getRoleSystem() {
		return roleSystem;
	}

	public void setRoleSystem(UUID roleSystem) {
		this.roleSystem = roleSystem;
	}

	public UUID getSystemAttributeMapping() {
		return systemAttributeMapping;
	}

	public void setSystemAttributeMapping(UUID systemAttributeMapping) {
		this.systemAttributeMapping = systemAttributeMapping;
	}

	public boolean isExtendedAttribute() {
		return extendedAttribute;
	}

	public void setExtendedAttribute(boolean extendedAttribute) {
		this.extendedAttribute = extendedAttribute;
	}

	public boolean isEntityAttribute() {
		return entityAttribute;
	}

	public void setEntityAttribute(boolean entityAttribute) {
		this.entityAttribute = entityAttribute;
	}

	public boolean isConfidentialAttribute() {
		return confidentialAttribute;
	}

	public void setConfidentialAttribute(boolean confidentialAttribute) {
		this.confidentialAttribute = confidentialAttribute;
	}

	public boolean isDisabledDefaultAttribute() {
		return disabledDefaultAttribute;
	}

	public void setDisabledDefaultAttribute(boolean disabledDefaultAttribute) {
		this.disabledDefaultAttribute = disabledDefaultAttribute;
	}

	public boolean isUid() {
		return uid;
	}

	public void setUid(boolean uid) {
		this.uid = uid;
	}

	public String getTransformScript() {
		return transformScript;
	}

	public void setTransformScript(String transformScript) {
		this.transformScript = transformScript;
	}

	public AttributeMappingStrategyType getStrategyType() {
		return strategyType;
	}

	public void setStrategyType(AttributeMappingStrategyType strategyType) {
		this.strategyType = strategyType;
	}

	public boolean isSendAlways() {
		return sendAlways;
	}

	public void setSendAlways(boolean sendAlways) {
		this.sendAlways = sendAlways;
	}

	public boolean isSendOnlyIfNotNull() {
		return sendOnlyIfNotNull;
	}

	public void setSendOnlyIfNotNull(boolean sendOnlyIfNotNull) {
		this.sendOnlyIfNotNull = sendOnlyIfNotNull;
	}

	@Override
	public UUID getSchemaAttribute() {
		return schemaAttribute;
	}

	@Override
	public void setSchemaAttribute(UUID schemaAttribute) {
		this.schemaAttribute = schemaAttribute;
	}

	@Override
	public String getTransformFromResourceScript() {
		return null;
	}

	@Override
	public void setTransformFromResourceScript(String transformFromResourceScript) {
	}

	@Override
	public String getTransformToResourceScript() {
		return this.getTransformScript();
	}

	@Override
	public void setTransformToResourceScript(String transformToResourceScript) {
		this.setTransformScript(transformToResourceScript);
	}

	@Override
	public boolean isDisabledAttribute() {
		return this.isDisabledDefaultAttribute();
	}

	@Override
	public void setDisabledAttribute(boolean disabled) {
		this.setDisabledDefaultAttribute(disabled);
		
	}

}