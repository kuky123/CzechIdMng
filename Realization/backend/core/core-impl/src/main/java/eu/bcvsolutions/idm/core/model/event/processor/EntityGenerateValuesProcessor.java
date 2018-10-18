package eu.bcvsolutions.idm.core.model.event.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.ValueGeneratorManager;

/**
 * Processor use {@link ValueGeneratorManager} for generating values
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Component(EntityGenerateValuesProcessor.PROCESSOR_NAME)
@Description("Generate values for entity, that supports generating.")
public class EntityGenerateValuesProcessor extends CoreEventProcessor<AbstractDto> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EntityGenerateValuesProcessor.class);
	public static final String PROCESSOR_NAME = "core-entity-generate-values-processor";
	//
	@Autowired private ValueGeneratorManager valueGeneratorManager;
	
	public EntityGenerateValuesProcessor() {
		super(CoreEventType.CREATE);
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	@Override
	public EventResult<AbstractDto> process(EntityEvent<AbstractDto> event) {
		AbstractDto entityDto = event.getContent();
		if (valueGeneratorManager.supportsGenerating(entityDto)) {
			LOG.info("Start generating for entity id [{}] and class [{}].", entityDto.getId(), entityDto.getClass().getCanonicalName());
			entityDto = valueGeneratorManager.generate(entityDto);
			event.setContent(entityDto);
		}
		return new DefaultEventResult<>(event, this);
	}

	@Override
	public int getOrder() {
		// before save
		return - 100;
	}
}
