package eu.bcvsolutions.idm;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.config.domain.TreeConfiguration;
import eu.bcvsolutions.idm.core.api.domain.RoleType;
import eu.bcvsolutions.idm.core.api.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmGenerateValueService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmScriptService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeTypeService;
import eu.bcvsolutions.idm.core.config.flyway.CoreFlywayConfig;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.generator.identity.IdentityFormDefaultValueGenerator;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmGroupPermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.evaluator.BasePermissionEvaluator;

/**
 * Initialize required application data:
 * * admin user admin/admin
 * * superAdminRole with system admin authority
 * 
 * TODO: split initializatons - in order - eav first, then users, LRT etc.
 * 
 * @author Radek Tomiška 
 *
 */
@DependsOn(CoreFlywayConfig.NAME)
@Component(InitApplicationData.NAME)
public class InitApplicationData implements ApplicationListener<ContextRefreshedEvent> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InitApplicationData.class);
	public static final String NAME = "initApplicationData";
	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "admin";
	public static final String ADMIN_ROLE = "superAdminRole";
	public static final String DEFAULT_TREE_TYPE = "ORGANIZATIONS";
	//
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdmRoleService roleService;
	@Autowired private IdmIdentityRoleService identityRoleService;	
	@Autowired private IdmTreeNodeService treeNodeService;	
	@Autowired private IdmTreeTypeService treeTypeService;
	@Autowired private SecurityService securityService;	
	@Autowired private IdmNotificationConfigurationService notificationConfigurationService;	
	@Autowired private IdmNotificationTemplateService notificationTemplateService;	
	@Autowired private LongRunningTaskManager longRunningTaskManager;	
	@Autowired private FormService formService;	
	@Autowired private IdmAuthorizationPolicyService authorizationPolicyService;
	@Autowired private IdmScriptService scriptService;
	@Autowired private TreeConfiguration treeConfiguration;
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private IdmGenerateValueService generateValueService;
	//
	private static final UUID DEFAULT_FORM_GENERATE_VALUE_ID = UUID.fromString("61ae4b97-421d-4075-8911-8003989f30df"); // static system generate value uuid

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		init();
	}

	protected void init() {
		securityService.setSystemAuthentication();
		//
		try {
			//
			// prepare default form definitions
			if (formService.getDefinition(IdmIdentity.class) == null) {
				formService.createDefinition(IdmIdentity.class, new ArrayList<>());
			}
			if (formService.getDefinition(IdmRole.class) == null) {
				formService.createDefinition(IdmRole.class, new ArrayList<>());
			}
			if (formService.getDefinition(IdmTreeNode.class) == null) {
				formService.createDefinition(IdmTreeNode.class, new ArrayList<>());
			}
			if (formService.getDefinition(IdmIdentityContract.class) == null) {
				formService.createDefinition(IdmIdentityContract.class, new ArrayList<>());
			}
			//
			// register default value generators
			if (generateValueService.get(DEFAULT_FORM_GENERATE_VALUE_ID) == null) {
				IdmGenerateValueDto generateValue = new IdmGenerateValueDto(DEFAULT_FORM_GENERATE_VALUE_ID);
				generateValue.setDtoType(IdmIdentityDto.class.getCanonicalName());
				generateValue.setGeneratorType(IdentityFormDefaultValueGenerator.class.getCanonicalName());
				generateValue.setSeq((short) 100);
				generateValue.setUnmodifiable(true);
				generateValueService.save(generateValue);
			}
			//
			// create super admin role
			IdmRoleDto existsSuperAdminRole = this.roleService.getByCode(ADMIN_ROLE);
			if (existsSuperAdminRole == null && this.roleService.find(new PageRequest(0, 1)).getTotalElements() == 0) {
				//
				final IdmRoleDto superAdminRole = new IdmRoleDto();
				superAdminRole.setCode(ADMIN_ROLE);
				superAdminRole.setRoleType(RoleType.SYSTEM);
				existsSuperAdminRole = this.roleService.save(superAdminRole);
				// super admin authorization policy
				IdmAuthorizationPolicyDto policy = new IdmAuthorizationPolicyDto();
				policy.setGroupPermission(IdmGroupPermission.APP.getName());
				policy.setPermissions(IdmBasePermission.ADMIN);
				policy.setRole(existsSuperAdminRole.getId());
				policy.setEvaluator(BasePermissionEvaluator.class);
				authorizationPolicyService.save(policy);
				//
				LOG.info(MessageFormat.format("Super admin Role created [id: {0}]", existsSuperAdminRole.getId()));
			}
			//
			// create super admin
			IdmIdentityDto existsSuperAdmin = this.identityService.getByUsername(ADMIN_USERNAME);
			if (existsSuperAdmin == null || this.identityService.find(new PageRequest(0, 1)).getTotalElements() == 0) {
				//
				IdmIdentityDto identityAdmin = new IdmIdentityDto();
				identityAdmin.setUsername(ADMIN_USERNAME);
				identityAdmin.setPassword(new GuardedString(ADMIN_PASSWORD));
				identityAdmin.setLastName("Administrator");
				identityAdmin = this.identityService.save(identityAdmin);
				LOG.info(MessageFormat.format("Super admin identity created [id: {0}]", identityAdmin.getId()));
				//
				// create prime contract
				IdmIdentityContractDto contract = identityContractService.getPrimeContract(identityAdmin.getId());
				if (contract == null) {
					contract = identityContractService.prepareMainContract(identityAdmin.getId());
					contract = identityContractService.save(contract);
				}
				//
				// assign super admin role
				IdmIdentityRoleDto identityRole = new IdmIdentityRoleDto();
				identityRole.setIdentityContract(contract.getId());
				identityRole.setRole(existsSuperAdminRole.getId());
				identityRoleService.save(identityRole);
			}
			//
			// create Node type for organization			
			IdmTreeTypeDto treeType = treeTypeService.getByCode(DEFAULT_TREE_TYPE);
			if (treeType == null && this.treeTypeService.find(new PageRequest(0, 1)).getTotalElements() == 0) {
				treeType = new IdmTreeTypeDto();
				treeType.setCode(DEFAULT_TREE_TYPE);
				treeType.setName("Organization structure");
				treeType = this.treeTypeService.save(treeType);
				treeConfiguration.setDefaultType(treeType.getId());
				//
				// create organization root
				if (treeNodeService.findRoots(treeType.getId(), new PageRequest(0, 1)).getTotalElements() == 0) {
					IdmTreeNodeDto organizationRoot = new IdmTreeNodeDto();
					organizationRoot.setCode("root");
					organizationRoot.setName("Root organization");
					organizationRoot.setTreeType(treeType.getId());
					organizationRoot = this.treeNodeService.save(organizationRoot);
				}
			}
			//
			// initial missing scripts, current scripts isn't redploy
			scriptService.init();
			// save only missing templates, current templates is not redeploys
			notificationTemplateService.init();
			//
			// init notification configuration, initialization topic need exists system templates!
			notificationConfigurationService.initDefaultTopics();
			//
			// Cancels all previously ran tasks
			longRunningTaskManager.init();
			//
			// Cancels all previously ran events
			entityEventManager.init();
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
