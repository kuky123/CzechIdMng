package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.AbstractSysSyncConfigDto;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;

/**
 * Synchronization schedule task
 * 
 * @author Radek Tomiška
 * @author svandav
 *
 */
@Service
@Description("Synchronization scheduling - publishes start event only")
public class SynchronizationSchedulableTaskExecutor extends AbstractSchedulableTaskExecutor<Boolean> {

	@Autowired
	private SynchronizationService synchronizationService;
	@Autowired
	private SysSyncConfigService service;
	//
	private UUID synchronizationId;

	public SynchronizationSchedulableTaskExecutor() {
		super();
	}

	public SynchronizationSchedulableTaskExecutor(UUID synchronizationId) {
		this.synchronizationId = synchronizationId;
	}

	@Override
	public void init(Map<String, Object> properties) {
		super.init(properties);
		//
		synchronizationId = getParameterConverter().toUuid(properties,
				SynchronizationService.PARAMETER_SYNCHRONIZATION_ID);
		//
		// validation only
	    getConfig();
	}

	@Override
	public Boolean process() {
		AbstractSysSyncConfigDto config = getConfig();
		if (service.isRunning(config)) {
			throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_IS_RUNNING,
					ImmutableMap.of("name", config.getName()));
		}
		synchronizationService.startSynchronization(config, this);
		//
		return Boolean.TRUE;
	}

	private AbstractSysSyncConfigDto getConfig() {
		AbstractSysSyncConfigDto config = service.get(synchronizationId);
		//
		if (config == null) {
			throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_NOT_FOUND,
					ImmutableMap.of("id", synchronizationId));
		}
		return config;
	}

	@Override
	public List<String> getPropertyNames() {
		List<String> params = super.getPropertyNames();
		params.add(SynchronizationService.PARAMETER_SYNCHRONIZATION_ID);
		return params;
	}

	@Override
	public String getDescription() {
		String deafultDescription =  "Synchronization long running task";
		if (synchronizationId == null) {
			return deafultDescription;
		}
		AbstractSysSyncConfigDto config = service.get(synchronizationId);
		if (config == null) {
			return deafultDescription;
		}
		return MessageFormat.format("Run synchronization name: [{0}] - system mapping id: [{1}]", config.getName(),
				config.getSystemMapping());
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> props = super.getProperties();
		props.put(SynchronizationService.PARAMETER_SYNCHRONIZATION_ID, synchronizationId);
		//
		return props;
	}

}
