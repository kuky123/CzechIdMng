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
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeType;

/**
 * <i>SysSystemMapping</i> is responsible for mapping attribute to entity
 * type and operations (Provisioning, Reconciliace, Synchronisation) to idm
 * entity
 * 
 * @author svandav
 *
 */
@Entity
@Table(name = "sys_system_mapping", indexes = {
		@Index(name = "ux_sys_s_mapping_name", columnList = "name, object_class_id", unique = true),
		@Index(name = "idx_sys_s_mapping_o_c_id", columnList = "object_class_id"),
		@Index(name = "idx_sys_s_mapping_o_type", columnList = "operation_type"),
		@Index(name = "idx_sys_s_mapping_e_type", columnList = "entity_type")
		})
public class SysSystemMapping extends AbstractEntity {

	private static final long serialVersionUID = -8492560756893726050L;

	@Audited
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "name", length = DefaultFieldLengths.NAME, nullable = false)
	private String name;
	
	@Audited
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false)
	private SystemEntityType entityType;
	
	@Audited
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "object_class_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private SysSchemaObjectClass objectClass;

	@Audited
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false)
	private SystemOperationType operationType;
	
	@Audited
	@ManyToOne(optional = true)
	@JoinColumn(name = "tree_type_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmTreeType treeType;
	
	@Audited
	@Column(name = "protection_enabled", nullable = true)
	private boolean protectionEnabled = false;
	
	@Audited
	@Column(name = "protection_interval", nullable = true)
	private Integer protectionInterval;
	
	@Audited
	@Type(type = "org.hibernate.type.StringClobType")
	@Column(name = "can_be_acc_created_script")
	private String canBeAccountCreatedScript;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public SystemEntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(SystemEntityType entityType) {
		this.entityType = entityType;
	}

	public SystemOperationType getOperationType() {
		return operationType;
	}

	public void setOperationType(SystemOperationType operationType) {
		this.operationType = operationType;
	}

	@JsonIgnore
	public SysSystem getSystem() {
		if (objectClass == null) {
			return null;
		}
		return objectClass.getSystem();
	}

	public void setObjectClass(SysSchemaObjectClass objectClass) {
		this.objectClass = objectClass;
	}
	
	public SysSchemaObjectClass getObjectClass() {
		return objectClass;
	}

	public IdmTreeType getTreeType() {
		return treeType;
	}

	public void setTreeType(IdmTreeType treeType) {
		this.treeType = treeType;
	}

	public boolean isProtectionEnabled() {
		return protectionEnabled;
	}

	public void setProtectionEnabled(boolean protectionEnabled) {
		this.protectionEnabled = protectionEnabled;
	}

	public Integer getProtectionInterval() {
		return protectionInterval;
	}

	public void setProtectionInterval(Integer protectionInterval) {
		this.protectionInterval = protectionInterval;
	}

	public String getCanBeAccountCreatedScript() {
		return canBeAccountCreatedScript;
	}

	public void setCanBeAccountCreatedScript(String canBeAccountCreatedScript) {
		this.canBeAccountCreatedScript = canBeAccountCreatedScript;
	}
	
}
