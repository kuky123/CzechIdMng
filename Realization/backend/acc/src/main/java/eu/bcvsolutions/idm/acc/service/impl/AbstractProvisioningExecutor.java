package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.AttributeMappingStrategyType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningContext;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.domain.ProvisioningOperationType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.EntityAccountDto;
import eu.bcvsolutions.idm.acc.dto.ProvisioningAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.EntityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.AccAccount_;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass_;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.acc.entity.SysSystemEntity_;
import eu.bcvsolutions.idm.acc.event.ProvisioningEvent;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningEntityExecutor;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningExecutor;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmAccountDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorKey;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;
import eu.bcvsolutions.idm.ic.impl.IcConnectorObjectImpl;
import eu.bcvsolutions.idm.ic.impl.IcObjectClassImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Abstract service for do provisioning
 * 
 * @author svandav
 * @author Radek Tomiška
 *
 * @param <DTO>
 *            provisioned dto
 * @param <F>
 *            dto's accounts filter
 */
public abstract class AbstractProvisioningExecutor<DTO extends AbstractDto> implements ProvisioningEntityExecutor<DTO> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractProvisioningExecutor.class);
	protected final SysSystemMappingService systemMappingService;
	protected final SysSystemAttributeMappingService attributeMappingService;
	private final IcConnectorFacade connectorFacade;
	private final SysSystemService systemService;
	protected final SysRoleSystemAttributeService roleSystemAttributeService;
	private final SysSystemEntityService systemEntityService;
	protected final AccAccountService accountService;
	private final ProvisioningExecutor provisioningExecutor;
	private final EntityEventManager entityEventManager;
	private final SysSchemaAttributeService schemaAttributeService;
	private final SysSchemaObjectClassService schemaObjectClassService;
	private final SysSystemAttributeMappingService systemAttributeMappingService;
	private final SysRoleSystemService roleSystemService;
	private final IdmRoleService roleService;

	@Autowired
	public AbstractProvisioningExecutor(SysSystemMappingService systemMappingService,
			SysSystemAttributeMappingService attributeMappingService, IcConnectorFacade connectorFacade,
			SysSystemService systemService, SysRoleSystemService roleSystemService,
			SysRoleSystemAttributeService roleSystemAttributeService, SysSystemEntityService systemEntityService,
			AccAccountService accountService, ProvisioningExecutor provisioningExecutor,
			EntityEventManager entityEventManager, SysSchemaAttributeService schemaAttributeService,
			SysSchemaObjectClassService schemaObjectClassService,
			SysSystemAttributeMappingService systemAttributeMappingService, IdmRoleService roleService) {

		Assert.notNull(systemMappingService);
		Assert.notNull(attributeMappingService);
		Assert.notNull(connectorFacade);
		Assert.notNull(systemService);
		Assert.notNull(roleSystemService);
		Assert.notNull(roleSystemAttributeService);
		Assert.notNull(systemEntityService);
		Assert.notNull(accountService);
		Assert.notNull(provisioningExecutor);
		Assert.notNull(entityEventManager);
		Assert.notNull(schemaAttributeService);
		Assert.notNull(schemaObjectClassService);
		Assert.notNull(systemAttributeMappingService);
		Assert.notNull(roleService);
		//
		this.systemMappingService = systemMappingService;
		this.attributeMappingService = attributeMappingService;
		this.connectorFacade = connectorFacade;
		this.systemService = systemService;
		this.roleSystemAttributeService = roleSystemAttributeService;
		this.systemEntityService = systemEntityService;
		this.accountService = accountService;
		this.provisioningExecutor = provisioningExecutor;
		this.entityEventManager = entityEventManager;
		this.schemaAttributeService = schemaAttributeService;
		this.schemaObjectClassService = schemaObjectClassService;
		this.systemAttributeMappingService = systemAttributeMappingService;
		this.roleSystemService = roleSystemService;
		this.roleService = roleService;
	}

	/**
	 * Returns entity type for this provisioning executor
	 * 
	 * @return
	 */
	protected SystemEntityType getEntityType() {
		return SystemEntityType.getByClass(getService().getDtoClass());
	}

	@Override
	public boolean supports(SystemEntityType delimiter) {
		return getEntityType() == delimiter;
	}

	@Override
	public void doProvisioning(AccAccountDto account) {
		Assert.notNull(account);

		EntityAccountFilter filter = createEntityAccountFilter();
		filter.setAccountId(account.getId());
		List<? extends EntityAccountDto> entityAccoutnList = getEntityAccountService().find(filter, null).getContent();
		if (entityAccoutnList == null) {
			return;
		}
		entityAccoutnList.stream().filter(entityAccount -> {
			return entityAccount.isOwnership();
		}).forEach((entityAccount) -> {
			doProvisioning(account, getService().get(entityAccount.getEntity()));
		});
	}

	@Override
	public void doProvisioning(DTO dto) {
		Assert.notNull(dto);
		//
		EntityAccountFilter filter = createEntityAccountFilter();
		filter.setEntityId(dto.getId());
		filter.setOwnership(Boolean.TRUE);
		List<? extends EntityAccountDto> entityAccoutnList = this.getEntityAccountService().find(filter, null)
				.getContent();

		List<UUID> accounts = new ArrayList<>();
		entityAccoutnList.stream().forEach((entityAccount) -> {
			if (!accounts.contains(entityAccount.getAccount())) {
				accounts.add(entityAccount.getAccount());
			}
		});

		accounts.stream().forEach(account -> {
			this.doProvisioning(accountService.get(account), dto);
		});
	}

	@Override
	public void doProvisioning(AccAccountDto account, DTO dto) {
		Assert.notNull(account, "Account cannot be null!");
		Assert.notNull(dto, "Dto cannot be null");
		//
		LOG.debug("Start provisioning for account [{}]", account.getUid());
		entityEventManager.process(new ProvisioningEvent(ProvisioningEvent.ProvisioningEventType.START, account,
				ImmutableMap.of(ProvisioningService.DTO_PROPERTY_NAME, dto)));
	}

	@Override
	public void doInternalProvisioning(AccAccountDto account, DTO dto) {
		Assert.notNull(account);
		Assert.notNull(dto);
		//
		ProvisioningOperationType operationType;
		SysSystemDto system = DtoUtils.getEmbedded(account, AccAccount_.system);
		SysSystemEntityDto systemEntity = getSystemEntity(account);
		SystemEntityType entityType = SystemEntityType.getByClass(dto.getClass());
		String uid = account.getUid();
		//
		if (systemEntity == null) {
			// prepare system entity - uid could be changed by provisioning, but
			// we need to link her with account
			// First we try find system entity with same uid.
			systemEntity = systemEntityService.getBySystemAndEntityTypeAndUid(system, entityType, uid);
			if (systemEntity == null) {
				systemEntity = new SysSystemEntityDto();
				systemEntity.setEntityType(entityType);
				systemEntity.setSystem(system.getId());
				systemEntity.setUid(uid);
				systemEntity.setWish(true);
				systemEntity = systemEntityService.save(systemEntity);
			}
			account.setSystemEntity(systemEntity.getId());
			account = accountService.save(account);
			// we wont create account, but after target system call can be
			// switched to UPDATE
			operationType = ProvisioningOperationType.CREATE;
		} else {
			// we wont update account, but after target system call can be
			// switched to CREATE
			operationType = ProvisioningOperationType.UPDATE;
		}

		List<AttributeMapping> finalAttributes = resolveMappedAttributes(account, dto, system,
				systemEntity.getEntityType());
		if (CollectionUtils.isEmpty(finalAttributes)) {
			// nothing to do - mapping is empty
			return;
		}

		doProvisioning(systemEntity, dto, dto.getId(), operationType, finalAttributes);
	}

	@Override
	public void doDeleteProvisioning(AccAccountDto account, UUID entityId) {
		Assert.notNull(account);
		SysSystemEntityDto systemEntity = getSystemEntity(account);
		//
		if (systemEntity != null) {
			doProvisioning(systemEntity, null, entityId, ProvisioningOperationType.DELETE, null);
		}
	}

	@Override
	public List<OperationResult> changePassword(DTO dto, PasswordChangeDto passwordChange) {
		Assert.notNull(dto);
		Assert.notNull(dto.getId(), "Password can be changed, when dto is already persisted.");
		Assert.notNull(passwordChange);
		List<SysProvisioningOperationDto> preparedOperations = new ArrayList<>();
		//
		EntityAccountFilter filter = this.createEntityAccountFilter();
		filter.setEntityId(dto.getId());
		List<? extends EntityAccountDto> entityAccountList = getEntityAccountService().find(filter, null).getContent();
		if (entityAccountList == null) {
			return Collections.<OperationResult>emptyList();
		}

		// Distinct by accounts
		List<UUID> accountIds = new ArrayList<>();
		entityAccountList.stream().filter(entityAccount -> {
			if (!entityAccount.isOwnership()) {
				return false;
			}
			if (passwordChange.isAll()) {
				// Add all account supports change password
				if (entityAccount.getAccount() == null) {
					return false;
				}
				// Check if system for this account support change password
				AccAccountFilter accountFilter = new AccAccountFilter();
				accountFilter.setSupportChangePassword(Boolean.TRUE);
				accountFilter.setId(entityAccount.getAccount());
				List<AccAccountDto> accountsChecked = accountService.find(accountFilter, null).getContent();
				if (accountsChecked.size() == 1) {
					return true;
				}
				return false;
			} else {
				return passwordChange.getAccounts().contains(entityAccount.getAccount().toString());
			}
		}).forEach(entityAccount -> {
			if (!accountIds.contains(entityAccount.getAccount())) {
				accountIds.add(entityAccount.getAccount());
			}
		});
		//
		List<AccAccountDto> accounts = new ArrayList<>();
		accountIds.forEach(accountId -> {
			AccAccountDto account = accountService.get(accountId);
			accounts.add(account);
			// find uid from system entity or from account
			String uid = account.getUid();
			SysSystemDto system = DtoUtils.getEmbedded(account, AccAccount_.system);
			SysSystemEntityDto systemEntity = systemEntityService.get(account.getSystemEntity());
			//
			// Find mapped attributes (include overloaded attributes)
			List<AttributeMapping> finalAttributes = resolveMappedAttributes(account, dto, system,
					systemEntity.getEntityType());
			if (CollectionUtils.isEmpty(finalAttributes)) {
				return;
			}

			// We try find __PASSWORD__ attribute in mapped attributes
			Optional<? extends AttributeMapping> attriubuteHandlingOptional = finalAttributes.stream()
					.filter((attribute) -> {
						SysSchemaAttributeDto schemaAttributeDto = getSchemaAttribute(attribute);
						return ProvisioningService.PASSWORD_SCHEMA_PROPERTY_NAME.equals(schemaAttributeDto.getName());
					}).findFirst();
			if (!attriubuteHandlingOptional.isPresent()) {
				throw new ProvisioningException(AccResultCode.PROVISIONING_PASSWORD_FIELD_NOT_FOUND,
						ImmutableMap.of("uid", uid, "system", system.getName()));
			}
			AttributeMapping mappedAttribute = attriubuteHandlingOptional.get();
			//
			// add all account attributes => standard provisioning
			SysProvisioningOperationDto additionalProvisioningOperation = null;
			List<AttributeMapping> additionalPasswordChangeAttributes = resolveAdditionalPasswordChangeAttributes(
					account, dto, system, systemEntity.getEntityType());
			if (!additionalPasswordChangeAttributes.isEmpty()) {
				additionalProvisioningOperation = prepareProvisioning(systemEntity, dto, dto.getId(),
						ProvisioningOperationType.UPDATE, additionalPasswordChangeAttributes);
			}
			//
			// password change operation
			SysProvisioningOperationDto operation;
			if (provisioningExecutor.getConfiguration().isSendPasswordAttributesTogether()
					&& additionalProvisioningOperation != null) {
				// all attributes as start
				operation = additionalProvisioningOperation;
				//
				// add wish for password
				ProvisioningAttributeDto passwordAttribute = ProvisioningAttributeDto.createProvisioningAttributeKey(
						mappedAttribute, schemaAttributeService.get(mappedAttribute.getSchemaAttribute()).getName() 
						// TODO: use previously loaded attribute
				);
				Object value = passwordChange.getNewPassword();
				if (!mappedAttribute.isEntityAttribute() && !mappedAttribute.isExtendedAttribute()) {
					// If is attribute handling resolve as constant, then we
					// don't want
					// do transformation again (was did in getAttributeValue)
				} else {
					value = attributeMappingService.transformValueToResource(systemEntity.getUid(), value,
							mappedAttribute, dto);
				}
				operation.getProvisioningContext().getAccountObject().put(passwordAttribute, value);
				//
				// do provisioning for additional attributes and password
				// together
				preparedOperations.add(operation);
			} else {
				// Change password on target system - only
				// TODO: refactor password change - use account wish instead
				// filling connector object attributes directly
				operation = prepareProvisioningForAttribute(systemEntity, mappedAttribute,
						passwordChange.getNewPassword(), ProvisioningOperationType.UPDATE, dto);
				preparedOperations.add(operation);
				//
				// do provisioning for additional attributes in second
				if (additionalProvisioningOperation != null) {
					preparedOperations.add(additionalProvisioningOperation);
				}
			}
		});
		//
		// execute prepared operations
		return preparedOperations.stream().map(operation -> {
			SysProvisioningOperationDto result = provisioningExecutor.executeSync(operation);
			Map<String, Object> parameters = new LinkedHashMap<String, Object>();			
			AccAccountDto account = accounts
					.stream()
					.filter(a -> {					
						return a.getRealUid().equals(result.getSystemEntityUid()) 
								&& a.getSystem().equals(operation.getSystem());
					})
					.findFirst().get();				
			SysSystemDto system = DtoUtils.getEmbedded(account, AccAccount_.system);
			//
			IdmAccountDto resultAccountDto = new IdmAccountDto();
			resultAccountDto.setId(account.getId());
			resultAccountDto.setUid(account.getUid());
			resultAccountDto.setRealUid(account.getRealUid());
			resultAccountDto.setSystemId(system.getId());
			resultAccountDto.setSystemName(system.getName());
			parameters.put(IdmAccountDto.PARAMETER_NAME, resultAccountDto);
			//
			if (result.getResult().getState() == OperationState.EXECUTED) {
				// Add success changed password account
				return new OperationResult.Builder(OperationState.EXECUTED)
						.setModel(new DefaultResultModel(CoreResultCode.PASSWORD_CHANGE_ACCOUNT_SUCCESS, parameters))
						.build();
			}
			OperationResult changeResult = new OperationResult.Builder(result.getResult().getState())
					.setModel(new DefaultResultModel(CoreResultCode.PASSWORD_CHANGE_ACCOUNT_FAILED, parameters))
					.build();
			changeResult.setCause(result.getResult().getCause());
			changeResult.setCode(result.getResult().getCode());
			return changeResult;
		}).collect(Collectors.toList());
	}
	
	@Override
	public boolean accountManagement(DTO dto) {
		this.createAccountsForAllSystems(dto);
		return true;
	}

	@Override
	public void createAccountsForAllSystems(DTO dto) {
		SystemEntityType entityType = SystemEntityType.getByClass(dto.getClass());
		List<SysSystemMappingDto> systemMappings = findSystemMappingsForEntityType(dto, entityType);
		systemMappings.forEach(mapping -> {
			SysSchemaObjectClassDto schemaObjectClassDto = schemaObjectClassService.get(mapping.getObjectClass());
			UUID systemId = schemaObjectClassDto.getSystem();
			UUID accountId = this.getAccountByEntity(dto.getId(), systemId);
			if (accountId != null) {
				// We already have account for this system -> next
				return;
			}
			SysSystemDto system = DtoUtils.getEmbedded(schemaObjectClassDto, SysSchemaObjectClass_.system);
			
			List<SysSystemAttributeMappingDto> mappedAttributes = attributeMappingService.findBySystemMapping(mapping);
			SysSystemAttributeMappingDto uidAttribute = attributeMappingService.getUidAttribute(mappedAttributes,
					system);
			String uid = attributeMappingService.generateUid(dto, uidAttribute);

			// Account management - can be the account created? - execute the script on the system mapping
			if (!this.canBeAccountCreated(uid, dto, mapping, system)) {
				String entityStr = dto.toString();
				if(dto instanceof Codeable){
					entityStr = ((Codeable)dto).getCode();
				}
				LOG.info(MessageFormat.format(
						"For entity [{0}] and entity type [{1}] cannot be created the account (on system [{2}]),"
								+ " because script \"Can be account created\" on the mapping returned \"false\"!",
								entityStr, entityType, system.getName()));
				return;
			}


			// Create AccAccount and relation between account and entity
			createEntityAccount(uid, dto.getId(), systemId);
		});
	}

	private boolean canBeAccountCreated(String uid, DTO dto, SysSystemMappingDto mapping, SysSystemDto system) {
		return systemMappingService.canBeAccountCreated(uid, dto, mapping.getCanBeAccountCreatedScript(), system);
	}

	/**
	 * Returns system entity associated to given account
	 * 
	 * @param account
	 * @return
	 */
	private SysSystemEntityDto getSystemEntity(AccAccountDto account) {
		if (account.getSystemEntity() == null) {
			return null;
		}
		//
		// TODO: we can find system entity on target system, if no one exists
		// etc.
		//
		return systemEntityService.get(account.getSystemEntity());
	}

	/**
	 * Validate attributes on incompatible strategies
	 * 
	 * @param finalAttributes
	 */
	protected void validateAttributesStrategy(List<AttributeMapping> finalAttributes) {
		if (finalAttributes == null) {
			return;
		}
		finalAttributes.forEach(parentAttribute -> {
			if (AttributeMappingStrategyType.MERGE == parentAttribute.getStrategyType()
					|| AttributeMappingStrategyType.AUTHORITATIVE_MERGE == parentAttribute.getStrategyType()) {
				Optional<AttributeMapping> conflictAttributeOptional = finalAttributes.stream().filter(att -> {
					SysSchemaAttributeDto attributeSchema = getSchemaAttribute(att);
					SysSchemaAttributeDto parentSchema = getSchemaAttribute(parentAttribute);
					return attributeSchema.equals(parentSchema)
							&& !(att.getStrategyType() == parentAttribute.getStrategyType()
									|| att.getStrategyType() == AttributeMappingStrategyType.CREATE
									|| att.getStrategyType() == AttributeMappingStrategyType.WRITE_IF_NULL);
				}).findFirst();
				if (conflictAttributeOptional.isPresent()) {
					AttributeMapping conflictAttribute = conflictAttributeOptional.get();

					IdmRoleDto roleParent = this.getRole(parentAttribute);
					IdmRoleDto roleConflict = this.getRole(conflictAttribute);

					throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_STRATEGY_CONFLICT,
							ImmutableMap.of("strategyParent", parentAttribute.getStrategyType(), //
									"strategyConflict",	conflictAttribute.getStrategyType(), //
									"attribute", conflictAttribute.getName(), //
					"roleParent",	roleParent != null ? roleParent.getCode() : "-", //
					"roleConflict",	roleConflict != null ? roleConflict.getCode() : "-")); //
				}
			}
		});
	}

	/**
	 * Get {@link IdmRoleDto} for attribute mapping. Attribute mapping must be instance of {@link SysRoleSystemAttributeDto}.
	 * And attribute must have role system connection.
	 *
	 * @param attribute
	 * @return
	 */
	private IdmRoleDto getRole(AttributeMapping attribute) {
		if (attribute instanceof SysRoleSystemAttributeDto) {
			SysRoleSystemAttributeDto conflictRoleAttribute = (SysRoleSystemAttributeDto) attribute;
			if (conflictRoleAttribute.getRoleSystem() == null) {
				return null;
			}
			SysRoleSystemDto roleSystem = roleSystemService.get(conflictRoleAttribute.getRoleSystem());
			if (roleSystem == null) {
				return null;
			}
			return roleService.get(roleSystem.getRole());
		}
		return null;
	}

	private SysProvisioningOperationDto prepareProvisioning(SysSystemEntityDto systemEntity, DTO dto, UUID entityId,
			ProvisioningOperationType operationType, List<? extends AttributeMapping> attributes) {
		Assert.notNull(systemEntity);
		Assert.notNull(systemEntity.getUid());
		Assert.notNull(systemEntity.getEntityType());
		SysSystemDto system = DtoUtils.getEmbedded(systemEntity, SysSystemEntity_.system);
		Assert.notNull(system);
		//
		// If are input attributes null, then we load default mapped attributes
		if (attributes == null) {
			attributes = findAttributeMappings(system, systemEntity.getEntityType());
		}
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}

		// Find connector identification persisted in system
		IcConnectorKey connectorKey = system.getConnectorKey();
		if (connectorKey == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_KEY_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}

		// Find connector configuration persisted in system
		IcConnectorConfiguration connectorConfig = systemService.getConnectorConfiguration(system);
		if (connectorConfig == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_CONFIGURATION_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		// One IDM object can be mapped to one connector object (= one connector
		// class).
		SysSystemMappingDto mapping = getMapping(system, systemEntity.getEntityType());
		if (mapping == null) {
			// mapping not found - nothing to do
			// TODO: delete operation?
			return null;
		}
		//
		Map<ProvisioningAttributeDto, Object> accountAttributes = prepareMappedAttributesValues(dto, operationType,
				systemEntity, attributes);
		// public provisioning event
		SysSchemaObjectClassDto schemaObjectClassDto = schemaObjectClassService.get(mapping.getObjectClass());
		IcConnectorObject connectorObject = new IcConnectorObjectImpl(systemEntity.getUid(),
				new IcObjectClassImpl(schemaObjectClassDto.getObjectClassName()), null);
		SysProvisioningOperationDto.Builder operationBuilder = new SysProvisioningOperationDto.Builder()
				.setOperationType(operationType).setSystemEntity(systemEntity).setEntityIdentifier(entityId)
				.setProvisioningContext(new ProvisioningContext(accountAttributes, connectorObject));
		//
		return operationBuilder.build();
	}

	/**
	 * Do provisioning on given system for given entity
	 * 
	 * @param systemEntity
	 * @param dto
	 * @param provisioningType
	 * @param attributes
	 */
	private void doProvisioning(SysSystemEntityDto systemEntity, DTO dto, UUID entityId,
			ProvisioningOperationType operationType, List<? extends AttributeMapping> attributes) {
		SysProvisioningOperationDto provisioningOperation = prepareProvisioning(systemEntity, dto, entityId,
				operationType, attributes);
		//
		if (provisioningOperation != null) {
			provisioningExecutor.execute(provisioningOperation);
		}
	}

	/**
	 * Prepare all mapped attribute values (= account)
	 * 
	 * @param dto
	 * @param operationType
	 * @param systemEntity
	 * @param attributes
	 * @return
	 */
	protected Map<ProvisioningAttributeDto, Object> prepareMappedAttributesValues(DTO dto,
			ProvisioningOperationType operationType, SysSystemEntityDto systemEntity,
			List<? extends AttributeMapping> attributes) {
		AccAccountDto account = getAccountSystemEntity(systemEntity.getId());
		String uid = systemEntity.getUid();
		SysSystemDto system = DtoUtils.getEmbedded(systemEntity, SysSystemEntity_.system);
		Map<ProvisioningAttributeDto, Object> accountAttributes = new HashMap<>();

		// delete - account attributes is not needed
		if (ProvisioningOperationType.DELETE == operationType) {
			return accountAttributes;
		}

		// First we will resolve attribute without MERGE strategy
		attributes.stream().filter(attribute -> {
			return !attribute.isDisabledAttribute()
					&& AttributeMappingStrategyType.AUTHORITATIVE_MERGE != attribute.getStrategyType()
					&& AttributeMappingStrategyType.MERGE != attribute.getStrategyType();
		}).forEach(attribute -> {
			SysSchemaAttributeDto schemaAttributeDto = getSchemaAttribute(attribute);
			if (attribute.isUid()) {
				// TODO: now we set UID from SystemEntity, may be UID from
				// AccAccount will be more correct
				Object uidValue = getAttributeValue(uid, dto, attribute);
				if (uidValue == null) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_GENERATED_UID_IS_NULL,
							ImmutableMap.of("system", system.getName()));
				}
				if (!(uidValue instanceof String)) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_IS_NOT_STRING,
							ImmutableMap.of("uid", uidValue, "system", system.getName()));
				}
				updateAccountUid(account, uid, (String) uidValue);
				accountAttributes.put(ProvisioningAttributeDto.createProvisioningAttributeKey(attribute,
						schemaAttributeDto.getName()), uidValue);
			} else {
				accountAttributes.put(ProvisioningAttributeDto.createProvisioningAttributeKey(attribute,
						schemaAttributeDto.getName()), getAttributeValue(uid, dto, attribute));
			}
		});

		// Second we will resolve MERGE attributes
		List<? extends AttributeMapping> attributesMerge = attributes.stream().filter(attribute -> {
			return !attribute.isDisabledAttribute()
					&& (AttributeMappingStrategyType.AUTHORITATIVE_MERGE == attribute.getStrategyType()
							|| AttributeMappingStrategyType.MERGE == attribute.getStrategyType());

		}).collect(Collectors.toList());

		for (AttributeMapping attributeParent : attributesMerge) {
			SysSchemaAttributeDto schemaAttributeParent = getSchemaAttribute(attributeParent);
			ProvisioningAttributeDto attributeParentKey = ProvisioningAttributeDto
					.createProvisioningAttributeKey(attributeParent, schemaAttributeParent.getName());
			if (!schemaAttributeParent.isMultivalued()) {
				throw new ProvisioningException(AccResultCode.PROVISIONING_MERGE_ATTRIBUTE_IS_NOT_MULTIVALUE,
						ImmutableMap.of("object", uid, "attribute", schemaAttributeParent.getName(), "system",
								system.getName()));
			}

			// we use SET collection because we want collection of merged values without duplicates
			Set<Object> mergedValues = new LinkedHashSet<>();
			attributes.stream().filter(attribute -> {
				SysSchemaAttributeDto schemaAttribute = getSchemaAttribute(attribute);
				return !accountAttributes.containsKey(attributeParentKey)
						&& schemaAttributeParent.equals(schemaAttribute)
						&& attributeParent.getStrategyType() == attribute.getStrategyType();
			}).forEach(attribute -> {
				Object value = getAttributeValue(uid, dto, attribute);
				// We don`t want null item in list (problem with
				// provisioning in IC)
				if (value != null) {
					// If is value collection, then we add all its items to
					// main list!
					if (value instanceof Collection) {
						Collection<?> collectionNotNull = ((Collection<?>) value).stream().filter(item -> {
							return item != null;
						}).collect(Collectors.toList());
						mergedValues.addAll(collectionNotNull);
					} else {
						mergedValues.add(value);
					}
				}
			});
			if (!accountAttributes.containsKey(attributeParentKey)) {
				// we must put merged values as array list
				accountAttributes.put(attributeParentKey, new ArrayList<>(mergedValues));
			}
		}
		return accountAttributes;
	}

	protected Object getAttributeValue(String uid, DTO dto, AttributeMapping attribute) {
		return attributeMappingService.getAttributeValue(uid, dto, attribute);
	}

	@Override
	public void doProvisioningForAttribute(SysSystemEntityDto systemEntity, AttributeMapping attributeMapping,
			Object value, ProvisioningOperationType operationType, DTO dto) {
		provisioningExecutor
				.execute(prepareProvisioningForAttribute(systemEntity, attributeMapping, value, operationType, dto));
	}

	private SysProvisioningOperationDto prepareProvisioningForAttribute(SysSystemEntityDto systemEntity,
			AttributeMapping attributeMapping, Object value, ProvisioningOperationType operationType, DTO dto) {

		Assert.notNull(systemEntity);
		Assert.notNull(systemEntity.getSystem());
		Assert.notNull(systemEntity.getEntityType());
		Assert.notNull(systemEntity.getUid());
		Assert.notNull(attributeMapping);

		SysSchemaAttributeDto schemaAttributeDto = getSchemaAttribute(attributeMapping);

		if (!schemaAttributeDto.isUpdateable()) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_SCHEMA_ATTRIBUTE_IS_NOT_UPDATEABLE,
					ImmutableMap.of("property", attributeMapping.getIdmPropertyName(), "uid", systemEntity.getUid()));
		}
		SysSchemaObjectClassDto schemaObjectClassDto = schemaObjectClassService
				.get(schemaAttributeDto.getObjectClass());
		String objectClassName = schemaObjectClassDto.getObjectClassName();
		// We do transformation to system if is attribute only constant
		Object valueTransformed = value;
		if (!attributeMapping.isEntityAttribute() && !attributeMapping.isExtendedAttribute()) {
			// If is attribute handling resolve as constant, then we don't want
			// do transformation again (was did in getAttributeValue)
		} else {
			valueTransformed = attributeMappingService.transformValueToResource(systemEntity.getUid(), value,
					attributeMapping, dto);
		}
		IcAttribute icAttributeForCreate = attributeMappingService.createIcAttribute(schemaAttributeDto,
				valueTransformed);
		//
		// Call ic modul for update single attribute
		IcConnectorObject connectorObject = new IcConnectorObjectImpl(systemEntity.getUid(),
				new IcObjectClassImpl(objectClassName), ImmutableList.of(icAttributeForCreate));
		SysProvisioningOperationDto.Builder operationBuilder = new SysProvisioningOperationDto.Builder()
				.setOperationType(ProvisioningEventType.UPDATE).setSystemEntity(systemEntity)
				.setEntityIdentifier(dto == null ? null : dto.getId())
				.setProvisioningContext(new ProvisioningContext(connectorObject));
		//
		return operationBuilder.build();
	}

	@Override
	public IcUidAttribute authenticate(String username, GuardedString password, SysSystemDto system,
			SystemEntityType entityType) {

		Assert.notNull(username);
		Assert.notNull(system);
		Assert.notNull(entityType);

		// Find connector configuration persisted in system
		IcConnectorConfiguration connectorConfig = systemService.getConnectorConfiguration(system);
		if (connectorConfig == null) {
			throw new ProvisioningException(AccResultCode.CONNECTOR_CONFIGURATION_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}

		// Call IC module for check authenticate
		return connectorFacade.authenticateObject(system.getConnectorInstance(), connectorConfig, null, username,
				password);
	}

	/**
	 * Return all mapped attributes for this account (include overloaded
	 * attributes)
	 * 
	 * @param uid
	 * @param account
	 * @param entity
	 * @param system
	 * @param entityType
	 * @return
	 */
	@Override
	public List<AttributeMapping> resolveMappedAttributes(AccAccountDto account, DTO dto, SysSystemDto system,
			SystemEntityType entityType) {
		EntityAccountFilter filter = this.createEntityAccountFilter();
		filter.setEntityId(dto.getId());
		filter.setSystemId(system.getId());
		filter.setOwnership(Boolean.TRUE);
		filter.setAccountId(account.getId());

		List<? extends EntityAccountDto> entityAccoutnList = this.getEntityAccountService().find(filter, null)
				.getContent();
		if (entityAccoutnList == null) {
			return null;
		}
		// All identity account with flag ownership on true

		// All role system attributes (overloading) for this uid and same system
		List<SysRoleSystemAttributeDto> roleSystemAttributesAll = findOverloadingAttributes(dto, system,
				entityAccoutnList, entityType);

		// All default mapped attributes from system
		List<? extends AttributeMapping> defaultAttributes = findAttributeMappings(system, entityType);

		// Final list of attributes use for provisioning
		return compileAttributes(defaultAttributes, roleSystemAttributesAll, entityType);
	}

	private List<AttributeMapping> resolveAdditionalPasswordChangeAttributes(AccAccountDto account, DTO dto,
			SysSystemDto system, SystemEntityType entityType) {
		EntityAccountFilter filter = this.createEntityAccountFilter();
		filter.setEntityId(dto.getId());
		filter.setSystemId(system.getId());
		filter.setOwnership(Boolean.TRUE);
		filter.setAccountId(account.getId());

		List<? extends EntityAccountDto> entityAccoutnList = this.getEntityAccountService().find(filter, null)
				.getContent();
		if (entityAccoutnList == null) {
			return Collections.<AttributeMapping>emptyList();
		}
		//
		SysSystemMappingDto mapping = getMapping(system, entityType);
		if (mapping == null) {
			return Collections.<AttributeMapping>emptyList();
		}
		//
		// All additional mapped attributes from system, witch has to be send on
		// password change
		SysSystemAttributeMappingFilter attributeFilter = new SysSystemAttributeMappingFilter();
		attributeFilter.setSystemMappingId(mapping.getId());
		attributeFilter.setSendOnPasswordChange(Boolean.TRUE);
		List<? extends AttributeMapping> additionalPasswordChangeAttributes = attributeMappingService
				.find(attributeFilter, null).getContent();
		//
		// All role system attributes (overloading) for this uid and same system
		List<SysRoleSystemAttributeDto> roleSystemAttributesAll = findOverloadingAttributes(dto, system,
				entityAccoutnList, entityType);
		//
		// Final list of attributes use for provisioning
		List<AttributeMapping> results = compileAttributes(additionalPasswordChangeAttributes, roleSystemAttributesAll,
				entityType);
		//
		return results == null ? Collections.<AttributeMapping>emptyList() : results;
	}

	/**
	 * Create final list of attributes for provisioning.
	 * 
	 * @param identityAccount
	 * @param defaultAttributes
	 * @param overloadingAttributes
	 * @return
	 */
	@Override
	public List<AttributeMapping> compileAttributes(List<? extends AttributeMapping> defaultAttributes,
			List<SysRoleSystemAttributeDto> overloadingAttributes, SystemEntityType entityType) {
		Assert.notNull(overloadingAttributes, "List of overloading attributes cannot be null!");

		List<AttributeMapping> finalAttributes = new ArrayList<>();
		if (defaultAttributes == null) {
			return null;
		}
		defaultAttributes.stream().forEach(defaultAttribute -> {
			for (AttributeMappingStrategyType strategy : AttributeMappingStrategyType.values()) {
				finalAttributes.addAll(compileAtributeForStrategy(strategy, defaultAttribute, overloadingAttributes));
			}
		});

		// Validate attributes on incompatible strategies
		validateAttributesStrategy(finalAttributes);

		return finalAttributes;
	}

	/**
	 * Compile given attribute for strategy
	 * 
	 * @param strategy
	 * @param defaultAttribute
	 * @param overloadingAttributes
	 * @return
	 */
	protected List<AttributeMapping> compileAtributeForStrategy(AttributeMappingStrategyType strategy,
			AttributeMapping defaultAttribute, List<SysRoleSystemAttributeDto> overloadingAttributes) {

		List<AttributeMapping> finalAttributes = new ArrayList<>();

		List<SysRoleSystemAttributeDto> attributesOrdered = overloadingAttributes.stream()
				.filter(roleSystemAttribute -> {
					// Search attribute override same schema attribute
					SysSystemAttributeMappingDto attributeMapping = systemAttributeMappingService
							.get(roleSystemAttribute.getSystemAttributeMapping());
					return attributeMapping.equals(defaultAttribute);
				}).sorted((att1, att2) -> {
					// Sort attributes by role priority
					SysRoleSystemDto roleSystem2 = roleSystemService.get(att2.getRoleSystem());
					SysRoleSystemDto roleSystem1 = roleSystemService.get(att1.getRoleSystem());
					IdmRoleDto role1 = roleService.get(roleSystem1.getRole());
					IdmRoleDto role2 = roleService.get(roleSystem2.getRole());
					return Integer.valueOf(role2.getPriority()).compareTo(Integer.valueOf(role1.getPriority()));
				}).collect(Collectors.toList());

		// We have some overloaded attributes
		if (!attributesOrdered.isEmpty()) {
			List<SysRoleSystemAttributeDto> attributesOrderedGivenStrategy = attributesOrdered.stream()
					.filter(attribute -> {
						return strategy == attribute.getStrategyType();
					}).collect(Collectors.toList());

			// We do not have overloaded attributes for given strategy
			if (attributesOrderedGivenStrategy.isEmpty()) {
				return finalAttributes;
			}

			// First element have role with max priority
			SysRoleSystemDto roleSystemForSetMaxPriority = roleSystemService
					.get(attributesOrderedGivenStrategy.get(0).getRoleSystem());
			IdmRoleDto roleForSetMaxPriority = roleService.get(roleSystemForSetMaxPriority.getRole());
			int maxPriority = roleForSetMaxPriority.getPriority();

			// We will search for attribute with highest priority (and role
			// name)
			Optional<SysRoleSystemAttributeDto> highestPriorityAttributeOptional = attributesOrderedGivenStrategy
					.stream().filter(attribute -> {
						SysRoleSystemDto roleSystem = roleSystemService.get(attribute.getRoleSystem());
						IdmRoleDto roleDto = roleService.get(roleSystem.getRole());
						// Filter attributes by max priority
						return maxPriority == roleDto.getPriority();
					}).sorted((att1, att2) -> {
						// Second filtering, if we have same priority, then
						// we will sort by role name
						SysRoleSystemDto roleSystem1 = roleSystemService.get(att1.getRoleSystem());
						SysRoleSystemDto roleSystem2 = roleSystemService.get(att2.getRoleSystem());
						//
						IdmRoleDto roleDto1 = roleService.get(roleSystem1.getRole());
						IdmRoleDto roleDto2 = roleService.get(roleSystem2.getRole());
						//
						return roleDto2.getCode().compareTo(roleDto1.getCode());
					}).findFirst();

			if (highestPriorityAttributeOptional.isPresent()) {
				SysRoleSystemAttributeDto highestPriorityAttribute = highestPriorityAttributeOptional.get();

				// For merge strategies, will be add to final list all
				// overloaded attributes
				if (strategy == AttributeMappingStrategyType.AUTHORITATIVE_MERGE
						|| strategy == AttributeMappingStrategyType.MERGE) {
					attributesOrderedGivenStrategy.forEach(attribute -> {
						// Disabled attribute will be skipped
						if (!attribute.isDisabledDefaultAttribute()) {
							// Default values (values from schema attribute
							// handling)
							attribute.setSchemaAttribute(defaultAttribute.getSchemaAttribute());
							attribute.setTransformFromResourceScript(defaultAttribute.getTransformFromResourceScript());

							// Common properties (for MERGE strategy) will be
							// set from MERGE attribute with highest priority
							attribute.setSendAlways(highestPriorityAttribute.isSendAlways());
							attribute.setSendOnlyIfNotNull(highestPriorityAttribute.isSendOnlyIfNotNull());

							// Add modified attribute to final list
							finalAttributes.add(attribute);
						}
					});
					return finalAttributes;
				}

				// We will search for disabled overloaded attribute
				Optional<SysRoleSystemAttributeDto> disabledOverloadedAttOptional = attributesOrderedGivenStrategy
						.stream().filter(attribute -> {
							// Filter attributes by max priority
							SysRoleSystemDto roleSystem = roleSystemService.get(attribute.getRoleSystem());
							IdmRoleDto roleDto = roleService.get(roleSystem.getRole());
							return maxPriority == roleDto.getPriority();
						}).filter(attribute -> {
							// Second filtering, we will search for disabled
							// overloaded attribute
							return attribute.isDisabledDefaultAttribute();
						}).findFirst();
				if (disabledOverloadedAttOptional.isPresent()) {
					// We found disabled overloaded attribute with highest
					// priority
					return finalAttributes;
				}

				// None overloaded attribute are disabled, we will search for
				// attribute with highest priority (and role name)
				// Disabled attribute will be skipped
				if (!highestPriorityAttribute.isDisabledDefaultAttribute()) {
					// Default values (values from schema attribute handling)
					highestPriorityAttribute.setSchemaAttribute(defaultAttribute.getSchemaAttribute());
					highestPriorityAttribute.setCached(defaultAttribute.isCached());
					highestPriorityAttribute
							.setTransformFromResourceScript(defaultAttribute.getTransformFromResourceScript());
					// Add modified attribute to final list
					finalAttributes.add(highestPriorityAttribute);
					return finalAttributes;
				}
			}
		}
		// We don't have overloading attribute, we will use default
		// if has given strategy
		// If is default attribute disabled, then we don't use him

		if (!defaultAttribute.isDisabledAttribute() && strategy == defaultAttribute.getStrategyType()) {
			finalAttributes.add(defaultAttribute);
		}

		return finalAttributes;
	}

	/**
	 * Return list of all overloading attributes for given identity, system and
	 * uid
	 * 
	 * @param identityAccount
	 * @param uid
	 * @param idenityAccoutnList
	 * @param entityType
	 * @return
	 */
	protected abstract List<SysRoleSystemAttributeDto> findOverloadingAttributes(DTO dto, SysSystemDto system,
			List<? extends EntityAccountDto> idenityAccoutnList, SystemEntityType entityType);

	private SysSystemMappingDto getMapping(SysSystemDto system, SystemEntityType entityType) {
		List<SysSystemMappingDto> systemMappings = systemMappingService.findBySystem(system,
				SystemOperationType.PROVISIONING, entityType);
		if (systemMappings == null || systemMappings.isEmpty()) {
			LOG.info(MessageFormat.format(
					"System [{0}] does not have mapping, provisioning will not be executed. Add some mapping for entity type [{1}]",
					system.getName(), entityType));
			return null;
		}
		if (systemMappings.size() != 1) {
			throw new IllegalStateException(MessageFormat.format(
					"System [{0}] is wrong configured! Remove duplicit mapping for entity type [{1}]", system.getName(),
					entityType));
		}
		return systemMappings.get(0);
	}

	/**
	 * Find list of {@link SysSystemAttributeMapping} by provisioning type and
	 * entity type on given system
	 * 
	 * @param provisioningType
	 * @param entityType
	 * @param system
	 * @return
	 */
	protected List<? extends AttributeMapping> findAttributeMappings(SysSystemDto system, SystemEntityType entityType) {
		SysSystemMappingDto mapping = getMapping(system, entityType);
		if (mapping == null) {
			return null;
		}
		return attributeMappingService.findBySystemMapping(mapping);
	}

	protected List<SysSystemMappingDto> findSystemMappingsForEntityType(DTO dto, SystemEntityType entityType) {
		SysSystemMappingFilter mappingFilter = new SysSystemMappingFilter();
		mappingFilter.setEntityType(entityType);
		mappingFilter.setOperationType(SystemOperationType.PROVISIONING);
		return systemMappingService.find(mappingFilter, null).getContent();
	}

	/**
	 * Create AccAccount and relation between account and entity
	 * 
	 * @param uid
	 * @param entityId
	 * @param systemId
	 * @return Id of new EntityAccount
	 */
	protected UUID createEntityAccount(String uid, UUID entityId, UUID systemId) {
		AccAccountDto account = new AccAccountDto();
		account.setSystem(systemId);
		account.setAccountType(AccountType.PERSONAL);
		account.setUid(uid);
		account.setEntityType(getEntityType());
		account = accountService.save(account);
		// Create new entity account relation
		EntityAccountDto entityAccount = this.createEntityAccountDto();
		entityAccount.setAccount(account.getId());
		entityAccount.setEntity(entityId);
		entityAccount.setOwnership(true);
		entityAccount = getEntityAccountService().save(entityAccount);
		return (UUID) entityAccount.getId();
	}

	protected UUID getAccountByEntity(UUID entityId, UUID systemId) {
		EntityAccountFilter entityAccountFilter = createEntityAccountFilter();
		entityAccountFilter.setEntityId(entityId);
		entityAccountFilter.setSystemId(systemId);
		entityAccountFilter.setOwnership(Boolean.TRUE);
		List<? extends EntityAccountDto> entityAccounts = this.getEntityAccountService().find(entityAccountFilter, null)
				.getContent();
		if (entityAccounts.isEmpty()) {
			return null;
		} else {
			// We assume that all entity accounts
			// (mark as
			// ownership) have same account!
			return entityAccounts.get(0).getAccount();
		}
	}

	protected UUID getEntityByAccount(UUID accountId, UUID systemId) {
		EntityAccountFilter entityAccountFilter = createEntityAccountFilter();
		entityAccountFilter.setAccountId(accountId);
		entityAccountFilter.setSystemId(systemId);
		entityAccountFilter.setOwnership(Boolean.TRUE);
		List<? extends EntityAccountDto> entityAccounts = this.getEntityAccountService().find(entityAccountFilter, null)
				.getContent();
		if (entityAccounts.isEmpty()) {
			return null;
		} else {
			// We assume that all entity accounts
			// (mark as
			// ownership) have same entity!
			return entityAccounts.get(0).getEntity();
		}
	}

	protected AccAccountDto getAccountSystemEntity(UUID systemEntity) {
		AccAccountFilter filter = new AccAccountFilter();
		filter.setSystemEntityId(systemEntity);
		List<AccAccountDto> accounts = this.accountService.find(filter, null).getContent();
		if (accounts.isEmpty()) {
			return null;
		} else {
			// We assume that system entity has only one account!
			return accounts.get(0);
		}
	}

	protected abstract <F extends EntityAccountFilter> F createEntityAccountFilter();

	protected abstract EntityAccountDto createEntityAccountDto();

	/**
	 * Returns service, which controls DTO's accounts
	 * 
	 * @return
	 */
	protected abstract <A extends EntityAccountDto, F extends EntityAccountFilter> ReadWriteDtoService<A, F> getEntityAccountService();

	/**
	 * Returns service, which controls DTO
	 * 
	 * @return
	 */
	protected abstract ReadWriteDtoService<DTO, ?> getService();

	/**
	 * Method get {@link SysSystemDto} from uuid schemaAttribute.
	 * 
	 * @param schemaAttributeId
	 * @return
	 */
	protected SysSystemDto getSytemFromSchemaAttribute(UUID schemaAttributeId) {
		Assert.notNull(schemaAttributeId);
		return getSytemFromSchemaAttribute(schemaAttributeService.get(schemaAttributeId));
	}

	/**
	 * Method get {@link SysSystemDto} from {@link SysSchemaAttributeDto}.
	 * 
	 * @param attributeDto
	 * @return
	 */
	protected SysSystemDto getSytemFromSchemaAttribute(SysSchemaAttributeDto attributeDto) {
		Assert.notNull(attributeDto);
		return getSystemFromSchemaObjectClass(schemaObjectClassService.get(attributeDto.getObjectClass()));
	}

	/**
	 * Method get {@link SysSystemDto} from {@link SysSchemaObjectClassDto}.
	 * 
	 * @param schemaObjectClassDto
	 * @return
	 */
	protected SysSystemDto getSystemFromSchemaObjectClass(SysSchemaObjectClassDto schemaObjectClassDto) {
		Assert.notNull(schemaObjectClassDto);
		return DtoUtils.getEmbedded(schemaObjectClassDto, SysSchemaObjectClass_.system);
	}

	/**
	 * Update account UID in IDM
	 * 
	 * @param account
	 * @param uid
	 * @param uidValue
	 * @return
	 */
	private AccAccountDto updateAccountUid(AccAccountDto account, String uid, String uidValue) {
		// If is value form UID attribute null, then we will use UID
		// from existed account (AccAccount/SystemEntity)
		uidValue = uidValue == null ? uid : uidValue;
		if (account != null && !account.getUid().equals(uidValue)) {
			// UID value must be string
			account.setUid(uidValue);
			account = accountService.save(account);
		}
		return account;
	}

	/**
	 * Method return schema attribute from interface attribute mapping. Schema
	 * may be null from RoleSystemAttribute
	 * 
	 * @return
	 */
	protected SysSchemaAttributeDto getSchemaAttribute(AttributeMapping attributeMapping) {
		if (attributeMapping.getSchemaAttribute() != null) {
			return schemaAttributeService.get(attributeMapping.getSchemaAttribute());
		} else {
			// schema attribute is null = roleSystemAttribute
			SysSystemAttributeMappingDto dto = systemAttributeMappingService
					.get(((SysRoleSystemAttributeDto) attributeMapping).getSystemAttributeMapping());
			return schemaAttributeService.get(dto.getSchemaAttribute());
		}
	}
}
