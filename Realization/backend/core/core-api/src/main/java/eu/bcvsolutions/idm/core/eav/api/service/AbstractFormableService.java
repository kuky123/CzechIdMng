package eu.bcvsolutions.idm.core.eav.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.FormableDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Abstract implementation for generic CRUD operations on a repository for a specific type with extended attributes. 
 * Owner type has to support event processing.
 * 
 * @author Radek Tomiška
 *
 * @see FormableSaveProcessor - saves eav values, after owner's dto is saved
 * @param <DTO> {@link FormableDto} type which supports extended attributes
 * @param <E> {@link FormableEntity} type which supports extended attributes
 * @param <F> {@link BaseFilter} type
 */
public abstract class AbstractFormableService<DTO extends FormableDto, E extends FormableEntity, F extends BaseFilter> 
		extends AbstractEventableDtoService<DTO, E, F> {

	private final FormService formService;
	//
	@Autowired private LookupService lookupService;
	
	@Autowired
	public AbstractFormableService(
			AbstractEntityRepository<E> repository,
			EntityEventManager entityEventManager,
			FormService formService) {
		super(repository, entityEventManager);
		//
		Assert.notNull(formService);
		//
		this.formService = formService;
	}
	
	@Override
	@Transactional
	public EventContext<DTO> publish(EntityEvent<DTO> event, EntityEvent<?> parentEvent, BasePermission... permission) {
		// check access to filled form values
		// access has to be evaluated before event is published - is not available in save internal method
		BasePermission[] permissions = PermissionUtils.trimNull(permission);
		if (!ObjectUtils.isEmpty(permissions)) {
			FormableEntity owner = getOwner(event.getContent());
			FormValueService<FormableEntity> formValueService = formService.getFormValueService(getDtoClass());
			event.getContent().getEavs().forEach(formInstance -> {
				formInstance.getValues().forEach(formValue -> {
					formValue.setOwner(owner); //  set owner is needed for checking access on new values
					formValueService.checkAccess(formValue, IdmBasePermission.UPDATE); // UPDATE is enough for all CUD
				});
			});
		}
		//
		return super.publish(event, parentEvent, permission);
	}
	
	public DTO saveInternal(DTO dto) {
		final DTO savedDto = super.saveInternal(dto);
		//
		// prevent to lose filled eav values - they are saved after owner is saved (see FormableSaveProcessor).
		// filled values are saved in FormableSaveProcessor - parent event is propagated, when form instance is saved
		savedDto.setEavs(dto.getEavs());
		//
		return savedDto;
	}
	
	/**
	 * Deletes a given entity with all extended attributes
	 * 
	 * @param entity
	 * @throws IllegalArgumentException in case the given entity is {@literal null}.
	 */
	@Override
	@Transactional
	public void deleteInternal(DTO dto) {
		formService.deleteValues(dto);
		//
		super.deleteInternal(dto);
	}
	
	/**
	 * Returns form service
	 * 
	 * @return
	 */
	protected FormService getFormService() {
		return formService;
	}
	
	/**
	 * Prepares new owner instance
	 * 
	 * TODO: move to form service, should be in api?
	 * 
	 * @param formDefinition
	 * @return
	 */
	private FormableEntity getOwner(DTO dto) {
		Assert.notNull(dto);
		//
		FormableEntity formableEntity = null;
		if (dto.getId() != null) {
			formableEntity = (FormableEntity) lookupService.lookupEntity(dto.getClass(), dto.getId());
		}
		// prepare empty owner
		if (formableEntity == null) {
			try {
				formableEntity = (FormableEntity) lookupService.getEntityClass(dto.getClass()).newInstance();
				// FIXME: #978 - map dto to entity. Some evaluator could intercept something else than class and identifier ...
				formableEntity.setId(dto.getId());
			} catch (InstantiationException | IllegalAccessException ex) {
				throw new ResultCodeException(CoreResultCode.BAD_VALUE, ImmutableMap.of("identifiableType", dto.getClass()), ex);
			}
		}
		//
		return formableEntity;
	}
	
}
