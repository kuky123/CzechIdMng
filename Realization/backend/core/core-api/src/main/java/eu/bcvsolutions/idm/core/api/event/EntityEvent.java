package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.Configurable;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Event state holder (content + metadata)
 * 
 * @param <E> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 */
public interface EntityEvent<E extends Serializable> extends ResolvableTypeProvider, Serializable {
	
	String EVENT_PROPERTY = "entityEvent";
	String EVENT_PROPERTY_EVENT_ID = "idm:event-id"; // persisted event id
	String EVENT_PROPERTY_EXECUTE_DATE = "idm:execute-date"; // asynchronous event processing time
	String EVENT_PROPERTY_PRIORITY = "idm:priority"; // event priority
	String EVENT_PROPERTY_ROOT_EVENT_ID = "idm:root-event-id"; // root event id
	String EVENT_PROPERTY_PARENT_EVENT_ID = "idm:parent-event-id"; // parent event id
	String EVENT_PROPERTY_PARENT_EVENT_TYPE = "idm:parent-event-type"; // parent event type
	String EVENT_PROPERTY_SUPER_OWNER_ID = "idm:super-owner-id"; // entity event super owner id (e.g. identity (~super owner) - identityRole (event owner))
	String EVENT_PROPERTY_PERMISSION = "idm:permission"; // permission to evaluate (AND)
	
	/**
	 * Operation type
	 * 
	 * @return
	 */
	EventType getType();
	
	/**
	 * Persistent event id. Can be {@code null} if event is not persisted.
	 * 
	 * @return
	 */
	UUID getId();
	
	/**
	 * Persistent event id.
	 * 
	 * @param id
	 */
	void setId(UUID id);
	
	/**
	 * Content's super owner identifier - e.g. event is for identity role, but we want to work with identity as super owner (~batch). 
	 * 
	 * @return
	 */
	UUID getSuperOwnerId();
	
	/**
	 * Content's super owner identifier
	 * 
	 * @param superOwnerId
	 */
	void setSuperOwnerId(UUID superOwnerId);
	
	/**
	 * Persistent root event id. Can be {@code null} if event is root, or all events are not persisted.
	 * If root event is set, then event will be persisted.
	 * 
	 * @return
	 */
	UUID getRootId();
	
	/**
	 * Persistent root event id.
	 * 
	 * @param rootId
	 */
	void setRootId(UUID rootId);
	
	/**
	 * Persistent parent event id. Can be {@code null} if event doesn't have parent, or all events are not persisted.
	 * If parent event is set, then event will be persisted.
	 * 
	 * @return
	 */
	UUID getParentId();
	
	/**
	 * Persistent parent event id.
	 * 
	 * @param parentId
	 */
	void setParentId(UUID parentId);
	
	/**
	 * Parent event type.
	 * 
	 * @return
	 */
	String getParentType();
	
	/**
	 * Parent event type.
	 * 
	 * @param parentType
	 */
	void setParentType(String parentType);
	
	/**
	 * Event execute date. Can be {@code null}, if event is not processed completely.
	 * 
	 * @return
	 */
	DateTime getExecuteDate();
	
	/**
	 * Event execute date.
	 * 
	 * @param executeDate
	 */
	void setExecuteDate(DateTime executeDate);
	
	/**
	 * Event priority. 
	 * 
	 * @return
	 */
	PriorityType getPriority();
	
	/**
	 * Event priority.
	 * 
	 * @param priority
	 */
	void setPriority(PriorityType priority);

	/**
	 * Starting event content =~ source entity. Could not be null. Events with empty content could not be processed.
	 *  
	 * @return
	 */
	E getSource();
	
	/**
	 * Event content - entity. Could not be null. Events with empty content could not be processed.
	 *  
	 * @return
	 */
	E getContent();
	
	/**
	 *  Event content - entity. Could not be null. Events with empty content could not be processed.
	 *  
	 * @param content
	 */
	void setContent(E content);
	
	/**
	 * Persisted event content before event starts. Usable in "check modifications" processors.
	 * 
	 * @return
	 */
	E getOriginalSource();
	
	/**
	 * Persisted event content before event starts. Usable in "check modifications" processors.
	 * 
	 * @param originalSource
	 */
	void setOriginalSource(E originalSource);
	
	/**
	 * Event properties (metadata)
	 * 
	 * TODO: ConfigurationMap should be used ... see {@link Configurable}.
	 * 
	 * @return
	 */
	Map<String, Serializable> getProperties();
	
	/**
	 * Event context
	 * 
	 * @return
	 */
	EventContext<E> getContext();
	
	/**
	 * Event is closed = no other events will be processed (break event chain)
	 * 
	 * @return
	 */
	boolean isClosed();
	
	/**
	 * Event is suspended = no other events will be processed, while event is suspended. 
	 * Suspended event could be republished again - when will continue when event was suspended - all processors 
	 * with greater order than getProcessedOrder will be called.
	 * 
	 * @return
	 */
	boolean isSuspended();
	
	/**
	 * Returns last processed order or {@code null}, if any processor was called (event is starting).
	 * 
	 * @return
	 */
	Integer getProcessedOrder();
	
	
	/**
	 * Returns true, if event's type equals given eventType.
	 * 
	 * @param event
	 * @param eventType
	 * @return
	 */
	default boolean hasType(EventType eventType) {
		Assert.notNull(eventType);
		//
		return eventType.name().equals(getType().name());
	}

	/**
	 * Event class type. If is not field 'eventClassType' sets (in constructor), then will be used class from content.
	 * Processors with this generic class will be called.
	 * @return
	 */
	Class<? extends E> getEventClassType();
	
	/**
	 * Permissions set to evaluate with this event (AND).
	 * Look out: permissions are not persisted (persistent events are executed under system)
	 * 
	 * @return
	 */
	BasePermission[] getPermission();
	
	/**
	 * Permissions set to evaluate with this event (AND).
	 * Look out: permissions are not persisted (persistent events are executed under system)
	 * 
	 * @param permission
	 */
	void setPermission(BasePermission... permission);
}
