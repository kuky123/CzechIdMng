package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.rest.lookup.CodeableDtoLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DefaultDtoLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DefaultEntityLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DtoLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.EntityLookup;
import eu.bcvsolutions.idm.core.api.service.CodeableService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;

/**
 * Provide entity services through whole application. 
 * Support for loading {@link BaseDto} and {@link BaseEntity} by identifier.
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultLookupService implements LookupService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultLookupService.class);
	private final ApplicationContext context;
	private final EntityManager entityManager;
	private final PluginRegistry<EntityLookup<?>, Class<?>> entityLookups;
	private final PluginRegistry<DtoLookup<?>, Class<?>> dtoLookups;
	// loaded services cache
	private final Map<Class<? extends Identifiable>, Object> services = new HashMap<>();
	
	@Autowired
	public DefaultLookupService(
			ApplicationContext context,
			EntityManager entityManager,
			List<? extends EntityLookup<?>> entityLookups,
			List<? extends DtoLookup<?>> dtoLookups) {
		Assert.notNull(context);
		Assert.notNull(entityManager);
		Assert.notNull(entityLookups, "Entity lookups are required");
		Assert.notNull(dtoLookups, "Dto lookups are required");
		//
		this.context = context;
		this.entityManager = entityManager;
		this.entityLookups = OrderAwarePluginRegistry.create(entityLookups);
		this.dtoLookups = OrderAwarePluginRegistry.create(dtoLookups);
	}
	
	@Override
	public BaseEntity lookupEntity(Class<? extends Identifiable> identifiableType, Serializable entityId) {
		EntityLookup<BaseEntity> lookup = getEntityLookup(identifiableType);
		if (lookup == null) {
			throw new IllegalArgumentException(String.format("Entity lookup for identifiable type [%s] is not supported", identifiableType));
		}
		BaseEntity entity = lookup.lookup(entityId);
		//
		LOG.trace("Identifiable type [{}] with identifier [{}] found [{}]", identifiableType, entityId, entity != null);
		return entity;
		
	}
	
	@Override
	public BaseDto lookupDto(Class<? extends Identifiable> identifiableType, Serializable entityId) {
		DtoLookup<BaseDto> lookup = getDtoLookup(identifiableType);
		if (lookup == null) {
			throw new IllegalArgumentException(String.format("Dto lookup for identifiable type [%s] is not supported", identifiableType));
		}
		BaseDto dto = lookup.lookup(entityId);
		//
		LOG.trace("Identifiable type [{}] with identifier [{}] found [{}]", identifiableType, entityId, dto != null);
		return dto;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public BaseDto lookupDto(String identifiableType, Serializable entityId) {
		try {
			return lookupDto((Class<? extends Identifiable>) Class.forName(identifiableType), entityId);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(String.format("Dto lookup for identifiable type [%s] is not supported", identifiableType), ex);
		}
	}
	
	@Override
	public ReadDtoService<?, ?> getDtoService(Class<? extends Identifiable> identifiableType) {
		Object service = getService(identifiableType);
		if (service == null) {
			LOG.debug("ReadDtoService for identifiable type [{}] is not found", identifiableType);
			return null;
		}
		//
		if (service instanceof ReadDtoService) {
			return (ReadDtoService<?, ?>) service;
		}
		LOG.debug("Service for identifiable type [{}] is not ReadDtoService, current type [{}] ", identifiableType, service.getClass().getCanonicalName());
		return null;
	}
	
	@Override
	@SuppressWarnings({ "unchecked" })
	public <E extends BaseEntity> EntityLookup<E> getEntityLookup(Class<? extends Identifiable> identifiableType) {			
		Class<E> entityClass = (Class<E>) getEntityClass(identifiableType);
		if (entityClass == null) {
			LOG.debug("Service for identifiable type [{}] is not found, lookup not found", identifiableType);
			return null;
		}
		//
		EntityLookup<E> lookup = (EntityLookup<E>) entityLookups.getPluginFor(entityClass);
		if (lookup == null) {
			return new DefaultEntityLookup<E>(entityManager, entityClass, getDtoLookup(identifiableType));
		}
		return lookup;	
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <I extends BaseDto> DtoLookup<I> getDtoLookup(Class<? extends Identifiable> identifiableType) {			
		ReadDtoService service = getDtoService(identifiableType);
		if (service == null) {
			LOG.debug("Service for identifiable type [{}] is not found, lookup not found.", identifiableType);
			return null;
		}
		//
		DtoLookup<I> lookup = (DtoLookup<I>) dtoLookups.getPluginFor(service.getDtoClass());
		if (lookup == null) {
			if (service instanceof CodeableService) {
				return new CodeableDtoLookup<I>((CodeableService<I>) service);
			}
			return new DefaultDtoLookup<I>(service);
		}
		return lookup;
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes"})
	public Class<? extends BaseEntity> getEntityClass(Class<? extends Identifiable> identifiableType) {
		Assert.notNull(identifiableType, "Identifiable type is required!");
		//
		// given identifiable type is already entity class
		if (BaseEntity.class.isAssignableFrom(identifiableType)) {
			return (Class<? extends BaseEntity>) identifiableType;
		}
		//
		// try to find entity class by dto class
		Object service = getService(identifiableType);
		if (service == null) {
			return null;
		}
		return ((ReadDtoService) service).getEntityClass();
	}
	
	@Override
	public UUID getOwnerId(Identifiable owner) {
		Assert.notNull(owner);
		if (owner.getId() == null) {
			return null;
		}		
		Assert.isInstanceOf(UUID.class, owner.getId(), "Entity with UUID identifier is supported as owner for some related entity.");
		//
		return (UUID) owner.getId();
	}
	
	@Override
	public String getOwnerType(Identifiable owner) {
		Assert.notNull(owner);
		//
		return getOwnerType(owner.getClass());
	}
	
	@Override
	public String getOwnerType(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType);
		//
		// dto class was given
		Class<? extends BaseEntity> ownerEntityType = getOwnerClass(ownerType);
		if (ownerEntityType == null) {
			throw new IllegalArgumentException(String.format("Owner type [%s] has to generalize [BaseEntity]", ownerType));
		}
		return ownerEntityType.getCanonicalName();
	}
	
	/**
	 * Returns service for given {@link Identifiable} type.
	 * 
	 * @param identifiableType
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private Object getService(Class<? extends Identifiable> identifiableType) {
		if (!services.containsKey(identifiableType)) {
			context.getBeansOfType(ReadDtoService.class).values().forEach(s -> {
				services.put(s.getEntityClass(), s);
				services.put(s.getDtoClass(), s);
			});
		}
		return services.get(identifiableType);
	}
	
	/**
	 * Returns {@link BaseEntity} class. Owner type has to be entity class - dto class can be given.
	 * 
	 * @param ownerType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends BaseEntity> getOwnerClass(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType, "Owner type is required!");
		// formable entity class was given
		if (BaseEntity.class.isAssignableFrom(ownerType)) {
			return (Class<? extends BaseEntity>) ownerType;
		}
		// dto class was given
		Class<?> ownerEntityType = getEntityClass(ownerType);
		if (ownerEntityType == null) {
			return null;
		}
		if (BaseEntity.class.isAssignableFrom(ownerEntityType)) {
			return (Class<? extends BaseEntity>) ownerEntityType;
		}
		return null;
	}
}
