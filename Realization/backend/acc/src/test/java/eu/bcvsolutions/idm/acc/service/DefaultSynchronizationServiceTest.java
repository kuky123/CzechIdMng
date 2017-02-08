package eu.bcvsolutions.idm.acc.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;

import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.acc.domain.OperationResultType;
import eu.bcvsolutions.idm.acc.domain.ReconciliationMissingAccountActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationLinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationMissingEntityActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationUnlinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.filter.IdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SyncActionLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SyncItemLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SynchronizationConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SynchronizationLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.AccAccount;
import eu.bcvsolutions.idm.acc.entity.AccIdentityAccount;
import eu.bcvsolutions.idm.acc.entity.SysSchemaAttribute;
import eu.bcvsolutions.idm.acc.entity.SysSchemaObjectClass;
import eu.bcvsolutions.idm.acc.entity.SysSyncActionLog;
import eu.bcvsolutions.idm.acc.entity.SysSyncConfig;
import eu.bcvsolutions.idm.acc.entity.SysSyncItemLog;
import eu.bcvsolutions.idm.acc.entity.SysSyncLog;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.acc.entity.SysSystemFormValue;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping;
import eu.bcvsolutions.idm.acc.entity.TestResource;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncActionLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncItemLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.eav.entity.AbstractFormValue;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition;
import eu.bcvsolutions.idm.core.eav.service.api.FormService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityService;
import eu.bcvsolutions.idm.ic.domain.IcFilterOperationType;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Synchronization tests
 * 
 * @author Svanda
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Rollback(false)
@Service
public class DefaultSynchronizationServiceTest extends AbstractIntegrationTest {
	private static final String IDENTITY_USERNAME_ONE = "syncUserOneTest";
	private static final String IDENTITY_USERNAME_TWO = "syncUserTwoTest";
	private static final String IDENTITY_USERNAME_THREE = "syncUserThreeTest";
	private static final String ATTRIBUTE_NAME = "__NAME__";
	private static final String ATTRIBUTE_MODIFIED = "modified";
	private static final String ATTRIBUTE_VALUE_CHANGED = "changed";

	private static final String SYNC_CONFIG_NAME = "syncConfigName";

	private static final String IDENTITY_EMAIL_WRONG = "email";
	private static final String IDENTITY_EMAIL_CORRECT = "email@test.cz";

	@Autowired
	private SysSystemService sysSystemService;
	
	@Autowired
	private IdmIdentityService idmIdentityService;

	@Autowired
	private AccIdentityAccountService identityAccoutnService;

	@Autowired
	private AccAccountService accountService;

	@Autowired
	private SysSystemMappingService systemMappingService;

	@Autowired
	private SysSystemAttributeMappingService schemaAttributeMappingService;

	@Autowired
	private SysSchemaAttributeService schemaAttributeService;

	@Autowired
	private SysSyncConfigService syncConfigService;

	@Autowired
	private SysSyncLogService syncLogService;

	@Autowired
	private SysSyncItemLogService syncItemLogService;

	@Autowired
	private SysSyncActionLogService syncActionLogService;

	@Autowired
	private SynchronizationService synchornizationService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private FormService formService;

	@Autowired
	DataSource dataSource;

	// Only for call method createTestSystem
	@Autowired
	private DefaultSysAccountManagementServiceTest defaultSysAccountManagementServiceTest;
	private SysSystem system;

