package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.OperationResultType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationContext;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.EntityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncActionLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncIdentityConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.EntityAccountFilter;
import eu.bcvsolutions.idm.acc.entity.SysSyncIdentityConfig_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationEntityExecutor;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.filter.CorrelationFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmConceptRoleRequestFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
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
			addToItemLog(context.getLogItem(), "For identity will be created default contract.");
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
	protected void doUpdateEntity(SynchronizationContext context) {

		String uid = context.getUid();
		SysSyncLogDto log = context.getLog();
		SysSyncItemLogDto logItem = context.getLogItem();
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
					"Identity-account relation deleted (without call delete provisioning) (username: {0}, id: {1})",
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
		// Default role is defines
		IdmRoleDto defaultRole = DtoUtils.getEmbedded(config, SysSyncIdentityConfig_.defaultRole);
		context.getLogItem()
				.addToLog(MessageFormat.format(
						"Default role [{1}] is defines and will be assigned to the identity [{0}].", entity.getCode(),
						defaultRole.getCode()));
		Assert.notNull(defaultRole, "Default role must be found for this sync configuration!");
		IdmIdentityContractDto primeContract = identityContractService.getPrimeValidContract(entity.getId());
		if (primeContract == null) {
			context.getLogItem().addToLog(
					"Warning! - Default role is set, but could not be assigned to identity, because was not found any valid identity contract!");
			this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
					context.getLog(), context.getActionLogs());
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
							+ "We will reusing already persisted identity-account [{3}]. "
							+ "Probable reason: Same  identity-account had to be created by assigned default role!",
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
	protected SysSyncLogDto syncCorrectlyEnded(SysSyncLogDto log, SynchronizationContext context) {
		log = super.syncCorrectlyEnded(log, context);

		if (getConfig(context).isStartAutoRoleRec()) {
			log = executeAutomaticRoleRecalculation(log);
		} else {
			log.addToLog(MessageFormat.format("Start automatic role recalculation (after sync) isn't allowed [{0}]",
					LocalDateTime.now()));
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
				"After success sync have to be run Automatic role by attribute recalculation. We start him (synchronously) now [{0}].",
				LocalDateTime.now()));
		Boolean executed = longRunningTaskManager.executeSync(executor);

		if (BooleanUtils.isTrue(executed)) {
			log.addToLog(MessageFormat.format("Recalculation automatic role by attribute ended in [{0}].",
					LocalDateTime.now()));
		} else {
			addToItemLog(log, "Warning - recalculation automatic role by attribute is not executed correctly.");
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
}
