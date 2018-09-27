package eu.bcvsolutions.idm.core.api.event;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Event state holder (content + metadata)
 * 
 * @param <E> {@link BaseEntity}, {@link BaseDto} or any other {@link Serializable} content type
 * @author Radek Tomiška
 */
public abstract class AbstractEntityEvent<E extends Serializable> extends ApplicationEvent implements EntityEvent<E> {

	private static final long serialVersionUID = 2309175762418747517L;
	//
	private E content; // mutable content - is changed during processing
	private E originalSource; // persisted content - before event starts
	private final EventType type;
	private final Map<String, Serializable> properties = new LinkedHashMap<>();
	@JsonIgnore
	private final EventContext<E> context;
	private Class<? extends E> eventClassType;
	
	public AbstractEntityEvent(EventType type, E content, Map<String, Serializable> properties, EventContext<E> context, Class<? extends E> eventClassType) {
		super(content);
		//
		Assert.notNull(type, "Operation is required!");
		//
		this.content = content;
		this.type = type;
		if (properties != null) {
			this.properties.putAll(properties);
		}
		this.context = context == null ? new DefaultEventContext<>() : context;
		this.eventClassType = eventClassType;
	}
	
	public AbstractEntityEvent(EventType type, E content, Map<String, Serializable> properties, EventContext<E> context) {
		this(type, content, properties, context, null);
	}
	
	public AbstractEntityEvent(EventType type, E content, Map<String, Serializable> properties) {
		this(type, content, properties, null, null);
	}

	public AbstractEntityEvent(EventType type, E content) {
		this(type, content, null, null, null);
	}

	@Override
	public EventType getType() {
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E getSource() {
		return (E) super.getSource();
	}
	
	@Override
	public E getContent() {
		return content;
	}
	
	@Override
	public void setContent(E content) {
		Assert.notNull(content, "Content is required!");
		//
		this.content = content;
	}
	
	@Override
	public E getOriginalSource() {
		return originalSource;
	}
	
	@Override
	public void setOriginalSource(E originalSource) {
		this.originalSource = originalSource;
	}
	
	@Override
	public Map<String, Serializable> getProperties() {
		return properties;
	}
	
	@Override
	public EventContext<E> getContext() {
		return context;
	}
	
	@Override
	public boolean isClosed() {
		return context.isClosed();
	}
	
	@Override
	public boolean isSuspended() {
		return context.isSuspended();
	}
	
	@Override
	public Integer getProcessedOrder() {
		return context.getProcessedOrder();
	}
	
	@Override
	@JsonIgnore
    public ResolvableType getResolvableType() {
		ResolvableType result =  ResolvableType.forClassWithGenerics(
				getClass().getSuperclass(),
				ResolvableType.forClass(this.getEventClassType()));
		//
		return result;
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends E> getEventClassType() {
		if (eventClassType != null) {
			return eventClassType;
		} else {
			if (this.content != null) {
				return (Class<? extends E>) this.content.getClass();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		// content cannot be null
		return String.format("%s [type: %s, content: %s, properties: %s]", getClass().getSimpleName(), type, content, properties);
	}
	
	@Override
	public UUID getId() {
		return EntityUtils.toUuid(getProperties().get(EVENT_PROPERTY_EVENT_ID));
	}
	
	@Override
	public void setId(UUID id) {
		getProperties().put(EVENT_PROPERTY_EVENT_ID, id);
	}
	
	@Override
	public UUID getSuperOwnerId() {
		return EntityUtils.toUuid(getProperties().get(EVENT_PROPERTY_SUPER_OWNER_ID));
	}
	
	@Override
	public void setSuperOwnerId(UUID superOwnerId) {
		getProperties().put(EVENT_PROPERTY_SUPER_OWNER_ID, superOwnerId);
	}
	
	@Override
	public UUID getRootId() {
		return EntityUtils.toUuid(getProperties().get(EVENT_PROPERTY_ROOT_EVENT_ID));
	}
	
	@Override
	public void setRootId(UUID rootId) {
		getProperties().put(EVENT_PROPERTY_ROOT_EVENT_ID, rootId);
	}
	
	@Override
	public UUID getParentId() {
		return EntityUtils.toUuid(getProperties().get(EVENT_PROPERTY_PARENT_EVENT_ID));
	}
	
	@Override
	public void setParentId(UUID parentId) {
		getProperties().put(EVENT_PROPERTY_PARENT_EVENT_ID, parentId);
	}
	
	@Override
	public DateTime getExecuteDate() {
		return (DateTime) getProperties().get(EVENT_PROPERTY_EXECUTE_DATE);
	}
	
	@Override
	public void setExecuteDate(DateTime executeDate) {
		getProperties().put(EVENT_PROPERTY_EXECUTE_DATE, executeDate);
	}
	
	@Override
	public PriorityType getPriority() {
		return (PriorityType) getProperties().get(EVENT_PROPERTY_PRIORITY);
	}
	
	@Override
	public void setPriority(PriorityType priority) {
		getProperties().put(EVENT_PROPERTY_PRIORITY, priority);
	}
	
	@Override
	public String getParentType() {
		return (String) getProperties().get(EVENT_PROPERTY_PARENT_EVENT_TYPE);
	}
	
	@Override
	public void setParentType(String parentType) {
		getProperties().put(EVENT_PROPERTY_PARENT_EVENT_TYPE, parentType);
	}
	
	@Override
	public BasePermission[] getPermission() {
		// TODO: PermissionUtils + conversions
		return (BasePermission[]) getProperties().get(EVENT_PROPERTY_PERMISSION);
	}
	
	@Override
	public void setPermission(BasePermission... permission) {
		getProperties().put(EVENT_PROPERTY_PERMISSION, permission);
	}
}
