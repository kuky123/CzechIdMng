package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.UUID;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.ConceptRoleRequestOperation;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestState;
import eu.bcvsolutions.idm.core.api.dto.IdmConceptRoleRequestDto;

/**
 * Filter for concept role request
 *
 * @author svandav
 */
public class IdmConceptRoleRequestFilter extends DataFilter {
	
    private UUID roleRequestId;
    private RoleRequestState state;
    private UUID identityRoleId;
    private UUID roleId;
    private UUID identityContractId;
    private UUID automaticRole;
    private ConceptRoleRequestOperation operation;
    
    public IdmConceptRoleRequestFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public IdmConceptRoleRequestFilter(MultiValueMap<String, Object> data) {
		super(IdmConceptRoleRequestDto.class, data);
	}

    public UUID getRoleRequestId() {
        return roleRequestId;
    }

    public void setRoleRequestId(UUID roleRequestId) {
        this.roleRequestId = roleRequestId;
    }

    public RoleRequestState getState() {
        return state;
    }

    public void setState(RoleRequestState state) {
        this.state = state;
    }

    public UUID getIdentityRoleId() {
        return identityRoleId;
    }

    public void setIdentityRoleId(UUID identityRoleId) {
        this.identityRoleId = identityRoleId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getIdentityContractId() {
        return identityContractId;
    }

    public void setIdentityContractId(UUID identityContractId) {
        this.identityContractId = identityContractId;
    }

    public UUID getAutomaticRole() {
		return automaticRole;
	}

	public void setAutomaticRole(UUID automaticRole) {
		this.automaticRole = automaticRole;
	}

	public ConceptRoleRequestOperation getOperation() {
        return operation;
    }

    public void setOperation(ConceptRoleRequestOperation operation) {
        this.operation = operation;
    }
    
    /**
     * @deprecated since 7.7.0 use {@link #getAutomaticRole()}
     * @return
     */
    @Deprecated
    public UUID getRoleTreeNodeId() {
    	return getAutomaticRole();
    }
    
    /**
     * @deprecated since 7.7.0 use {@link #setAutomaticRole(UUID)}
     * @param roleTreeNodeId
     */
    @Deprecated
    public void setRoleTreeNodeId(UUID roleTreeNodeId) {
    	setAutomaticRole(roleTreeNodeId);
    }

}
