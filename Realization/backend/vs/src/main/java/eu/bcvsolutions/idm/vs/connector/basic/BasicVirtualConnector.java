package eu.bcvsolutions.idm.vs.connector.basic;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemEntityFilter;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.api.IcAttributeInfo;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorInfo;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.api.IcObjectClassInfo;
import eu.bcvsolutions.idm.ic.api.IcSchema;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;
import eu.bcvsolutions.idm.ic.api.annotation.IcConnectorClass;
import eu.bcvsolutions.idm.ic.czechidm.domain.CzechIdMIcConvertUtil;
import eu.bcvsolutions.idm.ic.czechidm.domain.IcConnectorConfigurationCzechIdMImpl;
import eu.bcvsolutions.idm.ic.exception.IcException;
import eu.bcvsolutions.idm.ic.filter.api.IcFilter;
import eu.bcvsolutions.idm.ic.filter.api.IcResultsHandler;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.impl.IcAttributeInfoImpl;
import eu.bcvsolutions.idm.ic.impl.IcConnectorObjectImpl;
import eu.bcvsolutions.idm.ic.impl.IcObjectClassImpl;
import eu.bcvsolutions.idm.ic.impl.IcObjectClassInfoImpl;
import eu.bcvsolutions.idm.ic.impl.IcSchemaImpl;
import eu.bcvsolutions.idm.ic.impl.IcUidAttributeImpl;
import eu.bcvsolutions.idm.vs.connector.api.VsVirtualConnector;
import eu.bcvsolutions.idm.vs.domain.VsOperationType;
import eu.bcvsolutions.idm.vs.domain.VsRequestState;
import eu.bcvsolutions.idm.vs.dto.VsAccountDto;
import eu.bcvsolutions.idm.vs.dto.VsRequestDto;
import eu.bcvsolutions.idm.vs.dto.filter.VsAccountFilter;
import eu.bcvsolutions.idm.vs.entity.VsAccount;
import eu.bcvsolutions.idm.vs.entity.VsAccount_;
import eu.bcvsolutions.idm.vs.service.api.VsAccountService;
import eu.bcvsolutions.idm.vs.service.api.VsRequestService;

@IcConnectorClass(displayName = "Virtual system connector", framework = "czechidm", name = "virtual-system-basic", version = "1.0.1", configurationClass = BasicVirtualConfiguration.class)
public class BasicVirtualConnector implements VsVirtualConnector {

	private static final Logger LOG = LoggerFactory.getLogger(BasicVirtualConnector.class);

	@Autowired
	private FormService formService;
	@Autowired
	private IdmFormAttributeService formAttributeService;
	@Autowired
	private SysSystemService systemService;
	@Autowired
	private VsAccountService accountService;
	@Autowired
	private SysSystemEntityService systemEntityService;
	@Autowired
	private VsRequestService requestService;

	private BasicVirtualConfiguration virtualConfiguration;
	private IcConnectorConfiguration configuration;
	private IdmFormDefinitionDto formDefinition;
	private String connectorKey;
	private UUID systemId;

	@Override
	public void init(IcConnectorConfiguration configuration) {
		Assert.notNull(configuration);
		this.configuration = configuration;

		if (!(configuration instanceof IcConnectorConfigurationCzechIdMImpl)) {
			throw new IcException(
					MessageFormat.format("Connector configuration for virtual system must be instance of [{0}]",
							IcConnectorConfigurationCzechIdMImpl.class.getName()));
		}

		systemId = ((IcConnectorConfigurationCzechIdMImpl) configuration).getSystemId();
		if (systemId == null) {
			throw new IcException("System ID cannot be null (for virtual system)");
		}
		SysSystemDto system = this.systemService.get(systemId);
		if (system == null) {
			throw new IcException("System cannot be null (for virtual system)");
		}

		IcConnectorClass connectorAnnotation = this.getClass().getAnnotation(IcConnectorClass.class);
		IcConnectorInfo info = CzechIdMIcConvertUtil.convertConnectorClass(connectorAnnotation, this.getClass());

		// Load configuration object
		virtualConfiguration = (BasicVirtualConfiguration) CzechIdMIcConvertUtil
				.convertIcConnectorConfiguration(configuration, connectorAnnotation.configurationClass());

		// Validate configuration
		virtualConfiguration.validate();

		connectorKey = info.getConnectorKey().getFullName();
		
		String virtualSystemKey = MessageFormat.format("{0}:systemId={1}", connectorKey, systemId.toString());
		String type = VsAccount.class.getName();
		formDefinition = this.formService.getDefinition(type, virtualSystemKey);
	}

