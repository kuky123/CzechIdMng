package eu.bcvsolutions.idm.core.api.service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import eu.bcvsolutions.idm.core.api.config.domain.EventConfiguration;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.EntityEventProcessorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityEventDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.EntityEventProcessorFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.exception.EventContentDeletedException;

/**
 * Entity processing based on synchronous {@link ApplicationEvent} publishing.
 * {@link AbstractEntity} type or {@link AbstractDto} can be used as change owner.
 * If {@link AbstractDto} is given as owner type, then {@link AbstractEntity} owner will be found by 
 * {@link LookupService} => transformation to {@link AbstractEntity}. 
 * {@link AbstractDto} or {@link AbstractEntity} descendants are supported => {@link UUID} identifier is needed.
 * 
 * @see IdmEntityEventDto
 * @see AbstractDto
 * @see AbstractEntity
 * @see LookupService
 * @see EntityEventProcessor
 * @see EntityEvent
 * @see EventContext
 * 
 * @author Radek Tomiška
 */
public interface EntityEventManager {
	
	String EVENT_PROPERTY_SKIP_NOTIFY = "idm:skip-notify";
	String EVENT_PROPERTY_SKIP_NOTIFICATION = "idm:skip-notification"; // skip sending notifications
	
	/**
	 * Cancel all previously ran events
	 */
	void init();
	
	/**
	 * Process event through all registered entity processor in configured order with default context (newly created context). 
	 * Suspended event will continue.
	 * 
	 * @param event
	 * @return
	 */
	<E extends Serializable> EventContext<E> process(EntityEvent<E> event);
	
	/**
	 * Process event through all registered entity processor in configured order with default context (newly created context). 
	 * Suspended event will continue.
	 * 
	 * @param event
	 * @param parentEvent event is based on parent event
	 * @return
	 */
	<E extends Serializable> EventContext<E> process(EntityEvent<E> event, EntityEvent<?> parentEvent);
	
	/**
	 * Returns all registered entity event processors
	 * 
	 * @param filter
	 * @return
	 */
	List<EntityEventProcessorDto> find(EntityEventProcessorFilter filter);
	
	/**
	 * Get registered event processor by id
	 * 
	 * @param processorId
	 * @return
	 */
	EntityEventProcessorDto get(String processorId);
	
	/**
	 * Get registered event processor by id
	 * 
	 * @param processorId
	 * @return
	 */
	EntityEventProcessor<?> getProcessor(String processorId);
	
	/**
	 * Constructs new entity event.
	 * 
	 * @param identifiable
	 * @param originalEvent parent event
	 * @return
	 */
	IdmEntityEventDto prepareEvent(Identifiable identifiable, EntityEvent<? extends Identifiable> originalEvent);
	
	/**
	 * Publish common event to all listeners
	 * 
	 * @param event
	 */
	void publishEvent(Object event);
	
	/**
	 * Publish entity changed event.
	 * 
	 * @param abstract dto / entity
	 * @since 8.0.0
	 */
	<E extends Identifiable> void changedEntity(E owner);
	
	/**
	 * Publish entity changed event.
	 * 
	 * 
	 * @param owner can be different than original event content (e.g. original event is for contract, but change is registered for identity)
	 * @param originalEvent original event
	 * @since 8.0.0
	 */
	<E extends Identifiable> void changedEntity(E owner, EntityEvent<? extends Identifiable> originalEvent);
	
	/**
	 * Publish entity changed event.
	 * 
	 * @param ownerType
	 * @param ownerId
	 * @since 8.0.0
	 */
	void changedEntity(Class<? extends Identifiable> ownerType, UUID ownerId);
	
	/**
	 * Publish entity changed event.
	 * 
	 * 
	 * @param ownerType can be different than original event content type (e.g. original event is for contract, but change is on identity)
	 * @param ownerId
	 * @param originalEvent original event
	 * @since 8.0.0
	 */
	void changedEntity(
				Class<? extends Identifiable> ownerType, 
				UUID ownerId, 
				EntityEvent<? extends Identifiable> originalEvent);
	
	/**
	 * Owner type has to be entity class - dto instance can be given.
	 * 
	 * @param owner
	 * @return
	 * @since 9.0.0
	 */
	String getOwnerType(Identifiable owner);
	
