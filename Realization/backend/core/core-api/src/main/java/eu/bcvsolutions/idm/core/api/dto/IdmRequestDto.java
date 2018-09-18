package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.Size;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.RequestState;

/**
 * DTO for request
 *
 * @author svandav
 * @since 9.1.0
 */
@Relation(collectionRelation = "requests")
public class IdmRequestDto extends AbstractRequestDto {

	private static final long serialVersionUID = 1L;
	public static final String OWNER_FIELD = "ownerId";

	@Size(min = 1, max = DefaultFieldLengths.NAME)
	private String requestType;
	@Size(max = DefaultFieldLengths.NAME)
	private String name;
	private String ownerType;
	private UUID ownerId;

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public RequestState getState() {
		return state;
	}

	public void setState(RequestState state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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