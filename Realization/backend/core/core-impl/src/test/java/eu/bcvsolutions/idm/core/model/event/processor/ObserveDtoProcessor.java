package eu.bcvsolutions.idm.core.model.event.processor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.model.event.RoleRequestEvent.RoleRequestEventType;

/**
 * Listen execute event on registered dtos
 * 
 * @author Radek Tomiška
 *
 */
@Component
public class ObserveDtoProcessor extends CoreEventProcessor<BaseDto> {

	public static final String PROPERTY_OBSERVED = "observed";
	
	// observed requests
	private static Map<UUID, CountDownLatch> listenContents = new ConcurrentHashMap<>();
	
	public ObserveDtoProcessor() {
		super(RoleRequestEventType.NOTIFY);
	}
		
	@Override
	public EventResult<BaseDto> process(EntityEvent<BaseDto> event) {
		listenContents.get(event.getContent().getId()).countDown();
		//
		event.getProperties().put(PROPERTY_OBSERVED, Boolean.TRUE);
		//
		return null;
	}
	
	@Override
	public boolean supports(EntityEvent<?> event) {
		if (!super.supports(event)) {
			return false;
		}
		return listenContents.containsKey(((BaseDto) event.getContent()).getId());
	}
	
	public static void listenContent(UUID contentId) {
		listenContents.put(contentId, new CountDownLatch(1));
	}
	
	/**
	 * Wait for task ends
	 * 
	 * @throws InterruptedException
	 */
	public static void waitForEnd(UUID contentId) throws InterruptedException {
		listenContents.get(contentId).await();
	}

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}
}
