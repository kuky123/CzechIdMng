package eu.bcvsolutions.idm.acc.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;

@Relation(collectionRelation = "systemAttributeMappings")
public class SysSystemAttributeMappingDto extends AbstractDto implements AttributeMapping {

	private static final long serialVersionUID = 8415424250836600530L;

	private String name;
	private String idmPropertyName;
	@Embedded(dtoClass = SysSystemMappingDto.class)
	private UUID systemMapping;
	@Embedded(dtoClass = SysSchemaAttributeDto.class)
	private UUID schemaAttribute;
	private boolean disabledAttribute = false;
	private boolean extendedAttribute = false;
	private boolean entityAttribute = true;
	private boolean confidentialAttribute = false;
	private boolean passwordAttribute = false;
	private boolean uid = false;
	private String transformFromResourceScript;
	private String transformToResourceScript;
	private AttributeMappingStrategyType strategyType = AttributeMappingStrategyType.SET;
	private boolean sendAlways = false;
	private boolean sendOnlyIfNotNull = false;
	private boolean authenticationAttribute = false;
	private boolean sendOnPasswordChange = false;
	private boolean cached = true;
	private boolean evictControlledValuesCache = true;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getIdmPropertyName() {
		return idmPropertyName;
	}

	@Override
	public void setIdmPropertyName(String idmPropertyName) {
		this.idmPropertyName = idmPropertyName;
	}

	public UUID getSystemMapping() {
		return systemMapping;
	}

	public void setSystemMapping(UUID systemMapping) {
		this.systemMapping = systemMapping;
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
	public boolean isDisabledAttribute() {
		return disabledAttribute;
	}

	@Override
	public void setDisabledAttribute(boolean disabledAttribute) {
		this.disabledAttribute = disabledAttribute;
	}

	@Override
	public boolean isExtendedAttribute() {
		return extendedAttribute;
	}

	@Override
	public void setExtendedAttribute(boolean extendedAttribute) {
		this.extendedAttribute = extendedAttribute;
	}

	@Override
	public boolean isEntityAttribute() {
		return entityAttribute;
	}

	@Override
	public void setEntityAttribute(boolean entityAttribute) {
		this.entityAttribute = entityAttribute;
	}

	@Override
	public boolean isConfidentialAttribute() {
		return confidentialAttribute;
	}

	@Override
	public void setConfidentialAttribute(boolean confidentialAttribute) {
		this.confidentialAttribute = confidentialAttribute;
	}

	@Override
	public boolean isUid() {
		return uid;
	}

	@Override
	public void setUid(boolean uid) {
		this.uid = uid;
	}

	@Override
	public String getTransformFromResourceScript() {
		return transformFromResourceScript;
	}

	@Override
	public void setTransformFromResourceScript(String transformFromResourceScript) {
		this.transformFromResourceScript = transformFromResourceScript;
	}

	@Override
	public String getTransformToResourceScript() {
		return transformToResourceScript;
	}

	@Override
	public void setTransformToResourceScript(String transformToResourceScript) {
		this.transformToResourceScript = transformToResourceScript;
	}

	@Override
	public AttributeMappingStrategyType getStrategyType() {
		return strategyType;
	}

	@Override
	public void setStrategyType(AttributeMappingStrategyType strategyType) {
		this.strategyType = strategyType;
	}

	@Override
	public boolean isSendAlways() {
		return sendAlways;
	}

	@Override
	public void setSendAlways(boolean sendAlways) {
		this.sendAlways = sendAlways;
	}

	@Override
	public boolean isSendOnlyIfNotNull() {
		return sendOnlyIfNotNull;
	}

	@Override
	public void setSendOnlyIfNotNull(boolean sendOnlyIfNotNull) {
		this.sendOnlyIfNotNull = sendOnlyIfNotNull;
	}

	@Override
	public boolean isCached() {
		return cached;
	}

	@Override
	public void setCached(boolean cached) {
		this.cached = cached;
	}

	public boolean isAuthenticationAttribute() {
		return authenticationAttribute;
	}

	public void setAuthenticationAttribute(boolean authenticationAttribute) {
		this.authenticationAttribute = authenticationAttribute;
	}

	public boolean isSendOnPasswordChange() {
		return sendOnPasswordChange;
	}

	public void setSendOnPasswordChange(boolean sendOnPasswordChange) {
		this.sendOnPasswordChange = sendOnPasswordChange;
	}

	public boolean isEvictControlledValuesCache() {
		return evictControlledValuesCache;
	}

	public void setEvictControlledValuesCache(boolean evictControlledValuesCache) {
		this.evictControlledValuesCache = evictControlledValuesCache;
	}

	@Override
	public boolean isPasswordAttribute() {
		return passwordAttribute;
	}

	@Override
	public void setPasswordAttribute(boolean passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}
}
