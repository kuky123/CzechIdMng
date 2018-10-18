package eu.bcvsolutions.idm.core.ecm.api.dto.filter;

import java.io.Serializable;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.filter.DataFilter;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;

/**
 * Filter for attachments
 * 
 * @author Radek Tomiška
 * @since 7.6.0
 *
 */
public class IdmAttachmentFilter extends DataFilter {

	public static final String PARAMETER_OWNER_ID = "ownerId";
	public static final String PARAMETER_OWNER_TYPE = "ownerType";
	public static final String PARAMETER_CREATED_BEFORE= "createdBefore";
	public static final String PARAMETER_CREATED_AFTER = "createdAfter";
	//
	private String name;
	private Boolean lastVersionOnly; // true - last version only
	private UUID versionsFor; // attachment id - all versions for attachment
	
	public IdmAttachmentFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public IdmAttachmentFilter(MultiValueMap<String, Object> data) {
		super(IdmAttachmentDto.class, data);
	}

	public UUID getOwnerId() {
		return DtoUtils.toUuid(data.getFirst(PARAMETER_OWNER_ID));
	}

	public void setOwnerId(UUID ownerId) {
		data.set(PARAMETER_OWNER_ID, ownerId);
	}

	public String getOwnerType() {
		return (String) data.getFirst(PARAMETER_OWNER_TYPE);
	}

	public void setOwnerType(String ownerType) {
		data.set(PARAMETER_OWNER_TYPE, ownerType);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setLastVersionOnly(Boolean lastVersionOnly) {
		this.lastVersionOnly = lastVersionOnly;
	}
	
	public Boolean getLastVersionOnly() {
		return lastVersionOnly;
	}
	
	public UUID getVersionsFor() {
		return versionsFor;
	}
	
	public void setVersionsFor(UUID versionsFor) {
		this.versionsFor = versionsFor;
	}

	public DateTime getCreatedAfter() {
		// FIXME: parameter converter
		IdmFormValueDto value = new IdmFormValueDto();
		value.setPersistentType(PersistentType.DATETIME);
		value.setValue((Serializable) data.getFirst(PARAMETER_CREATED_AFTER));
		//
		return value.getDateValue();
	}

	public void setCreatedAfter(DateTime createdAfter) {
		data.set(PARAMETER_CREATED_AFTER, createdAfter);
	}

	public DateTime getCreatedBefore() {
		// FIXME: parameter converter
		IdmFormValueDto value = new IdmFormValueDto();
		value.setPersistentType(PersistentType.DATETIME);
		value.setValue((Serializable) data.getFirst(PARAMETER_CREATED_BEFORE));
		//
		return value.getDateValue();
	}

	public void setCreatedBefore(DateTime createdBefore) {
		data.set(PARAMETER_CREATED_BEFORE, createdBefore);
	}
}