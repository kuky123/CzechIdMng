package eu.bcvsolutions.idm.acc.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.domain.OperationResultType;
import eu.bcvsolutions.idm.acc.domain.ReconciliationMissingAccountActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationLinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationMissingEntityActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationUnlinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.AbstractSysSyncConfigDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncActionLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncIdentityConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncActionLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncItemLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.TestResource;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.scheduler.task.impl.SynchronizationSchedulableTaskExecutor;
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
import eu.bcvsolutions.idm.acc.service.impl.DefaultSynchronizationServiceTest;
import eu.bcvsolutions.idm.core.api.config.domain.EventConfiguration;
import eu.bcvsolutions.idm.core.api.config.domain.IdentityConfiguration;
import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleAttributeRuleComparison;
import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleAttributeRuleType;
import eu.bcvsolutions.idm.core.api.domain.IdmScriptCategory;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.ScriptAuthorityType;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmScriptAuthorityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmScriptDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmScriptAuthorityService;
import eu.bcvsolutions.idm.core.api.service.IdmScriptService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.scheduler.ObserveLongRunningTaskEndProcessor;
import eu.bcvsolutions.idm.core.scheduler.api.config.SchedulerConfiguration;
import eu.bcvsolutions.idm.core.scheduler.api.dto.DependentTaskTrigger;
import eu.bcvsolutions.idm.core.scheduler.api.dto.Task;
import eu.bcvsolutions.idm.core.scheduler.service.impl.DefaultSchedulerManager;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Identity synchronization tests (basic tests for identity sync are in
 * {@link DefaultSynchronizationServiceTest})
 * 
 * @author Svanda
 *
 */
@Service
public class IdentitySyncTest extends AbstractIntegrationTest {

	private static final String IDENTITY_ONE = "identityOne";
	private static final String IDENTITY_ONE_EMAIL = "email@test.cz";
	private static final String SYNC_CONFIG_NAME = "syncConfigNameContract";
	private static final String ATTRIBUTE_NAME = "__NAME__";
	private static final String ATTRIBUTE_EMAIL = "email";
	private static final String ATTRIBUTE_EMAIL_TWO = "emailTwo";

	@Autowired
	private TestHelper helper;
	@Autowired
	private SysSystemService systemService;
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
	private EntityManager entityManager;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private IdmIdentityContractService contractService;
	@Autowired
	private IdmIdentityRoleService identityRoleService;
	@Autowired
	private AccIdentityAccountService identityAccountService;
	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private IdmScriptAuthorityService scriptAuthrotityService;
	@Autowired
	private IdmScriptService scriptService;
	@Autowired
	private TestIdentityProcessor testIdentityProcessor;
	@Autowired
	private FormService formService;
	@Autowired
	private DefaultSchedulerManager manager;
	@Autowired
	private ConfigurationService configurationService;
	@Autowired
	private SynchronizationService synchronizationService;
	@Autowired
	private IdentityConfiguration identityConfiguration;

	@After
	public void logout() {
		if (identityService.getByUsername(IDENTITY_ONE) != null) {
			identityService.delete(identityService.getByUsername(IDENTITY_ONE));
		}
	}