	@Override
	public IcUidAttribute update(IcUidAttribute uid, IcObjectClass objectClass, List<IcAttribute> attributes) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(attributes, "Attributes cannot be null!");
		Assert.notNull(uid, "UID cannot be null!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}
		String uidValue = uid.getUidValue();

		if (uidValue == null) {
			throw new IcException("UID value cannot be null!");
		}

		VsRequestDto request = createRequest(objectClass, attributes, (String) uidValue, VsOperationType.UPDATE);
		return requestService.execute(request);
	}

	@Override
	public IcUidAttribute internalUpdate(IcUidAttribute uid, IcObjectClass objectClass, List<IcAttribute> attributes) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(attributes, "Attributes cannot be null!");
		Assert.notNull(uid, "UID cannot be null!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}
		String uidValue = uid.getUidValue();

		if (uidValue == null) {
			throw new IcException("UID value cannot be null!");
		}

		// Find account by UID and System ID
		VsAccountDto account = accountService.findByUidSystem(uidValue, systemId);
		if (account == null) {
			throw new IcException(MessageFormat.format("Vs account was not found for UID [{0}] and system ID [{1}]!",
					uidValue, systemId));
		}

		// Update UID - if is different
		IcAttribute uidAttribute = geAttribute(attributes, IcAttributeInfo.NAME);
		if (uidAttribute != null) {
			Object attributeUidValue = uidAttribute.getValue();
			if (!(attributeUidValue instanceof String)) {
				throw new IcException(
						MessageFormat.format("UID attribute value [{0}] must be String!", attributeUidValue));
			}
			if (!uidValue.equals(attributeUidValue)) {
				// TODO: Connector not supported more entity types!
				LOG.info("Update account - UID is different (old: {} new: {})", uidValue, attributeUidValue);
				account.setUid((String) attributeUidValue);
				account = accountService.save(account);
				// We have to change system entity directly from VS module
				// (request can be started/executed async => standard
				// process update UID in system entity (ACC module) will not
				// works!)
				updateSystemEntity(uidValue, attributeUidValue, true);
			}
		}

		// Update ENABLE - if is different
		IcAttribute enableAttribute = geAttribute(attributes, IcAttributeInfo.ENABLE);
		if (enableAttribute != null && this.virtualConfiguration.isDisableSupported()) {
			Object attributeEnableValue = enableAttribute.getValue();
			if (!(attributeEnableValue instanceof Boolean)) {
				throw new IcException(
						MessageFormat.format("ENABLE attribute value [{0}] must be Boolean!", attributeEnableValue));
			}
			if (account.isEnable() != (Boolean) attributeEnableValue) {
				account.setEnable((Boolean) attributeEnableValue);
				account = accountService.save(account);
			}
		}

		UUID accountId = account.getId();

		// Update extended attributes
		Arrays.asList(virtualConfiguration.getAttributes()).forEach(virtualAttirbute -> {
			updateFormAttributeValue(uidValue, virtualAttirbute, accountId, attributes);
		});

		return new IcUidAttributeImpl(IcAttributeInfo.NAME, account.getUid(), null);
	}

	@Override
	public IcUidAttribute create(IcObjectClass objectClass, List<IcAttribute> attributes) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(attributes, "Attributes cannot be null!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}
		IcAttribute uidAttribute = geAttribute(attributes, IcAttributeInfo.NAME);

		if (uidAttribute == null) {
			throw new IcException("UID attribute was not found!");
		}
		Object uidValue = uidAttribute.getValue();
		if (!(uidValue instanceof String)) {
			throw new IcException(MessageFormat.format("UID attribute value [{0}] must be String!", uidValue));
		}

		// Create and execute request
		VsRequestDto request = createRequest(objectClass, attributes, (String) uidValue, VsOperationType.CREATE);
		return requestService.execute(request);
	}

