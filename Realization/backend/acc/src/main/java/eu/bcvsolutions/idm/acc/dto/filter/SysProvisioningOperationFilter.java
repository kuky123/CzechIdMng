package eu.bcvsolutions.idm.acc.dto.filter;

import java.util.UUID;

import org.joda.time.DateTime;

import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;

/**
 * Provisioning operation filter
 * 
 * @author Radek Tomiška
 *
 */
public class SysProvisioningOperationFilter implements BaseFilter {

	private DateTime from;
	private DateTime till;
	private UUID systemId;
	private ProvisioningEventType operationType;
	private SystemEntityType entityType;
	private OperationState resultState;
	private UUID entityIdentifier;
	private UUID systemEntity;
	private String systemEntityUid;
	private UUID batchId;

	public DateTime getFrom() {
		return from;
	}

	public void setFrom(DateTime from) {
		this.from = from;
	}

	public DateTime getTill() {
		return till;
	}

	public void setTill(DateTime till) {
		this.till = till;
	}

	public UUID getSystemId() {
		return systemId;
	}

	public void setSystemId(UUID systemId) {
		this.systemId = systemId;
	}

	public ProvisioningEventType getOperationType() {
		return operationType;
	}

	public void setOperationType(ProvisioningEventType operationType) {
		this.operationType = operationType;
	}

	public SystemEntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(SystemEntityType entityType) {
		this.entityType = entityType;
	}

	public OperationState getResultState() {
		return resultState;
	}

	public void setResultState(OperationState resultState) {
		this.resultState = resultState;
	}

	public UUID getEntityIdentifier() {
		return entityIdentifier;
	}

	public void setEntityIdentifier(UUID entityIdentifier) {
		this.entityIdentifier = entityIdentifier;
	}
	
	public UUID getSystemEntity() {
		return systemEntity;
	}

	public void setSystemEntity(UUID systemEntity) {
		this.systemEntity = systemEntity;
	}

	public void setBatchId(UUID batchId) {
		this.batchId = batchId;
	}
	
	public UUID getBatchId() {
		return batchId;
	}

	public String getSystemEntityUid() {
		return systemEntityUid;
	}

	public void setSystemEntityUid(String systemEntityUid) {
		this.systemEntityUid = systemEntityUid;
	}
}
