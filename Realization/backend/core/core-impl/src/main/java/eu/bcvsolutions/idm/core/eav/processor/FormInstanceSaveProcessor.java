package eu.bcvsolutions.idm.core.eav.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;

/**
 * Persists form instance (eav attributes).
 * 
 * @author Radek Tomiška
 * @since 9.2.0
 */
@Component(FormInstanceSaveProcessor.PROCESSOR_NAME)
@Description("Persists entity event.")
public class FormInstanceSaveProcessor extends CoreEventProcessor<IdmFormInstanceDto> {
	
	public static final String PROCESSOR_NAME = "core-form-instance-save-processor";
	//
	@Autowired private FormService formService;
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private LookupService lookupService;
	
	public FormInstanceSaveProcessor() {
		super(CoreEventType.UPDATE); // eavs are updated for CUD
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmFormInstanceDto> process(EntityEvent<IdmFormInstanceDto> event) {
		IdmFormInstanceDto formInstance = formService.saveFormInstance(event);
		event.setContent(formInstance);
		//
		// "EAV_SAVE" is the synonym for the "NOTIFY" event, but is too late to remove it ...
		if(!getBooleanProperty(EntityEventManager.EVENT_PROPERTY_SKIP_NOTIFY, event.getProperties())) {
			entityEventManager.process(new CoreEvent<>(CoreEventType.EAV_SAVE, lookupService.lookupDto(formInstance.getOwnerType(), formInstance.getOwnerId())));
		}
		//
		return new DefaultEventResult<>(event, this);
	}
}
