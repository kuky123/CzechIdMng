package eu.bcvsolutions.idm.acc.entity;

import java.text.MessageFormat;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import javax.validation.constraints.NotNull;

import eu.bcvsolutions.idm.acc.domain.ProvisioningOperationType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningContext;
import eu.bcvsolutions.idm.acc.domain.ProvisioningOperation;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;

/**
 * Persisted "active" provisioning operation. Any operation has request and batch.
 * 
 * @author Radek Tomiška
 *
 */
@Entity
@Table(name = "sys_provisioning_operation", indexes = {
		@Index(name = "idx_sys_p_o_created", columnList = "created"),
		@Index(name = "idx_sys_p_o_operation_type", columnList = "operation_type"),
		@Index(name = "idx_sys_p_o_entity_sys_e_id", columnList = "system_entity_id"),
		@Index(name = "idx_sys_p_o_entity_identifier", columnList = "entity_identifier")
		})
public class SysProvisioningOperation extends AbstractEntity implements ProvisioningOperation {

	private static final long serialVersionUID = -6191740329296942394L;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false)
	private ProvisioningEventType operationType;
	
	@NotNull
	@Column(name = "provisioning_context", length = Integer.MAX_VALUE, nullable = false)
	private ProvisioningContext provisioningContext; 
	
	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "system_entity_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private SysSystemEntity systemEntity; // account, etc.
	
	@Column(name = "entity_identifier")
	private UUID entityIdentifier;
	
	@NotNull
	@OneToOne(mappedBy = "operation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private SysProvisioningRequest request;
	
	@Override
	public ProvisioningEventType getOperationType() {
		return operationType;
	}

	public void setOperationType(ProvisioningEventType operationType) {
		this.operationType = operationType;
	}

	
	public SysSystemEntity getSystemEntity() {
		return systemEntity;
	}
	
	public void setSystemEntity(SysSystemEntity systemEntity) {
		this.systemEntity = systemEntity;
	}
	
	@Override
	public SysSystem getSystem() {
		if (systemEntity == null) {
			return null;
		}
		return systemEntity.getSystem();
	}

	@Override
	public SystemEntityType getEntityType() {
		if (systemEntity == null) {
			return null;
		}
		return systemEntity.getEntityType();
	}	
	
	@Override
	public String getSystemEntityUid() {
		if (systemEntity == null) {
			return null;
		}
		return systemEntity.getUid();
	}

	@Override
	public UUID getEntityIdentifier() {
		return entityIdentifier;
	}

	public void setEntityIdentifier(UUID entityIdentifier) {
		this.entityIdentifier = entityIdentifier;
	}
	
	@Override
	public OperationState getResultState() {
		if (request != null && request.getResult() != null) {
			return request.getResult().getState();
		}
		return null;
	}
	
	public void setResultState(OperationState resultState) {
		if (request != null) {
			if (request.getResult() == null) {
				request.setResult(new OperationResult(resultState));
			} else {
				request.getResult().setState(resultState);
			}
		}
	}
	
	@Override
	public OperationResult getResult() {
		if (request != null) {
			return request.getResult();
		}
		return null;
	}
	
	public void setRequest(SysProvisioningRequest request) {
		this.request = request;
	}
	
	public SysProvisioningRequest getRequest() {
		return request;
	}
	
	@Override
	public ProvisioningContext getProvisioningContext() {
		return provisioningContext;
	}
	
	public void setProvisioningContext(ProvisioningContext provisioningContext) {
		this.provisioningContext = provisioningContext;
	}
	
	/**
	 * New {@link SysProvisioningOperation} builder.
	 * 
	 * @author Radek Tomiška
	 *
	 */
	public static class Builder {
		private ProvisioningEventType operationType;
		private ProvisioningContext provisioningContext;
		private UUID entityIdentifier;
		private SysSystemEntity systemEntity;
		
		public Builder setOperationType(ProvisioningEventType operationType) {
			this.operationType = operationType;
			return this;
		}
		
		/**
		 * Maps {@linkAccountOperationType} to {@link ProvisioningEventType}.
		 * @param operationType
		 * @return
		 */
		public Builder setOperationType(ProvisioningOperationType operationType) {
			switch (operationType) {
				case CREATE: {
					this.operationType = ProvisioningEventType.CREATE;
					break;
				}
				case UPDATE: {
					this.operationType = ProvisioningEventType.UPDATE;
					break;
				}
				case DELETE: {
					this.operationType = ProvisioningEventType.DELETE;
					break;
				}
				default: {
					throw new UnsupportedOperationException(MessageFormat.format("Account operation type [{}] is not supported for provisioning", operationType));
				}
			}
			
			return this;
		}
		
		public Builder setProvisioningContext(ProvisioningContext provisioningContext) {
			this.provisioningContext = provisioningContext;
			return this;
		}
		
		
		public Builder setEntityIdentifier(UUID entityIdentifier) {
			this.entityIdentifier = entityIdentifier;
			return this;
		}
		
		public Builder setSystemEntity(SysSystemEntity systemEntity) {
			this.systemEntity = systemEntity;
			return this;
		}
		
		/**
		 * Returns newly constructed SysProvisioningOperation object.
		 * 
		 * @return
		 */
		public SysProvisioningOperation build() {
			SysProvisioningOperation provisioningOperation = new SysProvisioningOperation();
			provisioningOperation.setOperationType(operationType);
			provisioningOperation.setSystemEntity(systemEntity);
			provisioningOperation.setEntityIdentifier(entityIdentifier);
			provisioningOperation.setProvisioningContext(provisioningContext);
			return provisioningOperation;
		}
	}
	
}
