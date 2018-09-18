package eu.bcvsolutions.idm.core.model.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;

/**
 * Entity for universal request
 * 
 * @author svandav
 * @since 9.1.0
 *
 */
@Entity
@Table(name = "idm_request", indexes = { 
		@Index(name = "idx_idm_req_wf", columnList = "wf_process_id"), //
		@Index(name = "idx_idm_req_name", columnList = "name"), //
		@Index(name = "idx_idm_req_o_id", columnList = "owner_id"), //
		@Index(name = "idx_idm_req_o_type", columnList = "owner_type"), //
		@Index(name = "idx_idm_req_state", columnList = "state") //
})
public class IdmRequest extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Audited
	@NotNull
	@Column(name = "request_type", nullable = false)
	private String requestType;

	@Audited
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "owner_type", length = DefaultFieldLengths.NAME, nullable = false)
	private String ownerType;

	@Audited
	@Column(name = "owner_id", length = 16)
	private UUID ownerId;

	@Audited
	@NotNull
	@Column(name = "state")
	@Enumerated(EnumType.STRING)
	private RequestState state = RequestState.CONCEPT;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "name", length = DefaultFieldLengths.NAME, nullable = true)
	private String name;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "wf_process_id")
	private String wfProcessId;

	@Embedded
	private OperationResult result;

	@Audited
	@NotNull
	@Column(name = "execute_immediately")
	private boolean executeImmediately = false;

	@Audited
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	@Column(name = "description")
	private String description;

	@Audited
	@Enumerated(EnumType.STRING)
	@Column(name = "operation")
	private RequestOperationType operation = RequestOperationType.UPDATE;

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

	public String getWfProcessId() {
		return wfProcessId;
	}

	public void setWfProcessId(String wfProcessId) {
		this.wfProcessId = wfProcessId;
	}

	public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
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

	public RequestOperationType getOperation() {
		return operation;
	}

	public void setOperation(RequestOperationType operation) {
		this.operation = operation;
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