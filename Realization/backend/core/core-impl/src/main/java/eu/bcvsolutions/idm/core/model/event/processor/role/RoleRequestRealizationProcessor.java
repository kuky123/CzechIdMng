package eu.bcvsolutions.idm.core.model.event.processor.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent.RoleRequestEventType;

/**
 * Realization request for change permissions processor
 * 
 * @author svandav
 *
 */
@Component
@Description("Realization request for change permissions")
public class RoleRequestRealizationProcessor extends CoreEventProcessor<IdmRoleRequestDto> {

	public static final String PROCESSOR_NAME = "role-request-realization-processor";

	private final IdmRoleRequestService service;

	@Autowired
	public RoleRequestRealizationProcessor(IdmRoleRequestService service) {
		super(RoleRequestEventType.EXCECUTE);
		//
		Assert.notNull(service);
		//
		this.service = service;
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmRoleRequestDto> process(EntityEvent<IdmRoleRequestDto> event) {
		event.setContent(service.executeRequest(event));
		//
		return new DefaultEventResult<>(event, this);
	}
}