	@Before
	public void init() {
		loginAsAdmin("admin");
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	@Transactional
	public void doCreateSyncConfig() {
		initData();

		SystemMappingFilter mappingFilter = new SystemMappingFilter();
		mappingFilter.setEntityType(SystemEntityType.IDENTITY);
		mappingFilter.setSystemId(system.getId());
		mappingFilter.setOperationType(SystemOperationType.SYNCHRONIZATION);
		List<SysSystemMapping> mappings = systemMappingService.find(mappingFilter, null).getContent();
		Assert.assertEquals(1, mappings.size());
		SysSystemMapping mapping = mappings.get(0);
		SystemAttributeMappingFilter attributeMappingFilter = new SystemAttributeMappingFilter();
		attributeMappingFilter.setSystemMappingId(mapping.getId());

		List<SysSystemAttributeMapping> attributes = schemaAttributeMappingService.find(attributeMappingFilter, null)
				.getContent();
		SysSystemAttributeMapping nameAttribute = attributes.stream().filter(attribute -> {
			return attribute.getName().equals(ATTRIBUTE_NAME);
		}).findFirst().get();

		SysSystemAttributeMapping modifiedAttribute = attributes.stream().filter(attribute -> {
			return attribute.getName().equals(ATTRIBUTE_MODIFIED);
		}).findFirst().get();

		// Create default synchronization config
		SysSyncConfig syncConfigCustom = new SysSyncConfig();
		syncConfigCustom.setCustomFilter(true);
		syncConfigCustom.setSystemMapping(mapping);
		syncConfigCustom.setCorrelationAttribute(nameAttribute);
		syncConfigCustom.setTokenAttribute(modifiedAttribute);
		syncConfigCustom.setFilterAttribute(modifiedAttribute);
		syncConfigCustom.setReconciliation(true);
		syncConfigCustom.setName(SYNC_CONFIG_NAME);
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.CREATE_ENTITY);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);

