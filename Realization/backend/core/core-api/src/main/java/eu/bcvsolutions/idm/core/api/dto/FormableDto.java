package eu.bcvsolutions.idm.core.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.bcvsolutions.idm.core.api.domain.Auditable;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;

/**
 * Common dto with embedded eav attributes.
 * 
 * Filled form instances are persisted as "PATCH" every time. 
 * Send {@code null} values, when delete attribute value is needed (single value with {@code null} is enough).
 * 
 * @author Radek Tomiška
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class FormableDto extends AbstractDto {

	private static final long serialVersionUID = 1L;
	@JsonProperty(value = "_eav")
	private List<IdmFormInstanceDto> eavs;
	
	public FormableDto() {
	}
	
	public FormableDto(UUID id) {
		super(id);
	}
	
	public FormableDto(Auditable auditable) {
		super(auditable);
	}
	
	public List<IdmFormInstanceDto> getEavs() {
		if (eavs == null) {
			eavs = new ArrayList<>();
		}
		return eavs;
	}
	
	public void setEavs(List<IdmFormInstanceDto> eavs) {
		this.eavs = eavs;
	}
}
