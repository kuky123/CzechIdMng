package eu.bcvsolutions.idm.vs.service.impl;

import com.google.common.collect.ImmutableList;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.AbstractSysSyncConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncConfigFilter;
import eu.bcvsolutions.idm.acc.scheduler.task.impl.ImportFromCSVToSystemExecutor;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;
import eu.bcvsolutions.idm.vs.TestHelper;
import eu.bcvsolutions.idm.vs.config.domain.VsConfiguration;
import eu.bcvsolutions.idm.vs.domain.VsRequestState;
import eu.bcvsolutions.idm.vs.dto.VsRequestDto;
import eu.bcvsolutions.idm.vs.dto.VsSystemDto;
import eu.bcvsolutions.idm.vs.dto.filter.VsRequestFilter;
import eu.bcvsolutions.idm.vs.service.api.VsRequestService;
import eu.bcvsolutions.idm.vs.service.api.VsSystemImplementerService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual system test
 *
 * @author Svanda
 */
@Transactional
public class VsSystemServiceTest extends AbstractIntegrationTest {

    private static String VS_SYSTEM = "VS_SYSTEM_ONE";
    private static final String FILE_PATH = System.getProperty("user.dir") + "/src/test/resources/service/impl/vsTestImport.csv";

    @Autowired
    private TestHelper helper;
    @Autowired
    private VsRequestService requestService;
    @Autowired
    private VsSystemImplementerService systemImplementersService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private SysSystemMappingService systemMappingService;
    @Autowired
    private SysSystemAttributeMappingService systemAttributeMappingService;
    @Autowired
    private SysSyncConfigService configService;
    @Autowired
    private LongRunningTaskManager longRunningTaskManager;
    @Autowired
    private SysSystemService systemService;

    @Before
    public void init() {
        loginAsAdmin();
    }

    @After
    public void logout() {
        super.logout();
    }

    @Test
    public void createVirtualSystemTest() {
        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        SysSystemDto system = helper.createVirtualSystem(config);
        Assert.assertNotNull(system);
        Assert.assertEquals(system.getName(), VS_SYSTEM);
        Assert.assertTrue(system.isVirtual());
    }

    @Test
    public void checkImplementersTest() {
        String userOneName = "vsUserOne";
        String roleOneName = "vsRoleOne";
        IdmIdentityDto userOne = helper.createIdentity(userOneName);
        IdmRoleDto roleOne = helper.createRole(roleOneName);

        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        config.setImplementers(ImmutableList.of(userOne.getId()));
        config.setImplementerRoles(ImmutableList.of(roleOne.getId()));
        SysSystemDto system = helper.createVirtualSystem(config);
        Assert.assertNotNull(system);

        List<IdmIdentityDto> implementes = systemImplementersService.findRequestImplementers(system.getId());
        Assert.assertEquals(1, implementes.size());
        Assert.assertEquals(userOneName, implementes.get(0).getUsername());
    }

    @Test
    /**
     * If none implementers role is set, then we use as implementers all users
     * with 'superAdminRole'
     */
    public void checkDefaultImplementersTest() {
        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        SysSystemDto system = helper.createVirtualSystem(config);
        Assert.assertNotNull(system);

        List<IdmIdentityDto> implementes = systemImplementersService.findRequestImplementers(system.getId());
        Assert.assertEquals(1, implementes.size());
        Assert.assertEquals("admin", implementes.get(0).getUsername());
    }

    @Test
    /**
     * If none implementers role is set, then we use as implementers all users
     * with 'superAdminRole'
     */
    public void checkSpecificImplementerRoleTest() {

        String userOneName = "vsUserOne";
        String roleOneName = "vsRoleOne";
        IdmIdentityDto userTwo = helper.createIdentity(userOneName);
        IdmRoleDto roleOne = helper.createRole(roleOneName);
        helper.assignRoles(helper.getPrimeContract(userTwo.getId()), false, roleOne);

        this.configurationService.setValue(VsConfiguration.PROPERTY_DEFAULT_ROLE, roleOneName);
        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        SysSystemDto system = helper.createVirtualSystem(config);
        Assert.assertNotNull(system);

        List<IdmIdentityDto> implementes = systemImplementersService.findRequestImplementers(system.getId());
        Assert.assertEquals(1, implementes.size());
        Assert.assertEquals(userOneName, implementes.get(0).getUsername());
    }