	@Override
	public IcUidAttribute internalCreate(IcObjectClass objectClass, List<IcAttribute> attributes) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(attributes, "Attributes cannot be null!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}
		IcAttribute uidAttribute = geAttribute(attributes, IcAttributeInfo.NAME);

		if (uidAttribute == null) {
			throw new IcException("UID attribute was not found!");
		}
		Object uidValue = uidAttribute.getValue();
		if (!(uidValue instanceof String)) {
			throw new IcException(MessageFormat.format("UID attribute value [{0}] must be String!", uidValue));
		}
		String uid = (String) uidValue;

		// Find account by UID and System ID - If will be found, then we will do
		// update instead create
		VsAccountDto account = accountService.findByUidSystem(uid, systemId);
		if (account != null) {
			LOG.info("Create account - Virtual system account for UID [{}] already exist. We will execute update!",
					uidValue);
			return this.internalUpdate(new IcUidAttributeImpl(null, uid, null), objectClass, attributes);
		}

		account = new VsAccountDto();
		// Set ENABLE - if is supported
		IcAttribute enableAttribute = geAttribute(attributes, IcAttributeInfo.ENABLE);
		if (enableAttribute != null && this.virtualConfiguration.isDisableSupported()) {
			Object attributeEnableValue = enableAttribute.getValue();
			if (!(attributeEnableValue instanceof Boolean)) {
				throw new IcException(
						MessageFormat.format("ENABLE attribute value [{0}] must be Boolean!", attributeEnableValue));
			}
			account.setEnable((Boolean) attributeEnableValue);
		}

		account.setUid(uid);
		account.setSystemId(this.systemId);
		account.setConnectorKey(connectorKey);

		account = accountService.save(account);
		UUID accountId = account.getId();

		// Attributes from definition and configuration
		Arrays.asList(virtualConfiguration.getAttributes()).forEach(virtualAttirbute -> {
			updateFormAttributeValue(uidValue, virtualAttirbute, accountId, attributes);
		});
		
		// We have to change system entity directly (set wish=false!!!) from VS module
		// (request can be started/executed async => standard
		// process update UID in system entity (ACC module) will not
		// works!)
		updateSystemEntity(uid, uid, false);

		return new IcUidAttributeImpl(IcAttributeInfo.NAME, account.getUid(), null);
	}

