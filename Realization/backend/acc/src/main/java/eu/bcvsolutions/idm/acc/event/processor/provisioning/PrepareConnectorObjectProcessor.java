package eu.bcvsolutions.idm.acc.event.processor.provisioning;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.config.domain.ProvisioningConfiguration;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningContext;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.ProvisioningAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningArchiveDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.entity.SysSchemaAttribute;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningArchiveService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningOperationService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;
import eu.bcvsolutions.idm.ic.impl.IcConnectorObjectImpl;
import eu.bcvsolutions.idm.ic.impl.IcUidAttributeImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Prepare provisioning - resolve connector object properties from account and resolve create or update operations
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Enabled(AccModuleDescriptor.MODULE_ID)
@Description("Prepares connector object from account properties. Resolves create or update provisioning operation (reads object from target system).")
public class PrepareConnectorObjectProcessor extends AbstractEntityEventProcessor<SysProvisioningOperationDto> {

	public static final String PROCESSOR_NAME = "prepare-connector-object-processor";
	private static final String MODIFIED_FIELD_NAME = "modified";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PrepareConnectorObjectProcessor.class);
	private final SysSystemMappingService systemMappingService;
	private final SysSystemAttributeMappingService attributeMappingService;
	private final IcConnectorFacade connectorFacade;
	private final SysSystemService systemService;
	private final SysProvisioningOperationService provisioningOperationService;
	private final SysSchemaAttributeService schemaAttributeService;
	private final SysProvisioningArchiveService provisioningArchiveService;
	private final SysSchemaObjectClassService schemaObjectClassService;
	private final ProvisioningConfiguration provisioningConfiguration;
	
	@Autowired
	public PrepareConnectorObjectProcessor(
			IcConnectorFacade connectorFacade,
			SysSystemService systemService,
			SysSystemEntityService systemEntityService,
			NotificationManager notificationManager, // @deprecated @since 9.2.2
			SysProvisioningOperationService provisioningOperationService,
			SysSystemMappingService systemMappingService,
			SysSystemAttributeMappingService attributeMappingService,
			SysSchemaAttributeService schemaAttributeService,
			SysProvisioningArchiveService provisioningArchiveService,
			SysSchemaObjectClassService schemaObjectClassService,
			ProvisioningConfiguration provisioningConfiguration) {
		super(ProvisioningEventType.CREATE, ProvisioningEventType.UPDATE);
		//
		Assert.notNull(systemEntityService);
		Assert.notNull(systemMappingService);
		Assert.notNull(attributeMappingService);
		Assert.notNull(connectorFacade);
		Assert.notNull(systemService);
		Assert.notNull(notificationManager);
		Assert.notNull(provisioningOperationService);
		Assert.notNull(schemaAttributeService);
		Assert.notNull(provisioningArchiveService);
		Assert.notNull(schemaObjectClassService);
		Assert.notNull(provisioningConfiguration);
		//
		this.systemMappingService = systemMappingService;
		this.attributeMappingService = attributeMappingService;
		this.connectorFacade = connectorFacade;
		this.systemService = systemService;
		this.provisioningOperationService = provisioningOperationService;
		this.schemaAttributeService = schemaAttributeService;
		this.provisioningArchiveService = provisioningArchiveService;
		this.schemaObjectClassService = schemaObjectClassService;
		this.provisioningConfiguration = provisioningConfiguration;
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	/**
	 * Prepare provisioning operation execution
	 */
	@Override
	public EventResult<SysProvisioningOperationDto> process(EntityEvent<SysProvisioningOperationDto> event) {
		SysProvisioningOperationDto provisioningOperation = event.getContent();
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		IcObjectClass objectClass = provisioningOperation.getProvisioningContext().getConnectorObject().getObjectClass();
		SysSystemEntityDto systemEntity = provisioningOperationService.getByProvisioningOperation(provisioningOperation);
		String uid = systemEntity.getUid();
		boolean isWish = systemEntity.isWish();
		LOG.debug("Start preparing attribubes for provisioning operation [{}] for object with uid [{}] and connector object [{}]", 
				provisioningOperation.getOperationType(),
				uid,
				objectClass.getType());
		//
		// Find connector identification persisted in system
		if (system.getConnectorKey() == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_KEY_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		// load connector configuration
		IcConnectorConfiguration connectorConfig = systemService.getConnectorConfiguration(system);
		if (connectorConfig == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_CONFIGURATION_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		//
		try {
			IcConnectorObject existsConnectorObject = null;
			// We do not want search account on the target system, when this is the first
			// call the connector and auto mapping is not allowed.
			if (!(isWish && !provisioningConfiguration.isAllowedAutoMappingOnExistingAccount())) {
				IcUidAttribute uidAttribute = new IcUidAttributeImpl(null, uid, null);
				existsConnectorObject = connectorFacade.readObject(system.getConnectorInstance(), connectorConfig,
						objectClass, uidAttribute);
			}
			if (existsConnectorObject == null) {
				processCreate(provisioningOperation);
			} else {
				processUpdate(provisioningOperation, connectorConfig, existsConnectorObject);
			}
			//
			LOG.debug("Preparing attribubes for provisioning operation [{}] for object with uid [{}] and connector object [{}] is sucessfully completed", 
					provisioningOperation.getOperationType(), 
					uid,
					objectClass.getType());
			// set back to event content
			provisioningOperation = provisioningOperationService.save(provisioningOperation);
			event.setContent(provisioningOperation);
			return new DefaultEventResult<>(event, this);
		} catch (Exception ex) {
			provisioningOperationService.handleFailed(provisioningOperation, ex);
			// set back to event content
			event.setContent(provisioningOperation);
			return new DefaultEventResult<>(event, this, true);
		}
	}
	
	/**
	 * Create object on target system
	 * 
	 * @param provisioningOperation
	 * @param connectorConfig
	 */
	private void processCreate(SysProvisioningOperationDto provisioningOperation) {
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		ProvisioningContext provisioningContext = provisioningOperation.getProvisioningContext();
		IcConnectorObject connectorObject = provisioningContext.getConnectorObject();
		//
		// prepare provisioning attributes from account attributes
		Map<ProvisioningAttributeDto, Object> fullAccountObject = provisioningOperationService.getFullAccountObject(provisioningOperation);
		if (fullAccountObject != null) {
			connectorObject.getAttributes().clear();
			
			SysSystemMappingDto mapping = getMapping(system, provisioningOperation.getEntityType());
			SysSchemaObjectClassDto schemaObjectClassDto = schemaObjectClassService.get(mapping.getObjectClass());
			
			List<SysSchemaAttributeDto> schemaAttributes = findSchemaAttributes(system, schemaObjectClassDto);
			
			for (Entry<ProvisioningAttributeDto, Object> entry : fullAccountObject.entrySet()) {
				
				ProvisioningAttributeDto provisioningAttribute = entry.getKey();
				Optional<SysSchemaAttributeDto> schemaAttributeOptional = schemaAttributes.stream().filter(schemaAttribute -> {
					return provisioningAttribute.getSchemaAttributeName().equals(schemaAttribute.getName());
				}).findFirst();
				
				if(!schemaAttributeOptional.isPresent()){
					throw new ProvisioningException(AccResultCode.PROVISIONING_SCHEMA_ATTRIBUTE_IS_FOUND, ImmutableMap.of("attribute", provisioningAttribute.getSchemaAttributeName()));
				}
				
				Object idmValue = fullAccountObject.get(provisioningAttribute);
				SysSchemaAttributeDto schemaAttribute = schemaAttributeOptional.get();
				
				if(provisioningAttribute.isSendOnlyIfNotNull()){
					if(this.isValueEmpty(idmValue)){
						// Skip this attribute (marked with flag sendOnlyIfNotNull), because IdM value is null							
						continue;
					}
				}
				
				if(AttributeMappingStrategyType.CREATE == provisioningAttribute.getStrategyType() 
						|| AttributeMappingStrategyType.WRITE_IF_NULL == provisioningAttribute.getStrategyType()){
					
					boolean existSetAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
						return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
								&& AttributeMappingStrategyType.SET == provisioningAttributeKey.getStrategyType();
					}).findFirst().isPresent();
					
					boolean existIfResourceNulltAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
						return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
								&& AttributeMappingStrategyType.WRITE_IF_NULL == provisioningAttributeKey.getStrategyType();
					}).findFirst().isPresent();
					
					boolean existMergeAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
						return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
								&& AttributeMappingStrategyType.MERGE == provisioningAttributeKey.getStrategyType();
					}).findFirst().isPresent();
					
					boolean existAuthMergeAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
						return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
								&& AttributeMappingStrategyType.AUTHORITATIVE_MERGE == provisioningAttributeKey.getStrategyType();
					}).findFirst().isPresent();
					
					if(AttributeMappingStrategyType.CREATE == provisioningAttribute.getStrategyType()){
				
						if(existIfResourceNulltAttribute || existSetAttribute || existAuthMergeAttribute || existMergeAttribute){
							// Skip this attribute (with Create strategy), because exists same attribute with SET/WRITE_IF_NULL/MERGE/AUTH_MERGE strategy 
							// (this strategies has higher priority)							
							continue;
						}
					}
					if(AttributeMappingStrategyType.WRITE_IF_NULL == provisioningAttribute.getStrategyType()){
						
						if(existSetAttribute || existAuthMergeAttribute || existMergeAttribute){
							// Skip this attribute (with WRITE_IF_NULL strategy), because exists same attribute with SET/MERGE/AUTH_MERGE strategy
							// (this strategies has higher priority)							
							continue;
						}
					}
				}
				
				IcAttribute createdAttribute = createAttribute( 
						schemaAttribute,
						fullAccountObject.get(provisioningAttribute));
				if (createdAttribute != null) {
					connectorObject.getAttributes().add(createdAttribute);
				}
			}
			provisioningContext.setConnectorObject(connectorObject);
		}
		provisioningOperation.setOperationType(ProvisioningEventType.CREATE);
	}
	
	@SuppressWarnings("unchecked")
	private void processUpdate(SysProvisioningOperationDto provisioningOperation, IcConnectorConfiguration connectorConfig, IcConnectorObject existsConnectorObject) {
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		String systemEntityUid = provisioningOperationService.getByProvisioningOperation(provisioningOperation).getUid();
		ProvisioningContext provisioningContext = provisioningOperation.getProvisioningContext();
		IcConnectorObject connectorObject = provisioningContext.getConnectorObject();
		IcObjectClass objectClass = connectorObject.getObjectClass();
		//
		IcConnectorObject updateConnectorObject;
		if (provisioningContext.getAccountObject() == null) {
			updateConnectorObject = connectorObject;
		} else {
			Map<ProvisioningAttributeDto, Object> fullAccountObject = provisioningOperationService.getFullAccountObject(provisioningOperation);
			updateConnectorObject = new IcConnectorObjectImpl(systemEntityUid, objectClass, null);
			
			SysSystemMappingDto mapping = getMapping(system, provisioningOperation.getEntityType());
			SysSchemaObjectClassDto schemaObjectClassDto = schemaObjectClassService.get(mapping.getObjectClass());
			List<SysSchemaAttributeDto> schemaAttributes = findSchemaAttributes(system, schemaObjectClassDto);
			
			SysProvisioningOperationFilter filter = new  SysProvisioningOperationFilter();
			filter.setEntityIdentifier(provisioningOperation.getEntityIdentifier());
			filter.setEntityType(provisioningOperation.getEntityType());
			filter.setResultState(OperationState.EXECUTED);
			
			SysProvisioningArchiveDto lastSuccessEntity = null;
			
			for ( Entry<ProvisioningAttributeDto, Object> entry : fullAccountObject.entrySet()) {
				
				ProvisioningAttributeDto provisioningAttribute = entry.getKey();  
				Optional<SysSchemaAttributeDto> schemaAttributeOptional = schemaAttributes.stream().filter(schemaAttribute -> {
					return provisioningAttribute.getSchemaAttributeName().equals(schemaAttribute.getName());
				}).findFirst();
				
				if(!schemaAttributeOptional.isPresent()){
					throw new ProvisioningException(AccResultCode.PROVISIONING_SCHEMA_ATTRIBUTE_IS_FOUND, ImmutableMap.of("attribute", provisioningAttribute.getSchemaAttributeName()));
				}

				SysSchemaAttributeDto schemaAttribute = schemaAttributeOptional.get();
				if (schemaAttribute.isUpdateable()) {
					if (schemaAttribute.isReturnedByDefault()) {
						Object idmValue = fullAccountObject.get(provisioningAttribute);
						IcAttribute attribute = existsConnectorObject.getAttributeByName(schemaAttribute.getName());
						Object connectorValue = attribute != null ? (attribute.isMultiValue() ? attribute.getValues() : attribute.getValue()) : null;
						if (connectorValue != null && !(connectorValue instanceof List)) {
							connectorValue = Lists.newArrayList(connectorValue);
						}
						
						Object resultValue = idmValue;
						
						
						if(AttributeMappingStrategyType.CREATE == provisioningAttribute.getStrategyType()){
							// We do update, attributes with create strategy will be skipped
							continue;
						}
						
						if(provisioningAttribute.isSendOnlyIfNotNull()){
							if(this.isValueEmpty(idmValue)){
								// Skip this attribute (marked with flag sendOnlyIfNotNull), because idm value is null							
								continue;
							}
						}
						
						if(AttributeMappingStrategyType.WRITE_IF_NULL == provisioningAttribute.getStrategyType()){
							
							boolean existSetAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
								return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
										&& AttributeMappingStrategyType.SET == provisioningAttributeKey.getStrategyType();
							}).findFirst().isPresent();
							
							boolean existMergeAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
								return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
										&& AttributeMappingStrategyType.MERGE == provisioningAttributeKey.getStrategyType();
							}).findFirst().isPresent();
							
							boolean existAuthMergeAttribute = fullAccountObject.keySet().stream().filter(provisioningAttributeKey -> {
								return provisioningAttributeKey.getSchemaAttributeName().equals(schemaAttribute.getName()) 
										&& AttributeMappingStrategyType.AUTHORITATIVE_MERGE == provisioningAttributeKey.getStrategyType();
							}).findFirst().isPresent();
							
							if(AttributeMappingStrategyType.WRITE_IF_NULL == provisioningAttribute.getStrategyType()){
								List<IcAttribute> icAttributes = existsConnectorObject.getAttributes();
								//
								Optional<IcAttribute> icAttributeOptional = icAttributes.stream()
										.filter(ica -> {
											return schemaAttribute.getName().equals(ica.getName());
										})
										.findFirst();
								IcAttribute icAttribute = null;
								if (icAttributeOptional.isPresent()) {
									icAttribute = icAttributeOptional.get();
								}
								// We need do transform from resource first
								Object transformedConnectorValue = this.transformValueFromResource(provisioningAttribute.getTransformValueFromResourceScript()
										, schemaAttribute, icAttribute, icAttributes, system);
								
								if(transformedConnectorValue != null || existSetAttribute || existAuthMergeAttribute || existMergeAttribute){
									// Skip this attribute (with Write if null strategy), because connector value is not null
									// or exists same attribute with  SET/MERGE/AUTH_MERGE strategy (this strategies has higher priority)								
									continue;
								}
							}
							
						}
						
						if(AttributeMappingStrategyType.MERGE == provisioningAttribute.getStrategyType()){
							
							// Load last provisioning history
							if(lastSuccessEntity == null){
								List<SysProvisioningArchiveDto> lastSuccessEntities = provisioningArchiveService.find(filter,
										new PageRequest(0, 1, new Sort(Direction.DESC, MODIFIED_FIELD_NAME))).getContent();
								if(!lastSuccessEntities.isEmpty()){
									lastSuccessEntity = lastSuccessEntities.get(0);
								}
							}
							
						 	// Merge IdM values with connector values
							if(connectorValue instanceof List){
								List<Object> connectorValues = new ArrayList<>((List<Object>)connectorValue);
								List<Object> idmValues = null;
								if(idmValue instanceof List){
									idmValues = (List<Object>) idmValue;
								}
								if(idmValues != null){
									idmValues.stream().forEach(value -> {
										if(!connectorValues.contains(value)){
											connectorValues.add(value);
										}
									});
								} 
	
								resultValue = connectorValues;
							}

							// Delete missing values by last provisioning history
							if( lastSuccessEntity != null && lastSuccessEntity.getProvisioningContext() != null 
									&& lastSuccessEntity.getProvisioningContext().getAccountObject() != null 
									&& lastSuccessEntity.getProvisioningContext().getAccountObject().containsKey(provisioningAttribute)){
								Object oldValue = lastSuccessEntity.getProvisioningContext().getAccountObject().get(provisioningAttribute);
								if(oldValue instanceof List){
									if(!oldValue.equals(idmValue)){
										// Search all deleted values (managed by IdM) by founded last provisioning values
										List<?> deletedValues = ((List<?>)oldValue).stream().filter(value -> {
											List<?> idmValues = null;
											if(idmValue instanceof List){
												idmValues = (List<?>) idmValue;
											}
											if(idmValues != null && idmValues.contains(value)){
												return false;
											} 
											return true;
										}).collect(Collectors.toList());
										if(resultValue instanceof List){
											List<?> resultValues = new ArrayList<>((List<Object>)resultValue);
											
											// Remove all deleted values (managed by IdM) 
											resultValues.removeAll(deletedValues);
											resultValue = resultValues;
										}
									}
								}
							}
						}
						
					 	// Update attribute on resource by given mapping
						// attribute and mapped value in entity
						IcAttribute updatedAttribute = updateAttribute(
								systemEntityUid, 
								resultValue, 
								schemaAttribute, 
								existsConnectorObject,
								system,
								provisioningAttribute);
						if (updatedAttribute != null) {
							updateConnectorObject.getAttributes().add(updatedAttribute);
						}
					} else {
						// create attribute without target system read - password etc.
						// filled values only
						if (fullAccountObject.get(provisioningAttribute) != null) {
							IcAttribute createdAttribute = createAttribute( 
									schemaAttribute,
									fullAccountObject.get(provisioningAttribute));
							if (createdAttribute != null) {
								updateConnectorObject.getAttributes().add(createdAttribute);
							}
						}
					}
				}
			}
		}
		//
		provisioningOperation.getProvisioningContext().setConnectorObject(updateConnectorObject);
		provisioningOperation.setOperationType(ProvisioningEventType.UPDATE);
	}
	
	private boolean isValueEmpty(Object idmValue){
		if(idmValue == null){
			return true;
		}
		
		if(idmValue instanceof String && Strings.isNullOrEmpty((String) idmValue)){
			return true;
		}
		
		if(idmValue instanceof List && (CollectionUtils.isEmpty((List<?>)idmValue) 
				|| (((List<?>)idmValue).size() == 1 && ((List<?>)idmValue).get(0) == null))){
			return true;
		}
		
		return false;
	}
	
	private SysSystemMappingDto getMapping(SysSystemDto system, SystemEntityType entityType) {
		List<SysSystemMappingDto> systemMappings = systemMappingService.findBySystem(system, SystemOperationType.PROVISIONING, entityType);
		if (systemMappings == null || systemMappings.isEmpty()) {
			throw new IllegalStateException(MessageFormat.format("System [{0}] does not have mapping, provisioning will not be executed. Add some mapping for entity type [{1}]", system.getName(), entityType));
		}
		if (systemMappings.size() != 1) {
			throw new IllegalStateException(MessageFormat.format("System [{0}] is wrong configured! Remove duplicit mapping for entity type [{1}]", system.getName(), entityType));
		}
		return systemMappings.get(0);
	}
	
	/**
	 * Find list of {@link SysSchemaAttribute} by system and objectClass
	 * 
	 * @param objectClass
	 * @param system
	 * @return
	 */
	private List<SysSchemaAttributeDto> findSchemaAttributes(SysSystemDto system, SysSchemaObjectClassDto objectClass) {
		
		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());
		schemaAttributeFilter.setObjectClassId(objectClass.getId());
		return schemaAttributeService.find(schemaAttributeFilter, null).getContent();
	}
	
	/**
	 * Create IC attribute by schema attribute. IC attribute will be set with
	 * value obtained form given entity. This value will be transformed to
	 * system value first.
	 * 
	 * @param uid
	 * @param entity
	 * @param connectorObjectForCreate
	 * @param attributeMapping
	 * @param schemaAttribute
	 * @param objectClassName
	 */
	private IcAttribute createAttribute(SysSchemaAttributeDto schemaAttribute, Object idmValue){
		if (!schemaAttribute.isCreateable()) {
			return null;
		}
		return attributeMappingService.createIcAttribute(schemaAttribute, idmValue);
	}

	/**
	 * Update attribute on resource by given handling attribute and mapped value
	 * in entity
	 * 
	 * @param uid
	 * @param entity
	 * @param attributeMapping
	 * @param schemaAttribute
	 * @param connectorObject
	 */
	private IcAttribute updateAttribute(String uid, Object idmValue, SysSchemaAttributeDto schemaAttribute, IcConnectorObject existsConnectorObject, SysSystemDto system, ProvisioningAttributeDto provisioningAttributeDto) {
		List<IcAttribute> icAttributes = existsConnectorObject.getAttributes();
		//
		Optional<IcAttribute> icAttributeOptional = icAttributes.stream()
				.filter(ica -> {
					return schemaAttribute.getName().equals(ica.getName());
					})
				.findFirst();
		IcAttribute icAttribute = null;
		if (icAttributeOptional.isPresent()) {
			icAttribute = icAttributeOptional.get();
		}
	
		return updateAttributeValue(uid, idmValue, schemaAttribute, icAttribute, icAttributes, system, provisioningAttributeDto.getTransformValueFromResourceScript(), provisioningAttributeDto.isSendAlways());
	}

	/**
	 * Check difference of attribute value on resource and in entity for given
	 * attribute. When is value changed, then add update of this attribute to
	 * map
	 * 
	 */
	private IcAttribute updateAttributeValue(String uid, Object idmValue, SysSchemaAttributeDto schemaAttribute, IcAttribute icAttribute, List<IcAttribute> icAttributes, SysSystemDto system, String transformValueFromResourceScript, boolean sendAlways){

		Object icValueTransformed = transformValueFromResource(transformValueFromResourceScript, schemaAttribute, icAttribute,
				icAttributes, system);
		if (sendAlways || (!isAttributeValueEquals(idmValue, icValueTransformed, schemaAttribute))) {
			// values is not equals
			// Or this attribute must be send every time (event if was not changed)
			return attributeMappingService.createIcAttribute(schemaAttribute, idmValue);
		}
		return null;
	}
	
	/**
	 * Check if is value from IDM and value from System equals. 
	 * If is attribute multivalued, then is IDM value transformed to List.
	 * @param idmValue
	 * @param icValueTransformed
	 * @param schemaAttribute
	 * @return
	 */
	private boolean isAttributeValueEquals(Object idmValue, Object icValueTransformed, SysSchemaAttributeDto schemaAttribute){
		if(schemaAttribute.isMultivalued() && idmValue != null && !(idmValue instanceof List)){
			List<Object> values = new ArrayList<>();
			values.add(idmValue);
			return Objects.equals(values, icValueTransformed);
		} 
		
		// Multivalued values are equals, when value from system is null and value in IdM is empty list
		if(schemaAttribute.isMultivalued() && idmValue instanceof Collection && ((Collection<?>)idmValue).isEmpty() && icValueTransformed == null) {
			return true;
		}
		
		// Multivalued values are equals, when value in IdM is null and value from system is empty list
		if(schemaAttribute.isMultivalued() && icValueTransformed instanceof Collection && ((Collection<?>)icValueTransformed).isEmpty() && idmValue == null) {
			return true;
		}
				
		return Objects.equals(idmValue, icValueTransformed);
	}

	private Object transformValueFromResource(String transformValueFromResourceScript,
			SysSchemaAttributeDto schemaAttribute, IcAttribute icAttribute, List<IcAttribute> icAttributes,
			SysSystemDto system) {
		
		Object icValueTransformed = null;
		if (schemaAttribute.isMultivalued()) {
			// Multi value
			List<Object> icValues = icAttribute != null ? icAttribute.getValues() : null;
			icValueTransformed = attributeMappingService.transformValueFromResource(icValues, transformValueFromResourceScript,
					icAttributes, system);
		} else {
			// Single value
			Object icValue = icAttribute != null ? icAttribute.getValue() : null;
			icValueTransformed = attributeMappingService.transformValueFromResource(icValue, transformValueFromResourceScript,
					icAttributes, system);
		}
		return icValueTransformed;
	}
	
	@Override
	public int getOrder() {
		// before realization
		return -1000;
	}
}
