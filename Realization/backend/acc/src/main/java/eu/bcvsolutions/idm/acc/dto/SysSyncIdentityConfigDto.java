package eu.bcvsolutions.idm.acc.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.acc.domain.SynchronizationInactiveOwnerBehaviorType;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;

/**
 * Identity sync configuration DTO
 * 
 * @author svandav
 *
 */

@Relation(collectionRelation = "synchronizationConfigs")
public class SysSyncIdentityConfigDto extends AbstractSysSyncConfigDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = IdmRoleDto.class)
	private UUID defaultRole;
	
	/*
	 * If the identity doesn't have any valid contract for assigning the default role,
	 * this choice specifies if the account should be linked.
	 */
	private SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior;
	
	/*
	 * Start recalculation after end synchronization for automatic roles by attribute
	 */
	private boolean startAutoRoleRec = true;
	
	/*
	 * During creating identity will be created default contract for it
	 */
	private boolean createDefaultContract = false;

	public UUID getDefaultRole() {
		return defaultRole;
	}

	public void setDefaultRole(UUID defaultRole) {
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
