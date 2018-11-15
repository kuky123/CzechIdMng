package eu.bcvsolutions.idm.acc.domain;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;

/**
 * Interface for mapping attribute dto
 * 
 * @author svandav
 *
 */

public interface AttributeMapping extends BaseDto {

	
	String getIdmPropertyName();

	void setIdmPropertyName(String idmPropertyName);

	UUID getSchemaAttribute();

	void setSchemaAttribute(UUID schemaAttribute);

	boolean isExtendedAttribute();

	void setExtendedAttribute(boolean extendedAttribute);

	String getTransformFromResourceScript();

	void setTransformFromResourceScript(String transformFromResourceScript);

	String getTransformToResourceScript();

	void setTransformToResourceScript(String transformToResourceScript);

	boolean isUid();

	void setUid(boolean uid);

	String getName();

	void setName(String name);

	boolean isEntityAttribute();

	void setEntityAttribute(boolean entityAttribute);

	boolean isConfidentialAttribute();

	void setConfidentialAttribute(boolean confidentialAttribute);

	boolean isDisabledAttribute();

	void setDisabledAttribute(boolean disabled);

	AttributeMappingStrategyType getStrategyType();

	void setStrategyType(AttributeMappingStrategyType strategyType);
	
	public boolean isSendAlways();

	public void setSendAlways(boolean sendAlways);

	public boolean isSendOnlyIfNotNull();

	public void setSendOnlyIfNotNull(boolean sendOnlyIfNotNull);

	boolean isCached();

	void setCached(boolean cached);

	void setPasswordAttribute(boolean passwordAttribute);

	boolean isPasswordAttribute();
}