package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

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

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.domain.ReconciliationMissingAccountActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationLinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationMissingEntityActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationUnlinkedActionType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.AbstractSysSyncConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncActionLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncActionLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncItemLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncLogFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSyncConfig_;
import eu.bcvsolutions.idm.acc.entity.TestTreeResource;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncActionLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncItemLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmTreeNodeFilter;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeTypeService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.exception.TreeNodeException;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Tree synchronization tests
 * 
 * @author Svanda
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Rollback(false)
@Service
public class DefaultTreeSynchronizationServiceTest extends AbstractIntegrationTest {

	private static final String SYNC_CONFIG_NAME = "syncConfigName";
	private static final String SYSTEM_NAME = "systemTreeName";
	private static final String TREE_TYPE_TEST = "TREE_TEST";
	private static final String NODE_NAME = "name";
	private static final String ATTRIBUTE_NAME = "__NAME__";
	private static final String CHANGED = "changed";

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
	private IdmTreeTypeService treeTypeService;
	@Autowired
	private IdmTreeNodeService treeNodeService;
	@Autowired
	private FormService formService;
	@Autowired
	private SysSchemaObjectClassService schemaObjectClassService;

	private SysSystemDto system;

	@Before
	public void init() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	@Transactional
	public void doCreateSyncConfig() {
		initData();

		SysSystemMappingFilter mappingFilter = new SysSystemMappingFilter();
		mappingFilter.setEntityType(SystemEntityType.TREE);
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
		}).findFirst().get();

		// Create default synchronization config
		AbstractSysSyncConfigDto syncConfigCustom = new SysSyncConfigDto();
		syncConfigCustom.setReconciliation(true);
		syncConfigCustom.setCustomFilter(true);
		syncConfigCustom.setSystemMapping(mapping.getId());
		syncConfigCustom.setCorrelationAttribute(uidAttribute.getId());
		syncConfigCustom.setReconciliation(true);
		syncConfigCustom.setName(SYNC_CONFIG_NAME);
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.CREATE_ENTITY);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);

		syncConfigService.save(syncConfigCustom);

		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setSystemId(system.getId());
		Assert.assertEquals(1, syncConfigService.find(configFilter, null).getTotalElements());
	}

	@Test
	public void doStartSyncA_MissingEntity() {
		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<AbstractSysSyncConfigDto> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		AbstractSysSyncConfigDto syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));
		//
		helper.startSynchronization(syncConfigCustom);
	
		//
		SysSyncLogFilter logFilter = new SysSyncLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLogDto> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLogDto log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SysSyncActionLogFilter actionLogFilter = new SysSyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLogDto> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLogDto createEntityActionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.CREATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SysSyncItemLogFilter itemLogFilter = new SysSyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(createEntityActionLog.getId());
		List<SysSyncItemLogDto> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(6, items.size());

		IdmTreeTypeDto treeType = treeTypeService.find(null).getContent().stream().filter(tree -> {
			return tree.getName().equals(TREE_TYPE_TEST);
		}).findFirst().get();

		Assert.assertEquals(1, treeNodeService.findRoots(treeType.getId(), null).getContent().size());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	public void doStartSyncB_Linked_doEntityUpdate() {
		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<AbstractSysSyncConfigDto> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		// Change node code to changed
		this.getBean().changeOne();

		Assert.assertEquals(1, syncConfigs.size());
		AbstractSysSyncConfigDto syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.UPDATE_ENTITY);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		IdmTreeNodeFilter nodeFilter = new IdmTreeNodeFilter();
		nodeFilter.setProperty(NODE_NAME);
		nodeFilter.setValue("111");
		nodeFilter.setTreeTypeId(DtoUtils
				.getEmbedded(syncConfigCustom, SysSyncConfig_.systemMapping, SysSystemMappingDto.class).getTreeType());
		IdmTreeNodeDto treeNode = treeNodeService.find(nodeFilter, null).getContent().get(0);
		Assert.assertEquals("111", treeNode.getCode());

		helper.startSynchronization(syncConfigCustom);
	
		//
		SysSyncLogFilter logFilter = new SysSyncLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLogDto> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLogDto log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SysSyncActionLogFilter actionLogFilter = new SysSyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLogDto> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(1, actions.size());

		SysSyncActionLogDto actionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.UPDATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SysSyncItemLogFilter itemLogFilter = new SysSyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(actionLog.getId());
		List<SysSyncItemLogDto> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(6, items.size());

		// Check state after sync
		treeNode = treeNodeService.get(treeNode.getId());
		Assert.assertEquals(CHANGED, treeNode.getCode());

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void doStartSyncB_MissingAccount_DeleteEntity() {
		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<AbstractSysSyncConfigDto> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		// Remove node code to changed
		this.getBean().removeOne();

		Assert.assertEquals(1, syncConfigs.size());
		AbstractSysSyncConfigDto syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));

		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.DELETE_ENTITY);
		syncConfigService.save(syncConfigCustom);

		// Check state before sync
		IdmTreeNodeFilter nodeFilter = new IdmTreeNodeFilter();
		nodeFilter.setProperty(NODE_NAME);
		nodeFilter.setValue("111");
		nodeFilter.setTreeTypeId(DtoUtils
				.getEmbedded(syncConfigCustom, SysSyncConfig_.systemMapping, SysSystemMappingDto.class).getTreeType());
		IdmTreeNodeDto treeNode = treeNodeService.find(nodeFilter, null).getContent().get(0);
		Assert.assertNotNull(treeNode.getCode());

		helper.startSynchronization(syncConfigCustom);
	
		//
		SysSyncLogFilter logFilter = new SysSyncLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLogDto> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLogDto log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		// Check state after sync
		treeNode = treeNodeService.get(treeNode.getId());
		Assert.assertNull(treeNode);

		// Delete log
		syncLogService.delete(log);

	}

	@Test
	public void doStartSyncC_MissingEntity() {
		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<AbstractSysSyncConfigDto> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		AbstractSysSyncConfigDto syncConfigCustom = syncConfigs.get(0);
		Assert.assertFalse(syncConfigService.isRunning(syncConfigCustom));
		syncConfigCustom.setRootsFilterScript(
				"if(account){ def parentValue = account.getAttributeByName(\"PARENT\").getValue();"
						+ " def uidValue = account.getAttributeByName(\"__NAME__\").getValue();"
						+ " if(parentValue != null && parentValue.equals(uidValue)){"
						+ "	 account.getAttributeByName(\"PARENT\").setValues(null); return Boolean.TRUE;}}"
						+ " \nreturn Boolean.FALSE;");
		// Set sync config
		syncConfigCustom.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
		syncConfigCustom.setUnlinkedAction(SynchronizationUnlinkedActionType.IGNORE);
		syncConfigCustom.setMissingEntityAction(SynchronizationMissingEntityActionType.CREATE_ENTITY);
		syncConfigCustom.setMissingAccountAction(ReconciliationMissingAccountActionType.IGNORE);
		syncConfigService.save(syncConfigCustom);
		//
		helper.startSynchronization(syncConfigCustom);
	
		//
		SysSyncLogFilter logFilter = new SysSyncLogFilter();
		logFilter.setSynchronizationConfigId(syncConfigCustom.getId());
		List<SysSyncLogDto> logs = syncLogService.find(logFilter, null).getContent();
		Assert.assertEquals(1, logs.size());
		SysSyncLogDto log = logs.get(0);
		Assert.assertFalse(log.isRunning());
		Assert.assertFalse(log.isContainsError());

		SysSyncActionLogFilter actionLogFilter = new SysSyncActionLogFilter();
		actionLogFilter.setSynchronizationLogId(log.getId());
		List<SysSyncActionLogDto> actions = syncActionLogService.find(actionLogFilter, null).getContent();
		Assert.assertEquals(2, actions.size());

		SysSyncActionLogDto createEntityActionLog = actions.stream().filter(action -> {
			return SynchronizationActionType.CREATE_ENTITY == action.getSyncAction();
		}).findFirst().get();

		SysSyncItemLogFilter itemLogFilter = new SysSyncItemLogFilter();
		itemLogFilter.setSyncActionLogId(createEntityActionLog.getId());
		List<SysSyncItemLogDto> items = syncItemLogService.find(itemLogFilter, null).getContent();
		Assert.assertEquals(6, items.size());

		IdmTreeTypeDto treeType = treeTypeService.find(null).getContent().stream().filter(tree -> {
			return tree.getName().equals(TREE_TYPE_TEST);
		}).findFirst().get();

		Assert.assertEquals(2, treeNodeService.findRoots(treeType.getId(), null).getContent().size());

		// Delete log
		syncLogService.delete(log);
	}

	@Test
	@Transactional
	public void provisioningA_CreateAccount_withOutMapping() {

		// Delete all resource data
		this.deleteAllResourceData();

		IdmTreeTypeDto treeType = treeTypeService.find(null).getContent().stream().filter(tree -> {
			return tree.getName().equals(TREE_TYPE_TEST);
		}).findFirst().get();

		// Create root node in IDM tree
		IdmTreeNodeDto nodeRoot = new IdmTreeNodeDto();
		nodeRoot.setCode("P1");
		nodeRoot.setName(nodeRoot.getCode());
		nodeRoot.setParent(null);
		nodeRoot.setTreeType(treeType.getId());
		nodeRoot = treeNodeService.save(nodeRoot);

		// Create node in IDM tree
		IdmTreeNodeDto nodeOne = new IdmTreeNodeDto();
		nodeOne.setCode("P12");
		nodeOne.setName(nodeOne.getCode());
		nodeOne.setParent(nodeRoot.getId());
		nodeOne.setTreeType(treeType.getId());
		nodeOne = treeNodeService.save(nodeOne);

		// Check state before provisioning
		TestTreeResource one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNull(one);
	}

	@Test(expected = ProvisioningException.class) // Provisioning tree in incorrect order
	public void provisioningB_CreateAccounts_withException() {

		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setProperty(NODE_NAME);
		filter.setValue("P1");

		IdmTreeNodeDto nodeRoot = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeRoot);

		filter.setValue("P12");
		IdmTreeNodeDto nodeOne = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeOne);

		// Check state before provisioning
		TestTreeResource one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNull(one);

		// Create mapping for provisioning
		this.createProvisionigMapping();

		// Save IDM node (must invoke provisioning)
		// We didn't provisioning for root first ... expect throw exception
		treeNodeService.save(nodeOne);
	}

	@Test
	public void provisioningC_CreateAccounts_correct() {

		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setProperty(NODE_NAME);
		filter.setValue("P1");

		IdmTreeNodeDto nodeRoot = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeRoot);

		filter.setValue("P12");
		IdmTreeNodeDto nodeOne = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeOne);

		// Check state before provisioning
		TestTreeResource one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNull(one);
		TestTreeResource root = entityManager.find(TestTreeResource.class, "P1");
		Assert.assertNull(root);

		// Save IDM node again (must invoke provisioning)
		// Root first
		treeNodeService.save(nodeRoot);
		// Node next
		treeNodeService.save(nodeOne);

		// Check state before provisioning
		root = entityManager.find(TestTreeResource.class, "P1");
		Assert.assertNotNull(root);
		one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNotNull(one);
	}

	@Test
	public void provisioningD_UpdateAccount() {

		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setProperty(NODE_NAME);
		filter.setValue("P1");

		IdmTreeNodeDto nodeRoot = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeRoot);

		filter.setValue("P12");
		IdmTreeNodeDto nodeOne = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeOne);

		// Check state before provisioning
		TestTreeResource one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNotNull(one);
		Assert.assertEquals("P12", one.getCode());

		nodeOne.setCode(CHANGED);

		// Save IDM changed node (must invoke provisioning)
		treeNodeService.save(nodeOne);

		// Check state before provisioning
		one = entityManager.find(TestTreeResource.class, "P12");
		Assert.assertNotNull(one);
		Assert.assertEquals(CHANGED, one.getCode());
	}

	@Test(expected = TreeNodeException.class)
	public void provisioningE_DeleteAccount_IntegrityException() {

		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setProperty(NODE_NAME);
		filter.setValue("P1");

		IdmTreeNodeDto nodeRoot = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeRoot);

		// Delete IDM node (must invoke provisioning) .. We delete node with some
		// children ... must throw integrity exception
		// Generally we counts with provisioning on every node ... include children
		// (Recursively delete is not good idea!)
		treeNodeService.delete(nodeRoot);
	}

	@Test
	public void provisioningF_DeleteAccount() {

		IdmTreeNodeFilter filter = new IdmTreeNodeFilter();
		filter.setProperty(NODE_NAME);
		filter.setValue("P12");
		IdmTreeNodeDto nodeOne = treeNodeService.find(filter, null).getContent().get(0);
		Assert.assertNotNull(nodeOne);

		// Delete IDM node (must invoke provisioning) .. We delete child
		treeNodeService.delete(nodeOne);

		Assert.assertTrue(treeNodeService.find(filter, null).getContent().isEmpty());
	}

	@Transactional
	public void deleteAllResourceData() {
		// Delete all
		Query q = entityManager.createNativeQuery("DELETE FROM test_tree_resource");
		q.executeUpdate();
	}

	private void createProvisionigMapping() {

		SysSyncConfigFilter configFilter = new SysSyncConfigFilter();
		configFilter.setName(SYNC_CONFIG_NAME);
		List<AbstractSysSyncConfigDto> syncConfigs = syncConfigService.find(configFilter, null).getContent();

		Assert.assertEquals(1, syncConfigs.size());
		AbstractSysSyncConfigDto syncConfigCustom = syncConfigs.get(0);

		SysSystemMappingDto systemMappingSync = systemMappingService.get(syncConfigCustom.getSystemMapping());

		// Create provisioning mapping
		SysSystemMappingDto systemMapping = new SysSystemMappingDto();
		systemMapping.setName("default_" + System.currentTimeMillis());
		systemMapping.setEntityType(SystemEntityType.TREE);
		systemMapping.setTreeType(systemMappingSync.getTreeType());
		systemMapping.setOperationType(SystemOperationType.PROVISIONING);
		systemMapping.setObjectClass(systemMappingSync.getObjectClass());
		final SysSystemMappingDto syncMapping = systemMappingService.save(systemMapping);
		SysSystemDto system = systemService
				.get(schemaObjectClassService.get(systemMapping.getObjectClass()).getSystem());
		createMapping(system, syncMapping);

	}

	private void initData() {

		// create test system
		system = helper.createSystem("test_tree_resource");
		system.setName(SYSTEM_NAME);
		system = systemService.save(system);
		// key to EAV
		IdmFormDefinitionDto formDefinition = systemService.getConnectorFormDefinition(system.getConnectorInstance());
		formService.saveValues(system, formDefinition, "keyColumn", ImmutableList.of("ID"));

		// generate schema for system
		List<SysSchemaObjectClassDto> objectClasses = systemService.generateSchema(system);

		IdmTreeTypeDto treeType = new IdmTreeTypeDto();
		treeType.setCode(TREE_TYPE_TEST);
		treeType.setName(TREE_TYPE_TEST);
		treeType = treeTypeService.save(treeType);

		// Create synchronization mapping
		SysSystemMappingDto syncSystemMapping = new SysSystemMappingDto();
		syncSystemMapping.setName("default_" + System.currentTimeMillis());
		syncSystemMapping.setEntityType(SystemEntityType.TREE);
		syncSystemMapping.setTreeType(treeType.getId());
		syncSystemMapping.setOperationType(SystemOperationType.SYNCHRONIZATION);
		syncSystemMapping.setObjectClass(objectClasses.get(0).getId());
		final SysSystemMappingDto syncMapping = systemMappingService.save(syncSystemMapping);

		createMapping(system, syncMapping);
		initTreeData();

		syncConfigService.find(null).getContent().forEach(config -> {
			syncConfigService.delete(config);
		});

	}

	private void initTreeData() {
		deleteAllResourceData();

		entityManager.persist(this.createNode("1", null));
		entityManager.persist(this.createNode("2", "2"));

		entityManager.persist(this.createNode("11", "1"));
		entityManager.persist(this.createNode("12", "1"));
		entityManager.persist(this.createNode("111", "11"));
		entityManager.persist(this.createNode("112", "11"));
		entityManager.persist(this.createNode("1111", "111"));

		entityManager.persist(this.createNode("21", "2"));
		entityManager.persist(this.createNode("22", "2"));
		entityManager.persist(this.createNode("211", "21"));
		entityManager.persist(this.createNode("212", "21"));
		entityManager.persist(this.createNode("2111", "211"));
	}

	private TestTreeResource createNode(String code, String parent) {
		TestTreeResource node = new TestTreeResource();
		node.setCode(code);
		node.setName(code);
		node.setParent(parent);
		node.setId(code);
		return node;
	}

	@Transactional
	public void changeOne() {
		TestTreeResource one = entityManager.find(TestTreeResource.class, "111");
		one.setCode(CHANGED);
		entityManager.persist(one);
	}

	@Transactional
	public void removeOne() {
		TestTreeResource one = entityManager.find(TestTreeResource.class, "111");
		entityManager.remove(one);
	}

	private void createMapping(SysSystemDto system, final SysSystemMappingDto entityHandlingResult) {
		SysSchemaAttributeFilter schemaAttributeFilter = new SysSchemaAttributeFilter();
		schemaAttributeFilter.setSystemId(system.getId());

		Page<SysSchemaAttributeDto> schemaAttributesPage = schemaAttributeService.find(schemaAttributeFilter, null);
		schemaAttributesPage.forEach(schemaAttr -> {
			if (ATTRIBUTE_NAME.equals(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeHandlingName = new SysSystemAttributeMappingDto();
				attributeHandlingName.setUid(true);
				attributeHandlingName.setEntityAttribute(false);
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSchemaAttribute(schemaAttr.getId());
				// For provisioning .. we need create UID
				attributeHandlingName.setTransformToResourceScript("if(uid){return uid;}\nreturn entity.getCode();");
				attributeHandlingName.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("CODE".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeHandlingName = new SysSystemAttributeMappingDto();
				attributeHandlingName.setIdmPropertyName("code");
				attributeHandlingName.setEntityAttribute(true);
				attributeHandlingName.setSchemaAttribute(schemaAttr.getId());
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("PARENT".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeHandlingName = new SysSystemAttributeMappingDto();
				attributeHandlingName.setIdmPropertyName("parent");
				attributeHandlingName.setEntityAttribute(true);
				attributeHandlingName.setSchemaAttribute(schemaAttr.getId());
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeHandlingName);

			} else if ("NAME".equalsIgnoreCase(schemaAttr.getName())) {
				SysSystemAttributeMappingDto attributeHandlingName = new SysSystemAttributeMappingDto();
				attributeHandlingName.setIdmPropertyName("name");
				attributeHandlingName.setName(schemaAttr.getName());
				attributeHandlingName.setEntityAttribute(true);
				attributeHandlingName.setSchemaAttribute(schemaAttr.getId());
				attributeHandlingName.setSystemMapping(entityHandlingResult.getId());
				schemaAttributeMappingService.save(attributeHandlingName);

			}
		});
	}

	private DefaultTreeSynchronizationServiceTest getBean() {
		return applicationContext.getBean(this.getClass());
	}
}
