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
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Identity role account management after save
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Enabled(AccModuleDescriptor.MODULE_ID)
@Description("Executes account management and provisioning after identity role is saved.")
public class IdentityRoleSaveProvisioningProcessor extends AbstractEntityEventProcessor<IdmIdentityRoleDto> {

	public static final String PROCESSOR_NAME = "identity-role-save-provisioning-processor";
	private static final Logger LOG = LoggerFactory.getLogger(IdentityRoleSaveProvisioningProcessor.class);
	//
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private ProvisioningService provisioningService;
	@Autowired private EntityEventManager entityEventManager;

	public IdentityRoleSaveProvisioningProcessor() {
		super(CoreEventType.NOTIFY);
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
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
	public EventResult<IdmIdentityRoleDto> process(EntityEvent<IdmIdentityRoleDto> event) {
		IdmIdentityRoleDto identityRole = event.getContent();
		IdmIdentityContractDto identityContract = identityContractService.get(identityRole.getIdentityContract());
		IdmIdentityDto identity = DtoUtils.getEmbedded(identityContract, IdmIdentityContract_.identity);
		//
		// TODO: full account management should be moved into NOTIFY on identity => super owner id can be removed then in IdentityRolePublishChangeProcessor
		// all identity roles are processed now => doesn't support concurrency - duplicate accounts can be created now (ux constraint ex. is thrown)
		LOG.debug("Call account management for identity [{}]", identity.getUsername());
		provisioningService.accountManagement(identity);
		LOG.debug("Register change for identity [{}]", identity.getUsername());
		entityEventManager.changedEntity(identity, event);
		//
		return new DefaultEventResult<>(event, this);
	}
	
	@Override
	public int getOrder() {
		return ProvisioningEvent.DEFAULT_PROVISIONING_ORDER;
	}
}