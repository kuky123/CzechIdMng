package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import org.joda.time.LocalDate;
import org.springframework.hateoas.core.Relation;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.entity.ValidableEntity;

/**
 * IdentityRole DTO
 *
 * @author svanda
 * @author Radek Tomiška
 */
@Relation(collectionRelation = "identityRoles")
public class IdmIdentityRoleDto extends AbstractDto implements ValidableEntity {
	
	private static final long serialVersionUID = 1L;
	public static final String PROPERTY_IDENTITY_CONTRACT = "identityContract";
	//
    @Embedded(dtoClass = IdmIdentityContractDto.class)
    private UUID identityContract;
    @Embedded(dtoClass = IdmRoleDto.class)
    private UUID role;
    private LocalDate validFrom;
    private LocalDate validTill;
    private boolean automaticRole;
    @Embedded(dtoClass = IdmRoleTreeNodeDto.class)
    private UUID roleTreeNode;

    public IdmIdentityRoleDto() {
    }

    public IdmIdentityRoleDto(UUID id) {
        super(id);
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTill() {
        return validTill;
    }

    public void setValidTill(LocalDate validTo) {
        this.validTill = validTo;
    }

    public UUID getIdentityContract() {
        return identityContract;
    }

    public void setIdentityContract(UUID identityContract) {
        this.identityContract = identityContract;
    }
    
    @JsonIgnore
    public void setIdentityContractDto(IdmIdentityContractDto identityContract) {
    	Assert.notNull(identityContract);
    	//
        this.identityContract = identityContract.getId();
        this.getEmbedded().put(PROPERTY_IDENTITY_CONTRACT, identityContract);
    }

    public UUID getRole() {
        return role;
    }

    public void setRole(UUID role) {
        this.role = role;
    }

    public boolean isAutomaticRole() {
        return automaticRole;
    }

    public void setAutomaticRole(boolean automaticRole) {
        this.automaticRole = automaticRole;
    }

    public UUID getRoleTreeNode() {
        return roleTreeNode;
    }

    public void setRoleTreeNode(UUID roleTreeNode) {
        this.roleTreeNode = roleTreeNode;
    }
}