	/**
	 * Owner type has to be entity class - dto class can be given.
	 * 
	 * @param ownerType
	 * @return
	 * @since 8.0.0
	 */
	String getOwnerType(Class<? extends Identifiable> ownerType);
	
	/**
	 * Returns given event owner instance.
	 * 
	 * @param change
	 * @return
	 * @since 8.0.0
	 */
	AbstractDto findOwner(IdmEntityEventDto event);
	
	/**
	 * Returns given owner instance by event owner type and identifier.
	 * 
	 * @param ownerType
	 * @param ownerId
	 * @return
	 */
	AbstractDto findOwner(String ownerType, Serializable ownerId);
	
	/**
	 * Returns entity event, if given event is based on persisted entity event, otherwise returns {@code null}. 
	 * 
	 * @param event
	 * @return
	 * @since 8.0.0
	 */
	IdmEntityEventDto getEvent(EntityEvent<? extends Serializable> event);
	
	/**
	 * Returns persisted event with given id, when event with given id is not persisted, then returns {@code null}.
	 * 
	 * @param event
	 * @return
	 * @since 9.1.0
	 */
	IdmEntityEventDto getEvent(UUID eventId);
	
	/**
	 * Returns entity event, if given event is based on persisted entity event, otherwise returns {@code null}. 
	 * 
	 * @param event
	 * @return
	 * @since 8.0.0
	 */
	UUID getEventId(EntityEvent<? extends Serializable> event);
	
	/**
	 * Returns true, when persisted event is runnable (created or running already). 
	 * 
	 * @see OperationState#isRunnable()
	 * @param eventId
	 * @return
	 * @since 9.1.0
	 */
	boolean isRunnable(UUID eventId);
	
	/**
	 * Resurrects given event from persisted state. Event can be processed.
	 * 
	 * Look out: content of resurrected event could be deleted - original content was deleted and original source was not saved, 
	 * when event was persisted. In this case {@link EventContentDeletedException} is thrown.
	 * 
	 * @param entityEvent
	 * @return
	 * @throws EventContentDeletedException if event content was deleted already - event cannot be constructed again.
	 * @since 8.0.0
	 */
	EntityEvent<? extends Identifiable> toEvent(IdmEntityEventDto entityEvent);
	
	/**
	 * Execute given persisted event. Event will be executed by event priority:
	 * - immediate - synchronously
	 * - high / normal - asynchronously in new thread
	 * 
	 * @param event
	 * @return
	 * @since 8.0.0
	 */
	void executeEvent(IdmEntityEventDto event);
	
	/**
	 * Persists event "as it is". Event id is set (if it was empty).
	 * 
	 * @param event
	 * @param result event result [optional]
	 * @since 9.1.0
	 */
	IdmEntityEventDto saveEvent(EntityEvent<? extends Identifiable> event, OperationResultDto result);
	
	/**
	 * Persists event
	 * 
	 * @param event
	 * @param result
	 * @since 8.0.0
	 */
	IdmEntityEventDto saveEvent(IdmEntityEventDto event);
	
	/**
	 * Saves event result
	 * 
	 * @param event
	 * @param result
	 * @since 8.0.0
	 */
	IdmEntityEventDto saveResult(UUID eventId, OperationResultDto result);
	
	/**
	 * Saves event states.
	 * 
	 * @param event
	 * @param previousStates
	 * @param result
	 * @return
	 */
	List<IdmEntityStateDto> saveStates(
			EntityEvent<?> event, 
			List<IdmEntityStateDto> previousStates,
			EventResult<?> result);
	
	/**
	 * Enable given processor. Throws {@link IllegalArgumentException} when processorId is not installed.
	 * 
	 * @param processorId
	 */
	void enable(String processorId);

	/**
	 * Disable given module. Throws {@link IllegalArgumentException} when processorId is not installed.
	 * 
	 * @param processorId
	 */
	void disable(String processorId);

	/**
	 * Enable / disable given processor.
	 *
	 * @param processorId
	 * @param enabled
	 */
	void setEnabled(String processorId, boolean enabled);
	
	/**
	 * Returns true, if asynchronous event processing is enabled
	 * 
	 * @see EventConfiguration
	 * @return
	 * @since 9.1.0
	 */
	boolean isAsynchronous();
}
