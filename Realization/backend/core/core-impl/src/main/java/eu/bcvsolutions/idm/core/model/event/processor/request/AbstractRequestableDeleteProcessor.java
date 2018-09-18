package eu.bcvsolutions.idm.core.model.event.processor.request;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.model.event.RequestEvent.RequestEventType;

/**
 * Invoked on delete {@link Requestable} DTO
 * 
 * @author svandav
 *
 */
public abstract class AbstractRequestableDeleteProcessor<E extends Requestable>
		extends AbstractEntityEventProcessor<E> {

	@Autowired
	private RequestManager requestManager;

	public AbstractRequestableDeleteProcessor() {
		super(RequestEventType.DELETE);
	}

	@Override
	public EventResult<E> process(EntityEvent<E> event) {
		E requestable = event.getContent();
		DefaultEventResult<E> result = new DefaultEventResult<>(event, this);

		if (requestable == null || requestable.getId() == null) {
			return result;
		}
		// Cancel requests and request items using that deleting DTO
		requestManager.onDeleteRequestable(requestable);

		return result;
	}

	/**
	 * Before standard delete
	 * 
	 * @return
	 */
	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER - 10;
	}

}
