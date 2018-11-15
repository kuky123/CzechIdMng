package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.OperationResultType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationContext;
import eu.bcvsolutions.idm.acc.domain.SynchronizationInactiveOwnerBehaviorType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.EntityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncActionLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncIdentityConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.EntityAccountFilter;
import eu.bcvsolutions.idm.acc.entity.SysSyncIdentityConfig_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationEntityExecutor;
import eu.bcvsolutions.idm.core.api.config.domain.IdentityConfiguration;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.filter.CorrelationFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmConceptRoleRequestFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeService;
import eu.bcvsolutions.idm.core.api.service.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent.IdentityEventType;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.scheduler.task.impl.ProcessAllAutomaticRoleByAttributeTaskExecutor;
import eu.bcvsolutions.idm.ic.api.IcAttribute;

/**
 * Identity sync executor
 * 
 * @author svandav
 *
 */
@Component
public class IdentitySynchronizationExecutor extends AbstractSynchronizationExecutor<IdmIdentityDto>
		implements SynchronizationEntityExecutor {

	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private AccIdentityAccountService identityAccoutnService;
	@Autowired
	private IdmIdentityRoleService identityRoleService;
	@Autowired
	private IdmRoleRequestService roleRequestService;
	@Autowired
	private IdmConceptRoleRequestService conceptRoleRequestService;
	@Autowired
	private IdmIdentityContractService identityContractService;
	@Autowired
	private LongRunningTaskManager longRunningTaskManager;
	@Autowired
	private IdentityConfiguration identityConfiguration;
	
	@Override
	protected SynchronizationContext validate(UUID synchronizationConfigId) {
		SynchronizationContext context = super.validate(synchronizationConfigId);

		SysSyncIdentityConfigDto config = this.getConfig(context);
		SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior = config.getInactiveOwnerBehavior();
		UUID defaultRole = config.getDefaultRole();
		if (defaultRole != null && inactiveOwnerBehavior == null) {
			throw new ResultCodeException(AccResultCode.SYNCHRONIZATION_INACTIVE_OWNER_BEHAVIOR_MUST_BE_SET);
		}
		if (inactiveOwnerBehavior != null && inactiveOwnerBehavior.equals(SynchronizationInactiveOwnerBehaviorType.LINK_PROTECTED)) {
			SysSystemMappingDto provisioningMapping = systemMappingService.findProvisioningMapping(
					context.getSystem().getId(),
					context.getEntityType());
			
			if (provisioningMapping == null) {
				throw new ResultCodeException(AccResultCode.SYNCHRONIZATION_PROVISIONING_MUST_EXIST,
						ImmutableMap.of("property", SynchronizationInactiveOwnerBehaviorType.LINK_PROTECTED));
			}
			if (!provisioningMapping.isProtectionEnabled()) {
				throw new ResultCodeException(AccResultCode.SYNCHRONIZATION_PROTECTION_MUST_BE_ENABLED,
						ImmutableMap.of( //
								"property", SynchronizationInactiveOwnerBehaviorType.LINK_PROTECTED, //
								"mapping", provisioningMapping.getName()));
			}
			context.addProtectionInterval(provisioningMapping.getProtectionInterval());
		}

		return context;
	}

	/**
	 * Delete entity linked with given account
	 * 
	 * @param account
	 * @param entityType
	 * @param log
	 * @param logItem
	 * @param actionLogs
	 */
	protected void doDeleteEntity(AccAccountDto account, SystemEntityType entityType, SysSyncLogDto log,
			SysSyncItemLogDto logItem, List<SysSyncActionLogDto> actionLogs) {
		UUID entityId = getEntityByAccount(account.getId());
		IdmIdentityDto identity = null;
		if (entityId != null) {
			identity = identityService.get(entityId);
		}
		if (identity == null) {
			addToItemLog(logItem, "Warning! -Identity account relation (with ownership = true) was not found!");
			initSyncActionLog(SynchronizationActionType.UPDATE_ENTITY, OperationResultType.WARNING, logItem, log,
					actionLogs);
			return;
		}
		// Delete identity
		identityService.delete(identity);
	}

	/**
	 * Call provisioning for given account
	 * 
	 * @param entity
	 * @param entityType
	 * @param logItem
	 */
	@Override
	protected void callProvisioningForEntity(IdmIdentityDto entity, SystemEntityType entityType,
			SysSyncItemLogDto logItem) {
		addToItemLog(logItem,
				MessageFormat.format(
						"Call provisioning (process IdentityEventType.SAVE) for identity ({0}) with username ({1}).",
						entity.getId(), entity.getUsername()));
		//
		IdentityEvent event = new IdentityEvent(IdentityEventType.UPDATE, entity);
		// We don't want recalculate automatic role by attribute recalculation for every
		// contract.
		// Recalculation will be started only once.
		event.getProperties().put(IdmAutomaticRoleAttributeService.SKIP_RECALCULATION, Boolean.TRUE);

		identityService.publish(event);
	}

	/**
	 * Save entity In the identity sync are creation of the default contract
	 * skipped.
	 * 
	 * @param entity
	 * @param skipProvisioning
	 * @return
	 */
	@Override
	protected IdmIdentityDto save(IdmIdentityDto entity, boolean skipProvisioning, SynchronizationContext context) {
		SysSyncIdentityConfigDto config = this.getConfig(context);
		boolean isNew = identityService.isNew(entity);
		boolean createDefaultContract = config.isCreateDefaultContract();
		
		if (isNew && createDefaultContract) {
			addToItemLog(context.getLogItem(), "The default contract will be created for the identity.");
		}
		//
		EntityEvent<IdmIdentityDto> event = new IdentityEvent(
				isNew ? IdentityEventType.CREATE : IdentityEventType.UPDATE, entity,
				ImmutableMap.of( //
						ProvisioningService.SKIP_PROVISIONING, skipProvisioning, //
						// In the identity sync are creation of the default contracts depend on synchronization setting.
						// Behavior with create default contract doesn't override property with create default contract. Both properties must be allowed.
						IdmIdentityContractService.SKIP_CREATION_OF_DEFAULT_POSITION, !createDefaultContract,
						// We don't want recalculate automatic role by attribute recalculation for every
						// contract.
						// Recalculation will be started only once.
						IdmAutomaticRoleAttributeService.SKIP_RECALCULATION, Boolean.TRUE));
		//
		return identityService.publish(event).getContent();
	}

	/**
	 * Fill data from IC attributes to entity (EAV and confidential storage too)
	 * 
	 * @param account
	 * @param entityType
	 * @param uid
	 * @param icAttributes
	 * @param mappedAttributes
	 * @param log
	 * @param logItem
	 * @param actionLogs
	 */
	@Override
	protected void doUpdateEntity(SynchronizationContext context) {
		String uid = context.getUid();
		SysSyncLogDto log = context.getLog();
		SysSyncItemLogDto logItem = context.getLogItem();

		if (context.isSkipEntityUpdate()) {
			addToItemLog(logItem, MessageFormat.format("Update of entity for account with uid {0} is skipped", uid));
			return;
		}

		List<SysSyncActionLogDto> actionLogs = context.getActionLogs();
		List<SysSystemAttributeMappingDto> mappedAttributes = context.getMappedAttributes();
		AccAccountDto account = context.getAccount();
		List<IcAttribute> icAttributes = context.getIcObject().getAttributes();
		SystemEntityType entityType = context.getEntityType();

		UUID entityId = getEntityByAccount(account.getId());
		IdmIdentityDto identity = null;
		if (entityId != null) {
			identity = identityService.get(entityId);
		}
		if (identity != null) {
			// Update identity
			identity = fillEntity(mappedAttributes, uid, icAttributes, identity, false, context);
			identity = this.save(identity, true, context);
			// Update extended attribute (entity must be persisted first)
			updateExtendedAttributes(mappedAttributes, uid, icAttributes, identity, false, context);
			// Update confidential attribute (entity must be persisted
			// first)
			updateConfidentialAttributes(mappedAttributes, uid, icAttributes, identity, false, context);

			// Identity Updated
			addToItemLog(logItem, MessageFormat.format("Identity with id {0} was updated", identity.getId()));
			if (logItem != null) {
				logItem.setDisplayName(identity.getUsername());
			}

			if (this.isProvisioningImplemented(entityType, logItem)) {
				// Call provisioning for this entity
				callProvisioningForEntity(identity, entityType, logItem);
			}

			return;
		} else {
			addToItemLog(logItem, "Warning! - Identity account relation (with ownership = true) was not found!");
			initSyncActionLog(SynchronizationActionType.UPDATE_ENTITY, OperationResultType.WARNING, logItem, log,
					actionLogs);
			return;
		}
	}

	/**
	 * Operation remove IdentityAccount relations and linked roles
	 * 
	 * @param account
	 * @param removeIdentityRole
	 * @param log
	 * @param logItem
	 * @param actionLogs
	 */
	@Override
	protected void doUnlink(AccAccountDto account, boolean removeIdentityRole, SysSyncLogDto log,
			SysSyncItemLogDto logItem, List<SysSyncActionLogDto> actionLogs) {

		EntityAccountFilter identityAccountFilter = new AccIdentityAccountFilter();
		identityAccountFilter.setAccountId(account.getId());
		List<AccIdentityAccountDto> identityAccounts = identityAccoutnService
				.find((AccIdentityAccountFilter) identityAccountFilter, null).getContent();
		if (identityAccounts.isEmpty()) {
			addToItemLog(logItem, "Warning! - Identity account relation was not found!");
			initSyncActionLog(SynchronizationActionType.UPDATE_ENTITY, OperationResultType.WARNING, logItem, log,
					actionLogs);
			return;
		}
		addToItemLog(logItem, MessageFormat.format("Identity-account relations to delete {0}", identityAccounts));

		identityAccounts.stream().forEach(identityAccount -> {
			// We will remove identity account, but without delete connected
			// account
			identityAccoutnService.delete(identityAccount, false);
			addToItemLog(logItem, MessageFormat.format(
					"Identity-account relation deleted (without calling the delete provisioning operation) (username: {0}, id: {1})",
					identityAccount.getIdentity(), identityAccount.getId()));
			UUID identityRole = identityAccount.getIdentityRole();

			if (removeIdentityRole && identityRole != null) {
				// We will remove connected identity role
				identityRoleService.deleteById(identityRole);
				addToItemLog(logItem, MessageFormat.format("Identity-role relation deleted (id: {0})", identityRole));
			}

		});
		return;
	}

	@Override
	protected EntityAccountDto createEntityAccount(AccAccountDto account, IdmIdentityDto entity,
			SynchronizationContext context) {
		Assert.notNull(account);
		Assert.notNull(entity);

		EntityAccountDto entityAccount = super.createEntityAccount(account, entity, context);
		Assert.isInstanceOf(AccIdentityAccountDto.class, entityAccount,
				"For identity sync must be entity-account relation instance of AccIdentityAccountDto!");
		AccIdentityAccountDto identityAccount = (AccIdentityAccountDto) entityAccount;
		SysSyncIdentityConfigDto config = this.getConfig(context);
		UUID defaultRoleId = config.getDefaultRole();
		if (defaultRoleId == null) {
			return identityAccount;
		}
		// Default role is defined
		IdmRoleDto defaultRole = DtoUtils.getEmbedded(config, SysSyncIdentityConfig_.defaultRole);
		Assert.notNull(defaultRole, "Default role must be found for this sync configuration!");
		context.getLogItem()
				.addToLog(MessageFormat.format(
						"Default role [{1}] is defined and will be assigned to the identity [{0}].", entity.getCode(),
						defaultRole.getCode()));
		
		IdmIdentityContractDto primeContract = identityContractService.getPrimeValidContract(entity.getId());
		if (primeContract == null) {
			SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior = config.getInactiveOwnerBehavior();
			if (inactiveOwnerBehavior.equals(SynchronizationInactiveOwnerBehaviorType.LINK_PROTECTED)) {
				context.getLogItem().addToLog(MessageFormat.format(
						"Default role is set, but it will not be assigned - no valid identity contract was found for identity [{0}],"
						+ " so the account will be in protection.", entity.getCode()));
			} else {
				context.getLogItem().addToLog(
						"Warning! - Default role is set, but could not be assigned to identity, because the identity has not any valid contract!");
				this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
						context.getLog(), context.getActionLogs());
			}
			return identityAccount;
		}

		// Create role request for default role and primary contract
		IdmRoleRequestDto roleRequest = roleRequestService.createRequest(primeContract, defaultRole);
		roleRequest = roleRequestService.startRequestInternal(roleRequest.getId(), false, true);

		// Load concept (can be only one)
		IdmConceptRoleRequestFilter conceptFilter = new IdmConceptRoleRequestFilter();
		conceptFilter.setRoleRequestId(roleRequest.getId());
		UUID identityRoleId = conceptRoleRequestService.find(conceptFilter, null).getContent().get(0).getIdentityRole();
		Assert.notNull(identityRoleId, "Identity role relation had to been created!");

		identityAccount.setIdentityRole(identityRoleId);
		AccIdentityAccountDto duplicate = this.findDuplicate(identityAccount);
		if (duplicate != null) {
			// This IdentityAccount is new and duplicated, we do not want create duplicated
			// relation.
			// Same IdentityAccount had to be created by assigned default role!
			context.getLogItem().addToLog(MessageFormat.format(
					"This identity-account (identity-role id: {2}) is new and duplicated, "
							+ "we do not want create duplicated relation! "
							+ "We will reuse already persisted identity-account [{3}]. "
							+ "Probable reason: Same identity-account had to be created by assigned default role!",
					identityAccount.getAccount(), identityAccount.getIdentity(), identityAccount.getIdentityRole(),
					duplicate.getId()));
			// Reusing duplicate
			return duplicate;
		}

		return identityAccount;

	}

	@Override
	protected EntityAccountFilter createEntityAccountFilter() {
		return new AccIdentityAccountFilter();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected ReadWriteDtoService getEntityAccountService() {
		return identityAccoutnService;
	}

	@Override
	protected EntityAccountDto createEntityAccountDto() {
		return new AccIdentityAccountDto();
	}

	@Override
	protected IdmIdentityService getService() {
		return identityService;
	}

	@Override
	protected CorrelationFilter getEntityFilter() {
		return new IdmIdentityFilter();
	}

	@Override
	protected IdmIdentityDto findByAttribute(String idmAttributeName, String value) {
		CorrelationFilter filter = getEntityFilter();
		filter.setProperty(idmAttributeName);
		filter.setValue(value);
		List<IdmIdentityDto> entities = identityService.find((IdmIdentityFilter) filter, null).getContent();

		if (CollectionUtils.isEmpty(entities)) {
			return null;
		}
		if (entities.size() > 1) {
			throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_CORRELATION_TO_MANY_RESULTS,
					ImmutableMap.of("correlationAttribute", idmAttributeName, "value", value));
		}
		if (entities.size() == 1) {
			return entities.get(0);
		}
		return null;
	}

	@Override
	protected IdmIdentityDto createEntityDto() {
		return new IdmIdentityDto();
	}

	@Override
	protected SysSyncLogDto syncStarted(SysSyncLogDto log, SynchronizationContext context) {
		log = super.syncStarted(log, context);

		SysSyncIdentityConfigDto config = this.getConfig(context);
		UUID defaultRoleId = config.getDefaultRole();
		SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior = config.getInactiveOwnerBehavior();
		boolean startAutoRoleRec = config.isStartAutoRoleRec();
		boolean createDefaultContract = config.isCreateDefaultContract();
		boolean createDefaultContractSystem = identityConfiguration.isCreateDefaultContractEnabled();

		String defaultRoleCode = "";
		if (defaultRoleId != null) {
			IdmRoleDto defaultRole = DtoUtils.getEmbedded(config, SysSyncIdentityConfig_.defaultRole);
			Assert.notNull(defaultRole, "Default role must be found for this sync configuration!");
			defaultRoleCode = defaultRole.getCode();
		}

		StringBuilder builder = new StringBuilder();
		builder.append("Specific settings:");
		builder.append(MessageFormat.format("\nDefault role: {0}", defaultRoleCode));
		builder.append(MessageFormat.format("\nBehavior of the default role for inactive identities: {0}", defaultRoleId == null ? "---" : inactiveOwnerBehavior));
		if (createDefaultContract && !createDefaultContractSystem) {
			builder.append("\nCreate default contract: WARNING! Creating default contract is enabled, but it's disabled on the system level. Contracts will not be created!");
		} else {
			builder.append(MessageFormat.format("\nCreate default contract: {0}", createDefaultContract));
		}
		builder.append(MessageFormat.format("\nAfter end, start the automatic role recalculation: {0}", startAutoRoleRec));

		log.addToLog(builder.toString());

		return log;
	}

	@Override
	protected SysSyncLogDto syncCorrectlyEnded(SysSyncLogDto log, SynchronizationContext context) {
		log = super.syncCorrectlyEnded(log, context);

		if (getConfig(context).isStartAutoRoleRec()) {
			log = executeAutomaticRoleRecalculation(log);
		} else {
			log.addToLog("Start of the automatic role recalculation (after sync) is not allowed");
		}

		return log;
	}

	/**
	 * Start automatic role by attribute recalculation synchronously.
	 *
	 * @param log
	 * @return
	 */
	private SysSyncLogDto executeAutomaticRoleRecalculation(SysSyncLogDto log) {
		ProcessAllAutomaticRoleByAttributeTaskExecutor executor = new ProcessAllAutomaticRoleByAttributeTaskExecutor();

		log.addToLog(MessageFormat.format(
				"An automatic role by attribute recalculation has to be start after the synchronization end. We start it (synchronously) now [{0}].",
				LocalDateTime.now()));
		Boolean executed = longRunningTaskManager.executeSync(executor);

		if (BooleanUtils.isTrue(executed)) {
			log.addToLog(MessageFormat.format("Recalculation of automatic roles by attribute ended in [{0}].",
					LocalDateTime.now()));
		} else {
			addToItemLog(log, "Warning - recalculation of automatic roles by attribute was not executed correctly.");
		}

		return synchronizationLogService.save(log);
	}

	private SysSyncIdentityConfigDto getConfig(SynchronizationContext context) {
		Assert.isInstanceOf(SysSyncIdentityConfigDto.class, context.getConfig(),
				"For identity sync must be sync configuration instance of SysSyncIdentityConfigDto!");
		return ((SysSyncIdentityConfigDto) context.getConfig());
	}

	/**
	 * Search duplicate for given identity-account relation. If some duplicate is
	 * found, then is returned first.
	 * 
	 * @param identityAccount
	 * @return
	 */
	private AccIdentityAccountDto findDuplicate(AccIdentityAccountDto identityAccount) {
		Assert.notNull(identityAccount);
		Assert.notNull(identityAccount.getAccount());
		Assert.notNull(identityAccount.getIdentity());

		AccIdentityAccountFilter filter = new AccIdentityAccountFilter();
		filter.setAccountId(identityAccount.getAccount());
		filter.setOwnership(identityAccount.isOwnership());
		filter.setIdentityId(identityAccount.getIdentity());
		filter.setIdentityRoleId(identityAccount.getIdentityRole());
		filter.setRoleSystemId(identityAccount.getRoleSystem());

		List<AccIdentityAccountDto> entityAccounts = identityAccoutnService.find(filter, null).getContent();
		if (entityAccounts.isEmpty()) {
			return null;
		}
		return entityAccounts.get(0);
	}
	
	/**
	 * Apply settings that are specific to this type of entity.
	 * Default implementation is empty.
	 * @param account
	 * @param entity - can be null in the case of Missing entity situation
	 * @param context
	 */
	@Override
	protected AccAccountDto applySpecificSettingsBeforeLink(AccAccountDto account, IdmIdentityDto entity, SynchronizationContext context) {
		SysSyncIdentityConfigDto config = this.getConfig(context);
		SysSyncItemLogDto logItem = context.getLogItem();
		SynchronizationInactiveOwnerBehaviorType inactiveOwnerBehavior = config.getInactiveOwnerBehavior();
		UUID defaultRoleId = config.getDefaultRole();
		if (defaultRoleId == null) {
			// Default role is not specified - no problem
			return account;
		}
		if (inactiveOwnerBehavior.equals(SynchronizationInactiveOwnerBehaviorType.LINK)) {
			return account;
		}

		IdmIdentityContractDto primeContract = entity != null ? identityContractService.getPrimeValidContract(entity.getId()) : null;
		if (primeContract != null) {
			// Default role can be assigned - no problem
			return account;
		}

		boolean contractCanBeCreated = config.isCreateDefaultContract() && identityConfiguration.isCreateDefaultContractEnabled();

		switch (inactiveOwnerBehavior) {
		case LINK_PROTECTED:
			if (entity != null || !contractCanBeCreated) {
				activateProtection(account, entity, context);
			}
			return account;
		case DO_NOT_LINK:
			if (entity == null) {
				if (contractCanBeCreated) {
					// (active) default contract will be created later on during creation of entity and the default role can be added to it
					// so the link can be created here
					return account;
				} else {
					// there will be no contract to assign the default role -> no link
					addToItemLog(logItem, MessageFormat.format(
							"New identity for account with uid [{0}] would not have any default contract, so the account could not be linked. So the identity will not be created.",
							account.getUid()));
					initSyncActionLog(SynchronizationActionType.MISSING_ENTITY, OperationResultType.IGNORE, logItem, context.getLog(), context.getActionLogs());
					return null;
				}
			}
			// We don't want to create account at all and also we don't want to continue updating entity if it was configured
			context.addSkipEntityUpdate(true);
			addToItemLog(logItem, MessageFormat.format(
					"Identity [{0}] does not have any valid contract, account with uid [{1}] will not be linked.",
					entity.getCode(), account.getUid()));
			initSyncActionLog(SynchronizationActionType.UNLINKED, OperationResultType.IGNORE, logItem, context.getLog(), context.getActionLogs());
			return null;
		default:
			return account;
		}
	}

	private void activateProtection(AccAccountDto account, IdmIdentityDto entity, SynchronizationContext context) {
		SysSyncItemLogDto logItem = context.getLogItem();
		//TODO configuration
		/* Current date + interval
		   Last valid contract + interval ... default
		     - enable past dates  .... default
		     - if no contract or past date, then set current date + interval
		*/
		
		// Compute the values for the protection
		Integer protectionInterval = context.getProtectionInterval();
		DateTime endOfProtection = null;
		LocalDate protectionStart = null;
		IdmIdentityContractDto lastExpiredContract = null;
		if (protectionInterval != null) {
			LocalDate now = new LocalDate();
			lastExpiredContract = entity != null ? identityContractService.findLastExpiredContract(entity.getId(), now) : null;
			protectionStart = (lastExpiredContract != null) ? lastExpiredContract.getValidTill() : now;
			// interval + 1 day = ensure that the account is in protection for at least specified number of days
			// after the contract ended. This can be in the past.
			endOfProtection = protectionStart.toDateTimeAtStartOfDay().plusDays(protectionInterval+1);
		}
		
		// Set the values to the account
		account.setInProtection(true);
		account.setEndOfProtection(endOfProtection);

		// Log the result
		String endOfProtectionString = endOfProtection == null ? "infinitely" : "until " + endOfProtection;
		if (entity != null) {
			addToItemLog(logItem, MessageFormat.format(
					"Identity [{0}] does not have any valid contract, account with uid [{1}] will be in protection {2}."
					+ " Last expired contract: {3}",
					entity.getCode(),
					account.getUid(),
					endOfProtectionString,
					lastExpiredContract == null ? "does not exist" : lastExpiredContract.getPosition() + " (valid till " + lastExpiredContract.getValidTill() + ")"));
		} else {
			addToItemLog(logItem, MessageFormat.format(
					"New identity for account with uid [{0}] will not have any valid contract, so the account will be in protection {1}.",
					account.getUid(),
					endOfProtectionString));
		}
	}
}
