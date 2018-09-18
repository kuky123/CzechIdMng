package eu.bcvsolutions.idm.core.model.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;

/**
 * Entity for universal request item
 *
 * @author svandav
 * @since 9.1.0
 */
@Entity
@Table(name = "idm_request_item", indexes = { //
		@Index(name = "idx_idm_req_item_o_id", columnList = "owner_id"), //
		@Index(name = "idx_idm_req_item_o_type", columnList = "owner_type"), //
		@Index(name = "idx_idm_req_item_operation", columnList = "operation"), //
		@Index(name = "idx_idm_req_item_req_id", columnList = "request_id"), //
		@Index(name = "idx_idm_req_item_so_id", columnList = "super_owner_id"), //
		@Index(name = "idx_idm_req_item_so_type", columnList = "super_owner_type"), //
})
public class IdmRequestItem extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@Audited
	@ManyToOne(optional = false)
	@JoinColumn(name = "request_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey(name = "none")
	private IdmRequest request;

	@Audited
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "owner_type", length = DefaultFieldLengths.NAME, nullable = false)
	private String ownerType;

	@Audited
	@Type(type = "org.hibernate.type.StringClobType")
	@Column(name = "data")
	private String data; // JSON represented target DTO
	
	@Audited
	@Column(name = "owner_id", length = 16)
	private UUID ownerId;

	@Audited
	@Column(name = "super_owner_type", length = DefaultFieldLengths.NAME)
	private String superOwnerType; // Super owner ... using for form value where super owner is FormableEntity

	@Audited
	@Column(name = "super_owner_id", length = 16)
	private UUID superOwnerId;
	
	@Audited
	@NotNull
	@Column(name = "state")
	@Enumerated(EnumType.STRING)
	private RequestState state = RequestState.CONCEPT;

	@Embedded
	private OperationResult result;

	@Audited
	@Enumerated(EnumType.STRING)
	@Column(name = "operation")
	private RequestOperationType operation = RequestOperationType.UPDATE;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "wf_process_id")
	private String wfProcessId;

	public IdmRequest getRequest() {
		return request;
	}

	public void setRequest(IdmRequest request) {
		this.request = request;
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

	public String getSuperOwnerType() {
		return superOwnerType;
	}

	public void setSuperOwnerType(String superOwnerType) {
		this.superOwnerType = superOwnerType;
	}

	public UUID getSuperOwnerId() {
		return superOwnerId;
	}

	public void setSuperOwnerId(UUID superOwnerId) {
		this.superOwnerId = superOwnerId;
	}

	public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
	}

	public RequestOperationType getOperation() {
		return operation;
	}

	public void setOperation(RequestOperationType operation) {
		this.operation = operation;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getWfProcessId() {
		return wfProcessId;
	}

	public void setWfProcessId(String wfProcessId) {
		this.wfProcessId = wfProcessId;
	}

	public RequestState getState() {
		return state;
	}

	public void setState(RequestState state) {
		this.state = state;
	}
}
