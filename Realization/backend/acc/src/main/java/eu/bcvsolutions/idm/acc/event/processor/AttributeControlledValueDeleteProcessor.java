package eu.bcvsolutions.idm.acc.event.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.dto.SysAttributeControlledValueDto;
import eu.bcvsolutions.idm.acc.event.AttributeControlledValueEvent.AttributeControlledValueEventType;
import eu.bcvsolutions.idm.acc.service.api.SysAttributeControlledValueService;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Delete attribute controlled value processor
 * 
 * @author Svanda
 */
@Component("sysAttributeControlledValueDeleteProcessor")
@Description("Ensures referential integrity. Cannot be disabled.")
public class AttributeControlledValueDeleteProcessor extends CoreEventProcessor<SysAttributeControlledValueDto> {

	private static final String PROCESSOR_NAME = "attribute-controlled-value-delete-processor";
	private final SysAttributeControlledValueService service;

	@Autowired
	public AttributeControlledValueDeleteProcessor(SysAttributeControlledValueService service) {
		super(AttributeControlledValueEventType.DELETE);
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
	public EventResult<SysAttributeControlledValueDto> process(EntityEvent<SysAttributeControlledValueDto> event) {
		SysAttributeControlledValueDto entity = event.getContent();

		service.deleteInternal(entity);

		return new DefaultEventResult<>(event, this);
	}

	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER;
	}

	@Override
	public boolean isDisableable() {
		return false;
	}

}
