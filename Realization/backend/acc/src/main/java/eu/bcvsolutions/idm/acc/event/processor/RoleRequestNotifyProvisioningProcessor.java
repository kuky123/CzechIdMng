package eu.bcvsolutions.idm.acc.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.event.ProvisioningEvent;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest_;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent.RoleRequestEventType;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Identity role account management after role request notify
 * 
 * @author Radek Tomiška
 *
 */
@Component(RoleRequestNotifyProvisioningProcessor.PROCESSOR_NAME)
@Enabled(AccModuleDescriptor.MODULE_ID)
@Description("Executes account management and provisioning after role request is executed.")
public class RoleRequestNotifyProvisioningProcessor extends AbstractEntityEventProcessor<IdmRoleRequestDto> {

	public static final String PROCESSOR_NAME = "acc-role-request-notify-provisioning-processor";
	private static final Logger LOG = LoggerFactory.getLogger(RoleRequestNotifyProvisioningProcessor.class);
	//
	@Autowired private ProvisioningService provisioningService;
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private LookupService lookupService;

	public RoleRequestNotifyProvisioningProcessor() {
		super(RoleRequestEventType.NOTIFY);
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	@Override
	public boolean conditional(EntityEvent<IdmRoleRequestDto> event) {
		return super.conditional(event)
				&& (event.getRootId() == null || event.getRootId().equals(event.getParentId())); 
	}
 
	@Override
	public EventResult<IdmRoleRequestDto> process(EntityEvent<IdmRoleRequestDto> event) {
		IdmRoleRequestDto request = event.getContent();		
		IdmIdentityDto identity = DtoUtils.getEmbedded(request, IdmRoleRequest_.applicant, (IdmIdentityDto) null);
		if (identity == null) {
			identity = (IdmIdentityDto) lookupService.lookupDto(IdmIdentityDto.class, request.getApplicant());
		}
		//
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