	@Override
	public void delete(IcUidAttribute uid, IcObjectClass objectClass) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		String uidValue = validateAndGetUid(uid);

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}

		// Create and execute request
		VsRequestDto request = createRequest(objectClass, null, (String) uidValue, VsOperationType.DELETE);
		requestService.execute(request);
	}

	@Override
	public void internalDelete(IcUidAttribute uid, IcObjectClass objectClass) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		String uidValue = this.validateAndGetUid(uid);

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}

		// Find account by UID and System ID
		VsAccountDto account = accountService.findByUidSystem(uidValue, systemId);
		if (account == null) {
			throw new IcException(MessageFormat.format("Vs account was not found for UID [{0}] and system ID [{1}]!",
					uidValue, systemId));
		}

		// Delete vs account and connected form values
		accountService.delete(account);
	}

	@Override
	public IcConnectorObject read(IcUidAttribute uid, IcObjectClass objectClass) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(uid, "UID cannot be null!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}
		String uidValue = uid.getUidValue();

		if (uidValue == null) {
			throw new IcException("UID value cannot be null!");
		}

		// Find account by UID and System ID
		VsAccountDto account = accountService.findByUidSystem(uidValue, systemId);

		// All attributes from VS account
		List<IcAttribute> vsAttributes = new ArrayList<>();

		// Create uid attribute
		IcAttributeImpl uidAttribute = new IcAttributeImpl(IcAttributeInfo.NAME, uidValue);
		vsAttributes.add(uidAttribute);

		if (account != null) {

			// Create enable attribute
			if (this.virtualConfiguration.isDisableSupported()) {
				IcAttributeImpl enableAttribute = new IcAttributeImpl(IcAttributeInfo.ENABLE, account.isEnable());
				vsAttributes.add(enableAttribute);
			}
			// Attributes from definition and configuration
			UUID accountId = account.getId();
			Arrays.asList(virtualConfiguration.getAttributes()).forEach(virtualAttirbute -> {
				IcAttribute attribute = accountService.getIcAttribute(accountId, virtualAttirbute, formDefinition);
				if (attribute == null) {
					return;
				}
				vsAttributes.add(attribute);
			});
		}

		// Overwrite attributes form VS account with attributes from unresloved
		// requests
		List<IcAttribute> attributes = this.overwriteAttributesByUnresolvedRequests(account, uidValue, vsAttributes);
		if (attributes == null) {
			return null;
		}

		IcConnectorObjectImpl connectorObject = new IcConnectorObjectImpl();
		connectorObject.setUidValue(uidValue);
		connectorObject.setObjectClass(new IcObjectClassImpl(IcObjectClassInfo.ACCOUNT));
		connectorObject.setAttributes(attributes);
		return connectorObject;
	}

	@Override
	public void search(IcObjectClass objectClass, IcFilter filter, IcResultsHandler handler) {
		Assert.notNull(objectClass, "Object class cannot be null!");
		Assert.notNull(handler, "Result handler cannot be null for search operation!");

		if (!IcObjectClassInfo.ACCOUNT.equals(objectClass.getType())) {
			throw new IcException("Only ACCOUNT object class is supported now!");
		}

		if (filter == null) {
			Pageable pageable = new PageRequest(0, 10);
			searchByPage(handler, pageable);
		} else {
			// TODO: Search by filter
			throw new IcException(
					"Virtual system connector does not support search by filter! Filter must be null!. It means search return always all accounts.");
		}

	}

	@Override
	public IcSchema schema() {
		if (this.formDefinition == null) {
			return null;
		}

		return generateSchema();
	}
	
	@Override
	public BasicVirtualConfiguration getVirtualConfiguration() {
		return virtualConfiguration;
	}

	@Override
	public IcConnectorConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Overwrite attributes form VS account with attributes from unresloved requests
	 * 
	 * @param account
	 * 
	 * @param account
	 * @param vsAttributes
	 * @return
	 */
	private List<IcAttribute> overwriteAttributesByUnresolvedRequests(VsAccountDto account, String uid,
			List<IcAttribute> vsAttributes) {
		Map<String, IcAttribute> attributesMap = new HashMap<>();
		List<VsRequestDto> unresolvedRequests = requestService.findDuplicities(uid, this.systemId);

		vsAttributes.forEach(attribute -> {
			attributesMap.put(attribute.getName(), attribute);
		});

		if (unresolvedRequests != null) {
			unresolvedRequests = Lists.reverse(unresolvedRequests);
			boolean deleteAccount = false;
			boolean createAccount = false;
			for (VsRequestDto request : unresolvedRequests) {
				if (VsOperationType.DELETE == request.getOperationType()) {
					deleteAccount = true;
					createAccount = false;
					continue;
				}
				if (VsOperationType.CREATE == request.getOperationType()) {
					deleteAccount = false;
					createAccount = true;
				}
				// VsRequestDto fullRequest =
				// requestService.get(request.getId());
				VsRequestDto fullRequest = request;
				if (fullRequest.getConnectorObject() != null
						&& fullRequest.getConnectorObject().getAttributes() != null) {
					fullRequest.getConnectorObject().getAttributes().forEach(attribute -> {
						attributesMap.put(attribute.getName(), attribute);
					});
				}
			}
			// If exits delete request (and not exist next create request), then
			// return null
			if (deleteAccount) {
				return null;
			}
			// If VS account not exists, then must exist create request, else
			// return null
			if (account == null && !createAccount) {
				return null;
			}
		}
		return new ArrayList<>(attributesMap.values());
	}

	/**
	 * Do search for given page and invoke result handler
	 * 
	 * @param handler
	 * @param pageable
	 */
	private void searchByPage(IcResultsHandler handler, Pageable pageable) {
		VsAccountFilter accountFilter = new VsAccountFilter();
		accountFilter.setSystemId(systemId);
		Page<VsAccountDto> resultsPage = accountService.find(accountFilter, pageable);
		List<VsAccountDto> results = resultsPage.getContent();
		for(VsAccountDto account : results) {
			boolean canContinue = handler
					.handle(this.read(new IcUidAttributeImpl(IcAttributeInfo.NAME, account.getUid(), null),
							new IcObjectClassImpl(IcObjectClassInfo.ACCOUNT)));
			if (!canContinue) {
				// Handler stop next searching
				return;
			}
		}
		if (resultsPage.hasNext()) {
			this.searchByPage(handler, resultsPage.nextPageable());
		}
	}

	/**
	 * Generate schema from connector configuration and form definition
	 * 
	 * @return
	 */
	private IcSchemaImpl generateSchema() {
		IcSchemaImpl schema = new IcSchemaImpl();
		List<IcObjectClassInfo> objectClasses = schema.getDeclaredObjectClasses();
		IcObjectClassInfoImpl objectClass = new IcObjectClassInfoImpl();
		objectClass.setType(IcObjectClassInfo.ACCOUNT);
		List<IcAttributeInfo> attributes = objectClass.getAttributeInfos();
		// Create UID schema attribute
		IcAttributeInfoImpl attributeUid = new IcAttributeInfoImpl();
		attributeUid.setClassType(String.class.getName());
		attributeUid.setCreateable(true);
		attributeUid.setMultivalued(false);
		attributeUid.setName(IcAttributeInfo.NAME);
		attributeUid.setNativeName(VsAccount_.uid.getName());
		attributeUid.setReadable(true);
		attributeUid.setRequired(true);
		attributeUid.setReturnedByDefault(true);
		attributeUid.setUpdateable(true);

		attributes.add(attributeUid);

		// Create ENABLE schema attribute
		if (this.virtualConfiguration.isDisableSupported()) {
			IcAttributeInfoImpl attributeDisabled = new IcAttributeInfoImpl();
			attributeDisabled.setClassType(Boolean.class.getName());
			attributeDisabled.setCreateable(true);
			attributeDisabled.setMultivalued(false);
			attributeDisabled.setName(IcAttributeInfo.ENABLE);
			attributeDisabled.setNativeName(VsAccount_.enable.getName());
			attributeDisabled.setReadable(true);
			attributeDisabled.setRequired(false);
			attributeDisabled.setReturnedByDefault(true);
			attributeDisabled.setUpdateable(true);
			attributes.add(attributeDisabled);
		}

		// Attributes from definition and configuration
		Arrays.asList(virtualConfiguration.getAttributes()).forEach(virtualAttirbute -> {
			IdmFormAttributeDto formAttribute = formAttributeService.findAttribute(VsAccount.class.getName(),
					formDefinition.getCode(), virtualAttirbute);
			if (formAttribute == null) {
				return;
			}
			IcAttributeInfoImpl attribute = new IcAttributeInfoImpl();
			String classType = this.convertToSchemaClassType(formAttribute.getPersistentType());
			attribute.setClassType(classType);
			attribute.setCreateable(!formAttribute.isReadonly());
			attribute.setMultivalued(formAttribute.isMultiple());
			attribute.setName(virtualAttirbute);
			attribute.setNativeName(virtualAttirbute);
			attribute.setReadable(true);
			attribute.setRequired(formAttribute.isRequired());
			attribute.setReturnedByDefault(true);
			attribute.setUpdateable(!formAttribute.isReadonly());

			attributes.add(attribute);
		});

		objectClasses.add(objectClass);
		return schema;
	}

	/**
	 * Find UID attribute
	 * 
	 * @param attributes
	 * @return
	 */
	private IcAttribute geAttribute(List<IcAttribute> attributes, String name) {
		Assert.notNull(attributes);
		Assert.notNull(name);

		return attributes.stream().filter(attribute -> name.equals(attribute.getName())).findFirst().orElse(null);
	}

	private String convertToSchemaClassType(PersistentType persistentType) {
		switch (persistentType) {
		case INT:
			return Integer.class.getName();
		case LONG:
			return Long.class.getName();
		case BOOLEAN:
			return Boolean.class.getName();
		case DATE:
		case DATETIME:
			return DateTime.class.getName();
		case DOUBLE:
			return Double.class.getName();
		case CHAR:
			return Character.class.getName();
		case BYTEARRAY: {
			return byte[].class.getName();
		}
		case ATTACHMENT:
		case UUID: {
			return UUID.class.getName();
		}
		default:
			return String.class.getName();
		}
	}

	/**
	 * We have to change system entity directly from VS module (request can be
	 * started/executed async => standard process update UID in system entity (ACC
	 * module) will not works!)
	 * 
	 * @param uidValue
	 * @param attributeUidValue
	 * @param systemEntityMustExists
	 */
	private void updateSystemEntity(String uidValue, Object attributeUidValue, boolean systemEntityMustExists) {
		SysSystemEntityFilter systemEntityFilter = new SysSystemEntityFilter();
		systemEntityFilter.setUid(uidValue);
		systemEntityFilter.setSystemId(systemId);

		List<SysSystemEntityDto> systemEntities = systemEntityService.find(systemEntityFilter, null).getContent();
		if(systemEntities.isEmpty() && !systemEntityMustExists){
			return;
		}
		if (systemEntities.isEmpty()) {
			throw new IcException(MessageFormat.format(
					"System entity was not found for UID [{0}] and system ID [{1}]! Change UID attribute (new [{2}]) cannot be executed!",
					uidValue, systemId, attributeUidValue));
		}
		if (systemEntities.size() > 1) {
			throw new IcException(MessageFormat.format(
					"For UID [{0}] and system ID [{1}] was found too many items [{2}]! Change UID attribute (new [{3}]) cannot be executed!",
					uidValue, systemId, systemEntities.size(), attributeUidValue));
		}
		SysSystemEntityDto systemEntity = systemEntities.get(0);
		systemEntity.setUid((String) attributeUidValue);
		systemEntity.setWish(false);
		// Save changed system entity
		systemEntityService.save(systemEntity);
		LOG.info("Update account - UID was changed (old: {} new: {}). System entity was updated.", uidValue,
				attributeUidValue);
	}

	private void updateFormAttributeValue(Object uidValue, String virtualAttirbute, UUID accountId,
			List<IcAttribute> attributes) {
		IcAttribute attribute = geAttribute(attributes, virtualAttirbute);
		if (attribute == null) {
			return;
		}
		List<Object> values = attribute.getValues();
		List<Serializable> serializableValues = new ArrayList<>();
		if (values != null) {
			values.forEach(value -> {
				if (!(value instanceof Serializable)) {
					throw new IcException(MessageFormat.format(
							"Ic attribute value [{0}] is not Serializable! For account with UID [{1}].", value,
							uidValue));
				}
				serializableValues.add((Serializable) value);
			});
		}

		formService.saveValues(accountId, VsAccount.class, this.formDefinition, virtualAttirbute, serializableValues);
	}

	/**
	 * Create new instance of request DTO. Method does not persist him.
	 * 
	 * @param objectClass
	 * @param attributes
	 * @param uidString
	 * @param operationType
	 * @return
	 */
	private VsRequestDto createRequest(IcObjectClass objectClass, List<IcAttribute> attributes, String uidString,
			VsOperationType operationType) {

		VsRequestDto request = new VsRequestDto();
		request.setUid(uidString);
		request.setState(VsRequestState.CONCEPT);
		request.setSystem(this.systemId);
		request.setConfiguration(this.configuration);
		request.setConnectorKey(connectorKey);
		request.setConnectorObject(new IcConnectorObjectImpl(uidString, objectClass, attributes));
		request.setExecuteImmediately(!this.virtualConfiguration.isRequiredConfirmation());
		request.setOperationType(operationType);
		// request.setImplementers(this.loadImplementers(this.virtualConfiguration.getImplementers()));
		return request;
	}

	/**
	 * Get UID string from UID attribute
	 * 
	 * @param uid
	 * @return
	 */
	private String validateAndGetUid(IcUidAttribute uid) {
		Assert.notNull(uid, "UID cannot be null!");
		String uidValue = uid.getUidValue();
		if (uidValue == null) {
			throw new IcException("UID value cannot be null!");
		}
		return uidValue;
	}
}
