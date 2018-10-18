package eu.bcvsolutions.idm;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.api.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmContractGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeTypeService;
import eu.bcvsolutions.idm.core.eav.api.domain.BaseFaceType;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmContractGuarantee;
import eu.bcvsolutions.idm.core.model.entity.IdmContractPosition;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.entity.IdmProfile;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleCatalogue;
import eu.bcvsolutions.idm.core.model.entity.IdmRoleRequest;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeType;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdentityBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;
import eu.bcvsolutions.idm.core.security.evaluator.BasePermissionEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.ContractGuaranteeByIdentityContractEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.ContractPositionByIdentityContractEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.IdentityContractByIdentityEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.IdentityRoleByIdentityEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.identity.SelfIdentityEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.profile.SelfProfileEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.role.RoleCanBeRequestedEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.role.RoleRequestByIdentityEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.role.RoleRequestByWfInvolvedIdentityEvaluator;
import eu.bcvsolutions.idm.core.security.evaluator.role.SelfRoleRequestEvaluator;

/**
 * Initialize demo data for application
 * 
 * @author Radek Tomiška 
 *
 */
@Component
@DependsOn("initApplicationData")
public class InitDemoData implements ApplicationListener<ContextRefreshedEvent> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InitDemoData.class);
	public static final String PARAMETER_DEMO_DATA_ENABLED = "idm.sec.core.demo.data.enabled";
	public static final String PARAMETER_DEMO_DATA_CREATED = "idm.sec.core.demo.data.created";
	public static final String FORM_ATTRIBUTE_PHONE = "phone";
	public static final String FORM_ATTRIBUTE_WWW = "webPages";
	public static final String FORM_ATTRIBUTE_UUID = "uuid";
	public static final String FORM_ATTRIBUTE_PASSWORD = "password";
	public static final String FORM_ATTRIBUTE_DATETIME = "datetime";
	public static final String DEFAULT_ROLE_NAME = "userRole";
	
	@Autowired private InitApplicationData initApplicationData;	
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmRoleService roleService;
	@Autowired private IdmIdentityRoleService identityRoleService;
	@Autowired private IdmTreeNodeService treeNodeService;	
	@Autowired private IdmTreeTypeService treeTypeService;
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdmContractGuaranteeService contractGuaranteeService;
	@Autowired private SecurityService securityService;	
	@Autowired private ConfigurationService configurationService;	
	@Autowired private FormService formService;	
	@Autowired private IdmPasswordPolicyService passwordPolicyService;
	@Autowired private IdmAuthorizationPolicyService authorizationPolicyService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// init only, when demo data is enabled
		if (configurationService.getBooleanValue(PARAMETER_DEMO_DATA_ENABLED, false)) {
			init();
		}
	}
	
	protected void init() {
		// we need to be ensured admin and and admin role exists.
		initApplicationData.init();
		//
		securityService.setSystemAuthentication();
		//
		try {
			IdmIdentityDto identityAdmin = this.identityService.getByUsername(InitApplicationData.ADMIN_USERNAME);
			//
			Page<IdmTreeNodeDto> rootsList = treeNodeService.findRoots((UUID) null, new PageRequest(0, 1));
			IdmTreeNodeDto rootOrganization = null;
			if (!rootsList.getContent().isEmpty()) {
				rootOrganization = rootsList.getContent().get(0);
			} else {
				IdmTreeNodeDto organizationRoot = new IdmTreeNodeDto();
				organizationRoot.setCode("root");
				organizationRoot.setName("Organization ROOT");
				organizationRoot.setTreeType(treeTypeService.getByCode(InitApplicationData.DEFAULT_TREE_TYPE).getId());
				organizationRoot = this.treeNodeService.save(organizationRoot);
			}
			//
			if (!configurationService.getBooleanValue(PARAMETER_DEMO_DATA_CREATED, false)) {
				LOG.info("Creating demo data ...");		
				//
				// create default password policy for validate
				IdmPasswordPolicyDto passValidate = null;
				try {
					passValidate = this.passwordPolicyService.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE);
				} catch (ResultCodeException e) {
					// nothing, password policy for validate not exist
				}
				// default password policy not exist, try to found by name
				if (passValidate == null) {
					passValidate = this.passwordPolicyService.findOneByName("DEFAULT_VALIDATE_POLICY");
				}
				// if password policy still not exist create default password policy
				if (passValidate == null) {
					passValidate = new IdmPasswordPolicyDto();
					passValidate.setName("DEFAULT_VALIDATE_POLICY");
					passValidate.setDefaultPolicy(true);
					passValidate.setType(IdmPasswordPolicyType.VALIDATE);
					passwordPolicyService.save(passValidate);
				}
				//
				// create default password policy for generate
				IdmPasswordPolicyDto passGenerate = null;
				try {
					passGenerate = this.passwordPolicyService.getDefaultPasswordPolicy(IdmPasswordPolicyType.GENERATE);
				} catch (ResultCodeException e) {
					// nothing, password policy for generate password not exist
				}
				// try to found password policy by name
				if (passGenerate == null) {
					passGenerate = this.passwordPolicyService.findOneByName("DEFAULT_GENERATE_POLICY");
				}
				// if still not exist create default generate password policy
				if (passGenerate == null) {
					passGenerate = new IdmPasswordPolicyDto();
					passGenerate.setName("DEFAULT_GENERATE_POLICY");
					passGenerate.setDefaultPolicy(true);
					passGenerate.setType(IdmPasswordPolicyType.GENERATE);
					passGenerate.setMinLowerChar(2);
					passGenerate.setMinNumber(2);
					passGenerate.setMinSpecialChar(2);
					passGenerate.setMinUpperChar(2);
					passGenerate.setMinPasswordLength(8);
					passGenerate.setMaxPasswordLength(12);
					passwordPolicyService.save(passGenerate);
				}
				//
				// role may exists from another module initialization
				IdmRoleDto role1 = this.roleService.getByCode(DEFAULT_ROLE_NAME);
				if (role1 == null) {
					role1 = new IdmRoleDto();
					role1.setCode(DEFAULT_ROLE_NAME);
					role1 = this.roleService.save(role1);
				}
				// self policy
				IdmAuthorizationPolicyDto selfPolicy = new IdmAuthorizationPolicyDto();
				selfPolicy.setPermissions(
						IdmBasePermission.AUTOCOMPLETE, 
						IdmBasePermission.READ, 
						IdentityBasePermission.PASSWORDCHANGE, 
						IdentityBasePermission.CHANGEPERMISSION);
				selfPolicy.setRole(role1.getId());
				selfPolicy.setGroupPermission(CoreGroupPermission.IDENTITY.getName());
				selfPolicy.setAuthorizableType(IdmIdentity.class.getCanonicalName());
				selfPolicy.setEvaluator(SelfIdentityEvaluator.class);
				authorizationPolicyService.save(selfPolicy);
				// read identity roles by identity
				IdmAuthorizationPolicyDto identityRolePolicy = new IdmAuthorizationPolicyDto();
				identityRolePolicy.setRole(role1.getId());
				identityRolePolicy.setGroupPermission(CoreGroupPermission.IDENTITYROLE.getName());
				identityRolePolicy.setAuthorizableType(IdmIdentityRole.class.getCanonicalName());
				identityRolePolicy.setEvaluator(IdentityRoleByIdentityEvaluator.class);
				authorizationPolicyService.save(identityRolePolicy);				
				// read identity contracts by identity
				IdmAuthorizationPolicyDto identityContractPolicy = new IdmAuthorizationPolicyDto();
				identityContractPolicy.setRole(role1.getId());
				identityContractPolicy.setGroupPermission(CoreGroupPermission.IDENTITYCONTRACT.getName());
				identityContractPolicy.setAuthorizableType(IdmIdentityContract.class.getCanonicalName());
				identityContractPolicy.setEvaluator(IdentityContractByIdentityEvaluator.class);
				authorizationPolicyService.save(identityContractPolicy);
				// read contract positions by contract
				IdmAuthorizationPolicyDto contractPositionPolicy = new IdmAuthorizationPolicyDto();
				contractPositionPolicy.setRole(role1.getId());
				contractPositionPolicy.setGroupPermission(CoreGroupPermission.CONTRACTPOSITION.getName());
				contractPositionPolicy.setAuthorizableType(IdmContractPosition.class.getCanonicalName());
				contractPositionPolicy.setEvaluator(ContractPositionByIdentityContractEvaluator.class);
				authorizationPolicyService.save(contractPositionPolicy);
				// read contract guarantees by identity contract
				IdmAuthorizationPolicyDto contractGuaranteePolicy = new IdmAuthorizationPolicyDto();
				contractGuaranteePolicy.setRole(role1.getId());
				contractGuaranteePolicy.setGroupPermission(CoreGroupPermission.CONTRACTGUARANTEE.getName());
				contractGuaranteePolicy.setAuthorizableType(IdmContractGuarantee.class.getCanonicalName());
				contractGuaranteePolicy.setEvaluator(ContractGuaranteeByIdentityContractEvaluator.class);
				authorizationPolicyService.save(contractGuaranteePolicy);
				// only autocomplete roles that can be requested
				IdmAuthorizationPolicyDto applyForPolicy = new IdmAuthorizationPolicyDto();
				applyForPolicy.setPermissions(IdmBasePermission.AUTOCOMPLETE);
				applyForPolicy.setRole(role1.getId());
				applyForPolicy.setGroupPermission(CoreGroupPermission.ROLE.getName());
				applyForPolicy.setAuthorizableType(IdmRole.class.getCanonicalName());
				applyForPolicy.setEvaluator(RoleCanBeRequestedEvaluator.class);
				authorizationPolicyService.save(applyForPolicy);
				// role requests by identity
				IdmAuthorizationPolicyDto roleRequestByIdentityPolicy = new IdmAuthorizationPolicyDto();
				roleRequestByIdentityPolicy.setRole(role1.getId());
				roleRequestByIdentityPolicy.setGroupPermission(CoreGroupPermission.ROLEREQUEST.getName());
				roleRequestByIdentityPolicy.setAuthorizableType(IdmRoleRequest.class.getCanonicalName());
				roleRequestByIdentityPolicy.setEvaluator(RoleRequestByIdentityEvaluator.class);
				authorizationPolicyService.save(roleRequestByIdentityPolicy);
				// self role requests
				IdmAuthorizationPolicyDto selfRoleRequestPolicy = new IdmAuthorizationPolicyDto();
				selfRoleRequestPolicy.setPermissions(IdmBasePermission.READ, IdmBasePermission.UPDATE, IdmBasePermission.CREATE, IdmBasePermission.DELETE);
				selfRoleRequestPolicy.setRole(role1.getId());
				selfRoleRequestPolicy.setGroupPermission(CoreGroupPermission.ROLEREQUEST.getName());
				selfRoleRequestPolicy.setAuthorizableType(IdmRoleRequest.class.getCanonicalName());
				selfRoleRequestPolicy.setEvaluator(SelfRoleRequestEvaluator.class);
				authorizationPolicyService.save(selfRoleRequestPolicy);
				// role rerquests in approval
				IdmAuthorizationPolicyDto roleRequestByWfPolicy = new IdmAuthorizationPolicyDto();
				roleRequestByWfPolicy.setPermissions(IdmBasePermission.READ, IdmBasePermission.UPDATE);
				roleRequestByWfPolicy.setRole(role1.getId());
				roleRequestByWfPolicy.setGroupPermission(CoreGroupPermission.ROLEREQUEST.getName());
				roleRequestByWfPolicy.setAuthorizableType(IdmRoleRequest.class.getCanonicalName());
				roleRequestByWfPolicy.setEvaluator(RoleRequestByWfInvolvedIdentityEvaluator.class);
				authorizationPolicyService.save(roleRequestByWfPolicy);
				// tree node - autocomplete
				IdmAuthorizationPolicyDto treeNodePolicy = new IdmAuthorizationPolicyDto();
				treeNodePolicy.setPermissions(IdmBasePermission.AUTOCOMPLETE);
				treeNodePolicy.setRole(role1.getId());
				treeNodePolicy.setGroupPermission(CoreGroupPermission.TREENODE.getName());
				treeNodePolicy.setAuthorizableType(IdmTreeNode.class.getCanonicalName());
				treeNodePolicy.setEvaluator(BasePermissionEvaluator.class);
				authorizationPolicyService.save(treeNodePolicy);
				// tree type - autocomplete all
				IdmAuthorizationPolicyDto treeTypePolicy = new IdmAuthorizationPolicyDto();
				treeTypePolicy.setPermissions(IdmBasePermission.AUTOCOMPLETE);
				treeTypePolicy.setRole(role1.getId());
				treeTypePolicy.setGroupPermission(CoreGroupPermission.TREETYPE.getName());
				treeTypePolicy.setAuthorizableType(IdmTreeType.class.getCanonicalName());
				treeTypePolicy.setEvaluator(BasePermissionEvaluator.class);
				authorizationPolicyService.save(treeTypePolicy);
				// workflow task read and execute
				IdmAuthorizationPolicyDto workflowTaskPolicy = new IdmAuthorizationPolicyDto();
				workflowTaskPolicy.setPermissions(IdmBasePermission.READ, IdmBasePermission.EXECUTE);
				workflowTaskPolicy.setRole(role1.getId());
				workflowTaskPolicy.setGroupPermission(CoreGroupPermission.WORKFLOWTASK.getName());
				workflowTaskPolicy.setEvaluator(BasePermissionEvaluator.class);
				authorizationPolicyService.save(workflowTaskPolicy);
				// role catalogue - autocomplete
				IdmAuthorizationPolicyDto cataloguePolicy = new IdmAuthorizationPolicyDto();
				cataloguePolicy.setPermissions(IdmBasePermission.AUTOCOMPLETE);
				cataloguePolicy.setRole(role1.getId());
				cataloguePolicy.setGroupPermission(CoreGroupPermission.ROLECATALOGUE.getName());
				cataloguePolicy.setAuthorizableType(IdmRoleCatalogue.class.getCanonicalName());
				cataloguePolicy.setEvaluator(BasePermissionEvaluator.class);
				authorizationPolicyService.save(cataloguePolicy);
				// autocomplete profile pictures
				IdmAuthorizationPolicyDto selfProfilePolicy = new IdmAuthorizationPolicyDto();
				selfProfilePolicy.setPermissions(IdmBasePermission.AUTOCOMPLETE);
				selfProfilePolicy.setRole(role1.getId());
				selfProfilePolicy.setGroupPermission(CoreGroupPermission.PROFILE.getName());
				selfProfilePolicy.setAuthorizableType(IdmProfile.class.getCanonicalName());
				selfProfilePolicy.setEvaluator(SelfProfileEvaluator.class);
				authorizationPolicyService.save(selfProfilePolicy);
				//
				LOG.info(MessageFormat.format("Role created [id: {0}]", role1.getId()));
				//
				IdmRoleDto role2 = new IdmRoleDto();
				role2.setCode("customRole");
				// TODO: subroles are disabled for now
				//List<IdmRoleComposition> subRoles = new ArrayList<>();
				//subRoles.add(new IdmRoleComposition(role2, superAdminRole));
				//role2.setSubRoles(subRoles);
				role2 = this.roleService.save(role2);
				LOG.info(MessageFormat.format("Role created [id: {0}]", role2.getId()));
				//
				IdmRoleDto roleManager = new IdmRoleDto();
				roleManager.setCode("manager");
				roleManager = this.roleService.save(roleManager);
				LOG.info(MessageFormat.format("Role created [id: {0}]", roleManager.getId()));
				//
				//
				IdmIdentityDto identity = new IdmIdentityDto();
				identity.setUsername("tomiska");
				identity.setPassword(new GuardedString("heslo"));
				identity.setFirstName("Radek");
				identity.setLastName("Tomiška");
				identity.setEmail("radek.tomiska@bcvsolutions.eu");
				identity = this.identityService.save(identity);
				LOG.info(MessageFormat.format("Identity created [id: {0}]", identity.getId()));
				//
				// create prime contract
				IdmIdentityContractDto identityContract = identityContractService.getPrimeContract(identity.getId());
				if (identityContract == null) {
					identityContract = identityContractService.prepareMainContract(identity.getId());
					identityContract = identityContractService.save(identityContract);
				}
				//
				IdmIdentityRoleDto identityRole1 = new IdmIdentityRoleDto();
				identityRole1.setIdentityContract(identityContract.getId());
				identityRole1.setRole(role1.getId());
				identityRole1 = identityRoleService.save(identityRole1);
				//
				IdmIdentityRoleDto identityRole2 = new IdmIdentityRoleDto();
				identityRole2.setIdentityContract(identityContract.getId());
				identityRole2.setRole(role2.getId());
				identityRole2 = identityRoleService.save(identityRole2);
				//
				IdmIdentityDto identity2 = new IdmIdentityDto();
				identity2.setUsername("svanda");
				identity2.setFirstName("Vít");
				identity2.setPassword(new GuardedString("heslo"));
				identity2.setLastName("Švanda");
				identity2.setEmail("vit.svanda@bcvsolutions.eu");
				identity2 = this.identityService.save(identity2);
				LOG.info(MessageFormat.format("Identity created [id: {0}]", identity2.getId()));
				//
				IdmIdentityDto identity3 = new IdmIdentityDto();
				identity3.setUsername("kopr");
				identity3.setFirstName("Ondrej");
				identity3.setPassword(new GuardedString("heslo"));
				identity3.setLastName("Kopr");
				identity3.setEmail("ondrej.kopr@bcvsolutions.eu");
				identity3 = this.identityService.save(identity3);
				LOG.info(MessageFormat.format("Identity created [id: {0}]", identity3.getId()));
				//
				// get tree type for organization
				IdmTreeTypeDto treeType = treeTypeService.getByCode(InitApplicationData.DEFAULT_TREE_TYPE);
				//
				IdmTreeNodeDto organization1 = new IdmTreeNodeDto();
				organization1.setCode("one");
				organization1.setName("Organization One");
				organization1.setParent(rootOrganization.getId());
				organization1.setTreeType(treeType.getId());
				organization1 = this.treeNodeService.save(organization1);
				//
				IdmTreeNodeDto organization2 = new IdmTreeNodeDto();
				organization2.setCode("two");
				organization2.setName("Organization Two");
				organization2.setCreator("ja");
				organization2.setParent(rootOrganization.getId());
				organization2.setTreeType(treeType.getId());
				organization2 = this.treeNodeService.save(organization2);
				//
				IdmIdentityContractDto identityWorkPosition = new IdmIdentityContractDto();
				identityWorkPosition.setIdentity(identityAdmin.getId());
				identityWorkPosition.setWorkPosition(organization2.getId());
				identityWorkPosition = identityContractService.save(identityWorkPosition);
				IdmContractGuaranteeDto contractGuarantee = new IdmContractGuaranteeDto();
				contractGuarantee.setIdentityContract(identityWorkPosition.getId());
				contractGuarantee.setGuarantee(identity2.getId());
				contractGuaranteeService.save(contractGuarantee);
				//
				LOG.info("Demo data was created.");
				//				
				configurationService.setBooleanValue(PARAMETER_DEMO_DATA_CREATED, true);
				//
				// demo eav identity form
				
				IdmFormAttributeDto letter = new IdmFormAttributeDto();
				letter.setCode("letter");
				letter.setName("Favorite letter");
				letter.setPlaceholder("Character");
				letter.setDescription("Some favorite character");
				letter.setPersistentType(PersistentType.CHAR);
				letter.setRequired(true);
				letter = formService.saveAttribute(IdmIdentity.class, letter);
				
				IdmFormAttributeDto phone = new IdmFormAttributeDto();
				phone.setCode(FORM_ATTRIBUTE_PHONE);
				phone.setName("Phone");
				phone.setDescription("Additional identitiy's phone");
				phone.setPersistentType(PersistentType.TEXT);
				phone = formService.saveAttribute(IdmIdentity.class, phone);
				
				IdmFormAttributeDto description = new IdmFormAttributeDto();
				description.setCode("description");
				description.setName("Description");
				description.setDescription("Some longer optional text (2000 characters)");
				description.setPersistentType(PersistentType.TEXT);
				description.setFaceType(BaseFaceType.TEXTAREA);
				description = formService.saveAttribute(IdmIdentity.class, description);
				
				IdmFormAttributeDto rich = new IdmFormAttributeDto();
				rich.setCode("rich");
				rich.setName("RichText");
				rich.setDescription("Some rich text (2000 characters)");
				rich.setPersistentType(PersistentType.TEXT);
				description.setFaceType(BaseFaceType.RICHTEXTAREA);
				rich = formService.saveAttribute(IdmIdentity.class, rich);
				
				IdmFormAttributeDto sure = new IdmFormAttributeDto();
				sure.setCode("sure");
				sure.setName("Registration");
				sure.setPersistentType(PersistentType.BOOLEAN);
				sure = formService.saveAttribute(IdmIdentity.class, sure);
				
				IdmFormAttributeDto intNumber = new IdmFormAttributeDto();
				intNumber.setCode("intNumber");
				intNumber.setName("Int number");
				intNumber.setPersistentType(PersistentType.INT);
				intNumber = formService.saveAttribute(IdmIdentity.class, intNumber);
				
				IdmFormAttributeDto longNumber = new IdmFormAttributeDto();
				longNumber.setCode("longNumber");
				longNumber.setName("Long number");
				longNumber.setPersistentType(PersistentType.LONG);
				longNumber = formService.saveAttribute(IdmIdentity.class, longNumber);
				
				IdmFormAttributeDto doubleNumber = new IdmFormAttributeDto();
				doubleNumber.setCode("doubleNumber");
				doubleNumber.setName("Double number");
				doubleNumber.setPersistentType(PersistentType.DOUBLE);
				doubleNumber = formService.saveAttribute(IdmIdentity.class, doubleNumber);
				
				IdmFormAttributeDto currency = new IdmFormAttributeDto();
				currency.setCode("currency");
				currency.setName("Price");
				currency.setPersistentType(PersistentType.DOUBLE);
				currency.setFaceType(BaseFaceType.CURRENCY);			
				currency = formService.saveAttribute(IdmIdentity.class, currency);
				
				IdmFormAttributeDto date = new IdmFormAttributeDto();
				date.setCode("date");
				date.setName("Date");
				date.setPersistentType(PersistentType.DATE);
				date.setRequired(true);
				date.setDescription("Important date");
				date = formService.saveAttribute(IdmIdentity.class, date);
				
				IdmFormAttributeDto datetime = new IdmFormAttributeDto();
				datetime.setCode(FORM_ATTRIBUTE_DATETIME);
				datetime.setName("Date and time");
				datetime.setPersistentType(PersistentType.DATETIME);
				datetime = formService.saveAttribute(IdmIdentity.class, datetime);
				
				IdmFormAttributeDto uuid = new IdmFormAttributeDto();
				uuid.setCode(FORM_ATTRIBUTE_UUID);
				uuid.setName("UUID");
				uuid.setDescription("Some uuid value");
				uuid.setPersistentType(PersistentType.UUID);
				uuid = formService.saveAttribute(IdmIdentity.class, uuid);
				
				IdmFormAttributeDto webPages = new IdmFormAttributeDto();
				webPages.setCode(FORM_ATTRIBUTE_WWW);
				webPages.setName("WWW");
				webPages.setDescription("Favorite web pages (every line in new value)");
				webPages.setPersistentType(PersistentType.TEXT);
				webPages.setMultiple(true);
				webPages = formService.saveAttribute(IdmIdentity.class, webPages);
				
				IdmFormAttributeDto password = new IdmFormAttributeDto();
				password.setCode(FORM_ATTRIBUTE_PASSWORD);
				password.setName("Custom password");
				password.setPersistentType(PersistentType.TEXT);
				password.setConfidential(true);
				password.setDescription("Test password");
				password = formService.saveAttribute(IdmIdentity.class, password);
				
				IdmFormAttributeDto byteArray = new IdmFormAttributeDto();
				byteArray.setCode("byteArray");
				byteArray.setName("Byte array");
				byteArray.setPersistentType(PersistentType.BYTEARRAY);
				byteArray.setConfidential(false);
				byteArray.setDescription("Test byte array");
				byteArray.setPlaceholder("or image :-)");
				byteArray = formService.saveAttribute(IdmIdentity.class, byteArray);				
				
				List<IdmFormValueDto> values = new ArrayList<>();				
				IdmFormValueDto phoneValue = new IdmFormValueDto();
				phoneValue.setFormAttribute(phone.getId());
				phoneValue.setStringValue("12345679");
				values.add(phoneValue);
				
				formService.saveValues(identity.getId(), IdmIdentity.class, null, values);
				
				//
				// demo eav role form
				IdmFormAttributeDto roleExt = new IdmFormAttributeDto();
				roleExt.setCode("extAttr");
				roleExt.setName("Ext.attr");
				roleExt.setPersistentType(PersistentType.TEXT);
				roleExt.setConfidential(false);
				roleExt.setDescription("Role's custom extended attribute");
				
				roleExt = formService.saveAttribute(IdmRole.class, roleExt);
				
				//
				// demo eav tree node form
				IdmFormAttributeDto treeNodeExt = new IdmFormAttributeDto();
				treeNodeExt.setCode("extAttr");
				treeNodeExt.setName("Ext.attr");
				treeNodeExt.setPersistentType(PersistentType.TEXT);
				treeNodeExt.setConfidential(false);
				treeNodeExt.setDescription("Tree node's custom extended attribute");
				
				treeNodeExt = formService.saveAttribute(IdmTreeNode.class, treeNodeExt);
				
				//
				// demo eav identity contract's form
				IdmFormAttributeDto identityContractExt = new IdmFormAttributeDto();
				identityContractExt.setCode("extAttr");
				identityContractExt.setName("Ext.attr");
				identityContractExt.setPersistentType(PersistentType.TEXT);
				identityContractExt.setConfidential(false);
				identityContractExt.setDescription("Identity contract's custom extended attribute");
				
				identityContractExt = formService.saveAttribute(IdmIdentityContract.class, identityContractExt);
			}
		} catch(Exception ex) {
			LOG.warn("Demo data was not created", ex);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

}
