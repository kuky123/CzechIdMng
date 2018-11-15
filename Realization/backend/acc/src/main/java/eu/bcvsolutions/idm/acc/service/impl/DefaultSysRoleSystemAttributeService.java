package eu.bcvsolutions.idm.acc.service.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaObjectClassFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem_;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping_;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemAttributeRepository;
import eu.bcvsolutions.idm.acc.scheduler.task.impl.AttributeControlledValuesRecalculationTaskExecutor;
import eu.bcvsolutions.idm.acc.service.api.SysAttributeControlledValueService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.GroovyScriptService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.AutowireHelper;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Mapping attribute to system for role
 * 
 * @author svandav
 * @author pstloukal
 *
 */
@Service
public class DefaultSysRoleSystemAttributeService extends
		AbstractReadWriteDtoService<SysRoleSystemAttributeDto, SysRoleSystemAttribute, SysRoleSystemAttributeFilter>
		implements SysRoleSystemAttributeService {

	private final SysRoleSystemAttributeRepository repository;
	@Autowired
	private GroovyScriptService groovyScriptService;
	@Autowired
	private FormService formService;
	@Autowired
	private SysRoleSystemService roleSystemService;
	@Autowired
	private SysSystemAttributeMappingService systemAttributeMappingService;
	@Autowired
	private SysSystemMappingService systemMappingService;
	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private SysSchemaObjectClassService schemaObjectClassService;
	@Autowired
	private SysSchemaAttributeService schemaAttributeService;
	@Autowired
	private RequestManager requestManager;
	@Autowired
	private SysAttributeControlledValueService attributeControlledValueService;
	@Autowired
	private LongRunningTaskManager longRunningTaskManager;

	@Autowired
	public DefaultSysRoleSystemAttributeService(SysRoleSystemAttributeRepository repository) {
		super(repository);
		//
		this.repository = repository;
	}

	@Override
	protected Page<SysRoleSystemAttribute> findEntities(SysRoleSystemAttributeFilter filter, Pageable pageable,
			BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}

	@Override
	@Transactional
	public SysRoleSystemAttributeDto save(SysRoleSystemAttributeDto dto, BasePermission... permission) {
		
		SysRoleSystemAttributeDto savedDto = super.save(dto, permission);
		SysSystemAttributeMappingDto attributeMappingDto = systemAttributeMappingService.get(savedDto.getSystemAttributeMapping());
		
		// If is mapped attribute marks as evicted, then we will start LRT for recalculation controlled values
		if (!systemAttributeMappingService.isNew(attributeMappingDto) && attributeMappingDto.isEvictControlledValuesCache() == true) {
			SysSystemMappingDto systemMappingDto = systemMappingService.get(attributeMappingDto.getSystemMapping());
			SysSchemaObjectClassDto objectClassDto = DtoUtils.getEmbedded(systemMappingDto,
					SysSystemMapping_.objectClass, SysSchemaObjectClassDto.class);

			// Init LRT
			AttributeControlledValuesRecalculationTaskExecutor attributeControlledValueRecalculationTask = AutowireHelper
					.createBean(AttributeControlledValuesRecalculationTaskExecutor.class);
			attributeControlledValueRecalculationTask
					.init(ImmutableMap.of(AttributeControlledValuesRecalculationTaskExecutor.PARAMETER_SYSTEM_UUID,
							objectClassDto.getSystem(), //
							AttributeControlledValuesRecalculationTaskExecutor.PARAMETER_ENTITY_TYPE,
							systemMappingDto.getEntityType(), //
							AttributeControlledValuesRecalculationTaskExecutor.PARAMETER_ONLY_EVICTED, true //
					)); //
			// Execute recalculation LRT
			longRunningTaskManager.execute(attributeControlledValueRecalculationTask);
		}
		
		return savedDto;
	}
	
	@Override
	public SysRoleSystemAttributeDto saveInternal(SysRoleSystemAttributeDto dto) {
		// Check if exist some else attribute which is defined like unique
				// identifier
				if (dto.isUid()) {
					SysRoleSystemAttributeFilter filter = new SysRoleSystemAttributeFilter();
					filter.setIsUid(Boolean.TRUE);
					filter.setRoleSystemId(dto.getRoleSystem());

					List<SysRoleSystemAttributeDto> list = this.find(filter, null).getContent();

					if (list.size() > 0 && !list.get(0).getId().equals(dto.getId())) {
						SysRoleSystemDto roleSystem = roleSystemService.get(dto.getRoleSystem());
						IdmRoleDto roleDto = roleService.get(roleSystem.getRole());
						SysSystemDto systemDto = DtoUtils.getEmbedded(dto, SysRoleSystem_.system);
						throw new ProvisioningException(AccResultCode.PROVISIONING_ROLE_ATTRIBUTE_MORE_UID,
								ImmutableMap.of("role", roleDto.getCode(), "system", systemDto.getName()));
					}
				}

				// We will check exists definition for extended attribute
				SysSystemAttributeMappingDto systemAttributeMapping = systemAttributeMappingService
						.get(dto.getSystemAttributeMapping());

				// Password can't be overridden
				SysSchemaAttributeDto schemaAttributeDto = DtoUtils.getEmbedded(systemAttributeMapping,
						SysSystemAttributeMapping_.schemaAttribute, SysSchemaAttributeDto.class);
				if (systemAttributeMapping.isPasswordAttribute()
						|| schemaAttributeDto.getName().equals(ProvisioningService.PASSWORD_SCHEMA_PROPERTY_NAME)) {
					throw new ResultCodeException(AccResultCode.SYSTEM_MAPPING_PASSWORD_OVERRIDE);
				}

				SysSystemMappingDto systemMapping = systemMappingService.get(systemAttributeMapping.getSystemMapping());
				Class<? extends Identifiable> entityType = systemMapping.getEntityType().getEntityType();
				if (dto.isExtendedAttribute() && formService.isFormable(entityType)) {
					systemAttributeMappingService.createExtendedAttributeDefinition(dto, entityType);
				}

				// We will do script validation (on compilation errors), before save
				if (dto.getTransformScript() != null) {
					groovyScriptService.validateScript(dto.getTransformScript());
				}

				// Save history of controlled value (if definition changed)
				if (!this.isNew(dto)) {

					SysRoleSystemAttributeDto oldRoleAttribute = this.get(dto.getId());
					// We predicate only static script (none input variables, only system)!
					Object oldControlledValue = systemAttributeMappingService.transformValueToResource(null, null,
							oldRoleAttribute, null);
					Object newControlledValue = systemAttributeMappingService.transformValueToResource(null, null, dto, null);

					// Check if parent attribute changed, if yes then old value is added to history
					// and new parent attribute is evicted
					if (!oldRoleAttribute.getSystemAttributeMapping().equals(dto.getSystemAttributeMapping())) {
						SysSystemAttributeMappingDto oldSystemAttributeMapping = systemAttributeMappingService
								.get(oldRoleAttribute.getSystemAttributeMapping());
						if (AttributeMappingStrategyType.MERGE == oldSystemAttributeMapping.getStrategyType()) {
							// Old attribute changed, so we need evict the cache
							oldSystemAttributeMapping.setEvictControlledValuesCache(true);
							systemAttributeMappingService.save(oldSystemAttributeMapping);
						}
						// Set old value as historic
						attributeControlledValueService.addHistoricValue(oldSystemAttributeMapping,
								(Serializable) oldControlledValue);
					}
					// Check if old and new controlled values are same. If not then we save old
					// value to the history on parent attribute
					else if (!Objects.equals(oldControlledValue, newControlledValue)
							&& AttributeMappingStrategyType.MERGE == oldRoleAttribute.getStrategyType()) {
						// Set old value as historic
						attributeControlledValueService.addHistoricValue(systemAttributeMapping,
								(Serializable) oldControlledValue);
					}
					// Check if disable of that attribute is changed and new value is disabled, then
					// we need add old value to history
					else if (oldRoleAttribute.isDisabledAttribute() != dto.isDisabledAttribute() && dto.isDisabledAttribute()
							&& AttributeMappingStrategyType.MERGE == oldRoleAttribute.getStrategyType()) {
						// Set old value as historic
						attributeControlledValueService.addHistoricValue(systemAttributeMapping,
								(Serializable) oldControlledValue);
					}
					// Check if strategy type changed, if yes and previous strategy was MERGE, then
					// old value will be added to history
					else if (oldRoleAttribute.getStrategyType() != dto.getStrategyType()
							&& AttributeMappingStrategyType.MERGE == oldRoleAttribute.getStrategyType()) {
						// Set old value as historic
						attributeControlledValueService.addHistoricValue(systemAttributeMapping,
								(Serializable) oldControlledValue);
					}
				}
				// Attribute created/updated, so we need evict the cache
				systemAttributeMapping.setEvictControlledValuesCache(true);
				systemAttributeMappingService.save(systemAttributeMapping);
		return super.saveInternal(dto);
	}

	@Override
	@Transactional
	public void delete(SysRoleSystemAttributeDto roleSystemAttribute, BasePermission... permission) {
		Assert.notNull(roleSystemAttribute);
		// Cancel requests and request items using that deleting DTO
		requestManager.onDeleteRequestable(roleSystemAttribute);

		// If has deletes attribute MERGE strategy, then we need save value to history
		// and evict cache on parent attribute
		if (AttributeMappingStrategyType.MERGE == roleSystemAttribute.getStrategyType()) {
			SysSystemAttributeMappingDto systemAttributeMapping = systemAttributeMappingService
					.get(roleSystemAttribute.getSystemAttributeMapping());
			Object value = systemAttributeMappingService.transformValueToResource(null, null, roleSystemAttribute,
					null);
			// Set old value as historic
			attributeControlledValueService.addHistoricValue(systemAttributeMapping, (Serializable) value);
			// Attribute changed, so we need evict the cache
			systemAttributeMapping.setEvictControlledValuesCache(true);
			systemAttributeMappingService.save(systemAttributeMapping);
		}

		super.delete(roleSystemAttribute, permission);
	}

	@Transactional
	@Override
	public SysRoleSystemAttributeDto addRoleMappingAttribute(UUID systemId, UUID roleId, String attributeName,
			String transformationScript, String objectClassName) { // ObjectClassName "__ACCOUNT__"
		Assert.notNull(systemId, "SystemId cannot be null!");
		Assert.notNull(roleId, "RoleId cannot be null!");
		Assert.notNull(attributeName, "Attribute name cannot be null");
		Assert.hasLength(attributeName, "Attribute name cannot be blank");

		UUID roleSystemId = getSysRoleSystem(systemId, roleId, objectClassName);
		SysRoleSystemAttributeDto systemAttribute = getSystemAttribute(roleSystemId, attributeName);
		if (systemAttribute == null) {
			systemAttribute = new SysRoleSystemAttributeDto();
		}

		systemAttribute.setEntityAttribute(false);
		systemAttribute.setStrategyType(AttributeMappingStrategyType.MERGE);

		UUID systemAttributeMappingId = getSystemAttributeMapping(systemId, attributeName, objectClassName).getId();

		systemAttribute.setName(attributeName);
		systemAttribute.setRoleSystem(roleSystemId);
		systemAttribute.setSystemAttributeMapping(systemAttributeMappingId);
		//
		if (transformationScript != null) {
			systemAttribute.setTransformScript(transformationScript);
		}

		return this.save(systemAttribute);
	}

	@Transactional
	@Override
	public SysSystemMappingDto getSystemMapping(UUID systemId, String objectClassName,
			SystemOperationType operationType) {
		Assert.notNull(systemId, "SystemId cannot be null!");
		Assert.notNull(objectClassName, "ObjectClassName cannot be null!");
		Assert.notNull(operationType, "OperationType cannot be null!");

		SysSystemMappingFilter filter = new SysSystemMappingFilter();
		filter.setSystemId(systemId);
		filter.setOperationType(operationType);
		filter.setObjectClassId(getObjectClassId(systemId, objectClassName));

		List<SysSystemMappingDto> systemMappings = systemMappingService.find(filter, null).getContent();
		if (systemMappings.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_MAPPING_NOT_FOUND,
					ImmutableMap.of("systemId", systemId, "objectClassName", objectClassName));
		}
		return systemMappings.get(0);
	}

	/**
	 * Returns existing role's system or returns newly created one.
	 * 
	 * @param systemId
	 * @param roleId
	 * @param objectClassName
	 * @return
	 */
	private UUID getSysRoleSystem(UUID systemId, UUID roleId, String objectClassName) {
		SysRoleSystemFilter filter = new SysRoleSystemFilter();
		filter.setRoleId(roleId);
		filter.setSystemId(systemId);
		List<SysRoleSystemDto> roleSystem = roleSystemService.find(filter, null).getContent();
		if (roleSystem.size() == 1) {
			return roleSystem.stream().findFirst().get().getId();
		}
		SysRoleSystemDto sys = new SysRoleSystemDto();
		sys.setRole(roleId);
		sys.setSystem(systemId);
		sys.setSystemMapping(getSystemMapping(systemId, objectClassName, SystemOperationType.PROVISIONING).getId());
		return roleSystemService.save(sys).getId();
	}

	/**
	 * Returns systems object's scheme
	 * 
	 * @param systemId
	 * @param objectClassName
	 * @return
	 */
	private UUID getObjectClassId(UUID systemId, String objectClassName) {
		SysSchemaObjectClassFilter filter = new SysSchemaObjectClassFilter();
		filter.setSystemId(systemId);
		filter.setObjectClassName(objectClassName);
		List<SysSchemaObjectClassDto> objectClasses = schemaObjectClassService.find(filter, null).getContent();
		if (objectClasses.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_SCHEMA_OBJECT_CLASS_NOT_FOUND,
					ImmutableMap.of("objectClassName", objectClassName, "systemId", systemId));
		}
		return objectClasses.get(0).getId();
	}

	/**
	 * Returns system's attribute mapping
	 * 
	 * @param systemId
	 * @param attributeName
	 * @param objectClassName
	 * @return
	 */
	private SysSystemAttributeMappingDto getSystemAttributeMapping(UUID systemId, String attributeName,
			String objectClassName) {
		SysSchemaAttributeDto schemaAttr = getSchemaAttr(systemId, attributeName, objectClassName);
		SysSystemAttributeMappingFilter filter = new SysSystemAttributeMappingFilter();
		filter.setSystemId(systemId);
		filter.setSchemaAttributeId(schemaAttr.getId());

		List<SysSystemAttributeMappingDto> attributeMappings = systemAttributeMappingService.find(filter, null)
				.getContent();
		if (attributeMappings.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_ATTRIBUTE_MAPPING_NOT_FOUND,
					ImmutableMap.of("schemaAttr", schemaAttr.getName(), "systemId", systemId));
		}
		return attributeMappings.get(0);
	}

	/**
	 * Returns schema attribute
	 * 
	 * @param systemId
	 * @param attributeName
	 * @param objectClassName
	 * @return
	 */
	private SysSchemaAttributeDto getSchemaAttr(UUID systemId, String attributeName, String objectClassName) {
		SysSchemaAttributeFilter filter = new SysSchemaAttributeFilter();
		filter.setObjectClassId(getObjectClassId(systemId, objectClassName));
		filter.setName(attributeName);
		List<SysSchemaAttributeDto> schemas = schemaAttributeService.find(filter, null).getContent();
		if (schemas.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_SCHEMA_ATTRIBUTE_NOT_FOUND,
					ImmutableMap.of("objectClassName", objectClassName, "attributeName", attributeName));
		}
		return schemas.get(0);
	}

	/**
	 * Returns existing system attribute or null
	 * 
	 * @param attr
	 * @return
	 */
	private SysRoleSystemAttributeDto getSystemAttribute(UUID roleSystem, String attributeName) {
		SysRoleSystemAttributeFilter filter = new SysRoleSystemAttributeFilter();
		filter.setRoleSystemId(roleSystem);
		List<SysRoleSystemAttributeDto> content = this.find(filter, null).getContent();
		for (SysRoleSystemAttributeDto attribute : content) {
			if (attribute.getName().equals(attributeName)) {
				return attribute;
			}
		}
		return null;
	}
}
