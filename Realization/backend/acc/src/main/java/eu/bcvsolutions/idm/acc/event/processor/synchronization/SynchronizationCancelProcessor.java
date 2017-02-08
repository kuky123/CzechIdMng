package eu.bcvsolutions.idm.acc.event.processor.synchronization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.domain.SynchronizationEventType;
import eu.bcvsolutions.idm.acc.entity.SysSyncConfig;
import eu.bcvsolutions.idm.acc.event.ProvisioningEvent;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Synchronization cancel event processor
 * @author svandav
 *
 */
@Component
@Description("Cancels synchronization process by given configuration")
public class SynchronizationCancelProcessor extends AbstractEntityEventProcessor<SysSyncConfig> {

	public static final String PROCESSOR_NAME = "synchronization-cancel-processor";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SynchronizationCancelProcessor.class);
	private final SynchronizationService synchronizationService;
	
	@Autowired
	public SynchronizationCancelProcessor(
			SynchronizationService synchronizationService) {
		super(SynchronizationEventType.CANCEL);
		//
		Assert.notNull(synchronizationService);
		//
		this.synchronizationService = synchronizationService;
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	@Override
	public EventResult<SysSyncConfig> process(EntityEvent<SysSyncConfig> event) {
		LOG.info("Synchronization event cancel");
		SysSyncConfig config = event.getContent();
		synchronizationService.stopSynchronization(config);
		return new DefaultEventResult<>(event, this, false);
	}
	
	@Override
	public boolean isClosable() {
		return true;
	}

	@Override
	public int getOrder() {
		return ProvisioningEvent.DEFAULT_PROVISIONING_ORDER;
	}
}