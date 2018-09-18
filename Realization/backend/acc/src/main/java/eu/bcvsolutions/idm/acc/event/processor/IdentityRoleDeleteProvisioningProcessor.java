package eu.bcvsolutions.idm.acc.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.event.ProvisioningEvent;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent.IdentityRoleEventType;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Identity provisioning after role has been deleted.
 *
 * @author Jan Helbich
 * @author Radek Tomiška
 */
@Component(IdentityRoleDeleteProvisioningProcessor.PROCESSOR_NAME)
@Enabled(AccModuleDescriptor.MODULE_ID)
@Description("Executes provisioning after identity role is deleted.")
public class IdentityRoleDeleteProvisioningProcessor extends AbstractEntityEventProcessor<IdmIdentityRoleDto> {

	public static final String PROCESSOR_NAME = "identity-role-delete-provisioning-processor";
	private static final Logger LOG = LoggerFactory.getLogger(IdentityRoleDeleteProvisioningProcessor.class);
	//
	@Autowired private ProvisioningService provisioningService;
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private EntityEventManager entityEventManager;

	public IdentityRoleDeleteProvisioningProcessor() {
		super(IdentityRoleEventType.DELETE);
	}
	
	/**
	 *  Account management should be executed from parent event - request. 
	 *  Look out, request event is already closed, when asynchronous processing is disabled.
	 */
	@Override
	public boolean conditional(EntityEvent<IdmIdentityRoleDto> event) {
		return super.conditional(event)
				&& (event.getRootId() == null || !entityEventManager.isRunnable(event.getRootId())) ;
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmIdentityRoleDto> process(EntityEvent<IdmIdentityRoleDto> event) {
		// TODO: contract is loaded to early ... 
		IdmIdentityRoleDto identityRole = event.getContent();
		IdmIdentityContractDto identityContract = identityContractService.get(identityRole.getIdentityContract());
		IdmIdentityDto identity = DtoUtils.getEmbedded(identityContract, IdmIdentityContract_.identity);
		
		LOG.debug("Call provisioning for identity [{}]", identity.getUsername());
		
		provisioningService.doProvisioning(identity);
		
		return new DefaultEventResult<>(event, this);
	}

	@Override
	public int getOrder() {
		return ProvisioningEvent.DEFAULT_PROVISIONING_ORDER;
	}
}