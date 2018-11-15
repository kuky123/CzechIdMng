package eu.bcvsolutions.idm.acc.event;

import java.io.Serializable;
import java.util.Map;

import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.EventType;

/**
 * Events for system attribute controlled values
 * 
 * @author Svanda
 *
 */
public class AttributeControlledValueEvent extends CoreEvent<AccAccountDto> {

	private static final long serialVersionUID = 1L;

	public enum AttributeControlledValueEventType implements EventType {
		CREATE, UPDATE, DELETE;
	}

	public AttributeControlledValueEvent(AttributeControlledValueEventType operation, AccAccountDto content) {
		super(operation, content);
	}

	public AttributeControlledValueEvent(AttributeControlledValueEventType operation, AccAccountDto content,
			Map<String, Serializable> properties) {
		super(operation, content, properties);
	}

}