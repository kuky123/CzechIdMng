package eu.bcvsolutions.idm.acc.event.processor.provisioning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.dto.ProvisioningAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningOperationService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Execute provisioning
 * 
 * @author Radek Tomiška
 *
 */
@Enabled(AccModuleDescriptor.MODULE_ID)
public abstract class AbstractProvisioningProcessor extends AbstractEntityEventProcessor<SysProvisioningOperationDto> {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractProvisioningProcessor.class);
	protected final IcConnectorFacade connectorFacade;
	protected final SysSystemService systemService;
	private final SysSystemEntityService systemEntityService;
	protected final SysProvisioningOperationService provisioningOperationService;

	public AbstractProvisioningProcessor(
			IcConnectorFacade connectorFacade,
			SysSystemService systemService,
			SysProvisioningOperationService provisioningOperationService,
			SysSystemEntityService systemEntityService,
			ProvisioningEventType... provisioningOperationType) {
		super(provisioningOperationType);
		//
		Assert.notNull(connectorFacade);
		Assert.notNull(systemService);
		Assert.notNull(provisioningOperationService);
		Assert.notNull(systemEntityService);
		//
		this.connectorFacade = connectorFacade;
		this.systemService = systemService;
		this.provisioningOperationService = provisioningOperationService;
		this.systemEntityService = systemEntityService;
	}
	
	/**
	 * Execute provisioning operation
	 * 
	 * @param provisioningOperation
	 * @param connectorConfig
	 */
	protected abstract IcUidAttribute processInternal(SysProvisioningOperationDto provisioningOperation, IcConnectorConfiguration connectorConfig);
	
	/**
	 * Prepare provisioning operation execution
	 */
	@Override
	public EventResult<SysProvisioningOperationDto> process(EntityEvent<SysProvisioningOperationDto> event) {				
		SysProvisioningOperationDto provisioningOperation = event.getContent();
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		IcConnectorObject connectorObject = provisioningOperation.getProvisioningContext().getConnectorObject();
		IcObjectClass objectClass = connectorObject.getObjectClass();
		SysSystemEntityDto systemEntity = systemEntityService.getByProvisioningOperation(provisioningOperation);
		LOG.debug("Start provisioning operation [{}] for object with uid [{}] and connector object [{}]", 
				provisioningOperation.getOperationType(),
				systemEntity.getUid(),
				objectClass.getType());
		//
		// Find connector identification persisted in system
		if (system.getConnectorKey() == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_KEY_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		// load connector configuration
		IcConnectorConfiguration connectorConfig = systemService.getConnectorConfiguration(systemService.get(provisioningOperation.getSystem()));
		if (connectorConfig == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_CONFIGURATION_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		//
		try {
			provisioningOperation = provisioningOperationService.save(provisioningOperation);
			// convert confidential string to guarded strings before provisioning realization
			connectorObject = provisioningOperationService.getFullConnectorObject(provisioningOperation);
			provisioningOperation.getProvisioningContext().setConnectorObject(connectorObject);
			//
			IcUidAttribute resultUid = processInternal(provisioningOperation, connectorConfig);
			// update system entity, when identifier on target system differs
			if (resultUid != null && resultUid.getUidValue() != null) {
				if(!systemEntity.getUid().equals(resultUid.getUidValue()) || systemEntity.isWish()) {
					systemEntity.setUid(resultUid.getUidValue());
					systemEntity.setWish(false);
					systemEntity = systemEntityService.save(systemEntity);
					LOG.info("UID was changed. System entity with uid [{}] was updated", systemEntity.getUid());
				}
			} else { // e.g. update doesn't return
				if (systemEntity.isWish()) {
					systemEntity.setWish(false);
					systemEntity = systemEntityService.save(systemEntity);
					LOG.info("UID was changed. System entity with uid [{}] was updated", systemEntity.getUid());
				}
			}
			
			provisioningOperationService.handleSuccessful(provisioningOperation);
		} catch (Exception ex) {
			provisioningOperationService.handleFailed(provisioningOperation, ex);
		}

		// set operation back to content
		event.setContent(provisioningOperation);
		return new DefaultEventResult<>(event, this);
	}
	
	@Override
	public int getOrder() {
		// default order - 0 - its default implementation
		return CoreEvent.DEFAULT_ORDER;
	}

	/**
	 * Transform all ic attributes that is in guarded string into simple string.
	 * Attribute class type in schema must be also String.
	 *
	 * @return 
	 *
	 */
	protected List<IcAttribute> transformGuardedStringToString(SysProvisioningOperationDto provisioningOperation, List<IcAttribute> attributes) {
		Map<ProvisioningAttributeDto, Object> accountObject = provisioningOperation.getProvisioningContext()
				.getAccountObject();

		// account object doesn't exist return given attributes
		if (accountObject == null) {
			return attributes;
		}
		
		Set<ProvisioningAttributeDto> keySet = accountObject.keySet();
		List<IcAttribute> finalAttributes = new ArrayList<>(attributes);

		// Iterate over all ic attributes and search all password attributes
		// with original class type String not guarded string
		for (IcAttribute attribute : attributes) {
			Optional<ProvisioningAttributeDto> firstProvisioningAttribute = keySet.stream()
					.filter( //
							provisioningAttribute -> provisioningAttribute.getSchemaAttributeName()
									.equals(attribute.getName())) //
					.findFirst(); //

			if (firstProvisioningAttribute.isPresent()) {
				ProvisioningAttributeDto provisioningAttributeDto = firstProvisioningAttribute.get();

				// Some process or etc create provisioning operation directly without set classType we can't transform guarded string back to string 
				if (provisioningAttributeDto.getClassType() == null) {
					continue;
				}

				// If is provisioning attribute password and his schema attribute class type is equals to string transform to string (temporar passwords or etc.)
				if (provisioningAttributeDto.isPasswordAttribute()
						&& provisioningAttributeDto.getClassType().equals(String.class.getName())) {
					Object valueAsObject = attribute.getValue();
					if (valueAsObject instanceof GuardedString) {
						GuardedString valueAsGuardedString = (GuardedString) valueAsObject;
						// replace attribute with attribute as simple string
						finalAttributes.remove(attribute);
						IcAttributeImpl attributeImpl = new IcAttributeImpl(attribute.getName(),
								valueAsGuardedString.asString());
						finalAttributes.add(attributeImpl);
					}
				}
			}
			
		}
		return finalAttributes;
	}
}
