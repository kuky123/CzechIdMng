package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import eu.bcvsolutions.idm.core.api.domain.Auditable;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.RequestState;

/**
 * DTO for request
 * 
 * @author svandav
 * @since 8.0.0
 *
 */
public abstract class AbstractRequestDto extends AbstractDto {

	private static final long serialVersionUID = 1L;
	public static final String WF_PROCESS_FIELD = "wfProcessId";
	
	@Size(max = DefaultFieldLengths.NAME)
	protected String wfProcessId;
	@JsonProperty(access = Access.READ_ONLY)
	protected OperationResultDto result;
	@NotNull
	protected RequestState state = RequestState.CONCEPT;
	@NotNull
	protected boolean executeImmediately = false;
	protected String description;

	public AbstractRequestDto() {
		super();
	}

	public AbstractRequestDto(UUID id) {
		super(id);
	}

	public AbstractRequestDto(Auditable auditable) {
		super(auditable);
	}

	public String getWfProcessId() {
		return wfProcessId;
	}

	public void setWfProcessId(String wfProcessId) {
		this.wfProcessId = wfProcessId;
	}

	public OperationResultDto getResult() {
		return result;
	}

	public void setResult(OperationResultDto result) {
		this.result = result;
	}

	public RequestState getState() {
		return state;
	}

	public void setState(RequestState state) {
		this.state = state;
	}

	public boolean isExecuteImmediately() {
		return executeImmediately;
	}

	public void setExecuteImmediately(boolean executeImmediately) {
		this.executeImmediately = executeImmediately;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}