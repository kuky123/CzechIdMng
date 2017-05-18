package eu.bcvsolutions.idm.core.model.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import eu.bcvsolutions.idm.core.api.rest.lookup.CodeableServiceEntityLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DefaultDtoLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DefaultEntityLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.DtoLookup;
import eu.bcvsolutions.idm.core.api.rest.lookup.EntityLookup;
import eu.bcvsolutions.idm.core.api.service.CodeableService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;
import eu.bcvsolutions.idm.core.api.service.ReadEntityService;

/**
 * Provide entity services through whole application. 
 * Support for loading {@link BaseEntity} by identifier.
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
	public BaseEntity lookupEntity(Class<? extends Identifiable> identifiableType, Serializable entityId) { // vracim entitu  - class muze by entita i dto
		EntityLookup<BaseEntity> lookup = getEntityLookup(identifiableType);
		if (lookup == null) {
			throw new IllegalArgumentException(String.format("Entity lookup for identifiable type [%s] is not supported", identifiableType));
		}
		return lookup.lookup(entityId);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public BaseDto lookupDto(Class<? extends Identifiable> identifiableType, Serializable entityId) { // vracim entitu  - class muze by entita i dto
		DtoLookup<BaseDto> lookup = getDtoLookup(identifiableType);
		if (lookup == null) {
			throw new IllegalArgumentException(String.format("Dto lookup for identifiable type [%s] is not supported", identifiableType));
		}
		return lookup.lookup(entityId);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <E extends BaseEntity> ReadEntityService<E, ?> getEntityService(Class<E> entityClass) {
		return (ReadEntityService<E, ?>) getService(entityClass);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <E extends BaseEntity, S extends ReadEntityService<E, ?>> S getEntityService(Class<E> entityClass, Class<S> entityServiceClass) {
		return (S) getService(entityClass);
	}
	
	@Override
	public ReadDtoService<?, ?> getDtoService(Class<? extends Identifiable> identifiableType) {
		Object service = getService(identifiableType);
		if (service == null) {
			LOG.debug("ReadDtoService for identifiable type [{}] is not found", identifiableType);
		}
		//
		if (service instanceof ReadDtoService) {
			return (ReadDtoService<?, ?>) service;
		}
		LOG.debug("Service for identifiable type [{}] is not ReadDtoService, type [{}] ", identifiableType, service.getClass().getCanonicalName());
		return null;
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	public <I extends BaseEntity> EntityLookup<I> getEntityLookup(Class<? extends Identifiable> identifiableType) {			
		Class<I> entityClass = (Class<I>) getEntityClass(identifiableType);
		if (entityClass == null) {
			LOG.debug("Service for identifiable type [{}] is not found, lookup not found", identifiableType);
			return null;
		}
		//
		EntityLookup<I> lookup = (EntityLookup<I>) entityLookups.getPluginFor(entityClass);
		if (lookup == null) {
			Object service = getService(identifiableType);
			if ((service instanceof ReadEntityService) && (service instanceof CodeableService)) {
				return new CodeableServiceEntityLookup<I>((CodeableService)service);
			}
			return new DefaultEntityLookup<I>(entityManager, entityClass);
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
	
	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	private Class<? extends BaseEntity> getEntityClass(Class<? extends Identifiable> identifiableType) {
		Object service = getService(identifiableType);
		if (service == null) {
			return null;
		}
		//
		if (service instanceof ReadDtoService) {
			return ((ReadDtoService) service).getEntityClass();
		}		
		return ((ReadEntityService) service).getEntityClass();
	}
	
	/**
	 * Returs service for given {@link Identifiable} type.
	 * 
	 * @param identifiableType
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private Object getService(Class<? extends Identifiable> identifiableType) {
		if (!services.containsKey(identifiableType)) {
			context.getBeansOfType(ReadEntityService.class).values().forEach(s -> {
				services.put(s.getEntityClass(), s);
			});
			context.getBeansOfType(ReadDtoService.class).values().forEach(s -> {
				services.put(s.getEntityClass(), s);
				services.put(s.getDtoClass(), s);
			});
		}
		return services.get(identifiableType);
	}
}