	@Test
	public void createIdentityWithDefaultRoleTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());

		helper.startSynchronization(config);

		// Have to be in the warning state, because default role cannot be assigned for
		// new identity, because sync do not creates the default contract. See
		// IdmIdentityContractService.SKIP_CREATION_OF_DEFAULT_POSITION.
		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1,
				OperationResultType.WARNING);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identities.get(0).getId());
		Assert.assertEquals(0, roles.size());

		// Delete log
		syncLogService.delete(log);
		syncConfigService.delete(config);
	}
	
	@Test
	public void testCreateIdentityWithDefaultContractAndRoleSync() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		IdmRoleDto defaultRole = helper.createRole();
		//
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config.setCreateDefaultContract(true);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		//
		// create default mapping for provisioning
		helper.createMapping(system);
		helper.createRoleSystem(defaultRole, system);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());

		helper.startSynchronization(config);

		// Have to be in the success state, because default role will be assigned to the default contract.
		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1, OperationResultType.SUCCESS);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		IdmIdentityDto identity = identities.get(0);
		List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identities.get(0).getId());
		Assert.assertEquals(1, roles.size());
		IdmIdentityRoleDto assignedRole = roles.get(0);
		Assert.assertEquals(defaultRole.getId(), assignedRole.getRole());
		
		// check only one identity account is created
		AccIdentityAccountFilter accountFilter = new AccIdentityAccountFilter();
		accountFilter.setIdentityId(identity.getId());
		List<AccIdentityAccountDto> identityAccounts = identityAccountService.find(accountFilter, null).getContent();
		Assert.assertEquals(1, identityAccounts.size());
		Assert.assertEquals(assignedRole.getId(), identityAccounts.get(0).getIdentityRole());
		
		// Delete log
		syncLogService.delete(log);
		syncConfigService.delete(config);
	}
	
	@Test
	public void testCreateIdentityWithDefaultContractAndRoleAsync() {
		try {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, true);
			SysSystemDto system = initData();
			Assert.assertNotNull(system);
			IdmRoleDto defaultRole = helper.createRole();
			
			SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
			// Set default role to sync configuration
			config.setDefaultRole(defaultRole.getId());
			config.setCreateDefaultContract(true);
			config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
			
			// create default mapping for provisioning
			helper.createMapping(system);
			helper.createRoleSystem(defaultRole, system);
	
			IdmIdentityFilter identityFilter = new IdmIdentityFilter();
			identityFilter.setUsername(IDENTITY_ONE);
			List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
			Assert.assertEquals(0, identities.size());
	
			helper.startSynchronization(config);
	
			// Have to be in the success state, because default role will be assigned to the default contract.
			SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1, OperationResultType.SUCCESS);
	
			Assert.assertFalse(log.isRunning());
			Assert.assertFalse(log.isContainsError());
	
			identities = identityService.find(identityFilter, null).getContent();
			Assert.assertEquals(1, identities.size());
			IdmIdentityDto identity = identities.get(0);
			List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identities.get(0).getId());
			Assert.assertEquals(1, roles.size());
			IdmIdentityRoleDto assignedRole = roles.get(0);
			Assert.assertEquals(defaultRole.getId(), assignedRole.getRole());
			
			// check only one identity account is created
			AccIdentityAccountFilter accountFilter = new AccIdentityAccountFilter();
			accountFilter.setIdentityId(identity.getId());
			List<AccIdentityAccountDto> identityAccounts = identityAccountService.find(accountFilter, null).getContent();
			Assert.assertEquals(1, identityAccounts.size());
			Assert.assertEquals(assignedRole.getId(), identityAccounts.get(0).getIdentityRole());
			
			// Delete log
			syncLogService.delete(log);
			syncConfigService.delete(config);
		} finally {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, false);
		}
	}

	@Test
	public void mappingTwoAttributesOnSchemaAttributeTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);

		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1,
				OperationResultType.SUCCESS);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		IdmIdentityDto identity = identities.get(0);
		List<IdmFormValueDto> emailValues = formService.getValues(identity, ATTRIBUTE_EMAIL_TWO);
		Assert.assertEquals(1, emailValues.size());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, emailValues.get(0).getValue());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, identity.getEmail());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void updateIdentityWithDefaultRoleTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		IdmIdentityDto identityOne = helper.createIdentity(IDENTITY_ONE);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.LINK, 1, OperationResultType.SUCCESS);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identityOne.getId());
		Assert.assertEquals(1, roles.size());
		Assert.assertEquals(defaultRole.getId(), roles.get(0).getRole());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void updateIdentityWithInvalidContractTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		IdmIdentityDto identityOne = helper.createIdentity(IDENTITY_ONE);
		IdmIdentityContractDto primeContract = contractService.getPrimeContract(identityOne.getId());
		Assert.assertNotNull(primeContract);
		primeContract.setValidTill(LocalDate.now().minusDays(10));
		primeContract = contractService.save(primeContract);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.LINK, 1, OperationResultType.WARNING);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identityOne.getId());
		Assert.assertEquals(0, roles.size());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void updateIdentityPropagateValidityTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		IdmIdentityDto identityOne = helper.createIdentity(IDENTITY_ONE);
		IdmIdentityContractDto primeContract = contractService.getPrimeContract(identityOne.getId());
		Assert.assertNotNull(primeContract);

		LocalDate validTill = LocalDate.now().plusDays(10);
		LocalDate validFrom = LocalDate.now().plusDays(-10);
		primeContract.setValidFrom(validFrom);
		primeContract.setValidTill(validTill);
		primeContract = contractService.save(primeContract);

		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.LINK, 1, OperationResultType.SUCCESS);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		List<IdmIdentityRoleDto> roles = identityRoleService.findAllByIdentity(identityOne.getId());
		Assert.assertEquals(1, roles.size());
		IdmIdentityRoleDto identityRole = roles.get(0);
		Assert.assertEquals(defaultRole.getId(), identityRole.getRole());
		Assert.assertEquals(identityRole.getValidFrom(), validFrom);
		Assert.assertEquals(identityRole.getValidTill(), validTill);

		AccIdentityAccountFilter identityAccountFilter = new AccIdentityAccountFilter();
		identityAccountFilter.setIdentityRoleId(identityRole.getId());
		Assert.assertEquals(1, identityAccountService.find(identityAccountFilter, null).getContent().size());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void deleteDefaulRoleIntegrityTest() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		Assert.assertNotNull(config.getDefaultRole());
		// Delete default role
		roleService.delete(defaultRole);
		config = (SysSyncIdentityConfigDto) syncConfigService.get(config.getId());
		Assert.assertNull(config.getDefaultRole());
	}

	@Test
	public void testSynchronizationCache() {
		SysSystemDto system = initData();
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		this.getBean().deleteAllResourceData();
		String testLastName = "test-last-name-same-" + System.currentTimeMillis();
		String testFirstName = "test-first-name";

		String userOne = "test-1-" + System.currentTimeMillis();
		this.getBean().setTestData(userOne, testFirstName, testLastName);

		String userTwo = "test-2-" + System.currentTimeMillis();
		this.getBean().setTestData(userTwo, testFirstName, testLastName);

		SysSystemMappingFilter mappingFilter = new SysSystemMappingFilter();
		mappingFilter.setEntityType(SystemEntityType.IDENTITY);
		mappingFilter.setSystemId(system.getId());
		mappingFilter.setOperationType(SystemOperationType.SYNCHRONIZATION);
		List<SysSystemMappingDto> mappings = systemMappingService.find(mappingFilter, null).getContent();
		Assert.assertEquals(1, mappings.size());
		SysSystemMappingDto defaultMapping = mappings.get(0);
		SysSystemAttributeMappingFilter attributeMappingFilter = new SysSystemAttributeMappingFilter();
		attributeMappingFilter.setSystemMappingId(defaultMapping.getId());

		List<SysSystemAttributeMappingDto> attributes = schemaAttributeMappingService.find(attributeMappingFilter, null)
				.getContent();

		SysSystemAttributeMappingDto firstNameAttribute = attributes.stream().filter(attribute -> {
			return attribute.getIdmPropertyName().equals(IdmIdentity_.firstName.getName());
		}).findFirst().orElse(null);

		Assert.assertNotNull(firstNameAttribute);
		StringBuilder scriptGenerateUuid = new StringBuilder();
		scriptGenerateUuid.append("import java.util.UUID;");
		scriptGenerateUuid.append(System.lineSeparator());
		scriptGenerateUuid.append("return UUID.randomUUID();");

		String scriptName = "generateUuid";
		IdmScriptDto scriptUuid = new IdmScriptDto();
		scriptUuid.setCategory(IdmScriptCategory.TRANSFORM_FROM);
		scriptUuid.setCode(scriptName);
		scriptUuid.setName(scriptName);
		scriptUuid.setScript(scriptGenerateUuid.toString());
		scriptUuid = scriptService.save(scriptUuid);

		IdmScriptAuthorityDto scriptAuth = new IdmScriptAuthorityDto();
		scriptAuth.setClassName("java.util.UUID");
		scriptAuth.setType(ScriptAuthorityType.CLASS_NAME);
		scriptAuth.setScript(scriptUuid.getId());
		scriptAuth = scriptAuthrotityService.save(scriptAuth);

		// we must call script
		StringBuilder transformationScript = new StringBuilder();

		transformationScript.append("return scriptEvaluator.evaluate(");
		transformationScript.append(System.lineSeparator());
		transformationScript.append("scriptEvaluator.newBuilder()");
		transformationScript.append(System.lineSeparator());
		transformationScript.append(".setScriptCode('" + scriptName + "')");
		transformationScript.append(System.lineSeparator());
		transformationScript.append(".build());");
		transformationScript.append(System.lineSeparator());

		firstNameAttribute.setTransformFromResourceScript(transformationScript.toString());
		firstNameAttribute.setCached(true);
		firstNameAttribute = schemaAttributeMappingService.save(firstNameAttribute);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 2,
				OperationResultType.WARNING);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		IdmIdentityFilter filter = new IdmIdentityFilter();
		filter.setLastName(testLastName);
		List<IdmIdentityDto> identities = identityService.find(filter, null).getContent();
		assertEquals(2, identities.size());
		//
		IdmIdentityDto identityOne = identities.get(0);
		IdmIdentityDto identityTwo = identities.get(1);
		//
		assertNotEquals(identityOne.getFirstName(), identityTwo.getFirstName());
	}

	@Test
	public void testEnableAutomaticRoleDuringSynchronization() {
		// default initialization of system and all necessary things
		SysSystemDto system = initData();
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config.setStartAutoRoleRec(true); // we want start recalculation after synchronization
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		this.getBean().deleteAllResourceData();

		String testLastName = "test-last-name-same-" + System.currentTimeMillis();
		String testFirstName = "test-first-name";

		String user1 = "test-1-" + System.currentTimeMillis();
		this.getBean().setTestData(user1, testFirstName, testLastName);

		String user2 = "test-2-" + System.currentTimeMillis();
		this.getBean().setTestData(user2, testFirstName, testLastName);

		String user3 = "test-3-" + System.currentTimeMillis();
		this.getBean().setTestData(user3, testFirstName, testLastName);

		IdmRoleDto role1 = helper.createRole();
		IdmAutomaticRoleAttributeDto automaticRole = helper.createAutomaticRole(role1.getId());
		helper.createAutomaticRoleRule(automaticRole.getId(), AutomaticRoleAttributeRuleComparison.EQUALS,
				AutomaticRoleAttributeRuleType.IDENTITY, IdmIdentity_.username.getName(), null, user1);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 3,
				OperationResultType.WARNING);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		IdmIdentityDto identity1 = identityService.getByUsername(user1);
		IdmIdentityDto identity2 = identityService.getByUsername(user2);
		IdmIdentityDto identity3 = identityService.getByUsername(user3);

		// we must change username, after create contract is also save identity (change
		// state)
		identity1.setUsername(user1 + System.currentTimeMillis());
		identity1 = identityService.save(identity1);

		helper.createIdentityContact(identity1);
		helper.createIdentityContact(identity2);
		helper.createIdentityContact(identity3);

		List<IdmIdentityRoleDto> identityRoles1 = identityRoleService.findAllByIdentity(identity1.getId());
		List<IdmIdentityRoleDto> identityRoles2 = identityRoleService.findAllByIdentity(identity2.getId());
		List<IdmIdentityRoleDto> identityRoles3 = identityRoleService.findAllByIdentity(identity3.getId());

		assertEquals(0, identityRoles1.size());
		assertEquals(0, identityRoles2.size());
		assertEquals(0, identityRoles3.size());

		// enable test processor
		testIdentityProcessor.enable();
		helper.startSynchronization(config);

		identityRoles1 = identityRoleService.findAllByIdentity(identity1.getId());
		identityRoles2 = identityRoleService.findAllByIdentity(identity2.getId());
		identityRoles3 = identityRoleService.findAllByIdentity(identity3.getId());

		assertEquals(1, identityRoles1.size());
		assertEquals(0, identityRoles2.size());
		assertEquals(0, identityRoles3.size());

		IdmIdentityRoleDto foundIdentityRole = identityRoles1.get(0);
		assertEquals(automaticRole.getId(), foundIdentityRole.getAutomaticRole());

		// synchronization immediately recalculate is disabled
		int size = testIdentityProcessor.getRolesByUsername(user1).size();
		assertEquals(0, size);
		size = testIdentityProcessor.getRolesByUsername(user2).size();
		assertEquals(0, size);
		size = testIdentityProcessor.getRolesByUsername(user3).size();
		assertEquals(0, size);
	}

	@Test
	public void testDisableAutomaticRoleDuringSynchronization() {
		// default initialization of system and all necessary things
		SysSystemDto system = initData();
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		IdmRoleDto defaultRole = helper.createRole();

		// Set default role to sync configuration
		config.setDefaultRole(defaultRole.getId());
		config.setStartAutoRoleRec(false); // we want start recalculation after synchronization
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		this.getBean().deleteAllResourceData();

		String testLastName = "test-last-name-same-" + System.currentTimeMillis();
		String testFirstName = "test-first-name";

		String user1 = "test-1-" + System.currentTimeMillis();
		this.getBean().setTestData(user1, testFirstName, testLastName);

		String user2 = "test-2-" + System.currentTimeMillis();
		this.getBean().setTestData(user2, testFirstName, testLastName);

		String user3 = "test-3-" + System.currentTimeMillis();
		this.getBean().setTestData(user3, testFirstName, testLastName);

		IdmRoleDto role1 = helper.createRole();
		IdmAutomaticRoleAttributeDto automaticRole = helper.createAutomaticRole(role1.getId());
		helper.createAutomaticRoleRule(automaticRole.getId(), AutomaticRoleAttributeRuleComparison.EQUALS,
				AutomaticRoleAttributeRuleType.IDENTITY, IdmIdentity_.username.getName(), null, user1);

		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 3,
				OperationResultType.WARNING);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		IdmIdentityDto identity1 = identityService.getByUsername(user1);
		IdmIdentityDto identity2 = identityService.getByUsername(user2);
		IdmIdentityDto identity3 = identityService.getByUsername(user3);

		// we must change username, after create contract is also save identity (change
		// state)
		identity1.setUsername(user1 + System.currentTimeMillis());
		identity1 = identityService.save(identity1);

		helper.createIdentityContact(identity1);
		helper.createIdentityContact(identity2);
		helper.createIdentityContact(identity3);

		List<IdmIdentityRoleDto> identityRoles1 = identityRoleService.findAllByIdentity(identity1.getId());
		List<IdmIdentityRoleDto> identityRoles2 = identityRoleService.findAllByIdentity(identity2.getId());
		List<IdmIdentityRoleDto> identityRoles3 = identityRoleService.findAllByIdentity(identity3.getId());

		assertEquals(0, identityRoles1.size());
		assertEquals(0, identityRoles2.size());
		assertEquals(0, identityRoles3.size());

		// enable test processor
		testIdentityProcessor.enable();
		helper.startSynchronization(config);

		identityRoles1 = identityRoleService.findAllByIdentity(identity1.getId());
		identityRoles2 = identityRoleService.findAllByIdentity(identity2.getId());
		identityRoles3 = identityRoleService.findAllByIdentity(identity3.getId());

		assertEquals(0, identityRoles1.size());
		assertEquals(0, identityRoles2.size());
		assertEquals(0, identityRoles3.size());

		// synchronization immediately recalculate is disabled
		int size = testIdentityProcessor.getRolesByUsername(user1).size();
		assertEquals(0, size);
		size = testIdentityProcessor.getRolesByUsername(user2).size();
		assertEquals(0, size);
		size = testIdentityProcessor.getRolesByUsername(user3).size();
		assertEquals(0, size);
	}

	@Test
	public void testLinkAndUpdateIdentity() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK_AND_UPDATE_ENTITY);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		this.getBean().deleteAllResourceData();

		String testLastName = "test-last-name-same-" + System.currentTimeMillis();
		String testFirstName = "test-first-name-";

		String user1 = "test-1-" + System.currentTimeMillis();
		IdmIdentityDto identity1 = helper.createIdentity(user1);
		this.getBean().setTestData(user1, testFirstName + 1, testLastName);

		String user2 = "test-2-" + System.currentTimeMillis();
		IdmIdentityDto identity2 = helper.createIdentity(user2);
		this.getBean().setTestData(user2, testFirstName + 2, testLastName);

		String user3 = "test-3-" + System.currentTimeMillis();
		IdmIdentityDto identity3 = helper.createIdentity(user3);
		this.getBean().setTestData(user3, testFirstName + 3, testLastName);

		assertNotEquals(testFirstName + 1, identity1.getFirstName());
		assertNotEquals(testFirstName + 2, identity2.getFirstName());
		assertNotEquals(testFirstName + 3, identity3.getFirstName());

		assertNotEquals(testLastName, identity1.getLastName());
		assertNotEquals(testLastName, identity2.getLastName());
		assertNotEquals(testLastName, identity3.getLastName());

		testIdentityProcessor.enable();
		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.LINK_AND_UPDATE_ENTITY, 3,
				OperationResultType.SUCCESS);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		IdmIdentityDto updatedIdentity1 = identityService.getByUsername(user1);
		IdmIdentityDto updatedIdentity2 = identityService.getByUsername(user2);
		IdmIdentityDto updatedIdentity3 = identityService.getByUsername(user3);

		assertNotEquals(updatedIdentity1.getFirstName(), identity1.getFirstName());
		assertNotEquals(updatedIdentity2.getFirstName(), identity2.getFirstName());
		assertNotEquals(updatedIdentity3.getFirstName(), identity3.getFirstName());

		assertNotEquals(updatedIdentity1.getLastName(), identity1.getLastName());
		assertNotEquals(updatedIdentity2.getLastName(), identity2.getLastName());
		assertNotEquals(updatedIdentity3.getLastName(), identity3.getLastName());

		assertNotEquals(updatedIdentity1.getModified(), identity1.getModified());
		assertNotEquals(updatedIdentity2.getModified(), identity2.getModified());
		assertNotEquals(updatedIdentity3.getModified(), identity3.getModified());

		assertEquals(testFirstName + 1, updatedIdentity1.getFirstName());
		assertEquals(testFirstName + 2, updatedIdentity2.getFirstName());
		assertEquals(testFirstName + 3, updatedIdentity3.getFirstName());

		assertEquals(testLastName, updatedIdentity1.getLastName());
		assertEquals(testLastName, updatedIdentity2.getLastName());
		assertEquals(testLastName, updatedIdentity3.getLastName());
	}

	@Test
	public void testTaskExecution() throws InterruptedException {
		getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, true);
		try {
			SysSystemDto system = initData();
			Assert.assertNotNull(system);
			SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
	
			config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
	
			IdmIdentityFilter identityFilter = new IdmIdentityFilter();
			identityFilter.setUsername(IDENTITY_ONE);
			List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
			Assert.assertEquals(0, identities.size());
	
			Task initiatorTask = createSyncTask(config.getId());
			ObserveLongRunningTaskEndProcessor.listenTask(initiatorTask.getId());
			// Execute
			manager.runTask(initiatorTask.getId());
			ObserveLongRunningTaskEndProcessor.waitForEnd(initiatorTask.getId());
	
			SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1,
					OperationResultType.SUCCESS);
	
			Assert.assertFalse(log.isRunning());
			Assert.assertFalse(log.isContainsError());
	
			identities = identityService.find(identityFilter, null).getContent();
			Assert.assertEquals(1, identities.size());
			IdmIdentityDto identity = identities.get(0);
			List<IdmFormValueDto> emailValues = formService.getValues(identity, ATTRIBUTE_EMAIL_TWO);
			Assert.assertEquals(1, emailValues.size());
			Assert.assertEquals(IDENTITY_ONE_EMAIL, emailValues.get(0).getValue());
			Assert.assertEquals(IDENTITY_ONE_EMAIL, identity.getEmail());
	
			// Delete log
			syncLogService.delete(log);
		} finally {
			getHelper().setConfigurationValue(SchedulerConfiguration.PROPERTY_TASK_ASYNCHRONOUS_ENABLED, false);
		}
	}

	@Test
	public void testDependentTaskExecution() throws InterruptedException {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);

		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		SysSystemDto systemTwo = initData();
		Assert.assertNotNull(systemTwo);
		SysSyncIdentityConfigDto configTwo = doCreateSyncConfig(systemTwo);

		configTwo = (SysSyncIdentityConfigDto) syncConfigService.save(configTwo);

		Task initiatorTask = createSyncTask(config.getId());
		Task dependentTask = createSyncTask(configTwo.getId());

		ObserveLongRunningTaskEndProcessor.listenTask(initiatorTask.getId());
		ObserveLongRunningTaskEndProcessor.listenTask(dependentTask.getId());

		DependentTaskTrigger trigger = new DependentTaskTrigger();
		trigger.setInitiatorTaskId(initiatorTask.getId());
		//
		// execute initiator
		manager.createTrigger(dependentTask.getId(), trigger);
		manager.runTask(initiatorTask.getId());

		DateTime startOne = DateTime.now();
		ObserveLongRunningTaskEndProcessor.waitForEnd(initiatorTask.getId());
		DateTime endOne = DateTime.now();
		ObserveLongRunningTaskEndProcessor.waitForEnd(dependentTask.getId());
		DateTime endTwo = DateTime.now();
		long durationOne = endOne.getMillis() - startOne.getMillis();
		long durationTwo = endTwo.getMillis() - endOne.getMillis();

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 1,
				OperationResultType.SUCCESS);

		SysSyncLogDto logTwo = checkSyncLog(configTwo, SynchronizationActionType.LINK, 1, OperationResultType.SUCCESS);

		long syncDurationOne = log.getEnded().toDateTime().getMillis() - log.getStarted().toDateTime().getMillis();
		long syncDurationTwo = logTwo.getEnded().toDateTime().getMillis()
				- logTwo.getStarted().toDateTime().getMillis();

		// We want to check if was the task ended after sync end.
		assertTrue(durationOne > syncDurationOne);
		assertTrue(durationTwo > syncDurationTwo);

		//
		assertEquals(OperationState.EXECUTED,
				ObserveLongRunningTaskEndProcessor.getResult(initiatorTask.getId()).getState());
		assertEquals(OperationState.EXECUTED,
				ObserveLongRunningTaskEndProcessor.getResult(dependentTask.getId()).getState());

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		Assert.assertFalse(logTwo.isRunning());
		Assert.assertFalse(logTwo.isContainsError());

		// Delete log
		syncLogService.delete(logTwo);
	}

	@Test(expected = ProvisioningException.class)
	public void testStopSync() {
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		// Stop sync - Sync is not running, so exception will be throw
		synchronizationService.stopSynchronization(config);
	}
	
	@Test
	public void testSyncWithWfSituationMissingEntity() {
		
		final String wfExampleKey =  "syncActionExampl";
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setLinkedActionWfKey(wfExampleKey);
		config.setMissingAccountActionWfKey(wfExampleKey);
		config.setMissingEntityActionWfKey(wfExampleKey);
		config.setUnlinkedActionWfKey(wfExampleKey);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		
		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());

		// Start sync
		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.MISSING_ENTITY, 1,
				OperationResultType.WF);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		IdmIdentityDto identity = identities.get(0);
		List<IdmFormValueDto> emailValues = formService.getValues(identity, ATTRIBUTE_EMAIL_TWO);
		Assert.assertEquals(1, emailValues.size());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, emailValues.get(0).getValue());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, identity.getEmail());

		// Delete log
		syncLogService.delete(log);
	}
	
	@Test
	public void testSyncWithWfSituationUnlinkEntity() {
		
		final String wfExampleKey =  "syncActionExampl";
		SysSystemDto system = initData();
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setLinkedActionWfKey(wfExampleKey);
		config.setMissingAccountActionWfKey(wfExampleKey);
		config.setMissingEntityActionWfKey(wfExampleKey);
		config.setUnlinkedActionWfKey(wfExampleKey);
		config.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK_AND_UPDATE_ENTITY);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		
		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());
		getHelper().createIdentity(IDENTITY_ONE);

		// Start sync
		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.UNLINKED, 1,
				OperationResultType.WF);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		IdmIdentityDto identity = identities.get(0);
		List<IdmFormValueDto> emailValues = formService.getValues(identity, ATTRIBUTE_EMAIL_TWO);
		Assert.assertEquals(1, emailValues.size());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, emailValues.get(0).getValue());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, identity.getEmail());

		// Delete log
		syncLogService.delete(log);
	}
	
	@Test
	public void testSyncWithWfSituationLinkedEntity() {
		SysSystemDto system = initData();
		
		IdmIdentityFilter identityFilter = new IdmIdentityFilter();
		identityFilter.setUsername(IDENTITY_ONE);
		List<IdmIdentityDto> identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(0, identities.size());
		
		// Create identity and account
		IdmIdentityDto identity = getHelper().createIdentity(IDENTITY_ONE);
		helper.createIdentityAccount(system, identity);
		Assert.assertNotEquals(IDENTITY_ONE_EMAIL, identity.getEmail());
		
		final String wfExampleKey =  "syncActionExampl";
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setLinkedActionWfKey(wfExampleKey);
		config.setMissingAccountActionWfKey(wfExampleKey);
		config.setMissingEntityActionWfKey(wfExampleKey);
		config.setUnlinkedActionWfKey(wfExampleKey);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		// Start sync
		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.LINKED, 1,
				OperationResultType.WF);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		identities = identityService.find(identityFilter, null).getContent();
		Assert.assertEquals(1, identities.size());
		identity = identities.get(0);
		List<IdmFormValueDto> emailValues = formService.getValues(identity, ATTRIBUTE_EMAIL_TWO);
		Assert.assertEquals(1, emailValues.size());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, emailValues.get(0).getValue());
		Assert.assertEquals(IDENTITY_ONE_EMAIL, identity.getEmail());

		// Delete log
		syncLogService.delete(log);
	}
	
	@Test
	public void testSyncWithWfSituationMissingAccount() {
		SysSystemDto system = initData();
		
		// Create identity and account (this account is not on the target system)
		// We need remove test from the name of identity ... because the WF create approving task for identity begins with 'test'.
		IdmIdentityDto identity = getHelper().createIdentity(getHelper().createName().substring(5));
		helper.createIdentityAccount(system, identity);
		// Delete all data on the target system
		this.getBean().deleteAllResourceData();
		
		final String wfExampleKey =  "syncActionExampl";
		Assert.assertNotNull(system);
		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setMissingAccountActionWfKey(wfExampleKey);
		config.setMissingAccountAction(ReconciliationMissingAccountActionType.CREATE_ACCOUNT);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);

		// Start sync
		helper.startSynchronization(config);

		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.MISSING_ACCOUNT, 1,
				OperationResultType.WF);

		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void syncIdentityWithDefaultContract() {
		// application property for creating default contract is allowed and also is allowed create 
		configurationService.setBooleanValue(IdentityConfiguration.PROPERTY_IDENTITY_CREATE_DEFAULT_CONTRACT, Boolean.TRUE);

		SysSystemDto system = initData();
		this.getBean().deleteAllResourceData();
		
		String usernameOne = getHelper().createName();
		this.getBean().setTestData(usernameOne, getHelper().createName(), getHelper().createName());
		
		String usernameTwo = getHelper().createName();
		this.getBean().setTestData(usernameTwo, getHelper().createName(), getHelper().createName());

		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setCreateDefaultContract(true);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		
		assertTrue(identityConfiguration.isCreateDefaultContractEnabled());
		
		helper.startSynchronization(config);
		
		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 2,
				OperationResultType.SUCCESS);
		
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());
		
		IdmIdentityDto identityOne = identityService.getByUsername(usernameOne);
		assertNotNull(identityOne);
		List<IdmIdentityContractDto> allByIdentity = contractService.findAllByIdentity(identityOne.getId());
		assertEquals(1, allByIdentity.size());

		IdmIdentityDto identityTwo = identityService.getByUsername(usernameTwo);
		assertNotNull(identityTwo);
		allByIdentity = contractService.findAllByIdentity(identityTwo.getId());
		assertEquals(1, allByIdentity.size());
	}

	@Test
	public void syncIdentityWithoutDefaultContract() {
		configurationService.setBooleanValue(IdentityConfiguration.PROPERTY_IDENTITY_CREATE_DEFAULT_CONTRACT, Boolean.TRUE);

		SysSystemDto system = initData();
		this.getBean().deleteAllResourceData();
		
		String usernameOne = getHelper().createName();
		this.getBean().setTestData(usernameOne, getHelper().createName(), getHelper().createName());
		
		String usernameTwo = getHelper().createName();
		this.getBean().setTestData(usernameTwo, getHelper().createName(), getHelper().createName());

		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setCreateDefaultContract(false);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		
		assertTrue(identityConfiguration.isCreateDefaultContractEnabled());
		
		helper.startSynchronization(config);
		
		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 2,
				OperationResultType.SUCCESS);
		
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());
		
		IdmIdentityDto identityOne = identityService.getByUsername(usernameOne);
		assertNotNull(identityOne);
		List<IdmIdentityContractDto> allByIdentity = contractService.findAllByIdentity(identityOne.getId());
		assertEquals(0, allByIdentity.size());

		IdmIdentityDto identityTwo = identityService.getByUsername(usernameTwo);
		assertNotNull(identityTwo);
		allByIdentity = contractService.findAllByIdentity(identityTwo.getId());
		assertEquals(0, allByIdentity.size());
	}

	@Test
	public void syncIdentityWithDefaultContractDisableByProperty() {
		configurationService.setBooleanValue(IdentityConfiguration.PROPERTY_IDENTITY_CREATE_DEFAULT_CONTRACT, Boolean.FALSE);

		SysSystemDto system = initData();
		this.getBean().deleteAllResourceData();
		
		String usernameOne = getHelper().createName();
		this.getBean().setTestData(usernameOne, getHelper().createName(), getHelper().createName());
		
		String usernameTwo = getHelper().createName();
		this.getBean().setTestData(usernameTwo, getHelper().createName(), getHelper().createName());

		SysSyncIdentityConfigDto config = doCreateSyncConfig(system);
		config.setCreateDefaultContract(true);
		config = (SysSyncIdentityConfigDto) syncConfigService.save(config);
		
		assertFalse(identityConfiguration.isCreateDefaultContractEnabled());
		
		helper.startSynchronization(config);
		
		SysSyncLogDto log = checkSyncLog(config, SynchronizationActionType.CREATE_ENTITY, 2,
				OperationResultType.SUCCESS);
		
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());
		
		IdmIdentityDto identityOne = identityService.getByUsername(usernameOne);
		assertNotNull(identityOne);
		List<IdmIdentityContractDto> allByIdentity = contractService.findAllByIdentity(identityOne.getId());
		assertEquals(0, allByIdentity.size());

		IdmIdentityDto identityTwo = identityService.getByUsername(usernameTwo);
		assertNotNull(identityTwo);
		allByIdentity = contractService.findAllByIdentity(identityTwo.getId());
		assertEquals(0, allByIdentity.size());
		
		// Set default
		configurationService.setBooleanValue(IdentityConfiguration.PROPERTY_IDENTITY_CREATE_DEFAULT_CONTRACT, Boolean.TRUE);
	}

	private Task createSyncTask(UUID syncConfId) {
		Task task = new Task();
		task.setInstanceId(configurationService.getInstanceId());
		task.setTaskType(SynchronizationSchedulableTaskExecutor.class);
		task.setDescription("test");
		task.getParameters().put(SynchronizationService.PARAMETER_SYNCHRONIZATION_ID, syncConfId.toString());
		//
		return manager.createTask(task);
	}

	private SysSyncLogDto checkSyncLog(AbstractSysSyncConfigDto config, SynchronizationActionType actionType, int count,
			OperationResultType resultType) {
		SysSyncLogFilter logFilter = new SysSyncLogFilter();
		logFilter.setSynchronizationConfigId(config.getId());
		List<SysSyncLogDto> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLogDto log = logs.get(0);
		if (actionType == null) {
			return log;
		}

		SysSyncActionLogFilter actionLogFilter = new SysSyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLogDto> actions = syncActionLogService.find(actionLogFilter, null).getContent();

		SysSyncActionLogDto actionLog = actions.stream().filter(action -> {
			return actionType == action.getSyncAction();
		}).findFirst().get();

		Assert.assertEquals(resultType, actionLog.getOperationResult());
		SysSyncItemLogFilter itemLogFilter = new SysSyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLogDto> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(count, items.size());

		return log;
	}

	public SysSyncIdentityConfigDto doCreateSyncConfig(SysSystemDto system) {

		SysSystemMappingFilter mappingFilter = new SysSystemMappingFilter();
		mappingFilter.setEntityType(SystemEntityType.IDENTITY);
		mappingFilter.setSystemId(system.getId());
		mappingFilter.setOperationType(SystemOperationType.SYNCHRONIZATION);
		List<SysSystemMappingDto> mappings = systemMappingService.find(mappingFilter, null).getContent();
		Assert.assertEquals(1, mappings.size());
		SysSystemMappingDto mapping = mappings.get(0);
		SysSystemAttributeMappingFilter attributeMappingFilter = new SysSystemAttributeMappingFilter();
		attributeMappingFilter.setSystemMappingId(mapping.getId());

		List<SysSystemAttributeMappingDto> attributes = schemaAttributeMappingService.find(attributeMappingFilter, null)
				.getContent();
		SysSystemAttributeMappingDto uidAttribute = attributes.stream().filter(attribute -> {
			return attribute.isUid();
		}).findFirst().orElse(null);

		// Create default synchronization config
		SysSyncIdentityConfigDto syncConfigCustom = new SysSyncIdentityConfigDto();
		syncConfigCustom.setReconciliation(true);
		syncConfigCustom.setCustomFilter(false);
		syncConfigCustom.setSystemMapping(mapping.getId());
		syncConfigCustom.setCorrelationAttribute(uidAttribute.getId());
		syncConfigCustom.setName(SYNC_CONFIG_NAME);
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.CREATE_ENTITY);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);

		syncConfigCustom = (SysSyncIdentityConfigDto) syncConfigService.save(syncConfigCustom);

		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setSystemId(system.getId());
		Assert.assertEquals(1, syncConfigService.find(configFilter, null).getTotalElements());
		return syncConfigCustom;
	}

	private SysSystemDto initData() {

		// create test system
		SysSystemDto system = helper.createSystem(TestResource.TABLE_NAME);
		Assert.assertNotNull(system);

		// generate schema for system
		List<SysSchemaObjectClassDto> objectClasses = systemService.generateSchema(system);

		// Create synchronization mapping
		SysSystemMappingDto syncSystemMapping = new SysSystemMappingDto();
		syncSystemMapping.setName("default_" + System.currentTimeMillis());
		syncSystemMapping.setEntityType(SystemEntityType.IDENTITY);
		syncSystemMapping.setOperationType(SystemOperationType.SYNCHRONIZATION);
		syncSystemMapping.setObjectClass(objectClasses.get(0).getId());
		final SysSystemMappingDto syncMapping = systemMappingService.save(syncSystemMapping);
		createMapping(system, syncMapping);
		this.getBean().initIdentityData();
		return system;

	}

	/**
	 * Method set and persist data to test resource
	 */
	@Transactional
	public void setTestData(String name, String firstName, String lastName) {
		TestResource resourceUser = new TestResource();
		resourceUser.setName(name);
		resourceUser.setFirstname(firstName);
		resourceUser.setLastname(lastName);
		entityManager.persist(resourceUser);
	}

	@Transactional
	public void initIdentityData() {
		deleteAllResourceData();

		TestResource resourceUserOne = new TestResource();
		resourceUserOne.setName(IDENTITY_ONE);
		resourceUserOne.setFirstname(IDENTITY_ONE);
		resourceUserOne.setLastname(IDENTITY_ONE);
		resourceUserOne.setEavAttribute("1");
		resourceUserOne.setEmail(IDENTITY_ONE_EMAIL);
		entityManager.persist(resourceUserOne);

	}

	private void createMapping(SysSystemDto system, final SysSystemMappingDto entityHandlingResult) {
		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());

		Page<SysSchemaAttributeDto> schemaAttributesPage = schemaAttributeService.find(schemaAttributeFilter, null);
		schemaAttributesPage.forEach(schemaAttr -> {
			if (ATTRIBUTE_NAME.equals(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setUid(true);
				attributeMapping.setEntityAttribute(true);
				attributeMapping.setIdmPropertyName("username");
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeMapping);

			} else if ("firstname".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName("firstName");
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeMapping);

			} else if ("lastname".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName("lastName");
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeMapping);

			} else if (ATTRIBUTE_EMAIL.equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeMapping = new SysSystemAttributeMappingDto();
				attributeMapping.setIdmPropertyName(ATTRIBUTE_EMAIL);
				attributeMapping.setName(schemaAttr.getName());
				attributeMapping.setSchemaAttribute(schemaAttr.getId());
				attributeMapping.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeMapping);
				SysSystemAttributeMappingDto attributeMappingTwo = new SysSystemAttributeMappingDto();
				attributeMappingTwo.setIdmPropertyName(ATTRIBUTE_EMAIL_TWO);
				attributeMappingTwo.setEntityAttribute(false);
				attributeMappingTwo.setExtendedAttribute(true);
				attributeMappingTwo.setName(ATTRIBUTE_EMAIL_TWO);
				attributeMappingTwo.setSchemaAttribute(schemaAttr.getId());
				attributeMappingTwo.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeMappingTwo);

			}
		});
	}

	@Transactional
	public void deleteAllResourceData() {
		// Delete all
		Query q = entityManager.createNativeQuery("DELETE FROM " + TestResource.TABLE_NAME);
		q.executeUpdate();
	}

	private IdentitySyncTest getBean() {
		return applicationContext.getBean(this.getClass());
	}
}
