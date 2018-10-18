package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.Configurable;

/**
 * Single entity event processor
 * <p>
 * Its better to use {@link Ordered} interface instead {@link Order} annotation - does not work with aspects.
 * Look out processors with the same order is sorted randomly (respectively in the same order, 
 * when beans were registered into spring context and this cannot be determined, when event is processed / published).
 * 
 * @param <T> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 * @see {@link ApplicationListener}
 * @see {@link Ordered}
 */
public interface EntityEventProcessor<T extends Serializable> extends Ordered, Configurable {
	
	String CONFIGURABLE_TYPE = "processor";
	
	/**
	 * Event types from configuration
	 */
	String PROPERTY_EVENT_TYPES = "eventTypes";
	
	/**
	 *  bean name / unique identifier (spring bean name)
	 *  
	 * @return
	 */
	String getId();
	
	/**
	 * Configurable type identifier {@value #CONFIGURABLE_TYPE}
	 * 
	 * @return
	 */
	@Override
	default String getConfigurableType() {
		return CONFIGURABLE_TYPE;
	}
	
	/**
	 * Returns entity class, which supports this processor
	 * 
	 * @return
	 */
	Class<T> getEntityClass();
	
	/**
	 * Returns event types, which supports this processor
	 * 
	 * @return
	 */
	String[] getEventTypes();
	
	/**
	 * Returns true, when processor supports given event
	 * 
	 * @param event
	 * @return
	 */
	boolean supports(EntityEvent<?> event);
	
	/**
	 * Execute processor conditionally.
	 * 
	 * @param event
	 * @return true, when processor can process given event.
	 * @since 7.7.0
	 */
	boolean conditional(EntityEvent<T> event);
	
	/**
	 * Process entity event without context.
	 * 
	 * @param event
	 * @return
	 */
	EventResult<T> process(EntityEvent<T> event);
	
	/**
	 * Returns true, when processor could close event (only documentation purpose now)
	 * 
	 * @return
	 */
	boolean isClosable();
}
