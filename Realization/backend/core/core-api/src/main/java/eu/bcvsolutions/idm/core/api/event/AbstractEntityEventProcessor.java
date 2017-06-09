package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.security.api.service.EnabledEvaluator;

/**
 * Single entity event processor
 * <p>
 * Types could be {@literal null}, then processor supports all event types
 * <p>
 * TODO: move @Autowire to @Configuration bean post processor
 * 
 * @param <E> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 */
public abstract class AbstractEntityEventProcessor<E extends Serializable> 
		implements EntityEventProcessor<E>, ApplicationListener<AbstractEntityEvent<E>> {

	private final Class<E> entityClass;
	private final Set<String> types = new HashSet<>();
	
	@Autowired(required = false)
	private EnabledEvaluator enabledEvaluator; // optional internal dependency - checks for module is enabled
	
	@Autowired(required = false)
	private ConfigurationService configurationService; // optional internal dependency - checks for processor is enabled
	
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
		this.enabledEvaluator = enabledEvaluator;
		this.configurationService = configurationService;
	}
	
	@Override
	public Class<E> getEntityClass() {
		return entityClass;
	}
	
	@Override
	public String[] getEventTypes() {
		return types.toArray(new String[types.size()]);
	}
	
	@Override
	public boolean supports(EntityEvent<?> entityEvent) {
		Assert.notNull(entityEvent);
		Assert.notNull(entityEvent.getContent(), "Entity event does not contain content, content is required!");
		//
		return entityEvent.getContent().getClass().isAssignableFrom(entityClass)
				&& (types.isEmpty() || types.contains(entityEvent.getType().name()));
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(java.lang.Object)
	 */
	@Override
	public void onApplicationEvent(AbstractEntityEvent<E> event) {
		// check for module is enabled, if evaluator is given
		if (enabledEvaluator != null && !enabledEvaluator.isEnabled(this.getClass())) {
			return;
		}
		// check for processor is enabled
		if (isDisabled()) {
			return;
		}
		//
		if (!supports(event)) {
			// event is not supported with this processor
			return;
		}
		if (event.isClosed()) {	
			// event is completely processed 
			return;
		}
		if (event.isSuspended()) {	
			// event is suspended
			return;
		}
		//
		EventContext<E> context = event.getContext();
		//
		Integer processedOrder = context.getProcessedOrder();
		if (processedOrder != null && processedOrder >= this.getOrder()) {	
			// event was processed with this processor
			return;
		}
		// prepare order ... in processing
		context.setProcessedOrder(this.getOrder());
		// process event
		EventResult<E> result = process(event);
		// add result to history
		context.addResult(result);
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
}
