package eu.bcvsolutions.idm.acc.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.FormableDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordPolicyDto;
import eu.bcvsolutions.idm.ic.api.IcConnectorInstance;
import eu.bcvsolutions.idm.ic.impl.IcConnectorInstanceImpl;

/**
 * Target system setting - is used for accont management and provisioning DTO
 * 
 * @author Svanda
 *
 */
@Relation(collectionRelation = "systems")
public class SysSystemDto extends FormableDto implements Codeable, Disableable {

	private static final long serialVersionUID = 1L;

	private String name;
	private String description;
	private boolean readonly;
	private boolean disabled;
	private boolean queue;
	@JsonProperty(access = Access.READ_ONLY)
	private boolean virtual;
	private Long version; // Optimistic lock - will be used with ETag
	private boolean remote;
	@Embedded(dtoClass = IdmPasswordPolicyDto.class)
	private UUID passwordPolicyValidate;
	@Embedded(dtoClass = IdmPasswordPolicyDto.class)
	private UUID passwordPolicyGenerate;
	private SysConnectorKeyDto connectorKey;
	private SysConnectorServerDto connectorServer;
	private SysBlockedOperationDto blockedOperation;

	public String getName() {
		return name;
	}

	@Override
	@JsonIgnore
	public String getCode() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isQueue() {
		return queue;
	}

	public void setQueue(boolean queue) {
		this.queue = queue;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public boolean isRemote() {
		return remote;
	}

	public void setRemote(boolean remote) {
		this.remote = remote;
	}

	public UUID getPasswordPolicyValidate() {
		return passwordPolicyValidate;
	}

	public void setPasswordPolicyValidate(UUID passwordPolicyValidate) {
		this.passwordPolicyValidate = passwordPolicyValidate;
	}

	public UUID getPasswordPolicyGenerate() {
		return passwordPolicyGenerate;
	}

	public void setPasswordPolicyGenerate(UUID passwordPolicyGenerate) {
		this.passwordPolicyGenerate = passwordPolicyGenerate;
	}

	public SysConnectorKeyDto getConnectorKey() {
		return connectorKey;
	}

	public void setConnectorKey(SysConnectorKeyDto connectorKey) {
		this.connectorKey = connectorKey;
	}

	public SysConnectorServerDto getConnectorServer() {
		return connectorServer;
	}

	public void setConnectorServer(SysConnectorServerDto connectorServer) {
		this.connectorServer = connectorServer;
	}
	
	@JsonIgnore
	public IcConnectorInstance getConnectorInstance() {
		return new IcConnectorInstanceImpl(this.getConnectorServer(), this.getConnectorKey(), this.isRemote());
	}

	public SysBlockedOperationDto getBlockedOperation() {
		return blockedOperation;
	}

	public void setBlockedOperation(SysBlockedOperationDto blockedOperation) {
		this.blockedOperation = blockedOperation;
	}
}
