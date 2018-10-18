package eu.bcvsolutions.idm.core.api.dto.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;

/**
 * Filter for universal requests
 *
 * @author svandav
 */
public class IdmRequestFilter extends DataFilter {
	
	private List<RequestState> states;
	private String ownerType;
	private UUID ownerId;
	private DateTime createdAfter;
	private DateTime createdBefore;

	public DateTime getCreatedAfter() {
		return createdAfter;
	}

	public void setCreatedAfter(DateTime createdAfter) {
		this.createdAfter = createdAfter;
	}

	public DateTime getCreatedBefore() {
		return createdBefore;
	}

	public void setCreatedBefore(DateTime createdBefore) {
		this.createdBefore = createdBefore;
	}

	public IdmRequestFilter() {
		this(new LinkedMultiValueMap<>());
	}

	public IdmRequestFilter(MultiValueMap<String, Object> data) {
		super(IdmRequestDto.class, data);
	}

	public List<RequestState> getStates() {
		if (states == null) {
			states = new ArrayList<>();
		}
		return states;
	}

	public void setStates(List<RequestState> states) {
		this.states = states;
	}

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}
}
