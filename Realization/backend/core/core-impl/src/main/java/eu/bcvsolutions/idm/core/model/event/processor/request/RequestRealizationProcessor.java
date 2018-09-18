package eu.bcvsolutions.idm.core.model.event.processor.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.AbstractRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.model.event.RequestEvent.RequestEventType;

/**
 * Processor realizes of the request
 * 
 * @author svandav
 *
 */
@Component(RequestRealizationProcessor.PROCESSOR_NAME)
@Description("Processor realizes of the request")
public class RequestRealizationProcessor extends CoreEventProcessor<IdmRequestDto> {

	public static final String PROCESSOR_NAME = "request-realization-processor";
	
	@Autowired
	private RequestManager manager;

	public RequestRealizationProcessor() {
		super(RequestEventType.EXECUTE);
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmRequestDto> process(EntityEvent<IdmRequestDto> event) {
		AbstractRequestDto dto = event.getContent();
		event.setContent(manager.executeRequest(dto.getId()));

		return new DefaultEventResult<>(event, this);
	}
}
