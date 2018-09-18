package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.entity.AccIdentityAccount_;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem_;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccAccountManagementService;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityStateFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.EntityStateManager;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;

/**
 * Service for control account management. Account management is supported for
 * {@link SystemEntityType#IDENTITY} only.
 * 
 * @author svandav
 *
 */
@Service
public class DefaultAccAccountManagementService implements AccAccountManagementService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
			.getLogger(DefaultAccAccountManagementService.class);
	private final AccAccountService accountService;
	private final SysRoleSystemService roleSystemService;
	private final AccIdentityAccountService identityAccountService;
	private final SysRoleSystemAttributeService roleSystemAttributeService;
	private final SysSystemAttributeMappingService systemAttributeMappingService;
	private final SysSystemMappingService systemMappingService;
	private final SysSchemaObjectClassService schemaObjectClassService;
	@Autowired
	private IdmIdentityRoleService identityRoleService;
	@Autowired
	private EntityStateManager entityStateManager;
	@Autowired
	private EntityEventManager entityEventManager;

	@Autowired
	public DefaultAccAccountManagementService(SysRoleSystemService roleSystemService, AccAccountService accountService,
			AccIdentityAccountService identityAccountService, SysRoleSystemAttributeService roleSystemAttributeService,
			SysSystemAttributeMappingService systemAttributeMappingService,
			SysSystemMappingService systemMappingService, SysSchemaObjectClassService schemaObjectClassService) {
		super();
		//
		Assert.notNull(identityAccountService);
		Assert.notNull(roleSystemService);
		Assert.notNull(accountService);
		Assert.notNull(roleSystemAttributeService);
		Assert.notNull(systemAttributeMappingService);
		Assert.notNull(systemMappingService);
		Assert.notNull(schemaObjectClassService);
		//
		this.roleSystemService = roleSystemService;
		this.accountService = accountService;
		this.identityAccountService = identityAccountService;
		this.roleSystemAttributeService = roleSystemAttributeService;
		this.systemAttributeMappingService = systemAttributeMappingService;
		this.systemMappingService = systemMappingService;
		this.schemaObjectClassService = schemaObjectClassService;
	}

	@Override
	public boolean resolveIdentityAccounts(IdmIdentityDto identity) {
		Assert.notNull(identity);
		// find not deleted identity accounts
		AccIdentityAccountFilter filter = new AccIdentityAccountFilter();
		filter.setIdentityId(identity.getId());
		List<AccIdentityAccountDto> allIdentityAccountList = identityAccountService.find(filter, null).getContent();
		List<IdmIdentityRoleDto> identityRoles = identityRoleService.findAllByIdentity(identity.getId());

		boolean provisioningRequired = false;

		if (CollectionUtils.isEmpty(identityRoles) && CollectionUtils.isEmpty(allIdentityAccountList)) {
			// No roles and accounts ... we don't have anything to do
			return false;
		}
		
		// account with delete accepted states wil be removed on the end
		IdmEntityStateFilter identityAccountStatesFilter = new IdmEntityStateFilter();
		identityAccountStatesFilter.setSuperOwnerId(identity.getId());
		identityAccountStatesFilter.setOwnerType(entityStateManager.getOwnerType(AccIdentityAccountDto.class));
		identityAccountStatesFilter.setResultCode(CoreResultCode.DELETED.getCode());
		List<IdmEntityStateDto> identityAccountStates = entityStateManager.findStates(identityAccountStatesFilter, null).getContent();
		List<AccIdentityAccountDto> identityAccountList = allIdentityAccountList //
				.stream() //
				.filter(ia -> {
					return !identityAccountStates //
							.stream() //
							.anyMatch(state -> { 
								return ia.getId().equals(state.getOwnerId());
							});
				}).collect(Collectors.toList());
		
		// create / remove accounts 
		if (!CollectionUtils.isEmpty(identityRoles) || !CollectionUtils.isEmpty(identityAccountList)) {
			List<AccIdentityAccountDto> identityAccountsToCreate = new ArrayList<>();
			List<AccIdentityAccountDto> identityAccountsToDelete = new ArrayList<>();
	
			// Is role valid in this moment
			resolveIdentityAccountForCreate(identity, identityAccountList, identityRoles, identityAccountsToCreate,
					identityAccountsToDelete);
	
			// Is role invalid in this moment
			resolveIdentityAccountForDelete(identityAccountList, identityRoles, identityAccountsToDelete);
	
			// Delete invalid identity accounts
			provisioningRequired = !identityAccountsToDelete.isEmpty() ? true : provisioningRequired;
			identityAccountsToDelete.forEach(identityAccount -> identityAccountService.deleteById(identityAccount.getId()));
	
			// Create new identity accounts
			provisioningRequired = !identityAccountsToCreate.isEmpty() ? true : provisioningRequired;
			identityAccountsToCreate.forEach(identityAccount -> identityAccountService.save(identityAccount));
		}		
		// clear identity accounts marked to be deleted
		provisioningRequired = allIdentityAccountList.size() != identityAccountList.size() ? true : provisioningRequired;
		identityAccountStates //
			.stream() //
			.forEach(state -> {	//
				AccIdentityAccountDto deleteIdentityAccount = identityAccountService.get(state.getOwnerId());
				if (deleteIdentityAccount != null) {
					// identity account can be deleted manually.
					identityAccountService.delete(deleteIdentityAccount);
				}
				entityStateManager.deleteState(state);
			});
		//
		return provisioningRequired;
	}

	/**
	 * Resolve identity account to delete
	 * 
	 * @param identityAccountList
	 * @param identityRoles
	 * @param identityAccountsToDelete
	 */
	private void resolveIdentityAccountForDelete(List<AccIdentityAccountDto> identityAccountList,
			List<IdmIdentityRoleDto> identityRoles, List<AccIdentityAccountDto> identityAccountsToDelete) {

		identityRoles.stream().filter(identityRole -> {
			return !identityRole.isValid();
		}).forEach(identityRole -> {
			// Search IdentityAccounts to delete

			// Identity-account is not removed (even if that identity-role is invalid) if
			// the role-system has enabled forward account management and identity-role will
			// be valid in the future.
			identityAccountList.stream() //
					.filter(identityAccount -> identityRole.getId().equals(identityAccount.getIdentityRole())) //
					.filter(identityAccount -> identityAccount.getRoleSystem() == null
							|| !( ((SysRoleSystemDto) DtoUtils
									.getEmbedded(identityAccount, AccIdentityAccount_.roleSystem))
									.isForwardAccountManagemen() && identityRole.isValidNowOrInFuture())) //
					.forEach(identityAccount -> {
						identityAccountsToDelete.add(identityAccount);
					});
		});
	}

	/**
	 * Resolve Identity account - to create
	 * 
	 * @param identity
	 * @param identityAccountList
	 * @param identityRoles
	 * @param identityAccountsToCreate
	 * @param identityAccountsToDelete
	 * @param resolvedRolesForCreate
	 */
	private void resolveIdentityAccountForCreate(IdmIdentityDto identity,
			List<AccIdentityAccountDto> identityAccountList, List<IdmIdentityRoleDto> identityRoles,
			List<AccIdentityAccountDto> identityAccountsToCreate,
			List<AccIdentityAccountDto> identityAccountsToDelete) {

		identityRoles.forEach(identityRole -> {

			UUID role = identityRole.getRole();
			SysRoleSystemFilter roleSystemFilter = new SysRoleSystemFilter();
			roleSystemFilter.setRoleId(role);
			List<SysRoleSystemDto> roleSystems = roleSystemService.find(roleSystemFilter, null).getContent();

			// Is role valid in this moment or
			// role-system has enabled forward account management (identity-role have to be
			// valid in the future)
			roleSystems.stream()
					.filter(roleSystem -> (identityRole.isValid()
							|| (roleSystem.isForwardAccountManagemen() && identityRole.isValidNowOrInFuture()))) //
					.filter(roleSystem -> { //
						// Filter out identity-accounts for same role-system, account (by UID)
						return !identityAccountList //
								.stream() //
								.filter(identityAccount -> { //
									if (roleSystem.getId().equals(identityAccount.getRoleSystem())) {
										// Has identity account same uid as account?
										String uid = generateUID(identity, roleSystem);
										AccAccountDto account = AccIdentityAccountService.getEmbeddedAccount(identityAccount);
										if (!uid.equals(account.getUid())) {
											// We found identityAccount for same identity and roleSystem, but this
											// identityAccount
											// is link to Account with different UID. It's probably means definition of UID
											// (transformation)\
											// on roleSystem was changed. We have to delete this identityAccount.
											identityAccountsToDelete.add(identityAccount);
										}
									}
									return false;
								}) //
								.findFirst() //
								.isPresent();

					}).forEach(roleSystem -> {
						// For this system we have to create new account
						UUID accountId = createAccountByRoleSystem(identity, roleSystem, identityAccountsToCreate);
						if (accountId == null) {
							return;
						}
						// prevent to create the same identity account - method is called multi times
						// TODO: find the better place for this check
						if (identityAccountList.stream().filter(identityAccount -> {
							return identityAccount.getAccount().equals(accountId)
									&& identityRole.getId().equals(identityAccount.getIdentityRole())
									&& roleSystem.getId().equals(identityAccount.getRoleSystem());
						}).count() == 0) {
							AccIdentityAccountDto identityAccount = new AccIdentityAccountDto();
							identityAccount.setAccount(accountId);
							identityAccount.setIdentity(identity.getId());
							identityAccount.setIdentityRole(identityRole.getId());
							identityAccount.setRoleSystem(roleSystem.getId());
							// TODO: Add flag ownership to SystemRole and set here.
							identityAccount.setOwnership(true);

							identityAccountsToCreate.add(identityAccount);
						}

					});
		});
	}

	/**
	 * Return UID for this identity and roleSystem. First will be find and use
	 * transform script from roleSystem attribute. If isn't UID attribute for
	 * roleSystem defined, then will be use default UID attribute handling.
	 * 
	 * @param entity
	 * @param roleSystem
	 * @return
	 */
	@Override
	public String generateUID(AbstractDto entity, SysRoleSystemDto roleSystem) {
		// Find attributes for this roleSystem
		SysRoleSystemAttributeFilter roleSystemAttrFilter = new SysRoleSystemAttributeFilter();
		roleSystemAttrFilter.setRoleSystemId(roleSystem.getId());
		List<SysRoleSystemAttributeDto> attributes = roleSystemAttributeService.find(roleSystemAttrFilter, null)
				.getContent();
		List<SysRoleSystemAttributeDto> attributesUid = attributes.stream().filter(attribute -> {
			return attribute.isUid();
		}).collect(Collectors.toList());

		if (attributesUid.size() > 1) {
			IdmRoleDto roleDto = DtoUtils.getEmbedded(roleSystem, SysRoleSystem_.role);
			SysSystemDto systemDto = DtoUtils.getEmbedded(roleSystem, SysRoleSystem_.system);
			throw new ProvisioningException(AccResultCode.PROVISIONING_ROLE_ATTRIBUTE_MORE_UID,
					ImmutableMap.of("role", roleDto.getCode(), "system", systemDto.getName()));
		}

		SysRoleSystemAttributeDto uidRoleAttribute = !attributesUid.isEmpty() ? attributesUid.get(0) : null;

		// If roleSystem UID attribute found, then we use his transformation
		// script.
		if (uidRoleAttribute != null) {
			// Default values (values from schema attribute handling)
			SysSystemAttributeMappingDto systemAttributeMapping = systemAttributeMappingService
					.get(uidRoleAttribute.getSystemAttributeMapping());
			uidRoleAttribute.setSchemaAttribute(systemAttributeMapping.getSchemaAttribute());
			uidRoleAttribute.setTransformFromResourceScript(systemAttributeMapping.getTransformFromResourceScript());
			Object uid = systemAttributeMappingService.getAttributeValue(null, entity, uidRoleAttribute);
			if (uid == null) {
				SysSystemDto systemEntity = DtoUtils.getEmbedded(roleSystem, SysRoleSystem_.system);
				throw new ProvisioningException(AccResultCode.PROVISIONING_GENERATED_UID_IS_NULL,
						ImmutableMap.of("system", systemEntity.getName()));
			}
			if (!(uid instanceof String)) {
				throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_IS_NOT_STRING,
						ImmutableMap.of("uid", uid));
			}
			return (String) uid;
		}

		SysSystemMappingDto mapping = systemMappingService.get(roleSystem.getSystemMapping());
		// If roleSystem UID was not found, then we use default UID schema
		// attribute handling
		SysSchemaObjectClassDto objectClassDto = schemaObjectClassService.get(mapping.getObjectClass());
		SysSystemDto system = DtoUtils.getEmbedded(objectClassDto, SysSchemaObjectClass_.system);
		SysSystemAttributeMappingFilter systeAttributeMappingFilter = new SysSystemAttributeMappingFilter();
		systeAttributeMappingFilter.setSystemMappingId(mapping.getId());
		List<SysSystemAttributeMappingDto> schemaHandlingAttributes = systemAttributeMappingService
				.find(systeAttributeMappingFilter, null).getContent();
		SysSystemAttributeMappingDto uidAttribute = systemAttributeMappingService
				.getUidAttribute(schemaHandlingAttributes, system);
		return systemAttributeMappingService.generateUid(entity, uidAttribute);
	}

	@Override
	@Transactional
	public void deleteIdentityAccount(IdmIdentityRoleDto entity) {
		AccIdentityAccountFilter filter = new AccIdentityAccountFilter();
		filter.setIdentityRoleId(entity.getId());
		Page<AccIdentityAccountDto> identityAccounts = identityAccountService.find(filter, null);
		List<AccIdentityAccountDto> identityAccountList = identityAccounts.getContent();

		identityAccountList.forEach(identityAccount -> {
			identityAccountService.delete(identityAccount);
		});
	}
	
	@Override
	@Transactional
	public void deleteIdentityAccount(EntityEvent<IdmIdentityRoleDto> event) {
		Assert.notNull(event);
		IdmIdentityRoleDto identityRole = event.getContent();
		Assert.notNull(identityRole);
		Assert.notNull(identityRole.getId());
		//
		if (event.getRootId() == null || !entityEventManager.isRunnable(event.getRootId())) {
			// role is deleted without request or without any parent ... we need to remove account synchronously
			deleteIdentityAccount(identityRole);
			return;
		}
		// role is deleted in bulk (e.g. role request) - account management has to be called outside
		// we just mark identity account to be deleted and remove identity role
		AccIdentityAccountFilter filter = new AccIdentityAccountFilter();
		filter.setIdentityRoleId(identityRole.getId());
		Page<AccIdentityAccountDto> identityAccounts = identityAccountService.find(filter, null);
		List<AccIdentityAccountDto> identityAccountList = identityAccounts.getContent();

		identityAccountList.forEach(identityAccount -> {
			identityAccount.setIdentityRole(null);
			//
			// create entity state for identity account
			IdmEntityStateDto stateDeleted = new IdmEntityStateDto();
			stateDeleted.setSuperOwnerId(identityAccount.getIdentity());
			stateDeleted.setResult(
					new OperationResultDto
						.Builder(OperationState.RUNNING)
						.setModel(new DefaultResultModel(CoreResultCode.DELETED))
						.build());
			entityStateManager.saveState(identityAccount, stateDeleted);
			//
			identityAccountService.save(identityAccount);
		});		
	}

	/**
	 * Create Account by given roleSystem
	 * 
	 * @param identity
	 * @param roleSystem
	 * @param identityAccountsToCreate
	 * @return
	 */
	private UUID createAccountByRoleSystem(IdmIdentityDto identity, SysRoleSystemDto roleSystem,
			List<AccIdentityAccountDto> identityAccountsToCreate) {
		String uid = generateUID(identity, roleSystem);

		// We try find account for same uid on same system
		// First we try search same account in list for create new accounts
		Optional<AccIdentityAccountDto> sameAccountOptional = identityAccountsToCreate.stream().filter(ia -> {
			AccAccountDto account = accountService.get(ia.getAccount());
			return account.getUid().equals(uid) && roleSystem.getId().equals(ia.getRoleSystem());
		}).findFirst();

		if (sameAccountOptional.isPresent()) {
			return sameAccountOptional.get().getAccount();
		}
		UUID accountId = null;
		// If account is not in the list accounts to create, then we will search in
		// database

		// Account management - can be the account created? - execute the script on the
		// system mapping
		SysSystemDto system = DtoUtils.getEmbedded(roleSystem, SysRoleSystem_.system);
		SysSystemMappingDto mapping = systemMappingService.findProvisioningMapping(system.getId(),
				SystemEntityType.IDENTITY);
		if (mapping == null) {
			return null;
		}
		if (!this.canBeAccountCreated(uid, identity, mapping, system)) {
			LOG.info(MessageFormat.format(
					"For entity [{0}] and entity type [{1}] cannot be created the account (on system [{2}]),"
							+ " because script \"Can be account created\" on the mapping returned \"false\"!",
					identity.getCode(), SystemEntityType.IDENTITY, system.getName()));
			return null;
		}

		AccAccountFilter accountFilter = new AccAccountFilter();
		accountFilter.setUid(uid);
		accountFilter.setSystemId(roleSystem.getSystem());
		List<AccAccountDto> sameAccounts = accountService.find(accountFilter, null).getContent();
		if (CollectionUtils.isEmpty(sameAccounts)) {
			// Create and persist new account
			accountId = createAccount(uid, roleSystem);
		} else {
			// We use existed account
			accountId = sameAccounts.get(0).getId();
		}
		return accountId;

	}

	private UUID createAccount(String uid, SysRoleSystemDto roleSystem) {
		AccAccountDto account = new AccAccountDto();
		account.setUid(uid);
		account.setEntityType(SystemEntityType.IDENTITY);
		account.setAccountType(AccountType.PERSONAL);
		account.setSystem(roleSystem.getSystem());
		account = accountService.save(account);
		return account.getId();
	}

	private boolean canBeAccountCreated(String uid, IdmIdentityDto dto, SysSystemMappingDto mapping,
			SysSystemDto system) {
		return systemMappingService.canBeAccountCreated(uid, dto, mapping.getCanBeAccountCreatedScript(), system);
	}
}
