package eu.bcvsolutions.idm.core.api.audit.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import io.swagger.annotations.ApiModelProperty;

/**
 * Default DTO for audit detail.
 * All default values for IdmAudit + Map with revision values
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 */
@Relation(collectionRelation = "audits")
public class IdmAuditDto implements BaseDto {

	private static final long serialVersionUID = 6910043282740335765L;
	
	public static final String CHANGED_COLUMNS_DELIMITER = ",";

	private Long id;

    private UUID entityId;

    private long timestamp;

    private String modification;

    private String type;

    private String changedAttributes;

    private UUID modifierId;

    private String modifier;
    
    private String originalModifier;
	
	private UUID originalModifierId;
	
	private String ownerId;
	
	private String ownerCode;
	
	private String ownerType;
	
	private String subOwnerId;
	
	private String subOwnerCode;
	
	private String subOwnerType;
	
	@JsonProperty(value = "_embedded", access = Access.READ_ONLY)
	@ApiModelProperty(readOnly = true)
	private Map<String, BaseDto> embedded;

    public UUID getModifierId() {
        return modifierId;
    }

    public void setModifierId(UUID modifierId) {
        this.modifierId = modifierId;
    }

    private Map<String, Object> revisionValues;

    public IdmAuditDto() {
        super();
        revisionValues = new HashMap<>();
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getChangedAttributes() {
        return changedAttributes;
    }

    public void setChangedAttributes(String changedAttributes) {
        this.changedAttributes = changedAttributes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModification() {
        return modification;
    }

    public void setModification(String modification) {
        this.modification = modification;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    public Map<String, Object> getRevisionValues() {
        return revisionValues;
    }

    public void setRevisionValues(Map<String, Object> revisionValues) {
        this.revisionValues = revisionValues;
    }

    public Long getId() {
        return id;
    }

    @Override
    public void setId(Serializable id) {
        this.id = Long.valueOf(id.toString());
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

	public String getOriginalModifier() {
		return originalModifier;
	}

	public void setOriginalModifier(String originalModifier) {
		this.originalModifier = originalModifier;
	}

	public UUID getOriginalModifierId() {
		return originalModifierId;
	}

	public void setOriginalModifierId(UUID originalModifierId) {
		this.originalModifierId = originalModifierId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerCode() {
		return ownerCode;
	}

	public void setOwnerCode(String ownerCode) {
		this.ownerCode = ownerCode;
	}

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}

	public String getSubOwnerId() {
		return subOwnerId;
	}

	public void setSubOwnerId(String subOwnerId) {
		this.subOwnerId = subOwnerId;
	}

	public String getSubOwnerCode() {
		return subOwnerCode;
	}

	public void setSubOwnerCode(String subOwnerCode) {
		this.subOwnerCode = subOwnerCode;
	}

	public String getSubOwnerType() {
		return subOwnerType;
	}

	public void setSubOwnerType(String subOwnerType) {
		this.subOwnerType = subOwnerType;
	}
	
	// TODO: DRY see IdmIAudit
	public void addChanged(String changedColumn) {
		if (StringUtils.isEmpty(changedAttributes)) {
			changedAttributes = changedColumn;
		} else {
			changedAttributes = String.format("%s%s%s", changedAttributes, CHANGED_COLUMNS_DELIMITER, changedColumn);
		}
	}
	
	// TODO: DRY see IdmIAudit
	public void addChanged(List<String> changedColumns) {
		if (changedColumns == null) {
			// nothing to add
			return;
		}
		changedColumns.forEach(changedColumn -> {
			addChanged(changedColumn);
		});
	}
	
	public Map<String, BaseDto> getEmbedded() {
		if(embedded == null){
			embedded = new HashMap<>();
		}
		return embedded;
	}

	public void setEmbedded(Map<String, BaseDto> emmbedded) {
		this.embedded = emmbedded;
	}
}
