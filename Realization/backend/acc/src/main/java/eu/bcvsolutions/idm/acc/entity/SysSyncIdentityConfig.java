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

import org.hibernate.envers.Audited;

import eu.bcvsolutions.idm.acc.domain.SynchronizationInactiveOwnerBehaviorType;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;

/**
 * <i>SysSyncContractConfig</i> is responsible for keep specific informations about
 * identity synchronization configuration
 * 
 * @author svandav
 *
 */
@Entity
@Table(name = "sys_sync_identity_config", indexes = {
		@Index(name = "idx_sys_s_iden_conf_role", columnList = "default_role_id")
		})
public class SysSyncIdentityConfig extends SysSyncConfig{

	private static final long serialVersionUID = 1L;
	
	@Audited
	@ManyToOne(optional = true)
	@JoinColumn(name = "default_role_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmRole defaultRole;
	
	@Audited
	@Enumerated(EnumType.STRING)
	@Column(name = "inactive_owner_behavior")
	private SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior;

	/*
	 * Start recalculation after end synchronization for automatic roles by attribute
	 */
	@Audited
	@NotNull
	@Column(name = "start_auto_role_rec", nullable = false)
	private boolean startAutoRoleRec = true;

	/*
	 * During creating identity will be created default contract for it
	 */
	@Audited
	@NotNull
	@Column(name = "create_default_contract", nullable = false)
	private boolean createDefaultContract = false;

	public IdmRole getDefaultRole() {
		return defaultRole;
	}

	public void setDefaultRole(IdmRole defaultRole) {
		this.defaultRole = defaultRole;
	}

	public boolean isStartAutoRoleRec() {
		return startAutoRoleRec;
	}

	public void setStartAutoRoleRec(boolean startAutoRoleRec) {
		this.startAutoRoleRec = startAutoRoleRec;
	}

	public boolean isCreateDefaultContract() {
		return createDefaultContract;
	}

	public void setCreateDefaultContract(boolean createDefaultContract) {
		this.createDefaultContract = createDefaultContract;
	}

	public SynchronizationInactiveOwnerBehaviorType getInactiveOwnerBehavior() {
		return inactiveOwnerBehavior;
	}

	public void setInactiveOwnerBehavior(SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior) {
		this.inactiveOwnerBehavior = inactiveOwnerBehavior;
	}
}
