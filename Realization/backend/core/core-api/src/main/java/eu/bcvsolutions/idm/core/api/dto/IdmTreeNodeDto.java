package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.ExternalIdentifiable;
import io.swagger.annotations.ApiModelProperty;

/**
 * Tree node dto
 *
 * @author Radek Tomiška
 */
@Relation(collectionRelation = "treeNodes")
public class IdmTreeNodeDto extends FormableDto implements Disableable, ExternalIdentifiable {

    private static final long serialVersionUID = 1337282508070610164L;
    //
    @Size(max = DefaultFieldLengths.NAME)
	@ApiModelProperty(notes = "Unique external identifier.")
	private String externalId;
    @NotEmpty
    @Size(min = 1, max = DefaultFieldLengths.NAME)
    private String code;
    @NotEmpty
    @Size(min = 1, max = DefaultFieldLengths.NAME)
    private String name;
    @Embedded(dtoClass = IdmTreeNodeDto.class)
    private UUID parent;
    @NotNull
    @Embedded(dtoClass = IdmTreeTypeDto.class)
    private UUID treeType;
    private boolean disabled;
    private long lft; // forest index
	private long rgt; // forest index

	public IdmTreeNodeDto() {
	}
	
	public IdmTreeNodeDto(UUID id) {
		super(id);
	}
	
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

    public UUID getParent() {
        return parent;
    }

    public void setParent(UUID parent) {
        this.parent = parent;
    }

    public UUID getTreeType() {
        return treeType;
    }

    public void setTreeType(UUID treeType) {
        this.treeType = treeType;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    public long getLft() {
		return lft;
	}

	public void setLft(long lft) {
		this.lft = lft;
	}

	public long getRgt() {
		return rgt;
	}

	public void setRgt(long rgt) {
		this.rgt = rgt;
	}

	/**
	 * Children count based on index
	 * 
	 * @return
	 */
	public Integer getChildrenCount() {
		return (int) ((rgt - lft) / 2);
	}
	
	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	@Override
	public String getExternalId() {
		return externalId;
	}
}