		syncConfigService.save(syncConfigCustom);

		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setSystemId(system.getId());
		Assert.assertEquals(1, syncConfigService.find(configFilter, null).getTotalElements());
	}

	@Test
	public void doStartSyncA_MissingEntity() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		synchornizationService.startSynchronizationEvent(syncConfigCustom);
		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog createEntityActionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.CREATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(createEntityActionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void doStartSyncB_Linked_doEntityUpdate() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		this.getBean().changeResourceData();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		Assert.assertEquals(IDENTITY_USERNAME_ONE,
				idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE).getFirstName());
		Assert.assertEquals(IDENTITY_USERNAME_TWO,
				idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO).getLastName());

		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UPDATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Check state after sync
		Assert.assertEquals(ATTRIBUTE_VALUE_CHANGED,
				idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE).getFirstName());
		Assert.assertEquals(ATTRIBUTE_VALUE_CHANGED,
				idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO).getLastName());

		// Delete log
		syncLogService.delete(log);

	}
	
	@Test
	/**
	 * We will do synchronize with use inner connector synch function.
	 */
	public void doStartSyncB_Linked_doEntityUpdate_Filtered() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		this.getBean().changeResourceData();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));
		
		SysSystem system = syncConfigCustom.getSystemMapping().getSystem();
		
		IdmFormDefinition savedFormDefinition = sysSystemService.getConnectorFormDefinition(system.getConnectorKey());
		List<AbstractFormValue<SysSystem>> values = formService.getValues(system, savedFormDefinition);
		AbstractFormValue<SysSystem> changeLogColumn = values.stream().filter(value -> {return "changeLogColumn".equals(value.getFormAttribute().getName());}).findFirst().get();
		formService.saveValues(system, changeLogColumn.getFormAttribute(), ImmutableList.of("modified"));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		
		syncConfigCustom.setCustomFilter(false);
		syncConfigCustom.setReconciliation(false);
		syncConfigCustom.setToken(LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss")); // We want do sync for account changed in future
		syncConfigCustom.setFilterOperation(IcFilterOperationType.ENDS_WITH); // We don`t use custom filter. This option will be not used.
		syncConfigService.save(syncConfigCustom);

		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UPDATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("x"+IDENTITY_USERNAME_TWO, items.get(0).getIdentification());

		// Delete log
		syncLogService.delete(log);
		
		// We have to change property of connector configuration "changeLogColumn" from "modified" on empty string. 
		// When is this property set, then custom filter not working. Bug in Table connector !!!
		formService.saveValues(system, changeLogColumn.getFormAttribute(), ImmutableList.of(""));

	}
	
	@Test
	/**
	 * We will do sync with use custom filter. Only account modified in last will be synchronized.
	 */
	public void doStartSyncB_Linked_doEntityUpdate_Filtered_Custom() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		this.getBean().changeResourceData();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		
		syncConfigCustom.setCustomFilter(true);
		syncConfigCustom.setReconciliation(false);
		syncConfigCustom.setToken(LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
		syncConfigCustom.setFilterOperation(IcFilterOperationType.LESS_THAN);
		syncConfigService.save(syncConfigCustom);

		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UPDATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(1, items.size());
		Assert.assertEquals("x"+IDENTITY_USERNAME_ONE, items.get(0).getIdentification());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	/*
	 * We will assert, that in log will be errors, when we will set incorrect
	 * email format.
	 */
	public void doStartSyncB_Linked_doEntityUpdate_WrongEmail() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		// Set wrong email to resource
		this.getBean().changeResourceDataWrongEmail();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		// Log must contains error
		Assert.assertTrue(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UPDATE_ENTITY == action.getSyncAction()
					&& OperationResultType.ERROR == action.getOperationResult();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void doStartSyncB_Linked_doUnLinked() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UNLINK);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		IdentityAccountFilter identityAccountFilterOne = new IdentityAccountFilter();
		identityAccountFilterOne.setIdentityId(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE).getId());
		Assert.assertEquals(1, identityAccoutnService.find(identityAccountFilterOne, null).getTotalElements());

		IdentityAccountFilter identityAccountFilterTwo = new IdentityAccountFilter();
		identityAccountFilterTwo.setIdentityId(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE).getId());
		Assert.assertEquals(1, identityAccoutnService.find(identityAccountFilterTwo, null).getTotalElements());

		// Start synchronization
		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UNLINK == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Check state after sync
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterOne, null).getTotalElements());
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterTwo, null).getTotalElements());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void doStartSyncC_Unlinked_doLink() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync

		IdmIdentity identityOne = idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE);
		IdmIdentity identityTwo = idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO);
		IdentityAccountFilter identityAccountFilterOne = new IdentityAccountFilter();
		identityAccountFilterOne.setIdentityId(identityOne.getId());
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterOne, null).getTotalElements());

		IdentityAccountFilter identityAccountFilterTwo = new IdentityAccountFilter();
		identityAccountFilterTwo.setIdentityId(identityTwo.getId());
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterTwo, null).getTotalElements());

		// Start synchronization
		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.LINK == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Check state after sync
		Assert.assertEquals(1, identityAccoutnService.find(identityAccountFilterOne, null).getTotalElements());
		Assert.assertEquals(1, identityAccoutnService.find(identityAccountFilterTwo, null).getTotalElements());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void doStartSyncC_Unlinked_doLinkAndUpdateAccount() {
		// We have to do unlink first
		this.doStartSyncB_Linked_doUnLinked();

		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK_AND_UPDATE_ACCOUNT);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		IdmIdentity identityOne = idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE);
		IdmIdentity identityTwo = idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO);

		identityOne.setFirstName(IDENTITY_USERNAME_ONE);
		identityTwo.setLastName(IDENTITY_USERNAME_TWO);
		idmIdentityService.save(identityOne);
		idmIdentityService.save(identityTwo);

		// Change account on resource
		getBean().changeResourceData();

		// Check state before sync
		IdentityAccountFilter identityAccountFilterOne = new IdentityAccountFilter();
		identityAccountFilterOne.setIdentityId(identityOne.getId());
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterOne, null).getTotalElements());

		IdentityAccountFilter identityAccountFilterTwo = new IdentityAccountFilter();
		identityAccountFilterTwo.setIdentityId(identityTwo.getId());
		Assert.assertEquals(0, identityAccoutnService.find(identityAccountFilterTwo, null).getTotalElements());

		Assert.assertEquals(IDENTITY_USERNAME_ONE, identityOne.getFirstName());
		Assert.assertEquals(IDENTITY_USERNAME_TWO, identityTwo.getLastName());
		Assert.assertNotEquals(IDENTITY_USERNAME_ONE,
				entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_ONE).getFirstname());
		Assert.assertNotEquals(IDENTITY_USERNAME_TWO,
				entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_TWO).getLastname());

		// Start synchronization
		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.LINK_AND_UPDATE_ACCOUNT == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(2, items.size());

		// Check state after sync
		Assert.assertEquals(IDENTITY_USERNAME_ONE,
				entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_ONE).getFirstname());
		Assert.assertEquals(IDENTITY_USERNAME_TWO,
				entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_TWO).getLastname());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void doStartSyncD_Missing_Account_doCreateAccount() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Create new identity THREE, with account
		IdmIdentity identity = new IdmIdentity();
		identity.setUsername("x" + IDENTITY_USERNAME_THREE);
		identity.setFirstName(IDENTITY_USERNAME_THREE);
		identity.setLastName(IDENTITY_USERNAME_THREE);
		identity = idmIdentityService.save(identity);

		AccAccount accountOne = new AccAccount();
		accountOne.setSystem(syncConfigCustom.getSystemMapping().getSystem());
		accountOne.setUid("x" + IDENTITY_USERNAME_THREE);
		accountOne.setAccountType(AccountType.PERSONAL);
		accountOne = accountService.save(accountOne);

		AccIdentityAccount accountIdentityOne = new AccIdentityAccount();
		accountIdentityOne.setIdentity(identity);
		accountIdentityOne.setOwnership(true);
		accountIdentityOne.setAccount(accountOne);

		accountIdentityOne = identityAccoutnService.save(accountIdentityOne);

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.CREATE_ACCOUNT);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		Assert.assertNull(entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_THREE));

		// Start synchronization
		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(2, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.CREATE_ACCOUNT == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(1, items.size());

		// Check state after sync
		Assert.assertNotNull(entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_THREE));

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void doStartSyncD_Missing_Account_doDeleteEntity() {
		SynchronizationConfigFilter configFilter = new SynchronizationConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<SysSyncConfig> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		SysSyncConfig syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Delete all accounts in resource
		this.getBean().deleteAllResourceData();

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.DELETE_ENTITY);
		syncConfigCustom.setReconciliation(true);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		Assert.assertNotNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE));
		Assert.assertNotNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO));
		Assert.assertNotNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_THREE));

		// Start synchronization
		synchornizationService.startSynchronizationEvent(syncConfigCustom);

		SynchronizationLogFilter logFilter = new SynchronizationLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLog> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLog log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SyncActionLogFilter actionLogFilter = new SyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLog> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLog actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.DELETE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SyncItemLogFilter itemLogFilter = new SyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLog> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(3, items.size());

		// Check state after sync
		Assert.assertNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_ONE));
		Assert.assertNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_TWO));
		Assert.assertNull(idmIdentityService.getByUsername("x" + IDENTITY_USERNAME_THREE));

		// Delete log
		syncLogService.delete(log);
	}

	@Transactional
	public void changeResourceData() {
		// Change data on resource
		TestResource one = entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_ONE);
		one.setFirstname(ATTRIBUTE_VALUE_CHANGED);
		TestResource two = entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_TWO);
		two.setLastname(ATTRIBUTE_VALUE_CHANGED);
		entityManager.persist(one);
		entityManager.persist(two);
	}

	@Transactional
	public void changeResourceDataWrongEmail() {
		// Set wrong email to resource
		TestResource one = entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_ONE);
		one.setEmail(IDENTITY_EMAIL_WRONG);
		TestResource two = entityManager.find(TestResource.class, "x" + IDENTITY_USERNAME_TWO);
		two.setEmail(IDENTITY_EMAIL_WRONG);
		// Change data on resource
		entityManager.persist(one);
		entityManager.persist(two);
	}

	@Transactional
	public void addIdentityThreeToResourceData() {
		// Change data on resource
		// Insert data to testResource table
		TestResource resourceUser = new TestResource();
		resourceUser.setName("x" + IDENTITY_USERNAME_THREE);
		resourceUser.setFirstname(IDENTITY_USERNAME_THREE);
		resourceUser.setLastname(IDENTITY_USERNAME_THREE);
		resourceUser.setEmail(IDENTITY_EMAIL_CORRECT);
		entityManager.persist(resourceUser);
	}

	@Transactional
	public void initResourceData() {
		// Delete all
		Query q = entityManager.createNativeQuery("DELETE FROM test_resource");
		q.executeUpdate();
		// Insert data to testResource table
		LocalDateTime paste = LocalDateTime.now().minusYears(1);
		LocalDateTime future = paste.plusYears(2);
		
		TestResource resourceUserOne = new TestResource();
		resourceUserOne.setName("x" + IDENTITY_USERNAME_ONE);
		resourceUserOne.setFirstname(IDENTITY_USERNAME_ONE);
		resourceUserOne.setLastname(IDENTITY_USERNAME_ONE);
		resourceUserOne.setEmail(IDENTITY_EMAIL_CORRECT);
		resourceUserOne.setModified(paste);
		entityManager.persist(resourceUserOne);

		TestResource resourceUserTwo = new TestResource();
		resourceUserTwo.setName("x" + IDENTITY_USERNAME_TWO);
		resourceUserTwo.setFirstname(IDENTITY_USERNAME_TWO);
		resourceUserTwo.setLastname(IDENTITY_USERNAME_TWO);
		resourceUserTwo.setEmail(IDENTITY_EMAIL_CORRECT);
		resourceUserTwo.setModified(future);
		entityManager.persist(resourceUserTwo);
	}

	@Transactional
	public void deleteAllResourceData() {
		// Delete all
		Query q = entityManager.createNativeQuery("DELETE FROM test_resource");
		q.executeUpdate();
	}

	private void initData() {

		// create test system
		system = defaultSysAccountManagementServiceTest.createTestSystem();

		// generate schema for system
		List<SysSchemaObjectClass> objectClasses = sysSystemService.generateSchema(system);

		// Create provisioning mapping
		SysSystemMapping systemMapping = new SysSystemMapping();
		systemMapping.setName("default_" + System.currentTimeMillis());
		systemMapping.setEntityType(SystemEntityType.IDENTITY);
		systemMapping.setOperationType(SystemOperationType.PROVISIONING);
		systemMapping.setObjectClass(objectClasses.get(0));
		final SysSystemMapping provisioningMapping = systemMappingService.save(systemMapping);

		createMapping(system, provisioningMapping);

		// Create synchronization mapping
		SysSystemMapping syncSystemMapping = new SysSystemMapping();
		syncSystemMapping.setName("default_" + System.currentTimeMillis());
		syncSystemMapping.setEntityType(SystemEntityType.IDENTITY);
		syncSystemMapping.setOperationType(SystemOperationType.SYNCHRONIZATION);
		syncSystemMapping.setObjectClass(objectClasses.get(0));
		final SysSystemMapping syncMapping = systemMappingService.save(syncSystemMapping);

		createMapping(system, syncMapping);

		initResourceData();

	}

	private void createMapping(SysSystem system, final SysSystemMapping entityHandlingResult) {
		SchemaAttributeFilter schemaAttributeFilter = new SchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());

		Page<SysSchemaAttribute> schemaAttributesPage = schemaAttributeService.find(schemaAttributeFilter, null);
		schemaAttributesPage.forEach(schemaAttr -> {
			if (ATTRIBUTE_NAME.equals(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setUid(true);
				attributeHandlingName.setEntityAttribute(true);
				attributeHandlingName.setIdmPropertyName("username");
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("firstname".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setIdmPropertyName("firstName");
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("lastname".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setIdmPropertyName("lastName");
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("email".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setIdmPropertyName("email");
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if (IcConnectorFacade.PASSWORD_ATTRIBUTE_NAME.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setIdmPropertyName("password");
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if (SystemOperationType.SYNCHRONIZATION == entityHandlingResult.getOperationType()
					&& ATTRIBUTE_MODIFIED.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMapping attributeHandlingName = new SysSystemAttributeMapping();
				attributeHandlingName.setEntityAttribute(false);
				attributeHandlingName.setSchemaAttribute(schemaAttr);
				attributeHandlingName.setName(ATTRIBUTE_MODIFIED);
				attributeHandlingName.setSystemMapping(entityHandlingResult);
				schemaAttributeMappingService.save(attributeHandlingName);

			}
		});
	}

	private DefaultSynchronizationServiceTest getBean() {
		return applicationContext.getBean(this.getClass());
	}

}