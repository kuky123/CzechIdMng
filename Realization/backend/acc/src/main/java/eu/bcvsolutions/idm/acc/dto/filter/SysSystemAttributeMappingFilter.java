package eu.bcvsolutions.idm.acc.dto.filter;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.filter.QuickFilter;

/**
 * Filter for attribute entity handling
 * 
 * @author Svanda
 *
 */
public class SysSystemAttributeMappingFilter extends QuickFilter {
	
	private UUID systemId;	
	private UUID systemMappingId;	
	private UUID schemaAttributeId;	
	private String idmPropertyName;	
	private String schemaAttributeName;
	private Boolean isUid;
	private Boolean sendOnPasswordChange;
	private Boolean passwordAttribute;

	public Boolean getIsUid() {
		return isUid;
	}

	public void setIsUid(Boolean isUid) {
		this.isUid = isUid;
	}

	public UUID getSystemMappingId() {
		return systemMappingId;
	}

	public void setSystemMappingId(UUID systemMappingId) {
		this.systemMappingId = systemMappingId;
	}

	public String getIdmPropertyName() {
		return idmPropertyName;
	}

	public void setIdmPropertyName(String idmPropertyName) {
		this.idmPropertyName = idmPropertyName;
	}

	public UUID getSystemId() {
		return systemId;
	}

	public void setSystemId(UUID systemId) {
		this.systemId = systemId;
	}

	public UUID getSchemaAttributeId() {
		return schemaAttributeId;
	}

	public void setSchemaAttributeId(UUID schemaAttributeId) {
		this.schemaAttributeId = schemaAttributeId;
	}
	
	public void setSendOnPasswordChange(Boolean sendOnPasswordChange) {
		this.sendOnPasswordChange = sendOnPasswordChange;
	}
	
	public Boolean getSendOnPasswordChange() {
		return sendOnPasswordChange;
	}

	public String getSchemaAttributeName() {
		return schemaAttributeName;
	}

	public void setSchemaAttributeName(String schemaAttributeName) {
		this.schemaAttributeName = schemaAttributeName;
	}
	
	public Boolean getPasswordAttribute() {
		return passwordAttribute;
	}

	public void setPasswordAttribute(Boolean passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}
}