    @Test
    public void checkCreatedMappingAndSync() {
        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        SysSystemDto system = helper.createVirtualSystem(config);
        //
        Assert.assertNotNull(system);
        //
        Assert.assertEquals(system.getName(), VS_SYSTEM);
        Assert.assertTrue(system.isVirtual());
        //
        List<SysSystemMappingDto> mappings = systemMappingService.findBySystemId(system.getId(), SystemOperationType.SYNCHRONIZATION, SystemEntityType.IDENTITY);
        Assert.assertEquals("Wrong size of found mappings!", 1, mappings.size());
        SysSystemMappingDto mapping = mappings.get(0);
        Assert.assertNotNull("Mapping is null!", mapping);
        //
        List<SysSystemAttributeMappingDto> attributeMappings = systemAttributeMappingService.findBySystemMapping(mapping);
        Assert.assertEquals("Wrong size of found attributes for mapping!", 1, attributeMappings.size());
        SysSystemAttributeMappingDto attributeMapping = attributeMappings.get(0);
        Assert.assertNotNull("Mapping is null!", attributeMapping);
        Assert.assertEquals("Wrong IdmPropertyName - username!", "username", attributeMapping.getIdmPropertyName());
        Assert.assertEquals("Wrong Name of attribute mapping - username!", "username", attributeMapping.getName());
        //
        SysSyncConfigFilter filter = new SysSyncConfigFilter();
        filter.setSystemId(system.getId());
        filter.setName("Link virtual accounts to identities");
        List<AbstractSysSyncConfigDto> syncConfigs = configService.find(filter, null).getContent();
        Assert.assertEquals("Wrong size of synchronizations!", 1, syncConfigs.size());
        Assert.assertNotNull("Sync config is null!", syncConfigs.get(0));
    }

    @Test
    public void importDataVirtualTest() {
        VsSystemDto config = new VsSystemDto();
        config.setName(VS_SYSTEM);
        SysSystemDto system = helper.createVirtualSystem(config);
        //
        Assert.assertNotNull(system);
        //
        Assert.assertEquals(system.getName(), VS_SYSTEM);
        Assert.assertTrue(system.isVirtual());
        //
        ImportFromCSVToSystemExecutor lrt = new ImportFromCSVToSystemExecutor();
        Map<String, Object> configOfLRT = new HashMap<>();
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_ATTRIBUTE_SEPARATOR, ";");
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_CSV_FILE_PATH, FILE_PATH);
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_MULTIVALUED_SEPARATOR, ",");
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_NAME_ATTRIBUTE, "username");
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_UID_ATTRIBUTE, "username");
        configOfLRT.put(ImportFromCSVToSystemExecutor.PARAM_SYSTEM_NAME, system.getName());
        lrt.init(configOfLRT);
        //
        Boolean obj = longRunningTaskManager.executeSync(lrt);
        Assert.assertNotNull(obj);
        Assert.assertTrue(obj);
        IdmLongRunningTaskDto task = longRunningTaskManager.getLongRunningTask(lrt);
        Long count = task.getCount();
        Long total = 3L;
        Assert.assertEquals(task.getCounter(), count);
        Assert.assertEquals(total, count);
        VsRequestFilter filter = new VsRequestFilter();
        filter.setSystemId(system.getId());
        filter.setState(VsRequestState.REALIZED);
        List<VsRequestDto> vsRequestDtos = requestService.find(filter, null).getContent();
        Assert.assertEquals("Realized request should be here!", 3, vsRequestDtos.size());
        filter.setState(VsRequestState.IN_PROGRESS);
        vsRequestDtos = requestService.find(filter, null).getContent();
        Assert.assertEquals("No request should be here!", 0, vsRequestDtos.size());
        //
        //delete system
        systemService.delete(system);
    }

}
