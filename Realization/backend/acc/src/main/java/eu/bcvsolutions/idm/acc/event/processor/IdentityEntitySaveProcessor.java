package eu.bcvsolutions.idm.acc.event.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.event.ProvisioningEvent;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Executes provisioing after identity entity is saved
 * 
 * @author Radek Tomiška
 * @deprecated Will be removed after EAV will be refactored to dto
 */
@Deprecated
@Component("accIdentityEntitySaveProcessor")
@Enabled(AccModuleDescriptor.MODULE_ID)
@Description("Executes provisioing after identity entity is saved (deprecated).")
public class IdentityEntitySaveProcessor extends AbstractEntityEventProcessor<IdmIdentity> {

	public static final String PROCESSOR_NAME = "identity-save-processor";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdentityEntitySaveProcessor.class);
	private ProvisioningService provisioningService;
	private final ApplicationContext applicationContext;
	
	@Autowired
	public IdentityEntitySaveProcessor(ApplicationContext applicationContext) {
		super(CoreEventType.EAV_SAVE);
		//
		Assert.notNull(applicationContext);
		//
		this.applicationContext = applicationContext;
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmIdentity> process(EntityEvent<IdmIdentity> event) {
		doProvisioning(event.getContent());
		return new DefaultEventResult<>(event, this);
	}
	
	private void doProvisioning(IdmIdentity identity) {
		LOG.debug("Call provisioning for identity [{}]", identity.getUsername());
		getProvisioningService().doProvisioning(identity);
	}
	
	@Override
	public int getOrder() {
		return ProvisioningEvent.DEFAULT_PROVISIONING_ORDER;
	}
	
	/**
	 * provisioningService has dependency everywhere - so we need lazy init ...
	 * 
	 * @return
	 */
	private ProvisioningService getProvisioningService() {
		if (provisioningService == null) {
			provisioningService = applicationContext.getBean(ProvisioningService.class);
		}
		return provisioningService;
	}
	
}