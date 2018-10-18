package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.security.api.service.EnabledEvaluator;

/**
 * Single entity event processor
 * <p>
 * Types could be {@literal null}, then processor supports all event types
 * <p>
 * 
 * @param <E> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 */
public abstract class AbstractEntityEventProcessor<E extends Serializable> implements 
		EntityEventProcessor<E>, 
		GenericApplicationListener,
		BeanNameAware {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractEntityEventProcessor.class);
	private final Class<E> entityClass;
	private final Set<String> types = new HashSet<>();
	private String beanName; // spring bean name - used as processor id
	//
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private EnabledEvaluator enabledEvaluator;
	@Autowired private ConfigurationService configurationService;
	
	@SuppressWarnings({"unchecked"})
	public AbstractEntityEventProcessor(EventType... types) {
		this.entityClass = (Class<E>)GenericTypeResolver.resolveTypeArgument(getClass(), EntityEventProcessor.class);
		if (types != null) {
			for(EventType type : types) {
				this.types.add(type.name());
			}
		}
	}
	
	public AbstractEntityEventProcessor(EnabledEvaluator enabledEvaluator, ConfigurationService configurationService, EventType... types) {
		this(types);
		//
		this.enabledEvaluator = enabledEvaluator;
		this.configurationService = configurationService;
	}
	
	@Override
	public Class<E> getEntityClass() {
		return entityClass;
	}

	@Override
	public String[] getEventTypes() {
		final Set<String> configTypes = getEventTypesFromConfiguration();
		// Default event types can be overwritten using config property
		final Set<String> eventTypesToUse = configTypes == null ? types : configTypes;
		//
		return eventTypesToUse.toArray(new String[eventTypesToUse.size()]);
	}

	@Override
	public boolean supports(EntityEvent<?> event) {
		Assert.notNull(event);
		Assert.notNull(event.getContent(), "Entity event does not contain content, content is required!");
		//
		final List<String> supportedEventTypes = Arrays.asList(getEventTypes());
		return entityClass.isAssignableFrom(event.getContent().getClass())
				&& (supportedEventTypes.isEmpty() || supportedEventTypes.contains(event.getType().name()));
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Processor listens entity events only
	 */
	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return EntityEvent.class.isAssignableFrom(eventType.getRawClass());
	}
	
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		if (sourceType == null) {
			return true; // solved by standard support method above
		}
		// only for interfaces and abstract classes (backward compatibility)
		if (entityClass.isInterface() || Modifier.isAbstract(entityClass.getModifiers())) {
			return entityClass.isAssignableFrom(sourceType);
		}
		return entityClass.equals(sourceType);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Returns true by default - processor will be processed.
	 */
	@Override
	public boolean conditional(EntityEvent<E> event) {
		return true;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(java.lang.Object)
	 * 
	 * {@link ApplicationEvent} is used - we want to handle events with super classes too (solved by {@link #support} method).
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onApplicationEvent(ApplicationEvent rawEvent) {
		if (!(rawEvent instanceof EntityEvent)) {
			// not an EntityEvent - just for sure, but its solved by GenericApplicationListener supports method.
			return;
		}
		EntityEvent event = (EntityEvent) rawEvent;
		if (!supports(event)) {
			// event is not supported with this processor
			// its on the start to prevent debug logging
			LOG.trace("Skipping processor [{}]([{}]) for [{}]. Processor don't support given event. ", getName(), getModule(), event);
			return;
		}
		// check for module is enabled, if evaluator is given
		if (enabledEvaluator != null && !enabledEvaluator.isEnabled(this)) {
			LOG.debug("Skipping processor [{}]([{}]) for [{}]. Module [{}] is disabled. ", getName(), getModule(), event, getModule());
			return;
		}
		// check for processor is enabled
		if (isDisabled()) {
			LOG.debug("Skipping processor [{}]([{}]) for [{}]. Module [{}] is disabled.", getName(), getModule(), event, getModule());
			return;
		}
		if (event.isClosed()) {	
			// event is completely processed 
			LOG.debug("Skipping processor [{}]([{}]). [{}] is completely processed.", getName(), getModule(), event);
			return;
		}
		if (event.isSuspended()) {	
			// event is suspended
			LOG.debug("Skipping processor [{}]([{}]). [{}] is suspended.", getName(), getModule(), event);
			return;
		}
		if (!conditional(event)) {
			// event doesn't met conditions
			LOG.debug("Skipping processor [{}]([{}]). [{}] event doesn't met conditions.", getName(), getModule(), event);
			return;
		}
		//
		EventContext<E> context = event.getContext();
		//
		Integer processedOrder = context.getProcessedOrder();
		if (processedOrder != null) {
			// event was processed with this processor
			if (processedOrder > this.getOrder()) {
				LOG.debug("Skipping processor [{}]([{}]). [{}] was already processed by this processor with order [{}].", 
						getName(), getModule(), event, getOrder());
				return;
			}
			// the same order - only different processor instance can process event
			if (processedOrder == this.getOrder()) {
				if (context.getResults().isEmpty()) {
					// TODO: try to find persisted entity states and skip processed processors
					//
					// if event was started in the middle manually => results are empty,
					// event could continue with processors with higher order only.					
					LOG.debug("Skipping processor [{}]([{}]). Processed context for [{}] is empty. Processor's order [{}] is the same as event start.", 
							getName(), getModule(), event, getOrder());
					return;
				}
				for(EventResult<E> result : Lists.reverse(context.getResults())) {
					if (result.getProcessedOrder() != this.getOrder()) {
						// only same order is interesting
						break;
					}
					EntityEventProcessor<E> resultProcessor = result.getProcessor();
					if (resultProcessor != null && resultProcessor.equals(this)) {
						// event was already processed by this processor
						LOG.debug("Skipping processor [{}]([{}]). [{}] was already processed by this processor with order [{}].",
								getName(), getModule(), event, getOrder());
						return;
					}	
				}
			}
		}
		LOG.info("Processor [{}]([{}]) start for [{}] with order [{}].", getName(), getModule(), event, getOrder());
		// prepare order ... in processing
		context.setProcessedOrder(this.getOrder());
		// persist "running" state
		List<IdmEntityStateDto> runningStates = new ArrayList<>();
		if (entityEventManager.getEventId(event) != null) {
			runningStates.addAll(
					entityEventManager.saveStates(event, null, new DefaultEventResult
							.Builder<>(event, this)
							.setResult(new OperationResult
								.Builder(OperationState.RUNNING)
								.build())
							.build())
					);
		}	
		// process event
		EventResult<E> result = null;
		try {			
			result = process(event);
		} catch(Exception ex) {
			// persist state if needed
			UUID eventId = entityEventManager.getEventId(event) ;
			// log error
			ResultModel resultModel;
			if (ex instanceof ResultCodeException) {
				resultModel = ((ResultCodeException) ex).getError().getError();
			} else {
				resultModel = new DefaultResultModel(
						CoreResultCode.EVENT_EXECUTE_PROCESSOR_FAILED, 
						ImmutableMap.of(
								"eventId", String.valueOf(eventId),
								"processor", getName()));
			}
			//
			LOG.error(resultModel.toString(), ex);
			//
			if (eventId != null) {
				//
				result = new DefaultEventResult.Builder<>(event, this)
						.setResult(new OperationResult
								.Builder(OperationState.EXCEPTION)
								.setCause(ex)
								.setModel(resultModel)
								.build())
						.build();
				entityEventManager.saveStates(event, runningStates, result);
			}
			throw ex;
		}
		// default result
		if (result == null) {
			// processor without result is added into history with empty result
			result = new DefaultEventResult<>(event, this);
		}
		// persist state if needed
		entityEventManager.saveStates(event, runningStates, result);
		// add result to history
		context.addResult(result);
		//
		LOG.info("Processor [{}]([{}]) end for [{}] with order [{}].", getName(), getModule(), event, getOrder());
	}
	
	@Override
	public boolean isClosable() {
		return false;
	}
	
	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}
	
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
	
	@Override
	public String getId() {
		return beanName;
	}
	
	/**
	 * Return true if event properties contains given property and this property is true.
	 * If event does not contains this property, then return false.
	 * 
	 * TODO: Move to utils
	 * @param property
	 * @param properties
	 * @return
	 */
	protected boolean getBooleanProperty(String property, Map<String, Serializable> properties) {
		Assert.notNull(property, "Name of event property cannot be null!");
		if (properties == null) {
			return false;
		}

		Object propertyValue = properties.get(property);

		if (propertyValue == null) {
			return false;
		}

		Assert.isInstanceOf(Boolean.class, propertyValue, MessageFormat
				.format("Property [{0}] must be Boolean, but is [{1}]!", property, propertyValue.getClass()));

		if ((Boolean) propertyValue) {
			return true;
		}

		return false;
	}
	
	/**
	 * Return true if event properties contains given property and this property is true.
	 * If event does not contains this property, then return false.
	 * 
	 * TODO: Move to utils
	 * @param property
	 * @param properties
	 * @return
	 */
	protected boolean getBooleanProperty(String property, ConfigurationMap properties) {
		Assert.notNull(property, "Name of event property cannot be null!");
		if (properties == null) {
			return false;
		}
		return properties.getBooleanValue(property);
	}
	
	/**
	 * Method returns {@link Collection} of event types for this processor.
	 *
	 * @return Collection of event types configured in app config. Null if configurationService is not defined, or if
	 * config property is not defined.
	 */
	private Set<String> getEventTypesFromConfiguration() {
		if (getConfigurationService() == null) {
			return null;
		}
		//
		final String configValue = getConfigurationService().getValue(
			getConfigurationPrefix()
				+ ConfigurationService.PROPERTY_SEPARATOR
				+ PROPERTY_EVENT_TYPES);
		//
		return configValue == null ? null : Arrays.stream(configValue.split(ConfigurationService.PROPERTY_MULTIVALUED_SEPARATOR))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
	}
}
