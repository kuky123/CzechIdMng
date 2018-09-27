package eu.bcvsolutions.idm.core.api.service;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.DataFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity_;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.repository.filter.FilterManager;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizationManager;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Provide additional methods to retrieve DTOs and entities using the pagination
 * and sorting abstraction.
 * 
 * @author Svanda
 * @author Radek Tomiška
 * @see Sort
 * @see Pageable
 * @see Page
 *
 * @param <DTO> dto type for entity type
 * @param <F> filter
 */
public abstract class AbstractReadDtoService<DTO extends BaseDto, E extends BaseEntity, F extends BaseFilter>
		implements ReadDtoService<DTO, F> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractReadDtoService.class);
	//
	@Autowired
	protected ModelMapper modelMapper;
	@Autowired
	private ApplicationContext context;
	@Autowired
	private EntityManager entityManager;
	//
	private final Class<E> entityClass;
	private final Class<F> filterClass;
	private final Class<DTO> dtoClass;
	private final AbstractEntityRepository<E> repository;
	private AuthorizationManager authorizationManager;
	private FilterManager filterManager;
	
	@SuppressWarnings("unchecked")
	public AbstractReadDtoService(AbstractEntityRepository<E> repository) {
		Class<?>[] genericTypes = GenericTypeResolver.resolveTypeArguments(getClass(), AbstractReadDtoService.class);
		entityClass = (Class<E>) genericTypes[1];
		filterClass = (Class<F>) genericTypes[2];
		dtoClass = (Class<DTO>) genericTypes[0];
		//
		Assert.notNull(repository, MessageFormat.format("Repository for class [{0}] is required!", entityClass));
		//
		this.repository = repository;
	}
	
	@Override
	public boolean supports(Class<?> delimiter) {
		return dtoClass.isAssignableFrom(delimiter);
	}

	/**
	 * Returns underlying repository
	 * 
	 * @return
	 */
	protected AbstractEntityRepository<E> getRepository() {
		return repository;
	}

	/**
	 * Returns {@link BaseDto} type class, which is controlled by this service
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class<DTO> getDtoClass() {
		return ((Class<DTO>)this.getDtoClass(null));
	}
	
	/**
	 * Returns {@link BaseDto} type class, which is controlled by this service
	 * @param entity 
	 * 
	 * @return
	 */
	protected Class<? extends DTO> getDtoClass(E entity) {
		return dtoClass;
	}

	/**
	 * Returns {@link BaseEntity} type class, which is controlled by this
	 * service
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Class<E> getEntityClass() {
		return (Class<E>) this.getEntityClass(null);
	}
	
	/**
	 * Returns {@link BaseEntity} type class, which is controlled by this
	 * service
	 * @param dto 
	 * 
	 * @return
	 */
	protected Class<? extends E> getEntityClass(DTO dto) {
		return entityClass;
	}

	/**
	 * Returns {@link BaseFilter} type class, which is controlled by this
	 * service
	 * 
	 * @return
	 */
	@Override
	public Class<F> getFilterClass() {
		return filterClass;
	}

	/**
	 * Returns DTO by given id. Returns null, if DTO is not exists. For
	 * AbstractDto uuid or string could be given.
	 */	
	@Override
	@Transactional(readOnly = true)
	public DTO get(Serializable id, BasePermission... permission) {
		return toDto(getEntity(id, permission));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DTO> find(Pageable pageable, BasePermission... permission) {
		return find(null, pageable, permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<DTO> find(final F filter, Pageable pageable, BasePermission... permission) {
		return toDtoPage(findEntities(filter, pageable, permission));
	}
	
	@Override
	public Page<UUID> findIds(Pageable pageable, BasePermission... permission) {
		return this.findIds(null, pageable, permission);
	}
	
	@Override
	public Page<UUID> findIds(F filter, Pageable pageable, BasePermission... permission) {
		Specification<E> criteria = toCriteria(filter, permission);
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<UUID> cq = criteriaBuilder.createQuery(UUID.class);
		Root<E> root = cq.from(getEntityClass());

		cq.select(root.get(AbstractEntity_.id.getName()));

		Predicate predicate = criteria.toPredicate(root, cq, criteriaBuilder);
		cq.where(predicate);

		// prepare sort
		if (pageable != null && pageable.getSort() != null) {
			List<Order> orders = QueryUtils.toOrders(pageable.getSort(), root, criteriaBuilder);
			cq.orderBy(orders);
		}

		TypedQuery<UUID> query = entityManager.createQuery(cq);

		// if pageable is empty return result
		if (pageable == null) {
			return new PageImpl<UUID>(query.getResultList());
		}
		
		// count query
		CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
		countQuery.where(predicate);
		Long total = entityManager.createQuery(countQuery).getSingleResult();
		
		query.setFirstResult(pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());
		
		List<UUID> content = total > pageable.getOffset() ? query.getResultList() : Collections.<UUID> emptyList();
		
		return new PageImpl<UUID>(content, pageable, total);
	}

	@Transactional(readOnly = true)
	public long count(final F filter, BasePermission... permission) {
		return getRepository().count(toCriteria(filter, permission));
	}
	
	/**
	 * Supposed to be overriden - use super.toPredicates to transform default DataFilter props. 
	 * Transforms given filter to jpa predicate, never returns null.
	 * 
	 * @param root
	 * @param query
	 * @param builder
	 * @param filter
	 * @return
	 */
	protected List<Predicate> toPredicates(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder builder, F filter) {
		if (filter instanceof DataFilter) {
			return getFilterManager().toPredicates(root, query, builder, (DataFilter) filter);
		}
		return new ArrayList<>();
	}

	/**
	 * Returns entity by given id. Returns null, if entity is not exists. For
	 * AbstractEntity uuid or uuid as string could be given.
	 */
	protected E getEntity(Serializable id, BasePermission... permission) {
		if (id instanceof Identifiable) {
			// dto or entity could be given
			id = ((Identifiable) id).getId();
		}
		//
		if (AbstractEntity.class.isAssignableFrom(getEntityClass()) && (id instanceof String)) {
			// workflow / rest usage with string uuid variant
			// EL does not recognize two methods with the same name and
			// different argument type
			try {
				return checkAccess(getRepository().findOne(UUID.fromString((String) id)), permission);
			} catch (IllegalArgumentException ex) {
				// simply not found
				return null;
			}
		}
		//
		E entity = getRepository().findOne((UUID) id);
		return checkAccess(entity, permission);
	}
	
	protected Page<E> findEntities(Pageable pageable, BasePermission... permission) {
		return findEntities(null, pageable, permission);
	}

	protected Page<E> findEntities(F filter, Pageable pageable, BasePermission... permission) {
		LOG.trace("Find entities for the filter [{}] with pageable [{}] starts", filter != null, pageable != null);
		//
		Page<E> entities = getRepository().findAll(toCriteria(filter, permission), pageable);
		//
		LOG.trace("Found entities [{}].", entities.getTotalElements());
		return entities;
	}
	
	/**
	 * Constructs find / count jpa criteria from given filter and permissions
	 * 
	 * @param filter
	 * @param permission
	 * @return
	 */
	protected Specification<E> toCriteria(F filter, BasePermission... permission) {
		return new Specification<E>() {
			public Predicate toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				List<Predicate> predicates = new ArrayList<>();
				//
				// if filter is null, no filter predicates will be built
				if (filter != null) {
					predicates.addAll(AbstractReadDtoService.this.toPredicates(root, query, builder, filter));
				}
				//
				// permisions are not evaluated, if no permission was given or authorizable type is null (=> authorization policies are not supported)
				BasePermission[] permissions = PermissionUtils.trimNull(permission);
				if (!ObjectUtils.isEmpty(permissions) && (AbstractReadDtoService.this instanceof AuthorizableService)) {					
					AuthorizableType authorizableType = ((AuthorizableService<?>) AbstractReadDtoService.this).getAuthorizableType();
					if (authorizableType != null && authorizableType.getType() != null) {					
						predicates.add(getAuthorizationManager().getPredicate(root, query, builder, permissions));
					}
				}
				//
				return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
			}
		};
	}
	
	@Override
	public boolean isNew(DTO dto) {
		Assert.notNull(dto);
		//
		return dto.getId() == null || !getRepository().exists((UUID) dto.getId());
	}
	
	
	/**
	 * Returns, what currently logged identity can do with given dto
	 * 
	 * @param id
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public Set<String> getPermissions(Serializable id) {
		E entity = getEntity(id);
		Assert.notNull(entity);
		//
		return getPermissions(entity);
	}
	
	@Override
	public Set<String> getPermissions(DTO dto) {
		E entity = toEntity(dto); // TODO: read entity?
		//
		return getPermissions(entity);
	}
	
	protected Set<String> getPermissions(E entity) {
		Assert.notNull(entity);
		//
		return getAuthorizationManager().getPermissions(entity);
	}

	/**
	 * 
	 * Converts entity to DTO
	 * 
	 * @param entity
	 * @return
	 */
	protected DTO toDto(E entity) {
		return toDto(entity, null);
	}

	/**
	 * Converts entity to DTO
	 * 
	 * @see Embedded
	 * @param entity
	 * @param dto
	 *            if is not null, then will be use as input to convert
	 * @return
	 */
	protected DTO toDto(E entity, DTO dto) {
		if (entity == null) {
			return null;
		}
		if (dto == null) {
			return modelMapper.map(entity, this.getDtoClass(entity));
		}
		modelMapper.map(entity, dto);
		return dto;
	}

	/**
	 * Converts list of entities wrapped to Page object to list of DTOs wrapped
	 * to Page object.
	 * 
	 * @param entityPage
	 * @return
	 */
	protected Page<DTO> toDtoPage(Page<E> entityPage) {
		List<DTO> dtos = this.toDtos(entityPage.getContent(), true);
		PageRequest pageRequest = null;
		if (entityPage.getSize() > 0) {
			pageRequest = new PageRequest(entityPage.getNumber(), entityPage.getSize(), entityPage.getSort());
		}
		Page<DTO> dtoPage = new PageImpl<>(dtos, pageRequest, entityPage.getTotalElements());
		return dtoPage;
	}

	/**
	 * Converts list of entities to list of DTOs
	 * 
	 * @param entities
	 * @param trimmed
	 * @return
	 */
	protected List<DTO> toDtos(List<E> entities, boolean trimmed) {
		if (entities == null) {
			return null;
		}
		List<DTO> dtos = new ArrayList<>();
		entities.forEach(entity -> {
			try {
				DTO newDto = this.getDtoClass(entity).newInstance();
				if (newDto instanceof AbstractDto) {
					((AbstractDto) newDto).setTrimmed(trimmed);
				}
				dtos.add(this.toDto(entity, newDto));
			} catch (InstantiationException | IllegalAccessException e) {
				throw new CoreException(e);
			}
		});
		return dtos;
	}
	
	/**
	 * Converts given DTO to entity
	 *
	 * @param dto
	 * @return
	 */
	protected E toEntity(DTO dto) {
		return toEntity(dto, null);
	}

	/**
	 * Converts DTO to entity
	 * 
	 * @see Embedded
	 * @param entity
	 *            if is not null, then will be use as input to convert
	 * @param dto
	 * @return
	 */
	protected E toEntity(DTO dto, E entity) {
		if (dto == null) {
			return null;
		}
		if (entity != null) {
			modelMapper.map(dto, entity);
			return entity;
		}
		return modelMapper.map(dto, getEntityClass(dto));
	}
	
	@Override
	public DTO checkAccess(DTO dto, BasePermission... permission) {
		checkAccess(toEntity(dto, null), permission);
		//
		return dto;
	}
	
	/**
	 * Evaluates authorization permission on given entity.
	 *  
	 * @param dto
	 * @param permission base permissions to evaluate (all permission needed)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	protected E checkAccess(E entity, BasePermission... permission) {
		if (entity == null) {
			// nothing to check
			return null;
		}
		//
		if (this instanceof AuthorizableService) {
			AuthorizableType authorizableType = ((AuthorizableService<?>) AbstractReadDtoService.this).getAuthorizableType();
			if (authorizableType != null && authorizableType.getType() != null) {
				BasePermission[] permissions = PermissionUtils.trimNull(permission);
				if (!ObjectUtils.isEmpty(permissions) && !getAuthorizationManager().evaluate(entity, permissions)) {
					throw new ForbiddenEntityException(entity.getId(), permissions);
				}
			}
		}
		return entity;
	}
	
	/**
	 * Returns authorization manager
	 * 
	 * @return
	 */
	protected AuthorizationManager getAuthorizationManager() {
		if (authorizationManager == null) {
			authorizationManager = context.getBean(AuthorizationManager.class);
		}
		return authorizationManager;
	}
	
	protected FilterManager getFilterManager() {
		if (filterManager == null) {
			filterManager = context.getBean(FilterManager.class);
		}
		return filterManager;
	}
	
	protected void setModelMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}
	
	/**
	 * Return {@link Pageable} to find all sorted records.
	 * 
	 * @param sort
	 * @return
	 */
	protected Pageable getPageableAll(Sort sort) {
		return new PageRequest(0, Integer.MAX_VALUE, sort);
	}
}
