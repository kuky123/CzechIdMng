package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.config.domain.EventConfiguration;
import eu.bcvsolutions.idm.core.api.domain.Auditable;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.domain.comparator.CreatedComparator;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.EntityEventProcessorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityEventDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.filter.EntityEventProcessorFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityEventFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityStateFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.event.AsyncEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.DefaultEventContext;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EmptyEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EntityEventEvent.EntityEventType;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.EventType;
import eu.bcvsolutions.idm.core.api.exception.EventContentDeletedException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.EntityStateManager;
import eu.bcvsolutions.idm.core.api.service.IdmEntityEventService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.scheduler.api.config.SchedulerConfiguration;
import eu.bcvsolutions.idm.core.security.api.service.EnabledEvaluator;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Entity (dto) processing based on event publishing.
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultEntityEventManager implements EntityEventManager {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultEntityEventManager.class);
	private static final ConcurrentHashMap<UUID, UUID> runningOwnerEvents = new ConcurrentHashMap<>();
	//
	private final ApplicationContext context;
	private final ApplicationEventPublisher publisher;
	private final EnabledEvaluator enabledEvaluator;
	private final LookupService lookupService;
	//
	@Autowired private IdmEntityEventService entityEventService;
	@Autowired private EntityStateManager entityStateManager;
	@Autowired private ConfigurationService configurationService;
	@Autowired private SecurityService securityService;
	@Autowired private EventConfiguration eventConfiguration;

	@Autowired
	public DefaultEntityEventManager(
			ApplicationContext context, 	
			ApplicationEventPublisher publisher,
			EnabledEvaluator enabledEvaluator,
			LookupService lookupService) {
		Assert.notNull(context, "Spring context is required");
		Assert.notNull(publisher, "Event publisher is required");
		Assert.notNull(enabledEvaluator, "Enabled evaluator is required");
		Assert.notNull(lookupService, "LookupService is required");
		//
		this.context = context;
		this.publisher = publisher;
		this.enabledEvaluator = enabledEvaluator;
		this.lookupService = lookupService;
	}
	
	/**
	 * Cancel all previously ran events
	 */
	@Override
	public void init() {
		LOG.info("Cancel unprocessed events - event was interrupt during instance restart");
		//
		String instanceId = configurationService.getInstanceId();
		entityEventService.findByState(instanceId, OperationState.RUNNING).forEach(event -> {
			LOG.info("Cancel unprocessed event [{}] - event was interrupt during instance [{}] restart", event.getId(), instanceId);
			//
			// cancel event
			ResultModel resultModel = new DefaultResultModel(
					CoreResultCode.EVENT_CANCELED_BY_RESTART, 
					ImmutableMap.of(
							"eventId", event.getId(), 
							"eventType", event.getEventType(),
							"ownerId", String.valueOf(event.getOwnerId()),
							"instanceId", event.getInstanceId()));		
			OperationResultDto result = new OperationResultDto.Builder(OperationState.CANCELED).setModel(resultModel).build();
			event.setResult(result);
			entityEventService.saveInternal(event);
			//
			// cancel event states		
			IdmEntityStateFilter filter = new IdmEntityStateFilter();
			filter.setEventId(event.getId());
			List<IdmEntityStateDto> states = entityStateManager.findStates(filter, null).getContent();
			states
				.stream()
				.filter(state -> {
					return OperationState.RUNNING == state.getResult().getState();
				})
				.forEach(state -> {		
					state.setResult(result);
					entityStateManager.saveState(null, state);
				});
		});
	}
	
	@Override
	public <E extends Serializable> EventContext<E> process(EntityEvent<E> event) {
		return process(event, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends Serializable> EventContext<E> process(EntityEvent<E> event, EntityEvent<?> parentEvent) {
		Assert.notNull(event);
		Serializable content = event.getContent();
		//
		LOG.info("Publishing event [{}]", event);
		//
		// continue suspended event
		event.getContext().setSuspended(false);
		//
		if (parentEvent != null) {
			event.setParentId(parentEvent.getId());
			event.setRootId(parentEvent.getRootId() == null ? parentEvent.getId() : parentEvent.getRootId());
			if (parentEvent.getPriority() != null 
					&& (event.getPriority() == null || event.getPriority().getPriority() < parentEvent.getPriority().getPriority())) {
				// parent has higher priority ... execute with the same priority as parent
				event.setPriority(parentEvent.getPriority());
			}
			// parent event type can be preset manually
			if (StringUtils.isEmpty(event.getParentType())) {
				event.setParentType(parentEvent.getType().name());
			}
		}
		//
		// read previous (original) dto source - usable in "check modification" processors
		if (event.getOriginalSource() == null && (content instanceof AbstractDto)) { // original source could be set externally
			AbstractDto contentDto = (AbstractDto) content;
			// works only for dto modification
			if (contentDto.getId() != null && lookupService.getDtoLookup(contentDto.getClass()) != null) {
				event.setOriginalSource((E) lookupService.lookupDto(contentDto.getClass(), contentDto.getId()));
			}
		}
		//
		// persist event if needed
		// event is persisted automatically, when parent event is persisted
		if (content instanceof BaseDto && event.getId() == null && event.getParentId() != null) {
			BaseDto dto = (BaseDto) content;
			if (dto.getId() == null) {
				// prepare id for new content - event is persisted before entity is persisted.
				dto.setId(UUID.randomUUID());
			}
			//
			IdmEntityEventDto preparedEvent = toDto(dto, (EntityEvent<AbstractDto>) event);
			preparedEvent.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build()); // RUNNING => prevent to start by async task
			preparedEvent.setRootId(event.getRootId() == null ? event.getParentId() : event.getRootId());
			preparedEvent = entityEventService.save(preparedEvent);
			event.setId(preparedEvent.getId());
			//
			// prepared event is be executed
			CoreEvent<IdmEntityEventDto> executeEvent = new CoreEvent<>(EntityEventType.EXECUTE, preparedEvent);
			publisher.publishEvent(executeEvent);
			//
			LOG.info("Event [{}] is completed", event);
			// fill original event result
			E processedContent = (E) preparedEvent.getContent();
			if (processedContent != null) {
				event.setContent(processedContent);
			}
			event.getContext().addResult(new DefaultEventResult<E>(event, new EmptyEntityEventProcessor<E>()));
			//
			return event.getContext();
		} else {
			publisher.publishEvent(event); 
			LOG.info("Event [{}] is completed", event);
			//
			return event.getContext();
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes" })
	public List<EntityEventProcessorDto> find(EntityEventProcessorFilter filter) {
		List<EntityEventProcessorDto> dtos = new ArrayList<>();
		Map<String, EntityEventProcessor> processors = context.getBeansOfType(EntityEventProcessor.class);
		for(Entry<String, EntityEventProcessor> entry : processors.entrySet()) {
			EntityEventProcessor<?> processor = entry.getValue();
			// entity event processor depends on module - we could not call any processor method
			// TODO: all processor should be returned - disbaled by filter
			if (!enabledEvaluator.isEnabled(processor)) {
				continue;
			}
			EntityEventProcessorDto dto = toDto(processor);
			//
			if (passFilter(dto, filter)) {
				dtos.add(dto);
			}

		}
		LOG.debug("Returning [{}] registered entity event processors", dtos.size());
		return dtos;
	}
	
	@Override
	public EntityEventProcessorDto get(String processorId) {
		EntityEventProcessor<?> processor = getProcessor(processorId);
		if (processor == null) {
			return null;
		}
		return toDto(processor);
	}
	
	/**
	 * Get processor from context by id
	 * 
	 * @param processorId
	 * @return
	 */
	@Override
	public EntityEventProcessor<?> getProcessor(String processorId) {
		Assert.notNull(processorId);
		//
		return (EntityEventProcessor<?>) context.getBean(processorId);
	}

	@Override
	public void publishEvent(Object event) {
		publisher.publishEvent(event);
	}
	
	@Override
	public <E extends Identifiable> void changedEntity(E owner) {
		changedEntity(owner, null);
	}
	
	@Override
	public <E extends Identifiable> void changedEntity(E owner, EntityEvent<? extends Identifiable> originalEvent) {
		Assert.notNull(owner);
		//
		changedEntity(owner.getClass(), lookupService.getOwnerId(owner), originalEvent);
	}
	
	@Override
	public void changedEntity(Class<? extends Identifiable> ownerType, UUID ownerId) {
		changedEntity(ownerType, ownerId, null);
	}
	
	@Override
	public void changedEntity(
			Class<? extends Identifiable> ownerType, 
			UUID ownerId, 
			EntityEvent<? extends Identifiable> originalEvent) {
		IdmEntityEventDto event = prepareEvent(ownerType, ownerId, originalEvent);
		event.setEventType(CoreEventType.NOTIFY.name());
		//
		putToQueue(event);
	}
	
	/**
	 * Spring schedule new task after previous task ended (don't run concurrently)
	 */
	@Scheduled(fixedDelayString = "${" + SchedulerConfiguration.PROPERTY_EVENT_QUEUE_PROCESS + ":" + SchedulerConfiguration.DEFAULT_EVENT_QUEUE_PROCESS + "}")
	public void scheduleProcessCreated() {
		if (!eventConfiguration.isAsynchronous()) {
			// asynchronous processing is disabled
			// prevent to debug some messages into log - usable for devs
			return;
		}
		processCreated();
	}
	
	/**
	 * Process created events from event queue
	 * 
	 * @return
	 */
	protected int processCreated() {
		// run as system - called from scheduler internally
		securityService.setSystemAuthentication();
		//
		// calculate events to process
		String instanceId = configurationService.getInstanceId();
		List<IdmEntityEventDto> events = getCreatedEvents(instanceId);
		LOG.trace("Events to process [{}] on instance [{}].", events.size(), instanceId);
		for (IdmEntityEventDto event : events) {
			// @Transactional
			context.getBean(this.getClass()).executeEvent(event);;
		}
		return events.size();
	}
	
	@Override
	public IdmEntityEventDto getEvent(EntityEvent<? extends Serializable> event) {
		Assert.notNull(event);
		//
		UUID eventId = getEventId(event);
		if (eventId == null) {
			// event doesn't contain entity change - event is not based on entity change
			return null;
		}
		return getEvent(eventId);
	}
	
	@Override
	public IdmEntityEventDto getEvent(UUID eventId) {
		return entityEventService.get(eventId);
	}
	
	@Override
	public UUID getEventId(EntityEvent<? extends Serializable> event) {
		Assert.notNull(event);
		//
		return event.getId();
	}
	
	@Override
	public boolean isRunnable(UUID eventId) {
		IdmEntityEventDto event = getEvent(eventId);
		if (event == null) {
			return false;
		}
		//
		return event.getResult().getState().isRunnable();
	}
	
	@Override
	public String getOwnerType(Identifiable owner) {
		return lookupService.getOwnerType(owner);
	}
	
	@Override
	public String getOwnerType(Class<? extends Identifiable> ownerType) {
		return lookupService.getOwnerType(ownerType);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public AbstractDto findOwner(IdmEntityEventDto change) {
		try {
			Class<?> ownerType = Class.forName(change.getOwnerType());
			if (!AbstractEntity.class.isAssignableFrom(ownerType)) {
				throw new IllegalArgumentException(String.format("Owner type [%s] has to generalize [AbstractEntity]", ownerType));
			}
			//
			return (AbstractDto) lookupService.lookupDto((Class<? extends AbstractEntity>) ownerType, change.getOwnerId());
		} catch (ClassNotFoundException ex) {
			LOG.error("Class [{}] for entity change [{}] not found, module or type was uninstalled, returning null",
					change.getOwnerType(), change.getId());
			return null;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public AbstractDto findOwner(String ownerType, Serializable ownerId) {
		try {
			Class<?> ownerTypeClass = Class.forName(ownerType);
			if (!AbstractEntity.class.isAssignableFrom(ownerTypeClass)) {
				throw new IllegalArgumentException(String.format("Owner type [%s] has to generalize [AbstractEntity]", ownerType));
			}
			//
			return (AbstractDto) lookupService.lookupDto((Class<? extends AbstractEntity>) ownerTypeClass, ownerId);
		} catch (ClassNotFoundException ex) {
			LOG.error("Class [{}] for entity change [{}] not found, module or type was uninstalled, returning null",
					ownerType, ownerId);
			return null;
		}
	}
	
	@Override
	@Transactional
	public void executeEvent(IdmEntityEventDto event) {
		Assert.notNull(event);
		Assert.notNull(event.getOwnerId());
		if (!eventConfiguration.isAsynchronous()) {
			// synchronous processing
			// we don't persist events and their states
			process(new CoreEvent<>(EntityEventType.EXECUTE, event));
			return;
		}
		if (event.getPriority() == PriorityType.IMMEDIATE) {
			// synchronous processing
			// we don't persist events and their states
			// TODO: what about running event with the same owner? And events in queue for the same owner
			process(new CoreEvent<>(EntityEventType.EXECUTE, event));
			return;
		}
		//
		if (runningOwnerEvents.putIfAbsent(event.getOwnerId(), event.getId()) != null) {
			LOG.debug("Previous event [{}] for owner with id [{}] is currently processed.", 
					runningOwnerEvents.get(event.getOwnerId()), event.getOwnerId());
			// event will be processed in another scheduling			
			return;
		}
		// check super owner is not processed
		UUID superOwnerId = event.getSuperOwnerId();
		if (superOwnerId != null && !superOwnerId.equals(event.getOwnerId())) {			
			if (runningOwnerEvents.putIfAbsent(superOwnerId, event.getId()) != null) {
				LOG.debug("Previous event [{}] for super owner with id [{}] is currently processed.", 
						runningOwnerEvents.get(superOwnerId), superOwnerId);
				runningOwnerEvents.remove(event.getOwnerId());
				// event will be processed in another scheduling		
				return;
			}
		}
		// execute event in new thread asynchronously
		try {
			eventConfiguration.getExecutor().execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						process(new CoreEvent<>(EntityEventType.EXECUTE, event));
					} catch (Exception ex) {
						// exception handling only ... all processor should persist their own entity state (see AbstractEntityEventProcessor)
						ResultModel resultModel;
						if (ex instanceof ResultCodeException) {
							resultModel = ((ResultCodeException) ex).getError().getError();
						} else {
							resultModel = new DefaultResultModel(
									CoreResultCode.EVENT_EXECUTE_FAILED, 
									ImmutableMap.of(
											"eventId", event.getId(), 
											"eventType", String.valueOf(event.getEventType()),
											"ownerId", String.valueOf(event.getOwnerId()),
											"instanceId", String.valueOf(event.getInstanceId())));
						}		
						context.getBean(DefaultEntityEventManager.this.getClass()).saveResult(event.getId(), new OperationResultDto
										.Builder(OperationState.EXCEPTION)
										.setCause(ex)
										.setModel(resultModel)
										.build());
						
						LOG.error(resultModel.toString(), ex);
					} finally {
						LOG.trace("Event [{}] ends for owner with id [{}].", event.getId(), event.getOwnerId());
						removeRunningEvent(event);
					}
				}
			});
			//
			LOG.trace("Running event [{}] for owner with id [{}].", event.getId(), event.getOwnerId());
		} catch (RejectedExecutionException ex) {
			// thread pool is full - wait for another try
			// TODO: Thread.wait(300) ?
			removeRunningEvent(event);
		}
	}
	
	private void removeRunningEvent(IdmEntityEventDto event) {
		runningOwnerEvents.remove(event.getOwnerId());
		UUID superOwnerId = event.getSuperOwnerId();
		if (superOwnerId != null) {
			runningOwnerEvents.remove(superOwnerId);
		}
	}
	
	/**
	 * TODO: Will be this method useful?
	 * 
	 * @param event
	 */
	@SuppressWarnings("unused")
	private void runOnBackground(EntityEvent<? extends Identifiable> event) {
		Assert.notNull(event);
		//
		putToQueue(prepareEvent(event.getContent(), event));
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdmEntityEventDto saveEvent(IdmEntityEventDto entityEvent) {
		return entityEventService.save(entityEvent);
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IdmEntityEventDto saveResult(UUID eventId, OperationResultDto result) {
		Assert.notNull(eventId);
		Assert.notNull(result);
		IdmEntityEventDto entityEvent = entityEventService.get(eventId);
		Assert.notNull(entityEvent);
		//
		entityEvent.setResult(result);
		return entityEventService.save(entityEvent);
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<IdmEntityStateDto> saveStates(
			EntityEvent<?> event, 
			List<IdmEntityStateDto> previousStates,
			EventResult<?> result) {
		IdmEntityEventDto entityEvent = getEvent(event);
		List<IdmEntityStateDto> results = new ArrayList<>();
		if (entityEvent == null) {
			return results;
		}
		// simple drop - we don't need to find and update results, we'll create new ones
		if (previousStates != null && !previousStates.isEmpty()) {
			previousStates.forEach(state -> {
				entityStateManager.deleteState(state);
			});
		}
		//
		if (result == null) {
			IdmEntityStateDto state = new IdmEntityStateDto(entityEvent);
			// default result without model
			state.setResult(new OperationResultDto
					.Builder(OperationState.EXECUTED)
					.build());
			results.add(entityStateManager.saveState(null, state));
			return results;
		}
		if (result.getResults().isEmpty()) {
			results.add(entityStateManager.saveState(null, createState(entityEvent, result, new OperationResultDto.Builder(OperationState.EXECUTED).build())));
			return results;
		}
		result.getResults().forEach(opeartionResult -> {
			results.add(entityStateManager.saveState(null, createState(entityEvent, result, opeartionResult.toDto())));
		});
		//
		return results;
	}
	
	@Override
	public EntityEvent<? extends Identifiable> toEvent(IdmEntityEventDto entityEvent) {
		Identifiable content = null;
		// try to use persisted event content
		// only if type and id is the same as owner can be used
		if (entityEvent.getContent() != null 
				&& getOwnerType(entityEvent.getContent().getClass()).equals(entityEvent.getOwnerType())
				&& entityEvent.getContent().getId().equals(entityEvent.getOwnerId())) {
			content = entityEvent.getContent();
		}
		if (content == null) {
			// content is not persisted - try to find actual entity
			content = findOwner(entityEvent);
		}
		if (content == null) {
			throw new EventContentDeletedException(entityEvent);
		}
		//
		Map<String, Serializable> eventProperties = entityEvent.getProperties().toMap();
		eventProperties.put(EntityEvent.EVENT_PROPERTY_EVENT_ID, entityEvent.getId());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_PRIORITY, entityEvent.getPriority());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_EXECUTE_DATE, entityEvent.getExecuteDate());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_PARENT_EVENT_TYPE, entityEvent.getParentEventType());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_PARENT_EVENT_ID, entityEvent.getParent());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_ROOT_EVENT_ID, entityEvent.getRootId());
		eventProperties.put(EntityEvent.EVENT_PROPERTY_SUPER_OWNER_ID, entityEvent.getSuperOwnerId());
		final String type = entityEvent.getEventType();
		DefaultEventContext<Identifiable> initContext = new DefaultEventContext<>();
		initContext.setProcessedOrder(entityEvent.getProcessedOrder());
		EventType eventType = (EventType) () -> type;
		EntityEvent<Identifiable> resurectedEvent = new CoreEvent<>(eventType, content, eventProperties, initContext);
		resurectedEvent.setOriginalSource(entityEvent.getOriginalSource());
		//
		return resurectedEvent;
	}
	
	@Override
	public void enable(String processorId) {
		setEnabled(processorId, true);
	}

	@Override
	public void disable(String processorId) {
		setEnabled(processorId, false);
	}

	@Override
	public void setEnabled(String processorId, boolean enabled) {
		setEnabled(getProcessor(processorId), enabled);
	}
	
	@Override
	public boolean isAsynchronous() {
		return eventConfiguration.isAsynchronous();
	}
	
	private void setEnabled(EntityEventProcessor<?> processor, boolean enabled) {
		String enabledPropertyName = processor.getConfigurationPropertyName(ConfigurationService.PROPERTY_ENABLED);
		configurationService.setBooleanValue(enabledPropertyName, enabled);
	}
	
	/**
	 * Convert processor to dto.
	 * 
	 * @param processor
	 * @return
	 */
	private EntityEventProcessorDto toDto(EntityEventProcessor<?> processor) {
		EntityEventProcessorDto dto = new EntityEventProcessorDto();
		dto.setId(processor.getId());
		dto.setName(processor.getName());
		dto.setModule(processor.getModule());
		dto.setContentClass(processor.getEntityClass());
		dto.setEntityType(processor.getEntityClass().getSimpleName());
		dto.setEventTypes(Lists.newArrayList(processor.getEventTypes()));
		dto.setClosable(processor.isClosable());
		dto.setDisabled(processor.isDisabled());
		dto.setDisableable(processor.isDisableable());
		dto.setOrder(processor.getOrder());
		// resolve documentation
		dto.setDescription(processor.getDescription());
		dto.setConfigurationProperties(processor.getConfigurationMap());
		//
		return dto;
	}
	
	@SuppressWarnings({"unchecked", "rawtypes" })
	private void putToQueue(IdmEntityEventDto entityEvent) {
		if (entityEvent.getPriority() == PriorityType.IMMEDIATE) {
			LOG.trace("Event type [{}] for owner with id [{}] will be executed synchronously.", 
					entityEvent.getEventType(), entityEvent.getOwnerId());	
			executeEvent(entityEvent);
			return;
		}
		if (!eventConfiguration.isAsynchronous()) {
			LOG.trace("Event type [{}] for owner with id [{}] will be executed synchronously, asynchronous event processing [{}] is disabled.", 
					entityEvent.getEventType(), entityEvent.getOwnerId(), EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED);	
			executeEvent(entityEvent);
			return;
		}
		//
		// get enabled processors
		final EntityEvent<? extends Serializable> event = toEvent(entityEvent);
		List<EntityEventProcessor> registeredProcessors = context
			.getBeansOfType(EntityEventProcessor.class)
			.values()
			.stream()
			.filter(enabledEvaluator::isEnabled)
			.filter(processor -> !processor.isDisabled())
			.filter(processor -> processor.supports(event))
			.filter(processor -> processor.conditional(event))
			.sorted(new AnnotationAwareOrderComparator())
			.collect(Collectors.toList());
		if (registeredProcessors.isEmpty()) {
			LOG.debug("Event type [{}] for owner with id [{}] will not be executed, no enabled processor is registered.", 
					entityEvent.getEventType(), entityEvent.getOwnerId());	
			return;
		}
		//
		// evaluate event priority by registered processors
		PriorityType priority = evaluatePriority(event, registeredProcessors);
		if (priority != null && priority.getPriority() < entityEvent.getPriority().getPriority()) {
			entityEvent.setPriority(priority);
		}
		//
		// registered processors voted about event will be processed synchronously
		if (entityEvent.getPriority() == PriorityType.IMMEDIATE) {
			LOG.trace("Event type [{}] for owner with id [{}] will be executed synchronously.", 
					entityEvent.getEventType(), entityEvent.getOwnerId());	
			executeEvent(entityEvent);
			return;
		}
		//
		// TODO: send notification only when event fails
		// notification - info about registered (asynchronous) processors
//		Map<String, Object> parameters = new LinkedHashMap<>();
//		parameters.put("eventType", entityEvent.getEventType());
//		parameters.put("ownerId", entityEvent.getOwnerId());
//		parameters.put("instanceId", entityEvent.getInstanceId());
//		parameters.put("processors", registeredProcessors
//				.stream()
//				.map(DefaultEntityEventManager.this::toDto)
//				.collect(Collectors.toList()));
//		notificationManager.send(
//				CoreModuleDescriptor.TOPIC_EVENT, 
//				new IdmMessageDto
//					.Builder()
//					.setLevel(NotificationLevel.INFO)
//					.setModel(new DefaultResultModel(CoreResultCode.EVENT_ACCEPTED, parameters))
//					.build());
		//
		// persist event - asynchronous processing
		entityEventService.save(entityEvent);
	}
	
	/**
	 * Evaluate event priority by registered processors
	 * 
	 * @param event
	 * @param registeredProcessors
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected PriorityType evaluatePriority(EntityEvent<?> event, List<EntityEventProcessor> registeredProcessors) {
		PriorityType priority = null;
		for (EntityEventProcessor processor : registeredProcessors) {
			if (!(processor instanceof AsyncEntityEventProcessor)) {
				continue;
			}
			AsyncEntityEventProcessor asyncProcessor = (AsyncEntityEventProcessor) processor; 
			PriorityType processorPriority = asyncProcessor.getPriority(event);
			if (processorPriority == null) {
				// processor doesn't vote about priority - preserve original event priority. 
				continue;
			}
			if (priority == null || processorPriority.getPriority() < priority.getPriority()) {
				priority = processorPriority;
			}
			if (priority == PriorityType.IMMEDIATE) {
				// nothing is higher
				break;
			}
		}
		//
		return priority;
	}
	
	/**
	 * Called from scheduler - concurrency is prevented.
	 * Returns events to process sorted by priority 7 / 3 (high / normal). 
	 * Immediate priority is executed synchronously.
	 * Cancel duplicate events (same type, owner and props) - last event is returned
	 * 
	 * @param instanceId
	 * @return
	 */
	protected List<IdmEntityEventDto> getCreatedEvents(String instanceId) {
		Assert.notNull(instanceId);
		//
		// load created events - high priority
		DateTime executeDate = new DateTime();
		Page<IdmEntityEventDto> highEvents = entityEventService.findToExecute(
				instanceId,
				executeDate,
				PriorityType.HIGH,
				new PageRequest(0, 100, new Sort(Direction.ASC, Auditable.PROPERTY_CREATED)));
		// load created events - low priority
		Page<IdmEntityEventDto> normalEvents = entityEventService.findToExecute(
				instanceId,
				executeDate,
				PriorityType.NORMAL,
				new PageRequest(0, 100, new Sort(Direction.ASC, Auditable.PROPERTY_CREATED)));
		// merge events
		List<IdmEntityEventDto> events = new ArrayList<>();
		events.addAll(highEvents.getContent());
		events.addAll(normalEvents.getContent());
		// sort by created date
		events.sort(new CreatedComparator());
		//
		// cancel duplicates - by owner => properties has to be the same
		// execute the first event for each owner only - preserve events order
		Map<UUID, IdmEntityEventDto> distinctEvents = new LinkedHashMap<>();	
		events.forEach(event -> {
			if (!distinctEvents.containsKey(event.getOwnerId())) {
				// the first event
				distinctEvents.put(event.getOwnerId(), event);
			} else {
				// cancel duplicate older event 
				IdmEntityEventDto olderEvent = distinctEvents.get(event.getOwnerId());
				if (isDuplicate(olderEvent, event)) {
					// try to set higher priority
					if (olderEvent.getPriority() == PriorityType.HIGH) {
						event.setPriority(PriorityType.HIGH);
					}
					distinctEvents.put(event.getOwnerId(), event);
					//
					LOG.debug(new DefaultResultModel(
							CoreResultCode.EVENT_DUPLICATE_CANCELED, 
							ImmutableMap.of(
									"eventId", olderEvent.getId(), 
									"eventType", String.valueOf(olderEvent.getEventType()),
									"ownerId", String.valueOf(olderEvent.getOwnerId()),
									"instanceId", String.valueOf(olderEvent.getInstanceId()),
									"neverEventId", event.getId())).toString());
					//
					IdmEntityEventFilter eventFilter = new IdmEntityEventFilter();
					eventFilter.setParentId(olderEvent.getId());
					if (entityEventService.find(eventFilter, new PageRequest(0, 1)).getTotalElements() == 0) {
						entityEventService.delete(olderEvent);
					}
				}
			}
		});
		// 
		// sort by priority
		events = distinctEvents
				.values()
				.stream()
				.sorted((o1, o2) -> {
					return Integer.compare(o1.getPriority().getPriority(), o2.getPriority().getPriority());
				})
				.collect(Collectors.toList());
		int normalCount = events.stream().filter(e -> e.getPriority() == PriorityType.NORMAL).collect(Collectors.toList()).size();
		int highMaximum = normalCount > 30 ? 70 : (100 - normalCount);
		// evaluate priority => high 70 / low 30
		int highCounter = 0;
		List<IdmEntityEventDto> prioritizedEvents = new ArrayList<>();
		for (IdmEntityEventDto event : events) {
			if (event.getPriority() == PriorityType.HIGH) {
				if (highCounter < highMaximum) {
					prioritizedEvents.add(event);
					highCounter++;
				}
			} else {
				// normal priority remains only
				if (prioritizedEvents.size() >= 100) {
					break;
				}
				prioritizedEvents.add(event);
			}
		}
		//
		return prioritizedEvents;
	}
	
	/**
	 * Returns true, when events are duplicates
	 * 
	 * @param olderEvent
	 * @param event
	 * @return
	 */
	protected boolean isDuplicate(IdmEntityEventDto olderEvent, IdmEntityEventDto event) {
		return Objects.equal(olderEvent.getEventType(), event.getEventType())
				&& Objects.equal(olderEvent.getParentEventType(), event.getParentEventType())
				&& Objects.equal(getProperties(olderEvent), getProperties(event));
	}
	
	/**
	 * Remove internal event properties needed for processing
	 * 
	 * @param event
	 * @return
	 */
	private ConfigurationMap getProperties(IdmEntityEventDto event) {
		ConfigurationMap copiedProperies = new ConfigurationMap();
		copiedProperies.putAll(event.getProperties());
		//
		// remove internal event properties needed for processing
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_EVENT_ID);
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_EXECUTE_DATE);
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_PRIORITY);
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_SUPER_OWNER_ID);
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_PARENT_EVENT_ID);
		copiedProperies.remove(EntityEvent.EVENT_PROPERTY_ROOT_EVENT_ID);
		//
		return copiedProperies;
	}
	
	private <E extends Serializable> IdmEntityStateDto createState(
			IdmEntityEventDto entityEvent, 
			EventResult<E> eventResult, 
			OperationResultDto operationResult) {
		IdmEntityStateDto state = new IdmEntityStateDto(entityEvent);
		//
		state.setClosed(eventResult.isClosed());
		state.setSuspended(eventResult.isSuspended());
		state.setProcessedOrder(eventResult.getProcessedOrder());
		state.setProcessorId(eventResult.getProcessor().getId());
		state.setProcessorModule(eventResult.getProcessor().getModule());
		state.setProcessorName(eventResult.getProcessor().getName());
		state.setResult(operationResult);
		//
		return state;
	}
	
	/**
	 * Returns true, when given processor pass given filter
	 * 
	 * @param processor
	 * @param filter
	 * @return
	 */
	private boolean passFilter(EntityEventProcessorDto processor, EntityEventProcessorFilter filter) {
		if (filter == null) {
			// empty filter
			return true;
		}
		// id - not supported
		if (filter.getId() != null) {
			throw new UnsupportedOperationException("Filtering event processors by [id] is not supported.");
		}
		// text - lowercase like in name, description, content class - canonical name
		if (StringUtils.isNotEmpty(filter.getText())) {
			if (!processor.getName().toLowerCase().contains(filter.getText().toLowerCase())
					&& (processor.getDescription() == null || !processor.getDescription().toLowerCase().contains(filter.getText().toLowerCase()))
					&& !processor.getContentClass().getCanonicalName().toLowerCase().contains(filter.getText().toLowerCase())) {
				return false;
			}
		}
		// processors name
		if (StringUtils.isNotEmpty(filter.getName()) && !processor.getName().equals(filter.getName())) {
			return false; 
		}
		// content ~ entity type - dto type
		if (filter.getContentClass() != null && !filter.getContentClass().isAssignableFrom(processor.getContentClass())) {
			return false;
		}
		// module id
		if (StringUtils.isNotEmpty(filter.getModule()) && !filter.getModule().equals(processor.getModule())) {
			return false;
		}
		// description - like
		if (StringUtils.isNotEmpty(filter.getDescription()) 
				&& StringUtils.isNotEmpty(processor.getDescription()) 
				&& !processor.getDescription().contains(filter.getDescription())) {
			return false;
		}
		// entity ~ content type - simple name
		if (StringUtils.isNotEmpty(filter.getEntityType()) && !processor.getEntityType().equals(filter.getEntityType())) {
			return false;
		}
		// event types
		if (!filter.getEventTypes().isEmpty() && !processor.getEventTypes().containsAll(filter.getEventTypes())) {
			return false;
		}
		//
		return true;
	}
	
	public IdmEntityEventDto saveEvent(EntityEvent<? extends Identifiable> event, OperationResultDto result) {
		Assert.notNull(event);
		Identifiable content = event.getContent();
		Assert.notNull(content);
		//
		IdmEntityEventDto savedEvent = toDto(event);
		savedEvent.setOwnerId(lookupService.getOwnerId(content));
		savedEvent.setOwnerType(getOwnerType(content));
		savedEvent.setResult(result);
		//
		if (savedEvent.getPriority() == null) {
			savedEvent.setPriority(PriorityType.NORMAL);
		}
		//
		savedEvent = entityEventService.save(savedEvent);
		//
		event.setId(savedEvent.getId());
		event.setPriority(savedEvent.getPriority());
		//
		return savedEvent;
	}
	
	/**
	 * Constructs entity event
	 * 
	 * @param identifiable
	 * @param originalEvent
	 * @return
	 */
	public IdmEntityEventDto prepareEvent(Identifiable owner, EntityEvent<? extends Identifiable> originalEvent) {
		Assert.notNull(owner);
		Assert.notNull(owner.getId(), "Change can be published after entity id is assigned at least.");
		//
		IdmEntityEventDto event = prepareEvent(owner.getClass(), lookupService.getOwnerId(owner), originalEvent);
		event.setContent(owner);
		//
		return event;
	}
	
	private IdmEntityEventDto prepareEvent(Class<? extends Identifiable> ownerType, UUID ownerId, EntityEvent<? extends Identifiable> originalEvent) {
		Assert.notNull(ownerType);
		Assert.notNull(ownerId, "Change can be published after entity id is assigned at least.");
		//
		IdmEntityEventDto savedEvent = toDto(originalEvent);
		savedEvent.setId(null);
		savedEvent.setOwnerId(ownerId);
		savedEvent.setOwnerType(getOwnerType(ownerType));
		//
		if (originalEvent != null) {
			savedEvent.setParent(originalEvent.getId());
			savedEvent.setRootId(originalEvent.getRootId() == null ? originalEvent.getId() : originalEvent.getRootId());
			savedEvent.setParentEventType(originalEvent.getType().name());
		} else {
			// notify as default event type
			savedEvent.setEventType(CoreEventType.NOTIFY.name());
		}
		//
		return savedEvent;
	}
	
	/**
	 * Usable for newly created events
	 * 
	 * @param owner
	 * @param event
	 * @return
	 */
	private IdmEntityEventDto toDto(Identifiable owner, EntityEvent<? extends Identifiable> event) {
		IdmEntityEventDto entityEvent = toDto(event);
		if (owner != null) {
			entityEvent.setOwnerId(lookupService.getOwnerId(owner));
			entityEvent.setOwnerType(getOwnerType(owner.getClass()));
		}
		//
		return entityEvent;
	}
	
	private IdmEntityEventDto toDto(EntityEvent<? extends Identifiable> event) {
		IdmEntityEventDto entityEvent = new IdmEntityEventDto();
		//
		entityEvent.setResult(new OperationResultDto.Builder(OperationState.CREATED).build());
		entityEvent.setInstanceId(eventConfiguration.getAsynchronousInstanceId());
		if (event == null) {
			entityEvent.setPriority(PriorityType.NORMAL);
			return entityEvent;
		}
		entityEvent.setId(event.getId());
		entityEvent.setSuperOwnerId(event.getSuperOwnerId());
		entityEvent.setEventType(event.getType().name());
		entityEvent.getProperties().putAll(event.getProperties());
		entityEvent.setParent(event.getParentId());
		entityEvent.setRootId(event.getRootId());
		entityEvent.setParentEventType(event.getParentType());
		entityEvent.setExecuteDate(event.getExecuteDate()); // look out - it's the wish - when asynchronous event should be executed...
		entityEvent.setPriority(event.getPriority() != null ? event.getPriority() : PriorityType.NORMAL);
		entityEvent.setContent(event.getContent());
		entityEvent.setOriginalSource(event.getOriginalSource());
		entityEvent.setClosed(event.isClosed());
		if (entityEvent.isClosed()) {
			entityEvent.setResult(new OperationResultDto
					.Builder(OperationState.EXECUTED)
					.setModel(new DefaultResultModel(CoreResultCode.EVENT_ALREADY_CLOSED))
					.build());
		}
		entityEvent.setSuspended(event.isSuspended());
		//
		return entityEvent;
	}
}
