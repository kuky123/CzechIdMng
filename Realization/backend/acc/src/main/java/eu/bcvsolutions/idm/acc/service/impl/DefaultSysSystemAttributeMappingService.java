package eu.bcvsolutions.idm.acc.service.impl;

import java.beans.IntrospectionException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.SysAttributeControlledValueDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysAttributeControlledValueFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass_;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemAttributeRepository;
import eu.bcvsolutions.idm.acc.repository.SysSyncConfigRepository;
import eu.bcvsolutions.idm.acc.repository.SysSystemAttributeMappingRepository;
import eu.bcvsolutions.idm.acc.scheduler.task.impl.AttributeControlledValuesRecalculationTaskExecutor;
import eu.bcvsolutions.idm.acc.service.api.FormPropertyManager;
import eu.bcvsolutions.idm.acc.service.api.SysAttributeControlledValueService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.domain.IdmScriptCategory;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.GroovyScriptService;
import eu.bcvsolutions.idm.core.api.utils.AutowireHelper;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.script.evaluator.AbstractScriptEvaluator;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.impl.IcPasswordAttributeImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Default schema attributes mapping
 * 
 * @author svandav
 *
 */
@Service
public class DefaultSysSystemAttributeMappingService extends
		AbstractReadWriteDtoService<SysSystemAttributeMappingDto, SysSystemAttributeMapping, SysSystemAttributeMappingFilter>
		implements SysSystemAttributeMappingService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(DefaultSysSystemAttributeMappingService.class);

	private final SysSystemAttributeMappingRepository repository;
	private final GroovyScriptService groovyScriptService;
	private final FormService formService;
	private final ConfidentialStorage confidentialStorage;
	private final SysRoleSystemAttributeRepository roleSystemAttributeRepository;
	private final FormPropertyManager formPropertyManager;
	private final SysSyncConfigRepository syncConfigRepository;
	private final PluginRegistry<AbstractScriptEvaluator, IdmScriptCategory> pluginExecutors;
	private final SysSchemaAttributeService schemaAttributeService;
	private final SysSchemaObjectClassService schemaObjectClassService;
	private final SysSystemMappingService systemMappingService;
	@Autowired
	private SysAttributeControlledValueService attributeControlledValueService;
	@Autowired
	private SysRoleSystemAttributeService roleSystemAttributeService;

	@Autowired
	public DefaultSysSystemAttributeMappingService(SysSystemAttributeMappingRepository repository,
			GroovyScriptService groovyScriptService, FormService formService,
			SysRoleSystemAttributeRepository roleSystemAttributeRepository, FormPropertyManager formPropertyManager,
			SysSyncConfigRepository syncConfigRepository, List<AbstractScriptEvaluator> evaluators,
			ConfidentialStorage confidentialStorage, SysSchemaAttributeService schemaAttributeService,
			SysSchemaObjectClassService schemaObjectClassService, SysSystemMappingService systemMappingService) {
		super(repository);
		//
		Assert.notNull(groovyScriptService);
		Assert.notNull(formService);
		Assert.notNull(roleSystemAttributeRepository);
		Assert.notNull(formPropertyManager);
		Assert.notNull(syncConfigRepository);
		Assert.notNull(evaluators);
		Assert.notNull(confidentialStorage);
		Assert.notNull(schemaAttributeService);
		Assert.notNull(schemaObjectClassService);
		Assert.notNull(systemMappingService);
		//
		this.formService = formService;
		this.repository = repository;
		this.groovyScriptService = groovyScriptService;
		this.roleSystemAttributeRepository = roleSystemAttributeRepository;
		this.formPropertyManager = formPropertyManager;
		this.syncConfigRepository = syncConfigRepository;
		this.confidentialStorage = confidentialStorage;
		this.schemaAttributeService = schemaAttributeService;
		this.schemaObjectClassService = schemaObjectClassService;
		this.systemMappingService = systemMappingService;
		//
		this.pluginExecutors = OrderAwarePluginRegistry.create(evaluators);
	}

	@Override
	protected Page<SysSystemAttributeMapping> findEntities(SysSystemAttributeMappingFilter filter, Pageable pageable,
			BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SysSystemAttributeMappingDto> findBySystemMapping(SysSystemMappingDto systemMapping) {
		Assert.notNull(systemMapping);
		//
		return toDtos(repository.findAllBySystemMapping_Id(systemMapping.getId()), true);
	}

	@Override
	@Transactional(readOnly = true)
	public SysSystemAttributeMappingDto findBySystemMappingAndName(UUID systemMappingId, String name) {
		return toDto(repository.findBySystemMapping_IdAndName(systemMappingId, name));
	}

	@Override
	public Object transformValueToResource(String uid, Object value, AttributeMapping attributeMapping,
			AbstractDto entity) {
		Assert.notNull(attributeMapping);
		return transformValueToResource(uid, value, attributeMapping.getTransformToResourceScript(), entity,
				getSystemFromAttributeMapping(attributeMapping));
	}

	@Override
	public Object transformValueToResource(String uid, Object value, String script, AbstractDto entity,
			SysSystemDto system) {
		if (!StringUtils.isEmpty(script)) {
			Map<String, Object> variables = new HashMap<>();
			variables.put(ACCOUNT_UID, uid);
			variables.put(ATTRIBUTE_VALUE_KEY, value);
			variables.put(SYSTEM_KEY, system);
			variables.put(ENTITY_KEY, entity);
			variables.put(AbstractScriptEvaluator.SCRIPT_EVALUATOR,
					pluginExecutors.getPluginFor(IdmScriptCategory.TRANSFORM_TO)); // add default script evaluator, for
																					// call another scripts
			//
			// Add access for script evaluator
			List<Class<?>> extraClass = new ArrayList<>();
			extraClass.add(AbstractScriptEvaluator.Builder.class);
			//
			return groovyScriptService.evaluate(script, variables, extraClass);
		}

		return value;
	}

	@Override
	public Object transformValueFromResource(Object value, AttributeMapping attributeMapping,
			List<IcAttribute> icAttributes) {
		Assert.notNull(attributeMapping);
		//
		return transformValueFromResource(value, attributeMapping.getTransformFromResourceScript(), icAttributes,
				getSystemFromAttributeMapping(attributeMapping));
	}

	@Override
	public Object transformValueFromResource(Object value, String script, List<IcAttribute> icAttributes,
			SysSystemDto system) {

		if (!StringUtils.isEmpty(script)) {
			Map<String, Object> variables = new HashMap<>();
			variables.put(ATTRIBUTE_VALUE_KEY, value);
			variables.put(SYSTEM_KEY, system);
			variables.put(IC_ATTRIBUTES_KEY, icAttributes);
			variables.put(AbstractScriptEvaluator.SCRIPT_EVALUATOR,
					pluginExecutors.getPluginFor(IdmScriptCategory.TRANSFORM_FROM)); // add default script evaluator,
																						// for call another scripts
			// Add access for script evaluator
			List<Class<?>> extraClass = new ArrayList<>();
			extraClass.add(AbstractScriptEvaluator.Builder.class);
			//
			return groovyScriptService.evaluate(script, variables, extraClass);
		}

		return value;
	}


	@Override
	@Transactional
	public SysSystemAttributeMappingDto saveInternal(SysSystemAttributeMappingDto dto) {
		Assert.notNull(dto, "Attribute is mandatory!");
		Assert.notNull(dto.getSystemMapping(), "System mapping is mandatory!");
		SysSystemMappingDto systemMappingDto = systemMappingService.get(dto.getSystemMapping());

		validate(dto, systemMappingDto);

		// Check if exist some else attribute which is defined like unique identifier
		// If exists, then we will set they to uid = false. Only currently saving
		// attribute will be unique identifier
		if (dto.isUid() && dto.getSystemMapping() != null) {
			this.repository.clearIsUidAttribute(dto.getSystemMapping(), dto.getId());
		}
		// We will do script validation (on compilation errors), before save
		if (dto.getTransformFromResourceScript() != null) {
			groovyScriptService.validateScript(dto.getTransformFromResourceScript());
		}
		if (dto.getTransformToResourceScript() != null) {
			groovyScriptService.validateScript(dto.getTransformToResourceScript());
		}

		Class<? extends Identifiable> entityType = systemMappingDto.getEntityType().getExtendedAttributeOwnerType();
		if (dto.isExtendedAttribute() && formService.isFormable(entityType)) {
			createExtendedAttributeDefinition(dto, entityType);
		}
		return super.saveInternal(dto);
	}

	/**
	 * Validation. If validation does not pass, then runtime exception is throw.
	 * 
	 * @param dto
	 * @return
	 */
	@Override
	public void validate(SysSystemAttributeMappingDto dto, SysSystemMappingDto systemMappingDto) {

		/**
		 * When provisioning is set, then can be schema attribute mapped only once.
		 */
		if (dto.getSchemaAttribute() != null && systemMappingDto != null
				&& SystemOperationType.PROVISIONING == systemMappingDto.getOperationType()) {
			SysSystemAttributeMappingFilter systemAttributeMappingFilter = new SysSystemAttributeMappingFilter();
			systemAttributeMappingFilter.setSchemaAttributeId(dto.getSchemaAttribute());
			systemAttributeMappingFilter.setSystemMappingId(systemMappingDto.getId());

			long count = this.find(systemAttributeMappingFilter, null).getContent() //
					.stream() //
					.filter(attribute -> !attribute.getId().equals(dto.getId())) //
					.count(); //

			if (count > 0) {
				throw new ResultCodeException(AccResultCode.PROVISIONING_DUPLICATE_ATTRIBUTE_MAPPING,
						ImmutableMap.of("schemaAttribute", dto.getSchemaAttribute()));
			}
		}
	}

	@Override
	public List<SysSystemAttributeMappingDto> getAllPasswordAttributes(UUID systemId, UUID systemMappingId) {
		SysSystemAttributeMappingFilter filter = new SysSystemAttributeMappingFilter();
		filter.setPasswordAttribute(true);
		filter.setSystemId(systemId);
		filter.setSystemMappingId(systemMappingId);
		return this.find(filter, null).getContent();
	}

	/**
	 * Check on exists EAV definition for given attribute. If the definition not
	 * exist, then we try create it.
	 * 
	 * @param entity
	 * @param ownerType
	 */
	@Override
	@Transactional
	public void createExtendedAttributeDefinition(AttributeMapping entity, Class<? extends Identifiable> ownerType) {
		IdmFormAttributeDto attribute = formService.getAttribute(ownerType, entity.getIdmPropertyName());
		if (attribute == null) {
			log.info(MessageFormat.format(
					"IdmFormAttribute for identity and property {0} not found. We will create definition now.",
					entity.getIdmPropertyName()));
			formService.saveAttribute(ownerType, convertMappingAttribute(entity));
		}
	}

	@Override
	@Transactional
	public void delete(SysSystemAttributeMappingDto dto, BasePermission... permission) {
		Assert.notNull(dto);
		SysSystemAttributeMapping entity = this.getEntity(dto.getId());
		Assert.notNull(entity);

		if (syncConfigRepository.countByCorrelationAttribute_Id(dto.getId()) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC,
					ImmutableMap.of("attribute", dto.getName()));
		}
		if (syncConfigRepository.countByFilterAttribute(entity) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC,
					ImmutableMap.of("attribute", dto.getName()));
		}
		if (syncConfigRepository.countByTokenAttribute(entity) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC,
					ImmutableMap.of("attribute", dto.getName()));
		}
		// Delete attributes
		roleSystemAttributeRepository.deleteBySystemAttributeMapping(entity);

		// Delete of controlled and historic values
		if (dto.getId() != null) {
			SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
			attributeControlledValueFilter.setAttributeMappingId(dto.getId());

			List<SysAttributeControlledValueDto> controlledAndHistoricValues = attributeControlledValueService
					.find(attributeControlledValueFilter, null).getContent();
			controlledAndHistoricValues.forEach(value -> attributeControlledValueService.delete(value));
		}

		super.delete(dto, permission);
	}

	/**
	 * Create instance of IC attribute for given name. Given idm value will be
	 * transformed to resource.
	 * 
	 * @param attributeMapping
	 * @param idmValue
	 * @return
	 */
	@Override
	public IcAttribute createIcAttribute(SysSchemaAttributeDto schemaAttribute, Object idmValue) {
		// Check type of value
		try {
			Class<?> classType = Class.forName(schemaAttribute.getClassType());

			// If is multivalue and value is list, then we will iterate list and check every
			// item on correct type
			if (schemaAttribute.isMultivalued() && idmValue instanceof List) {
				((List<?>) idmValue).stream().forEachOrdered(value -> {
					if (value != null && !(classType.isAssignableFrom(value.getClass()))) {
						throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_VALUE_WRONG_TYPE,
								ImmutableMap.of("attribute", schemaAttribute.getName(), "schemaAttributeType",
										schemaAttribute.getClassType(), "valueType", value.getClass().getName()));
					}
				});

				// Check single value on correct type
			} else if (idmValue != null && !(classType.isAssignableFrom(idmValue.getClass()))) {
				if (idmValue instanceof GuardedString && classType.isAssignableFrom(String.class)) {
					// Value can be different type from schema but the type must be instance of
					// guarded string
					// and schema type must be assignable from string. Value to string will be
					// transform at the end.
				} else {
					throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_VALUE_WRONG_TYPE,
							ImmutableMap.of("attribute", schemaAttribute.getName(), "schemaAttributeType",
									schemaAttribute.getClassType(), "valueType", idmValue.getClass().getName()));
				}
			}
		} catch (ClassNotFoundException e) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_TYPE_NOT_FOUND, ImmutableMap.of(
					"attribute", schemaAttribute.getName(), "schemaAttributeType", schemaAttribute.getClassType()), e);
		}

		IcAttributeImpl icAttributeForUpdate = null;
		if (IcConnectorFacade.PASSWORD_ATTRIBUTE_NAME.equals(schemaAttribute.getName())) {
			// Attribute is password type
			icAttributeForUpdate = new IcPasswordAttributeImpl(schemaAttribute.getName(), (GuardedString) idmValue);

		} else {
			if (idmValue == null && schemaAttribute.isMultivalued()) {
				icAttributeForUpdate = new IcAttributeImpl(schemaAttribute.getName(), null, true);
			} else if (idmValue instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>) idmValue;
				icAttributeForUpdate = new IcAttributeImpl(schemaAttribute.getName(), values, true);
			} else {
				icAttributeForUpdate = new IcAttributeImpl(schemaAttribute.getName(), idmValue);
			}
			// Multivalued must be set by schema setting
			icAttributeForUpdate.setMultiValue(schemaAttribute.isMultivalued());
		}
		return icAttributeForUpdate;
	}

	@Override
	public SysSystemAttributeMappingDto clone(UUID id) {
		SysSystemAttributeMappingDto original = this.get(id);
		Assert.notNull(original, "Schema attribute must be found!");

		original.setId(null);
		EntityUtils.clearAuditFields(original);
		return original;
	}

	/**
	 * Convert schema attribute handling to Form attribute
	 * 
	 * @param entity
	 * @return
	 */
	private IdmFormAttributeDto convertMappingAttribute(AttributeMapping entity) {
		SysSchemaAttributeDto schemaAttribute = getSchemaAttribute(entity);
		IdmFormAttributeDto attributeDefinition = new IdmFormAttributeDto();
		attributeDefinition.setSeq((short) 0);
		attributeDefinition.setCode(entity.getIdmPropertyName());
		attributeDefinition.setName(entity.getName());
		attributeDefinition.setPersistentType(formPropertyManager.getPersistentType(schemaAttribute.getClassType()));
		attributeDefinition.setRequired(schemaAttribute.isRequired());
		attributeDefinition.setMultiple(schemaAttribute.isMultivalued());
		attributeDefinition.setReadonly(!schemaAttribute.isUpdateable());
		attributeDefinition.setConfidential(entity.isConfidentialAttribute());
		attributeDefinition.setUnmodifiable(false); // attribute can be deleted
		//
		SysSystemDto system = getSystemFromSchemaAttribute(schemaAttribute);
		//
		attributeDefinition.setDescription(
				MessageFormat.format("Genereted by schema attribute {0} in resource {1}. Created by SYSTEM.",
						schemaAttribute.getName(), system.getName()));
		return attributeDefinition;
	}

	@Override
	public SysSystemAttributeMappingDto getAuthenticationAttribute(UUID systemId, SystemEntityType entityType) {
		Assert.notNull(systemId);
		Assert.notNull(entityType);
		// authentication attribute is only from provisioning operation type
		SysSystemAttributeMappingDto attr = toDto(
				this.repository.findAuthenticationAttribute(systemId, SystemOperationType.PROVISIONING, entityType));
		// defensive, if authentication attribute don't exists find attribute flagged as
		// UID
		if (attr == null) {
			return toDto(this.repository.findUidAttribute(systemId, SystemOperationType.PROVISIONING, entityType));
		}
		return attr;
	}

	/**
	 * Find value for this mapped attribute by property name. Returned value can be
	 * list of objects. Returns transformed value.
	 * 
	 * @param uid               - Account identifier
	 * @param entity
	 * @param attributeHandling
	 * @param idmValue
	 * @return
	 * @throws IntrospectionException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@Override
	public Object getAttributeValue(String uid, AbstractDto entity, AttributeMapping attributeHandling) {
		Object idmValue = null;
		//
		if (attributeHandling.isPasswordAttribute()) {
			// if attribute is mapped to PASSWORD transformation will be process
			// there but in PrepareConnectorObjectProcessor
			return null;
		}
		//
		SysSchemaAttributeDto schemaAttributeDto = getSchemaAttribute(attributeHandling);
		//
		if (attributeHandling.isExtendedAttribute() && entity != null && formService.isFormable(entity.getClass())) {
			List<IdmFormValueDto> formValues = formService.getValues(entity, attributeHandling.getIdmPropertyName());
			if (formValues.isEmpty()) {
				idmValue = null;
			} else if (schemaAttributeDto.isMultivalued()) {
				// Multiple value extended attribute
				List<Object> values = new ArrayList<>();
				formValues.stream().forEachOrdered(formValue -> {
					values.add(formValue.getValue());
				});
				idmValue = values;
			} else {
				// Single value extended attribute
				IdmFormValueDto formValue = formValues.get(0);
				if (formValue.isConfidential()) {
					Object confidentialValue = formService.getConfidentialPersistentValue(formValue);
					// If is confidential value String and schema attribute is GuardedString type,
					// then convert to GuardedString will be did.
					if (confidentialValue instanceof String
							&& schemaAttributeDto.getClassType().equals(GuardedString.class.getName())) {
						idmValue = new GuardedString((String) confidentialValue);
					} else {
						idmValue = confidentialValue;
					}
				} else {
					idmValue = formValue.getValue();
				}
			}
		}
		// Find value from entity
		else if (attributeHandling.isEntityAttribute()) {
			if (attributeHandling.isConfidentialAttribute()) {
				// If is attribute isConfidential, then we will find value in
				// secured storage
				idmValue = confidentialStorage.getGuardedString(entity.getId(), entity.getClass(),
						attributeHandling.getIdmPropertyName());
			} else {
				try {
					// We will search value directly in entity by property name
					idmValue = EntityUtils.getEntityValue(entity, attributeHandling.getIdmPropertyName());
				} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | ProvisioningException o_O) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_IDM_FIELD_NOT_FOUND,
							ImmutableMap.of("property", attributeHandling.getIdmPropertyName(), "entityType",
									entity.getClass(), "schemaAtribute",
									attributeHandling.getSchemaAttribute().toString()),
							o_O);
				}
			}
		} else {
			// If Attribute value is not in entity nor in extended attribute, then idmValue
			// is null.
			// It means attribute is static ... we will call transformation to resource.
		}
		return this.transformValueToResource(uid, idmValue, attributeHandling, entity);
	}

	@Override
	public String generateUid(AbstractDto entity, SysSystemAttributeMappingDto uidAttribute) {
		Object uid = this.getAttributeValue(null, entity, uidAttribute);
		if (uid == null) {
			SysSystemDto systemEntity = getSystemFromAttributeMapping(uidAttribute);
			throw new ProvisioningException(AccResultCode.PROVISIONING_GENERATED_UID_IS_NULL,
					ImmutableMap.of("system", systemEntity.getName()));
		}
		if (!(uid instanceof String)) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_IS_NOT_STRING,
					ImmutableMap.of("uid", uid));
		}
		return (String) uid;
	}

	@Override
	public SysSystemAttributeMappingDto getUidAttribute(List<SysSystemAttributeMappingDto> mappedAttributes,
			SysSystemDto system) {
		List<SysSystemAttributeMappingDto> systemAttributeMappingUid = mappedAttributes.stream().filter(attribute -> {
			return !attribute.isDisabledAttribute() && attribute.isUid();
		}).collect(Collectors.toList());

		if (systemAttributeMappingUid.size() > 1) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_MORE_UID,
					ImmutableMap.of("system", system.getName()));
		}
		if (systemAttributeMappingUid.isEmpty()) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		return systemAttributeMappingUid.get(0);
	}

	@Override
	public Object getValueByMappedAttribute(AttributeMapping attribute, List<IcAttribute> icAttributes) {
		Object icValue = null;
		Optional<IcAttribute> optionalIcAttribute = icAttributes.stream().filter(icAttribute -> {
			SysSchemaAttributeDto schemaAttributeDto = getSchemaAttribute(attribute);
			return schemaAttributeDto.getName().equals(icAttribute.getName());
		}).findFirst();
		if (optionalIcAttribute.isPresent()) {
			IcAttribute icAttribute = optionalIcAttribute.get();
			if (icAttribute.isMultiValue()) {
				icValue = icAttribute.getValues();
			} else {
				icValue = icAttribute.getValue();
			}
		}
		Object transformedValue = this.transformValueFromResource(icValue, attribute, icAttributes);
		return transformedValue;
	}

	@Override
	public String getUidValueFromResource(List<IcAttribute> icAttributes,
			List<SysSystemAttributeMappingDto> mappedAttributes, SysSystemDto system) {
		SysSystemAttributeMappingDto uidAttribute = this.getUidAttribute(mappedAttributes, system);
		Object uid = this.getValueByMappedAttribute(uidAttribute, icAttributes);

		if (uid == null) {
			SysSystemDto systemEntity = getSystemFromAttributeMapping(uidAttribute);
			throw new ProvisioningException(AccResultCode.PROVISIONING_GENERATED_UID_IS_NULL,
					ImmutableMap.of("system", systemEntity.getName()));
		}
		if (!(uid instanceof String)) {
			SysSystemDto systemEntity = getSystemFromAttributeMapping(uidAttribute);
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_IS_NOT_STRING,
					ImmutableMap.of("uid", uid.getClass(), "system", systemEntity.getName()));
		}
		return (String) uid;
	}

	@Override
	public List<Serializable> getControlledAttributeValues(UUID systemId, SystemEntityType entityType,
			String schemaAttributeName) {
		Assert.notNull(systemId, "System ID is mandatory for get controlled values!");
		Assert.notNull(entityType, "Entity type is mandatory for get controlled values!");
		Assert.notNull(schemaAttributeName, "Schema attribute name is mandatory for get controlled values!");

		SysSystemMappingDto mapping = systemMappingService.findProvisioningMapping(systemId, entityType);
		Assert.notNull(mapping, "System provisioning mapping is mandatory for search controlled attribute values!");
		List<Serializable> results = Lists.newArrayList();

		// Obtains controlled values from role-attributes
		SysRoleSystemAttributeFilter roleSystemAttributeFilter = new SysRoleSystemAttributeFilter();
		roleSystemAttributeFilter.setSystemMappingId(mapping.getId());
		roleSystemAttributeFilter.setSchemaAttributeName(schemaAttributeName);
		List<SysRoleSystemAttributeDto> roleSystemAttributes = roleSystemAttributeService
				.find(roleSystemAttributeFilter, null).getContent();

		roleSystemAttributes.stream() // We need values for merge and enabled attributes only
				.filter(roleSystemAttr -> AttributeMappingStrategyType.MERGE == roleSystemAttr.getStrategyType() //
						&& !roleSystemAttr.isDisabledAttribute()) //
				.forEach(roleSystemAttr -> { //
					Serializable value = getControlledValue(roleSystemAttr, systemId, schemaAttributeName);
					if (value != null && !results.contains(value)) {
						results.add(value);
					}
				});

		return results;
	}

	/**
	 * Get controlled merge value. Evaluates transformation to resource.
	 * 
	 * @param roleSystemAttr
	 * @param systemId
	 * @param schemaAttributeName
	 * @return
	 */
	private Serializable getControlledValue(AttributeMapping roleSystemAttr, UUID systemId,
			String schemaAttributeName) {
		// We predicate only static script (none input variables, only system)!
		Object value = this.transformValueToResource(null, null, roleSystemAttr, null);
		if (value != null) {
			if (!(value instanceof Serializable)) {
				throw new ResultCodeException(AccResultCode.PROVISIONING_CONTROLLED_VALUE_IS_NOT_SERIALIZABLE,
						ImmutableMap.of("value", value, "name", schemaAttributeName, "system", systemId));
			}
			return (Serializable) value;
		}
		return null;
	}

	@Override
	public List<Serializable> getCachedControlledAndHistoricAttributeValues(UUID systemId, SystemEntityType entityType,
			String schemaAttributeName) {
		Assert.notNull(systemId, "System ID is mandatory for get controlled values!");
		Assert.notNull(entityType, "Entity type is mandatory for get controlled values!");
		Assert.notNull(schemaAttributeName, "Schema attribute name is mandatory for get controlled values!");

		SysSystemMappingDto mapping = systemMappingService.findProvisioningMapping(systemId, entityType);
		Assert.notNull(mapping, "System provisioning mapping is mandatory for search controlled attribute values!");

		List<SysSystemAttributeMappingDto> attributes = this.getAttributeMapping(schemaAttributeName, mapping,
				systemId);
		Assert.notEmpty(attributes, "Mapping attribute must exists!");
		Assert.isTrue(attributes.size() == 1,
				"Only one mapping attribute is allowed for same schema attribute in the provisioning!");
		SysSystemAttributeMappingDto attributeMapping = attributes.get(0);

		SysAttributeControlledValueFilter attributeControlledValueFilter = new SysAttributeControlledValueFilter();
		attributeControlledValueFilter.setAttributeMappingId(attributeMapping.getId());
		attributeControlledValueFilter.setHistoricValue(Boolean.FALSE);

		// Search controlled values for that attribute
		List<Serializable> cachedControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		if (attributeMapping.isEvictControlledValuesCache()) {
			List<Serializable> controlledAttributeValues = recalculateAttributeControlledValues(systemId, entityType,
					schemaAttributeName, attributeMapping);
			cachedControlledValues = controlledAttributeValues;
		}

		// Set filter for search historic values
		attributeControlledValueFilter.setHistoricValue(Boolean.TRUE);

		// Search historic controlled values for that attribute
		List<Serializable> historicControlledValues = attributeControlledValueService //
				.find(attributeControlledValueFilter, null) //
				.getContent() //
				.stream() //
				.map(SysAttributeControlledValueDto::getValue) //
				.collect(Collectors.toList());

		List<Serializable> controlledValues = Lists.newArrayList();
		controlledValues.addAll(cachedControlledValues);
		controlledValues.addAll(historicControlledValues);

		return controlledValues;
	}

	@Override
	public List<Serializable> recalculateAttributeControlledValues(UUID systemId, SystemEntityType entityType,
			String schemaAttributeName, SysSystemAttributeMappingDto attributeMapping) {
		// Computes values
		List<Serializable> controlledAttributeValues = this.getControlledAttributeValues(systemId, entityType,
				schemaAttributeName);
		// Save results
		attributeControlledValueService.setControlledValues(attributeMapping, controlledAttributeValues);
		return controlledAttributeValues;
	}

	/**
	 * Method return schema attribute from interface attribute mapping. Schema may
	 * be null from RoleSystemAttribute
	 * 
	 * @return
	 */
	private SysSchemaAttributeDto getSchemaAttribute(AttributeMapping attributeMapping) {
		if (attributeMapping.getSchemaAttribute() != null) {
			return schemaAttributeService.get(attributeMapping.getSchemaAttribute());
		} else {
			// schema attribute is null = roleSystemAttribute
			SysSystemAttributeMappingDto dto = this
					.get(((SysRoleSystemAttributeDto) attributeMapping).getSystemAttributeMapping());
			return schemaAttributeService.get(dto.getSchemaAttribute());
		}
	}

	/**
	 * Method return {@link SysSystemDto} from {@link AttributeMapping}
	 * 
	 * @param attributeMapping
	 * @return
	 */
	private SysSystemDto getSystemFromAttributeMapping(AttributeMapping attributeMapping) {
		SysSchemaAttributeDto schemaAttrDto = getSchemaAttribute(attributeMapping);
		return getSystemFromSchemaAttribute(schemaAttrDto);
	}

	private SysSystemDto getSystemFromSchemaAttribute(SysSchemaAttributeDto schemaAttrDto) {
		SysSchemaObjectClassDto schemaObject = schemaObjectClassService.get(schemaAttrDto.getObjectClass());
		return getSystemFromSchemaObjectClass(schemaObject);
	}

	private SysSystemDto getSystemFromSchemaObjectClass(SysSchemaObjectClassDto schemaObject) {
		return DtoUtils.getEmbedded(schemaObject, SysSchemaObjectClass_.system);
	}

	/**
	 * Find all mapped attribute for given schema attribute name and mapping.
	 * 
	 * @param schemaAttributeName
	 * @param mapping
	 * @param system
	 * @return
	 */
	private List<SysSystemAttributeMappingDto> getAttributeMapping(String schemaAttributeName,
			SysSystemMappingDto mapping, UUID systemId) {
		Assert.notNull(schemaAttributeName);
		Assert.notNull(mapping);
		Assert.notNull(systemId);

		SysSystemAttributeMappingFilter filter = new SysSystemAttributeMappingFilter();
		filter.setSchemaAttributeName(schemaAttributeName);
		filter.setSystemMappingId(mapping.getId());
		filter.setSystemId(systemId);

		return this.find(filter, null).getContent();
	}
}
