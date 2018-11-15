package eu.bcvsolutions.idm.acc.entity;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;

/**
 * <i>SysSystemAttributeMapping</i> is responsible for mapping schema attribute
 * to idm entity
 * 
 * @author svandav
 *
 */
@Entity
@Table(name = "sys_system_attribute_mapping", indexes = {
		@Index(name = "ux_sys_attr_m_name_enth", columnList = "name,system_mapping_id", unique = true)})
public class SysSystemAttributeMapping extends AbstractEntity {

	private static final long serialVersionUID = -8492560756893726050L;

	@Audited
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "name", length = DefaultFieldLengths.NAME, nullable = false)
	private String name;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "idm_property_name", length = DefaultFieldLengths.NAME, nullable = true)
	private String idmPropertyName;
	
	@Audited
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "system_mapping_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey(name = "none")
	private SysSystemMapping systemMapping;

	@Audited
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "schema_attribute_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey(name = "none")
	private SysSchemaAttribute schemaAttribute;

	@Audited
	@Column(name = "disabled_attribute", nullable = false)
	private boolean disabledAttribute = false;
	
	@Audited
	@Column(name = "extended_attribute", nullable = false)
	private boolean extendedAttribute = false;

	@Audited
	@Column(name = "entity_attribute", nullable = false)
	private boolean entityAttribute = true;

	@Audited
	@Column(name = "confidential_attribute", nullable = false)
	private boolean confidentialAttribute = false;
	
	@Audited
	@Column(name = "password_attribute", nullable = false)
	private boolean passwordAttribute = false;

	@Audited
	@Column(name = "uid", nullable = false)
	private boolean uid = false;

	@Audited
	@Type(type = "org.hibernate.type.StringClobType") // TODO: test on oracle/ mysql
	@Column(name = "transform_from_res_script")
	private String transformFromResourceScript;

	@Audited
	@Type(type = "org.hibernate.type.StringClobType") // TODO: test on oracle/ mysql
	@Column(name = "transform_to_res_script")
	private String transformToResourceScript;
	
	@Audited
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "strategy_type", nullable = false)
	private AttributeMappingStrategyType strategyType = AttributeMappingStrategyType.SET;

	@Audited
	@Column(name = "send_always", nullable = false)
	private boolean sendAlways = false;
	
	@Audited
	@Column(name = "send_only_if_not_null", nullable = false)
	private boolean sendOnlyIfNotNull = false;
	
	@Audited
	@Column(name = "authentication_attribute", nullable = false)
	private boolean authenticationAttribute = false;
	
	@Audited
	@Column(name = "send_on_password_change", nullable = false)
	private boolean sendOnPasswordChange = false;
	
	@Audited
	@Column(name = "attribute_cached", nullable = false)
	private boolean cached = true;
	
	@Audited
	@Column(name = "evict_contr_values_cache")
	private boolean evictControlledValuesCache = true;
	
	public String getIdmPropertyName() {
		return idmPropertyName;
	}

	public void setIdmPropertyName(String idmPropertyName) {
		this.idmPropertyName = idmPropertyName;
	}

	public SysSchemaAttribute getSchemaAttribute() {
		return schemaAttribute;
	}

	public void setSchemaAttribute(SysSchemaAttribute schemaAttribute) {
		this.schemaAttribute = schemaAttribute;
	}

	public boolean isExtendedAttribute() {
		return extendedAttribute;
	}

	public void setExtendedAttribute(boolean extendedAttribute) {
		this.extendedAttribute = extendedAttribute;
	}

	public String getTransformFromResourceScript() {
		return transformFromResourceScript;
	}

	public void setTransformFromResourceScript(String transformFromResourceScript) {
		this.transformFromResourceScript = transformFromResourceScript;
	}

	public String getTransformToResourceScript() {
		return transformToResourceScript;
	}

	public void setTransformToResourceScript(String transformToResourceScript) {
		this.transformToResourceScript = transformToResourceScript;
	}

	public SysSystemMapping getSystemMapping() {
		return systemMapping;
	}

	public void setSystemMapping(SysSystemMapping systemMapping) {
		this.systemMapping = systemMapping;
	}

	public boolean isUid() {
		return uid;
	}

	public void setUid(boolean uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public boolean isDisabledAttribute() {
		return disabledAttribute;
	} 

	public void setDisabledAttribute(boolean disabledAttribute) {
		this.disabledAttribute = disabledAttribute;
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

	public boolean isCached() {
		return cached;
	}

	public void setCached(boolean cached) {
		this.cached = cached;
	}

	public boolean isEvictControlledValuesCache() {
		return evictControlledValuesCache;
	}

	public void setEvictControlledValuesCache(boolean evictControlledValuesCache) {
		this.evictControlledValuesCache = evictControlledValuesCache;
	}

	public boolean isPasswordAttribute() {
		return passwordAttribute;
	}

	public void setPasswordAttribute(boolean passwordAttribute) {
		this.passwordAttribute = passwordAttribute;
	}
}
