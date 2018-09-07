package eu.bcvsolutions.idm.core.eav.service.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationClass;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationClassProperty;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormDefinitionFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormValueFilter;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.FormValueService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute_;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition_;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Work with form definitions, attributes and their values
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultFormService implements FormService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultFormService.class);
	//
	private final IdmFormDefinitionService formDefinitionService;
	private final IdmFormAttributeService formAttributeService;
	private final PluginRegistry<FormValueService<?>, Class<?>> formValueServices;
	private final EntityEventManager entityEventManager;
	private final LookupService lookupService;

	@Autowired
	public DefaultFormService(
			IdmFormDefinitionService formDefinitionService,
			IdmFormAttributeService formAttributeService,
			List<? extends FormValueService<?>> formValueServices,
			EntityEventManager entityEventManager,
			LookupService lookupService) {
		Assert.notNull(formDefinitionService);
		Assert.notNull(formAttributeService);
		Assert.notNull(formValueServices);
		Assert.notNull(entityEventManager);
		Assert.notNull(lookupService);
		//
		this.formDefinitionService = formDefinitionService;
		this.formAttributeService = formAttributeService;
		this.formValueServices = OrderAwarePluginRegistry.create(formValueServices);
		this.entityEventManager = entityEventManager;
		this.lookupService = lookupService;
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(String type, BasePermission... permission) {
		return this.getDefinition(type, null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(String type, BasePermission... permission) {
		IdmFormDefinitionFilter filter = new IdmFormDefinitionFilter();
		filter.setType(type);
		//
		return formDefinitionService
				.find(
					filter,
					new PageRequest(0, Integer.MAX_VALUE, new Sort(IdmFormDefinition_.code.getName())),
					permission)
				.getContent();
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(String type, String code, BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null; 
		if (StringUtils.isEmpty(code)) {
			formDefinition = formDefinitionService.findOneByMain(type);
		} else {
			formDefinition = formDefinitionService.findOneByTypeAndCode(type, code);
		}
		return formDefinitionService.checkAccess(formDefinition, permission);	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getDefinition(ownerType, null, permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(Identifiable owner, BasePermission... permission) {
		Assert.notNull(owner);
		//
		return getDefinitions(owner.getClass(), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getDefinitions(getDefaultDefinitionType(ownerType), permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, String code, BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required!");
		//
		return getDefinition(getDefaultDefinitionType(ownerType), code, permission);
	}

	/**
	 * Return default form definition, if no definition was given. Returns given
	 * definition otherwise.
	 * 
	 * @param ownerType
	 * @param formDefinitionId
	 * @return
	 * @throws IllegalArgumentException
	 *             if default definition does not exist and no definition was given.
	 */
	private UUID checkDefaultDefinition(Class<? extends Identifiable> ownerType, UUID formDefinitionId) {
		if (formDefinitionId != null) {
			return formDefinitionId;
		}
		// values from default form definition
		return checkDefaultDefinition(ownerType, (IdmFormDefinitionDto) null).getId();
	}

	/**
	 * Return default form definition, if no definition was given. Returns given
	 * definition otherwise.
	 * 
	 * @param ownerType
	 * @param formDefinition
	 * @return
	 * @throws IllegalArgumentException if default definition does not exist and no definition was given.
	 */
	private IdmFormDefinitionDto checkDefaultDefinition(
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition) {
		if (formDefinition == null) {
			// values from default form definition
			formDefinition = getDefinition(ownerType);
			Assert.notNull(formDefinition, MessageFormat.format(
					"Default form definition for ownerType [{0}] not found and is required, fix application configuration!",
					ownerType));
		}
		return formDefinition;
	}

	@Override
	public String getDefaultDefinitionType(Class<? extends Identifiable> ownerType) {
		return formDefinitionService.getOwnerType(ownerType);
	}

	@Override
	public boolean isFormable(Class<? extends Identifiable> ownerType) {
		return formDefinitionService.isFormable(ownerType);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(Class<? extends Identifiable> ownerType, String attributeCode, BasePermission... permission) {
		return getAttribute(ownerType, null, attributeCode, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(
			Class<? extends Identifiable> ownerType,
			String definitionCode,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required!");
		Assert.hasLength(attributeCode, "Form attribute code is required!");
		//
		IdmFormDefinitionDto definition = getDefinition(ownerType, definitionCode);
		Assert.notNull(definition, "Definition is required!");
		//
		return formAttributeService.findAttribute(
				getDefaultDefinitionType(ownerType), 
				definition.getCode(),
				attributeCode,
				permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(formDefinition, "Definition is required!");
		Assert.hasLength(attributeCode, "Form attribute code is required!");
		//
		return formAttributeService.findAttribute(
				formDefinition.getType(),
				formDefinition.getCode(),
				attributeCode,
				permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormAttributeDto> getAttributes(IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(formDefinition);
		//
		IdmFormAttributeFilter filter = new IdmFormAttributeFilter();
		filter.setDefinitionType(formDefinition.getType());
		filter.setDefinitionCode(formDefinition.getCode());
		//
		return formAttributeService.find(filter, new PageRequest(0, Integer.MAX_VALUE, new Sort(IdmFormAttribute_.seq.getName())), permission).getContent();
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto saveDefinition(IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		return formDefinitionService.save(formDefinition, permission);
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			String type,
			String code,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		Assert.hasLength(type);
		//
		// create definition
		IdmFormDefinitionDto formDefinition = new IdmFormDefinitionDto();
		formDefinition.setType(type);
		formDefinition.setCode(code);
		formDefinition = formDefinitionService.save(formDefinition, permission);
		//
		// and their attributes
		if (formAttributes != null) {
			Short seq = 0;
			for (IdmFormAttributeDto formAttribute : formAttributes) {
				// default attribute order
				if (formAttribute.getSeq() == null) {
					formAttribute.setSeq(seq);
					seq++;
				}
				formAttribute.setFormDefinition(formDefinition.getId());
				formAttribute = formAttributeService.save(formAttribute, permission);
			}
			formDefinition = formDefinitionService.get(formDefinition);
		}
		return formDefinition;
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			Class<? extends Identifiable> ownerType,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required!");
		//
		return createDefinition(getDefaultDefinitionType(ownerType), null, formAttributes, permission);
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			Class<? extends Identifiable> ownerType,
			String name,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required!");
		//
		return createDefinition(getDefaultDefinitionType(ownerType), name, formAttributes, permission);
	}

	@Override
	@Transactional
	public IdmFormAttributeDto saveAttribute(IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(attribute);
		Assert.notNull(attribute.getFormDefinition(),
				String.format("Form definition for attribute [%s] is required!", attribute.getCode()));
		//
		return formAttributeService.save(attribute, permission);
	}

	@Override
	@Transactional
	public IdmFormAttributeDto saveAttribute(Class<? extends Identifiable> ownerType, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(attribute);
		attribute.setFormDefinition(checkDefaultDefinition(ownerType, attribute.getFormDefinition()));
		//
		return saveAttribute(attribute, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		return saveFormInstance(owner, formDefinition, values, permission).getValues();
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			UUID formDefinitionId,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null;
		if (formDefinitionId != null) {
			formDefinition = formDefinitionService.get(formDefinitionId);
			if (formDefinition == null) {
				throw new ResultCodeException(CoreResultCode.NOT_FOUND,
						ImmutableMap.of("formDefinition", formDefinitionId));
			}
		}
		return saveFormInstance(owner, formDefinition, values, permission).getValues();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Only given form attributes by the given values will be saved. Other attributes will be left untouched.
	 * 
	 * TODO: validations by given form definition? I don't think, it will not be
	 * useful in synchronization etc. - only FE validations will be enough ...
	 */
	@Override
	@Transactional
	public IdmFormInstanceDto saveFormInstance(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> newValues,
			BasePermission... permission) {
		FormableEntity ownerEntity = getOwnerEntity(owner);
		Assert.notNull(ownerEntity, "Form values owner is required!");
		formDefinition = checkDefaultDefinition(ownerEntity.getClass(), formDefinition);
		IdmFormInstanceDto formInstance = new IdmFormInstanceDto(ownerEntity, formDefinition, newValues);
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		//
		Map<UUID, Map<UUID, IdmFormValueDto>> previousValues = new HashMap<>(); // values by attributes
		formValueService.getValues(ownerEntity, formDefinition).forEach(formValue -> {
			if (!previousValues.containsKey(formValue.getFormAttribute())) {
				previousValues.put(formValue.getFormAttribute(), new LinkedHashMap<>()); // sort by seq
			}
			previousValues.get(formValue.getFormAttribute()).put(formValue.getId(), formValue);
		});
		//
		List<IdmFormValueDto> results = new ArrayList<>();
		for (Entry<String, List<IdmFormValueDto>> attributeEntry : formInstance.toValueMap().entrySet()) {
			IdmFormAttributeDto attribute = formInstance.getMappedAttributeByCode(attributeEntry.getKey());
			List<IdmFormValueDto> attributePreviousValues = new ArrayList<>();
			if (previousValues.containsKey(attribute.getId())) {
				attributePreviousValues.addAll(previousValues.get(attribute.getId()).values());
			}
			results.addAll(
					saveAttributeValues(
						ownerEntity, 
						attribute, 
						attributePreviousValues,
						attributeEntry.getValue(),
						permission));
		}
		//
		// publish event - eav was saved
		entityEventManager.process(new CoreEvent<>(CoreEventType.EAV_SAVE, lookupService.lookupDto(ownerEntity.getClass(), ownerEntity.getId())));
		//
		return new IdmFormInstanceDto(ownerEntity, formDefinition, results);
	}
	
	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.notNull(attribute, "Form attribute definition is required!");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		//
		// get previous (old) values
		List<IdmFormValueDto> previousValues = formValueService.getValues(ownerEntity, attribute);
		// prepare new values
		List<IdmFormValueDto> newValues = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(persistentValues)) {
			for (short seq = 0; seq < persistentValues.size(); seq++) {
				IdmFormValueDto newValue = new IdmFormValueDto();
				newValue.setOwnerAndAttribute(ownerEntity, attribute);
				newValue.setValue(persistentValues.get(seq));
				newValue.setSeq(seq);
				newValues.add(newValue);
			}
		}
		return saveAttributeValues(ownerEntity, attribute, previousValues, newValues, permission);
	}
	
	/**
	 * Save single attribute values
	 * 
	 * @param ownerEntity
	 * @param attribute
	 * @param previousValues
	 * @param newValues
	 * @param permission
	 * @return
	 */
	private List<IdmFormValueDto> saveAttributeValues(
			FormableEntity ownerEntity,
			IdmFormAttributeDto attribute,
			List<IdmFormValueDto> previousValues,
			List<IdmFormValueDto> newValues,
			BasePermission... permission) {
		//
		IdmFormDefinitionDto formDefinition = formDefinitionService.get(attribute.getFormDefinition());
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		List<IdmFormValueDto> results = new ArrayList<>();
		Map<UUID, IdmFormValueDto> unprocessedPreviousValues = new LinkedHashMap<>(); // ordered by seq
		if (CollectionUtils.isNotEmpty(previousValues)) {
			previousValues.forEach(previousValue -> {
				unprocessedPreviousValues.put(previousValue.getId(), previousValue);
			});
		}
		//
		if (newValues == null || newValues.isEmpty()) {
			// confidential values has to removed directly, they could not be sent with form (only changed values)
			if (!attribute.isConfidential()) {
				// delete previous attributes
				unprocessedPreviousValues.values().forEach(value -> {
					formValueService.delete(value, permission);
					LOG.trace("FormValue [{}:{}] for owner [{}] was deleted", value.getFormAttribute(), value.getId(), ownerEntity);
				});
			}
			return results;
		}
		//
		if (!attribute.isMultiple() && newValues.size() > 1) {
			throw new IllegalArgumentException(
					MessageFormat.format("Form attribute [{0}:{1}] does not support multivalue, sent [{2}] values.",
							formDefinition.getCode(), attribute.getCode(), newValues.size()));
		}
		//
		// compare values
		IdmFormValueDto[] sortedPreviousValues = resolvePreviousValues(unprocessedPreviousValues, newValues);
		for (short index = 0; index < newValues.size(); index++) {
			IdmFormValueDto previousValue = sortedPreviousValues[index];
			IdmFormValueDto newValue = newValues.get(index);			
			newValue.setOwnerAndAttribute(ownerEntity, attribute);
			newValue.setSeq(index);
			//
			if (previousValue == null) {
				if (!newValue.isNull()) { // null values are not saved
					results.add(formValueService.save(newValue, permission));
					LOG.trace("FormValue [{}:{}] for owner [{}] was created", attribute.getCode(), newValue.getId(), ownerEntity);
				}
			} else {
				//
				// we using filled value only and set her into previous value => value id is preserved 
				// the same value should not be updated
				// confidential value is always updated - only new values are sent from client
				if (newValue.isConfidential() || !previousValue.isEquals(newValue)) {
					// set value for the previous value
					previousValue.setValue(newValue.getValue());
					// attribute persistent type could be changed
					previousValue.setOwnerAndAttribute(ownerEntity, attribute);
					previousValue.setSeq(index);
					if (!previousValue.isNull()) { // null values are not saved
						results.add(formValueService.save(previousValue, permission));
						LOG.trace("FormValue [{}:{}] for owner [{}] was updated", attribute.getCode(), previousValue.getId(), ownerEntity);
					} else {
						formValueService.delete(previousValue, permission);
						LOG.trace("FormValue [{}:{}] for owner [{}] was deleted", attribute.getCode(), previousValue.getId(), ownerEntity);
					}
				}
			}
		}
		// remove unprocessed values
		// confidential property will be removed too => none or all confidential values have to be given for multiple attributes
		unprocessedPreviousValues
			.values()
			.forEach(previousValue -> {
				formValueService.delete(previousValue, permission);
				LOG.trace("FormValue [{}:{}] for owner [{}] was deleted", attribute.getCode(), previousValue.getId(), ownerEntity);
			});

		return results;
	}
	
	/**
	 * Returns previous values sorted accordingly to given new values => previous(index) ~ new(index).
	 * Returned array length is the same as size of list with new values.
	 * if previous value is not found, then position in array will be null.
	 * 
	 * Values are paired by:
	 * - 1. by id (if new value has id)
	 * - 2. by value
	 * - 3. by seq
	 * - 4. use remaining unprocessed values
	 * 
	 * @param unprocessedPreviousValues if value is resolved, value is removed from this map (=> processed)
	 * @param newValue
	 * @return
	 */
	protected IdmFormValueDto[] resolvePreviousValues(Map<UUID, IdmFormValueDto> unprocessedPreviousValues, List<IdmFormValueDto> newValues) {
		IdmFormValueDto[] sortedPreviousValues = new IdmFormValueDto[newValues.size()];
		// by id - highest priority
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			IdmFormValueDto newValue = newValues.get(index);
			if (newValue.getId() != null && unprocessedPreviousValues.containsKey(newValue.getId())) {
				sortedPreviousValues[index] = unprocessedPreviousValues.remove(newValue.getId());
			}
		}
		// by value
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			if (sortedPreviousValues[index] != null) {
				// mapped by previous iteration
				continue;
			}
			IdmFormValueDto newValue = newValues.get(index);
			IdmFormValueDto previousValue = unprocessedPreviousValues
					.values()
					.stream()
					.filter(p -> p.isEquals(newValue))
					.findFirst()
					.orElse(null);
			if (previousValue != null) {
				sortedPreviousValues[index] = previousValue;
				unprocessedPreviousValues.remove(previousValue.getId());
			}
		}
		// by seq
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			if (sortedPreviousValues[index] != null) {
				// mapped by previous iteration
				continue;
			}
			IdmFormValueDto newValue = newValues.get(index);
			IdmFormValueDto previousValue = unprocessedPreviousValues
					.values()
					.stream()
					.filter(p -> p.getSeq() == newValue.getSeq())
					.findFirst()
					.orElse(null);
			if (previousValue != null) {
				sortedPreviousValues[index] = previousValue;
				unprocessedPreviousValues.remove(previousValue.getId());
			}
		}
		//
		// try to fill remaining holes
		if (!unprocessedPreviousValues.isEmpty()) {
			for (int index = 0; index < sortedPreviousValues.length; index++) {
				if (sortedPreviousValues[index] != null) {
					// mapped by previous iteration
					continue;
				}
				if (unprocessedPreviousValues.isEmpty()) {
					break;
				}
				// get the first previos value
				sortedPreviousValues[index] = unprocessedPreviousValues.entrySet().iterator().next().getValue();
				unprocessedPreviousValues.remove(sortedPreviousValues[index].getId());
			}
		}
		return sortedPreviousValues;
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(owner, null, attributeName, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.hasLength(attributeCode, "Form attribute code is required!");
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		//
		return saveValues(owner, getAttribute(formDefinition, attributeCode), persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), attribute, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), formDefinition, attributeName, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), formDefinition, values, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), attributeName, persistentValues, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, BasePermission... permission) {
		return getValues(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, UUID formDefinitionId, BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null;
		if (formDefinitionId != null) {
			formDefinition = formDefinitionService.get(formDefinitionId);
			if (formDefinition == null) {
				throw new ResultCodeException(CoreResultCode.NOT_FOUND,
						ImmutableMap.of("formDefinition", formDefinitionId));
			}
		}
		return getValues(owner, formDefinition, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		return getFormInstance(owner, formDefinition ,permission).getValues();
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormInstanceDto getFormInstance(Identifiable owner, BasePermission... permission) {
		return getFormInstance(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormInstanceDto getFormInstance(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		//
		BasePermission[] permissions = PermissionUtils.trimNull(permission);
		FormableEntity ownerEntity = getOwnerEntity(owner);
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		FormValueService<FormableEntity> formValueService = getFormValueService(owner);
		List<IdmFormValueDto> values = formValueService.getValues(ownerEntity, formDefinition, permission);
		IdmFormInstanceDto formInstance = new IdmFormInstanceDto(ownerEntity, formDefinition, values);
		//
		// evaluate permissions for form definition attributes by values - change attribute properties ao remove attribute at all
		if (!ObjectUtils.isEmpty(permissions)) {
			Set<UUID> checkedAttributes = new HashSet<>();
			for(IdmFormValueDto value : values) {
				checkedAttributes.add(value.getFormAttribute());
				Set<String> valuePermissions = formValueService.getPermissions(value);
				if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.READ)) {
					// TODO: hidden?
					formInstance.getFormDefinition().removeFormAttribute(value.getFormAttribute());
				} else if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.UPDATE)) {
					formInstance.getMappedAttribute(value.getFormAttribute()).setReadonly(true);
				}
			}
			// evaluate permissions for new values - iterate through unprocessed attributes and check update permission
			List<IdmFormAttributeDto> formAttributes = Lists.newArrayList(formInstance.getFormDefinition().getFormAttributes());
			for(IdmFormAttributeDto formAttribute : formAttributes) {
				if (checkedAttributes.contains(formAttribute.getId())) {
					continue;
				}
				IdmFormValueDto newValue = new IdmFormValueDto();
				newValue.setOwnerAndAttribute(ownerEntity, formAttribute);
				Set<String> valuePermissions = formValueService.getPermissions(newValue);
				if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.READ)) {
					// TODO: hidden?
					formInstance.getFormDefinition().removeFormAttribute(newValue.getFormAttribute());
				} else if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.UPDATE)) {
					formAttribute.setReadonly(true);
				}
			}
		}
		//
		return formInstance;
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.notNull(attribute, "Form attribute definition is required!");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		return formValueService.getValues(ownerEntity, attribute, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, String attributeCode, BasePermission... permission) {
		return getValues(owner, null, attributeCode, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.hasLength(attributeCode, "Attribute code is required");
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		//
		return getValues(owner, getAttribute(formDefinition, attributeCode), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(UUID ownerId, Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), formDefinition, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeName,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), formDefinition, attributeName, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), attribute, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			String attributeName,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), attributeName, permission);
	}

	@Override
	@Transactional
	public void deleteAttribute(IdmFormAttributeDto attribute, BasePermission... permission) {
		formAttributeService.delete(attribute, permission);
	}

	@Override
	public void deleteValue(IdmFormDefinitionDto formDefinition, String valueId, BasePermission... permissions) {
		Assert.notNull(formDefinition, "Form definition is required!");
		Identifiable owner = getEmptyOwner(formDefinition);
		FormableEntity ownerEntity = getOwnerEntity(owner);
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		formValueService.deleteById(valueId, permissions);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, BasePermission... permission) {
		deleteValues(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		FormableEntity ownerEntity = getOwnerEntity(owner);
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		formValueService.deleteValues(ownerEntity, formDefinition, permission);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.notNull(attribute, "Form attribute definition is required!");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		formValueService.deleteValues(ownerEntity, attribute, permission);
	}

	@Override
	public String getConfidentialStorageKey(Identifiable owner, IdmFormAttributeDto attribute) {
		Assert.notNull(owner, "Form values owner is required!");
		Assert.notNull(owner.getId(), "Owner id is required!");
		Assert.notNull(attribute, "Form attribute is required!");
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(owner);
		String key = formValueService.getConfidentialStorageKey(attribute.getId());
		LOG.debug("Confidential storage key for attribute [{}] is [{}].", attribute.getCode(), key);
		return key;
	}

	@Override
	public Serializable getConfidentialPersistentValue(IdmFormValueDto guardedValue) {
		Assert.notNull(guardedValue);
		Assert.notNull(guardedValue.getOwnerId());
		//
		FormableEntity ownerEntity = getOwnerEntity(guardedValue.getOwnerId(), guardedValue.getOwnerType());
		FormValueService<?> formValueService = getFormValueService(ownerEntity);
		return formValueService.getConfidentialPersistentValue(guardedValue);
	}

	@Override
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public <O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute, Serializable persistentValue, Pageable pageable) {
		Assert.notNull(ownerType, "Owner type is required!");
		Assert.notNull(attribute, "Form attribute is required!");
		if (attribute.isConfidential()) {
			throw new UnsupportedOperationException(MessageFormat
					.format("Find owners by confidential attributes [{0}] are not supported.", attribute.getCode()));
		}
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerType);
		//
		Page<FormableEntity> ownerEntities = formValueService.findOwners(attribute, persistentValue, pageable);
		//
		// convert to dtos
		List<O> ownerDtos = ownerEntities
				.getContent()
				.stream()
				.map(ownerEntity -> {
					return (O) lookupService.lookupDto(ownerType, ownerEntity.getId());
				})
				.collect(Collectors.toList());

		return new PageImpl<>(ownerDtos, pageable, ownerEntities.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public <O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType, String attributeName,
			Serializable persistentValue, Pageable pageable) {
		IdmFormAttributeDto attribute = getAttribute(ownerType, attributeName);
		Assert.notNull(attribute, MessageFormat.format(
				"Attribute [{0}] does not exist in default form definition for owner [{1}]", attributeName, ownerType));
		//
		return findOwners(ownerType, attribute, persistentValue, pageable);
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional(readOnly = true)
	public Page<IdmFormValueDto> findValues(IdmFormValueFilter filter, Pageable pageable, BasePermission... permission) {
		Assert.notNull(filter);
		//
		// resolve owner by definition
		IdmFormDefinitionDto formDefinition = null;
		UUID definitionId = filter.getDefinitionId();
		if (definitionId != null) {
			formDefinition = formDefinitionService.get(definitionId);
		}
		//
		UUID attributeId = filter.getAttributeId();
		if (formDefinition == null && attributeId != null) {
			IdmFormAttributeDto formAttribute = formAttributeService.get(attributeId);
			if (formAttribute != null) {
				formDefinition = DtoUtils.getEmbedded(formAttribute, IdmFormAttribute_.formDefinition);
			}
		}
		if (formDefinition == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", "formDefinition"));
		}
		filter.setOwner(getEmptyOwner(formDefinition));
		Identifiable owner = (Identifiable) filter.getOwner();
		Assert.notNull(owner, "Filter - attribute owner is required. Is possible to filter form values by given owner only");
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(owner.getClass());
		//
		return formValueService.find(filter, pageable, permission);
	}

	@Override
	public List<String> getOwnerTypes() {
		return formValueServices
				.getPlugins()
				.stream()
				.map(service -> {
					return getDefaultDefinitionType(service.getOwnerClass());
				})
				.sorted()
				.collect(Collectors.toList());
	}

	/**
	 * Returns FormValueService for given owner
	 * 
	 * @param owner
	 * @param <O> values owner
	 * @return
	 */
	private <O extends FormableEntity> FormValueService<O> getFormValueService(Identifiable owner) {
		return getFormValueService(owner.getClass());
	}
	
	/**
	 * Returns FormValueService for given owner
	 * 
	 * @param ownerType
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private <O extends FormableEntity> FormValueService<O> getFormValueService(Class<? extends Identifiable> ownerType) {
		FormValueService<O> formValueService = (FormValueService<O>) formValueServices.getPluginFor(lookupService.getEntityClass(ownerType));
		if (formValueService == null) {
			throw new IllegalStateException(MessageFormat.format(
					"FormValueService for class [{0}] not found, please check configuration", ownerType));
		}
		return formValueService;
	}
	
	/**
	 * 
	 * @param owner
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <O extends FormableEntity> O getOwnerEntity(Identifiable owner) {
		Assert.notNull(owner, "Form values owner instance is required!");
		if (owner instanceof FormableEntity) {
			return (O) owner;
		}
		//
		return getOwnerEntity(owner.getId(), owner.getClass());
	}

	/**
	 * Returns owner entity by given id and type
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <O extends FormableEntity> O getOwnerEntity(Serializable ownerId, Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerId, "Form values owner id is required!");
		Assert.notNull(ownerType, "Form values owner type is required!");
		O owner = (O) lookupService.lookupEntity(ownerType, ownerId);
		Assert.notNull(owner, "Form values owner is required!");
		//
		return owner;
	}

	/**
	 * Create instance form definition from the given configuration class
	 * 
	 * @param configurationClass
	 * @return
	 */
	@Override
	public IdmFormDefinitionDto convertConfigurationToFormDefinition(
			Class<? extends ConfigurationClass> configurationClass) {
		Assert.notNull(configurationClass, "Class with the configuration is required!");
		try {
			ConfigurationClass configurationClassInstance = configurationClass.newInstance();
			List<IdmFormAttributeDto> properties = new ArrayList<>();

			PropertyDescriptor[] descriptors = Introspector.getBeanInfo(configurationClass).getPropertyDescriptors();

			Lists.newArrayList(descriptors).stream().forEach(descriptor -> {
				Method readMethod = descriptor.getReadMethod();
				String propertyName = descriptor.getName();
				ConfigurationClassProperty property = readMethod.getAnnotation(ConfigurationClassProperty.class);
				if (property != null) {
					IdmFormAttributeDto formAttribute = this.convertConfigurationProperty(property);
					formAttribute.setCode(propertyName);
					// TODO: Better convertors  (move from IC and ACC module to the Core)!
					initPersistentType(readMethod, formAttribute);
					
					try {
						formAttribute.setDefaultValue(this.convertDefaultValue(readMethod.invoke(configurationClassInstance), formAttribute.isMultiple()));
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new CoreException("Cannot read value of connector configuration property!", e);
					}

					properties.add(formAttribute);
				}
			});

			IdmFormDefinitionDto definition = new IdmFormDefinitionDto();
			definition.setFormAttributes(properties);

			return definition;

		} catch (IntrospectionException | InstantiationException | IllegalAccessException e) {
			throw new CoreException("Cannot read configuration property!", e);
		}
	}

	private void initPersistentType(Method readMethod, IdmFormAttributeDto formAttribute) {
		Assert.notNull(readMethod);
		Assert.notNull(formAttribute);
		
		String typeName = readMethod.getGenericReturnType().getTypeName();
		if (typeName.equals(boolean.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.BOOLEAN);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(String[].class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.TEXT);
			formAttribute.setMultiple(true);
		} else if (typeName.equals(String.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.TEXT);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(UUID.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.UUID);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(UUID[].class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.UUID);
			formAttribute.setMultiple(true);
		} else {
			throw new CoreException(
					MessageFormat.format("For return type [{0}] was not found persistent type!", typeName));
		}

	}

	private IdmFormAttributeDto convertConfigurationProperty(ConfigurationClassProperty property) {
		if (property == null) {
			return null;
		}
		IdmFormAttributeDto icProperty = new IdmFormAttributeDto();
		icProperty.setConfidential(property.confidential());
		icProperty.setName(property.displayName());
		icProperty.setDescription(property.helpMessage());
		icProperty.setRequired(property.required());
		icProperty.setSeq((short) property.order());
		icProperty.setFaceType(property.face());

		return icProperty;
	}
	
	private String convertDefaultValue(Object value, boolean multivalue) {
		//
		if (value == null) {
			return null;
		}		
		if (!multivalue) {
			return value.toString();
		}		
		StringBuilder result = new StringBuilder();
		// arrays only
		// override for other data types
		Object[] values = (Object[]) value;
		for (Object singleValue : values) {
			if (result.length() > 0) {
				result.append(System.getProperty("line.separator"));
			}
			result.append(singleValue);
		}
		return result.toString();
	}
	
	/**
	 * Prepares new owner instance
	 * 
	 * @param formDefinition
	 * @return
	 */
	private FormableEntity getEmptyOwner(IdmFormDefinitionDto formDefinition) {
		Assert.notNull(formDefinition);
		//
		try {
			return (FormableEntity) Class.forName(formDefinition.getType()).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
			throw new ResultCodeException(CoreResultCode.BAD_VALUE, ImmutableMap.of("formDefinition", formDefinition.getType()), ex);
		}
	}
}
