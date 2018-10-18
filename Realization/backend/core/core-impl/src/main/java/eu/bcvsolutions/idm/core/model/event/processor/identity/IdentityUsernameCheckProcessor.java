package eu.bcvsolutions.idm.core.model.event.processor.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.processor.IdentityProcessor;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent.IdentityEventType;

/**
 * Check identity username for duplicites before identity was created.
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Component(IdentityUsernameCheckProcessor.PROCESSOR_NAME)
@Description("Check identity username for duplicites before identity was created.")
public class IdentityUsernameCheckProcessor
		extends CoreEventProcessor<IdmIdentityDto> 
		implements IdentityProcessor {

	public static final String PROCESSOR_NAME = "identity-username-check-processor";

	private final IdmIdentityService service;
	
	@Autowired
	public IdentityUsernameCheckProcessor(
			IdmIdentityService service) {
		super(IdentityEventType.CREATE);
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
	public EventResult<IdmIdentityDto> process(EntityEvent<IdmIdentityDto> event) {
		IdmIdentityDto identity = event.getContent();
		//
		String username = identity.getUsername();
		IdmIdentityDto duplicity = service.getByUsername(username);
		if (duplicity != null) {
			throw new ResultCodeException(CoreResultCode.IDENTITY_USERNAME_EXIST,
					ImmutableMap.of(
							"username", username));
		}
		//
		return new DefaultEventResult<>(event, this);
	}

	@Override
	public int getOrder() {
		// before save
		return -10;
	}
}