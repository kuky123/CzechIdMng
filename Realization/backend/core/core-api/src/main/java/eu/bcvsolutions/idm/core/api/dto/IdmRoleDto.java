package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.ExternalIdentifiable;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.domain.RoleType;
import io.swagger.annotations.ApiModelProperty;

/**
 * Dto for role
 *
 * @author svandav
 * @author Radek Tomiška
 */
@Relation(collectionRelation = "roles")
public class IdmRoleDto extends FormableDto implements Disableable, Codeable, ExternalIdentifiable, Requestable {

    private static final long serialVersionUID = 1L;

    @NotEmpty
    @Size(min = 1, max = DefaultFieldLengths.NAME)
    private String code;
    @NotEmpty
    @Size(min = 1, max = DefaultFieldLengths.NAME)
    private String name;
    @Size(max = DefaultFieldLengths.NAME)
	@ApiModelProperty(notes = "Unique external identifier.")
	private String externalId;
    private boolean disabled;
    private boolean canBeRequested;
    private RoleType roleType = RoleType.TECHNICAL;
    private int priority = 0;
    private boolean approveRemove;
    @Size(max = DefaultFieldLengths.DESCRIPTION)
    private String description;
    @Embedded(dtoClass = IdmRequestItemDto.class)
    private UUID requestItem; // Isn't persist in the entity


    public IdmRoleDto() {
	}
    
    public IdmRoleDto(UUID id) {
    	super(id);
	}
    
    @Override
    public String getCode() {
    	return code;
    }
    
    public void setCode(String code) {
		this.code = code;
	}
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
    	this.name = name;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;

    }

    public boolean isApproveRemove() {
        return approveRemove;
    }

    public void setApproveRemove(boolean approveRemove) {
        this.approveRemove = approveRemove;
    }
    
    public boolean isCanBeRequested() {
		return canBeRequested;
	}
    
    public void setCanBeRequested(boolean canBeRequested) {
		this.canBeRequested = canBeRequested;
	}
	
	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	@Override
	public String getExternalId() {
		return externalId;
	}

	@Override
	public UUID getRequestItem() {
		return requestItem;
	}

	@Override
	public void setRequestItem(UUID requestItem) {
		this.requestItem = requestItem;
	}
}