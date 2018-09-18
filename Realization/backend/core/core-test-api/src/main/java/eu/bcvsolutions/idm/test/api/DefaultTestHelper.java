package eu.bcvsolutions.idm.test.api;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleAttributeRuleComparison;
import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleAttributeRuleType;
import eu.bcvsolutions.idm.core.api.domain.ConceptRoleRequestOperation;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationMap;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestedByType;
import eu.bcvsolutions.idm.core.api.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeRuleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmConceptRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractPositionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractSliceDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractSliceGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmProfileDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCompositionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.repository.filter.FilterBuilder;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeRuleService;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeService;
import eu.bcvsolutions.idm.core.api.service.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmContractGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmContractPositionService;
import eu.bcvsolutions.idm.core.api.service.IdmContractSliceGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmContractSliceService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmProfileService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCompositionService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeTypeService;
import eu.bcvsolutions.idm.core.api.service.ModuleService;
import eu.bcvsolutions.idm.core.api.service.ReadDtoService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmScheduledTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmScheduledTaskService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.GroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.dto.LoginDto;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizationEvaluator;
import eu.bcvsolutions.idm.core.security.api.service.LoginService;

/**
 * Creates common test entities
 *
 * @author Radek Tomiška
 *
 */
@Component("testHelper")
public class DefaultTestHelper implements TestHelper {

	@Autowired private ApplicationContext context;
	@Autowired private ConfigurationService configurationService;
	@Autowired private IdmTreeNodeService treeNodeService;
	@Autowired private IdmTreeTypeService treeTypeService;
	@Autowired private IdmRoleService roleService;
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdmContractGuaranteeService contractGuaranteeService;
	@Autowired private IdmContractPositionService contractPositionService;
	@Autowired private IdmRoleTreeNodeService roleTreeNodeService;
	@Autowired private IdmAuthorizationPolicyService authorizationPolicyService;
	@Autowired private IdmIdentityRoleService identityRoleService;
	@Autowired private IdmRoleCatalogueService roleCatalogueService;
	@Autowired private IdmRoleCatalogueRoleService roleCatalogueRoleService;
	@Autowired private IdmRoleRequestService roleRequestService;
	@Autowired private IdmConceptRoleRequestService conceptRoleRequestService;
	@Autowired private IdmScheduledTaskService scheduledTaskService;
	@Autowired private IdmAutomaticRoleAttributeService automaticRoleAttributeService;
	@Autowired private IdmAutomaticRoleAttributeRuleService automaticRoleAttributeRuleService;
	@Autowired private FormService formService;
	@Autowired private IdmFormDefinitionService formDefinitionService;
	@Autowired private IdmFormAttributeService formAttributeService;
	@Autowired private LoginService loginService;
	@Autowired private IdmContractSliceService contractSliceService;
	@Autowired private IdmContractSliceGuaranteeService contractSliceGuaranteeService;
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private IdmRoleGuaranteeService roleGuaranteeService;
	@Autowired private IdmRoleGuaranteeRoleService roleGuaranteeRoleService;
	@Autowired private IdmProfileService profileService;
	@Autowired private IdmRoleCompositionService roleCompositionService;
	@Autowired private ModuleService moduleService;
	
	@Override
	public LoginDto loginAdmin() {
		return loginService.login(new LoginDto(TestHelper.ADMIN_USERNAME, new GuardedString(TestHelper.ADMIN_PASSWORD)));
	}
	
	@Override
	public LoginDto login(String username, String password) {
		return login(username, new GuardedString(password));
	}
	
	@Override
	public LoginDto login(String username, GuardedString password) {
		return loginService.login(new LoginDto(username, password));
	}
	
	@Override
	public void logout() {
		loginService.logout();
	}
	
	@Override
	public <T extends ReadDtoService<?, ?>> T getService(Class<T> dtoServiceType) {
		return context.getBean(dtoServiceType);
	}

	/**
	 * Creates random unique name
	 *
	 * @return
	 */
	@Override
	public String createName() {
		return "test" + "-" + UUID.randomUUID();
	}

	@Override
	public IdmIdentityDto createIdentity() {
		return createIdentity(null, new GuardedString(DEFAULT_PASSWORD));
	}

	@Override
	public IdmIdentityDto createIdentity(String username) {
		return createIdentity(username, new GuardedString(DEFAULT_PASSWORD));
	}
	
	@Override
	public IdmIdentityDto createIdentity(GuardedString password) {
		return createIdentity(null, password);
	}
	
	@Override
	public IdmIdentityDto createIdentity(String name, GuardedString password) {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername(name == null ? createName() : name);
		identity.setFirstName("Test");
		identity.setLastName("Identity");
		identity.setPassword(password);
		identity = identityService.save(identity);
		// password is transient, some test except password back in identity
		identity.setPassword(password);
		//
		return identity;
	}

	@Override
	public void deleteIdentity(UUID id) {
		identityService.deleteById(id);
	}

	@Override
	public IdmRoleCatalogueDto createRoleCatalogue(){
		return createRoleCatalogue(null);
	}

	@Override
	public IdmRoleCatalogueDto createRoleCatalogue(String code){
		IdmRoleCatalogueDto roleCatalogue = new IdmRoleCatalogueDto();
		code = code == null ? createName() : code;
		roleCatalogue.setName(code);
		roleCatalogue.setCode(code);
		return roleCatalogueService.save(roleCatalogue);
	}

	@Override
	public IdmTreeTypeDto createTreeType() {
		return createTreeType(null);
	}

	@Override
	public IdmTreeTypeDto createTreeType(String name) {
		IdmTreeTypeDto treeType = new IdmTreeTypeDto();
		name = name == null ? createName() : name;
		treeType.setCode(name);
		treeType.setName(name);
		return treeTypeService.save(treeType);
	}
	
	@Override
	public IdmTreeTypeDto getDefaultTreeType() {
		return treeTypeService.getDefaultTreeType();
	}

	@Override
	public IdmTreeNodeDto createTreeNode() {
		return createTreeNode((String) null, null);
	}

	@Override
	public IdmTreeNodeDto createTreeNode(String name, IdmTreeNodeDto parent) {
		return createTreeNode(getDefaultTreeType(), name, parent);
	}

	@Override
	public IdmTreeNodeDto createTreeNode(IdmTreeTypeDto treeType, IdmTreeNodeDto parent) {
		return createTreeNode(treeType, null, parent);
	}

	@Override
	public IdmTreeNodeDto createTreeNode(IdmTreeTypeDto treeType, String name, IdmTreeNodeDto parent) {
		Assert.notNull(treeType, "Tree type is required - test environment is wrong configured, test data is not prepared!");
		//
		name = name == null ? createName() : name;
		IdmTreeNodeDto node = new IdmTreeNodeDto();
		node.setParent(parent == null ? null : parent.getId());
		node.setCode(name);
		node.setName(name);
		node.setTreeType(treeType.getId());
		//
		return treeNodeService.save(node);
	}

	@Override
	public void deleteTreeNode(UUID id) {
		treeNodeService.deleteById(id);
	}

	@Override
	public IdmRoleDto createRole() {
		return createRole(null);
	}

	@Override
	public IdmRoleDto createRole(String code) {
		return createRole(null, code);
	}

	@Override
	public IdmRoleDto createRole(UUID id, String code) {
		IdmRoleDto role = new IdmRoleDto();
		if (id != null) {
			role.setId(id);
		}
		role.setCode(code == null ? createName() : code);
		return roleService.save(role);
	}
	
	@Override
	public IdmRoleCompositionDto createRoleComposition(IdmRoleDto superior, IdmRoleDto sub) {
		IdmRoleCompositionDto roleComposition = new IdmRoleCompositionDto();
		roleComposition.setSuperior(superior.getId());
		roleComposition.setSub(sub.getId());
		//
		return roleCompositionService.save(roleComposition);
	}
	
	
	@Override
	public IdmRoleGuaranteeDto createRoleGuarantee(IdmRoleDto role, IdmIdentityDto guarantee) {
		IdmRoleGuaranteeDto dto = new IdmRoleGuaranteeDto();
		dto.setRole(role.getId());
		dto.setGuarantee(guarantee.getId());
		//
		return roleGuaranteeService.save(dto);
	}
	
	@Override
	public IdmRoleGuaranteeRoleDto createRoleGuaranteeRole(IdmRoleDto role, IdmRoleDto guarantee) {
		IdmRoleGuaranteeRoleDto dto = new IdmRoleGuaranteeRoleDto();
		dto.setRole(role.getId());
		dto.setGuaranteeRole(guarantee.getId());
		//
		return roleGuaranteeRoleService.save(dto);
	}

	@Override
	public void deleteRole(UUID id) {
		roleService.deleteById(id);
	}

	@Override
	public IdmRoleTreeNodeDto createRoleTreeNode(IdmRoleDto role, IdmTreeNodeDto treeNode, boolean skipLongRunningTask) {
		Assert.notNull(role);
		Assert.notNull(treeNode);
		//
		IdmRoleTreeNodeDto roleTreeNode = new IdmRoleTreeNodeDto();
		roleTreeNode.setRole(role.getId());
		roleTreeNode.setTreeNode(treeNode.getId());
		roleTreeNode.setName(DEFAULT_AUTOMATIC_ROLE_NAME);
		if (skipLongRunningTask) {
			return roleTreeNodeService.saveInternal(roleTreeNode);
		}
		return roleTreeNodeService.save(roleTreeNode);
	}

	@Override
	public IdmAuthorizationPolicyDto createBasePolicy(UUID role, BasePermission... permission) {
		return createBasePolicy(role, null, null, permission);
	}

	@Override
	public IdmAuthorizationPolicyDto createSpecificPolicy(UUID role, GroupPermission groupPermission, Class<?> authorizableType, String evaluatorType, BasePermission... permission) {
		IdmAuthorizationPolicyDto dto = new IdmAuthorizationPolicyDto();
		dto.setRole(role);
		dto.setEvaluatorType(evaluatorType);
		dto.setGroupPermission(groupPermission == null ? null : groupPermission.getName());
		dto.setAuthorizableType(authorizableType == null ? null : authorizableType.getCanonicalName());
		dto.setPermissions(permission);
		return authorizationPolicyService.save(dto);
	}
	
	@Override
	public IdmAuthorizationPolicyDto createAuthorizationPolicy(
			UUID role, 
			GroupPermission groupPermission,
			Class<? extends AbstractEntity> authorizableType,
			Class<? extends AuthorizationEvaluator<? extends Identifiable>> evaluator,
			BasePermission... permission) {
		return createAuthorizationPolicy(role, groupPermission, authorizableType, evaluator, null, permission);
	}
	
	@Override
	public IdmAuthorizationPolicyDto createAuthorizationPolicy(
			UUID role, 
			GroupPermission groupPermission,
			Class<? extends AbstractEntity> authorizableType,
			Class<? extends AuthorizationEvaluator<? extends Identifiable>> evaluator,
		    ConfigurationMap evaluatorProperties,
			BasePermission... permission) {
		IdmAuthorizationPolicyDto dto = new IdmAuthorizationPolicyDto();
		dto.setRole(role);
		dto.setEvaluator(evaluator);
		dto.setGroupPermission(groupPermission == null ? null : groupPermission.getName());
		dto.setAuthorizableType(authorizableType == null ? null : authorizableType.getCanonicalName());
		dto.setEvaluatorProperties(evaluatorProperties);
		dto.setPermissions(permission);
		//
		return authorizationPolicyService.save(dto);
	}

	@Override
	public IdmAuthorizationPolicyDto createBasePolicy(UUID role, GroupPermission groupPermission, Class<?> authorizableType, BasePermission... permission) {
		return this.createSpecificPolicy(role, groupPermission, authorizableType, "eu.bcvsolutions.idm.core.security.evaluator.BasePermissionEvaluator", permission);
	}
	
	@Override
	public IdmAuthorizationPolicyDto createUuidPolicy(IdmRoleDto role, Identifiable authorizableEntity, BasePermission... permission) {
		Assert.notNull(role);
		Assert.notNull(authorizableEntity);
		//
		return createUuidPolicy(role.getId(), (UUID) authorizableEntity.getId(), permission);
	}

	@Override
	public IdmAuthorizationPolicyDto createUuidPolicy(UUID role, UUID authorizableEntity, BasePermission... permission) {
		Assert.notNull(role);
		Assert.notNull(authorizableEntity);
		//
		IdmAuthorizationPolicyDto dto = new IdmAuthorizationPolicyDto();
		dto.setRole(role);
		dto.setEvaluatorType("eu.bcvsolutions.idm.core.security.evaluator.UuidEvaluator"); // ouch: evaluator is in core-impl 
		dto.getEvaluatorProperties().put("uuid", authorizableEntity);
		dto.setPermissions(permission);
		return authorizationPolicyService.save(dto);
	}
	
	@Override
	public IdmIdentityRoleDto createIdentityRole(IdmIdentityDto identity, IdmRoleDto role) {
		return createIdentityRole(identity, role, null, null);
	}

	@Override
	public IdmIdentityRoleDto createIdentityRole(IdmIdentityDto identity, IdmRoleDto role, LocalDate validFrom, LocalDate validTill) {
		return createIdentityRole(getPrimeContract(identity.getId()), role, validFrom, validTill);
	}

	@Override
	public IdmIdentityRoleDto createIdentityRole(IdmIdentityContractDto identityContract, IdmRoleDto role) {
		return createIdentityRole(identityContract, role, null, null);
	}
	
	@Override
	public IdmIdentityRoleDto createIdentityRole(IdmContractPositionDto contractPosition, IdmRoleDto role) {
		return createIdentityRole(contractPosition, role, null, null);
	}
	
	@Override
	public IdmIdentityRoleDto createIdentityRole(IdmIdentityContractDto identityContract, IdmRoleDto role, LocalDate validFrom, LocalDate validTill) {
		IdmIdentityRoleDto identityRole = new IdmIdentityRoleDto();
		identityRole.setIdentityContract(identityContract.getId());
		identityRole.setRole(role.getId());
		identityRole.setValidFrom(validFrom);
		identityRole.setValidTill(validTill);
		return identityRoleService.save(identityRole);
	}
	
	@Override
	public IdmIdentityRoleDto createIdentityRole(
			IdmContractPositionDto contractPosition, 
			IdmRoleDto role, 
			LocalDate validFrom, 
			LocalDate validTill) {
		IdmIdentityRoleDto identityRole = new IdmIdentityRoleDto();
		identityRole.setIdentityContract(contractPosition.getIdentityContract());
		identityRole.setContractPosition(contractPosition.getId());
		identityRole.setRole(role.getId());
		identityRole.setValidFrom(validFrom);
		identityRole.setValidTill(validTill);
		return identityRoleService.save(identityRole);
	}
	
	@Override
	public IdmIdentityContractDto getPrimeContract(IdmIdentityDto identity) {
		Assert.notNull(identity);
		//
		return getPrimeContract(identity.getId());
	}

	@Override
	public IdmIdentityContractDto getPrimeContract(UUID identityId) {
		return identityContractService.getPrimeContract(identityId);
	}

	@Override
	public IdmIdentityContractDto createIdentityContact(IdmIdentityDto identity) {
		return createIdentityContact(identity, null);
	}

	@Override
	public IdmIdentityContractDto createIdentityContact(IdmIdentityDto identity, IdmTreeNodeDto position) {
		return createIdentityContact(identity, position, null, null);
	}

	@Override
	public IdmIdentityContractDto createIdentityContact(IdmIdentityDto identity, IdmTreeNodeDto position, LocalDate validFrom, LocalDate validTill) {
		IdmIdentityContractDto contract = new IdmIdentityContractDto();
		contract.setIdentity(identity.getId());
		contract.setPosition(createName());
		contract.setWorkPosition(position == null ? null : position.getId());
		contract.setValidFrom(validFrom);
		contract.setValidTill(validTill);
		return identityContractService.save(contract);
	}
	
	@Override
	public IdmContractSliceDto createContractSlice(IdmIdentityDto identity) {
		return createContractSlice(identity, null, null, null, null);
	}

	
	@Override
	public IdmContractSliceDto createContractSlice(IdmIdentityDto identity, IdmTreeNodeDto position,
			LocalDate validFrom, LocalDate contractValidFrom, LocalDate contractValidTill) {
		return createContractSlice(identity, null, position, validFrom, contractValidFrom, contractValidTill);
	}

	@Override
	public IdmContractSliceDto createContractSlice(IdmIdentityDto identity, String contractCode,
			IdmTreeNodeDto position, LocalDate validFrom, LocalDate contractValidFrom, LocalDate contractValidTill) {
		IdmContractSliceDto contract = new IdmContractSliceDto();
		contract.setIdentity(identity.getId());
		contract.setPosition(createName());
		contract.setContractCode(contractCode);
		contract.setWorkPosition(position == null ? null : position.getId());
		contract.setValidFrom(validFrom);
		contract.setContractValidFrom(contractValidFrom);
		contract.setContractValidTill(contractValidTill);
		return contractSliceService.save(contract);
	}


	@Override
	public void deleteIdentityContact(UUID id) {
		identityContractService.deleteById(id);
	}

	@Override
	public IdmContractGuaranteeDto createContractGuarantee(UUID identityContractId, UUID identityId) {
		return contractGuaranteeService.save(new IdmContractGuaranteeDto(identityContractId, identityId));
	}
	
	@Override
	public IdmContractPositionDto createContractPosition(UUID identityContractId, UUID treeNodeId) {
		return contractPositionService.save(new IdmContractPositionDto(identityContractId, treeNodeId));
	}
	
	@Override
	public IdmContractPositionDto createContractPosition(UUID identityContractId) {
		return createContractPosition(identityContractId, createTreeNode().getId());
	}
	
	@Override
	public IdmContractPositionDto createContractPosition(IdmIdentityContractDto contract) {
		Assert.notNull(contract);
		//
		return createContractPosition(contract.getId());
	}
	
	@Override
	public IdmContractPositionDto createContractPosition(IdmIdentityContractDto contract, IdmTreeNodeDto treeNode) {
		Assert.notNull(contract);
		//
		return createContractPosition(contract.getId(), treeNode == null ? null : treeNode.getId());
	}
	
	@Override
	public IdmContractSliceGuaranteeDto createContractSliceGuarantee(UUID sliceId, UUID identityId) {
		return contractSliceGuaranteeService.save(new IdmContractSliceGuaranteeDto(sliceId, identityId));
	}
	
	@Override
	public IdmRoleRequestDto createRoleRequest(IdmIdentityDto identity, IdmRoleDto... roles) {
		return createRoleRequest(getPrimeContract(identity.getId()), roles);
	}
	
	@Override
	public IdmRoleRequestDto createRoleRequest(IdmIdentityContractDto contract, IdmRoleDto... roles) {
		IdmRoleRequestDto roleRequest = new IdmRoleRequestDto();
		roleRequest.setApplicant(contract.getIdentity());
		roleRequest.setRequestedByType(RoleRequestedByType.MANUALLY);
		roleRequest.setExecuteImmediately(true);
		roleRequest = roleRequestService.save(roleRequest);
		//
		for (IdmRoleDto role : roles) {
			IdmConceptRoleRequestDto conceptRoleRequest = new IdmConceptRoleRequestDto();
			conceptRoleRequest.setRoleRequest(roleRequest.getId());
			conceptRoleRequest.setIdentityContract(contract.getId());
			conceptRoleRequest.setValidFrom(contract.getValidFrom());
			conceptRoleRequest.setValidTill(contract.getValidTill());
			conceptRoleRequest.setRole(role.getId());
			//
			conceptRoleRequest.setOperation(ConceptRoleRequestOperation.ADD);
			//
			conceptRoleRequestService.save(conceptRoleRequest);
		}
		return roleRequest;
	}

	@Override
	public IdmRoleRequestDto assignRoles(IdmIdentityContractDto contract, IdmRoleDto... roles) {
		return this.assignRoles(contract, true, roles);
	}

	@Override
	public IdmRoleRequestDto assignRoles(IdmIdentityContractDto contract, boolean startInNewTransaction, IdmRoleDto... roles) {
		IdmRoleRequestDto roleRequest = createRoleRequest(contract, roles);
		//
		return executeRequest(roleRequest, startInNewTransaction);
	}
	
	@Override
	public IdmRoleRequestDto executeRequest(IdmRoleRequestDto roleRequest, boolean startInNewTransaction) {
		return executeRequest(roleRequest, startInNewTransaction, false);
	}
	
	@Override
	public IdmRoleRequestDto executeRequest(IdmRoleRequestDto roleRequest, boolean startInNewTransaction, boolean immediate) {
		if (startInNewTransaction) {
			return roleRequestService.startRequest(roleRequest.getId(), false);
		}
		return roleRequestService.startRequestInternal(roleRequest.getId(), false, immediate);
	}

	@Override
	public IdmRoleCatalogueDto createRoleCatalogue(String code, UUID parentId) {
		IdmRoleCatalogueDto roleCatalogue = new IdmRoleCatalogueDto();
		code = code == null ? createName() : code;
		roleCatalogue.setName(code);
		roleCatalogue.setParent(parentId);
		roleCatalogue.setCode(code);
		//
		return roleCatalogueService.save(roleCatalogue);
	}
	
	@Override
	public IdmRoleCatalogueRoleDto createRoleCatalogueRole(IdmRoleDto role, IdmRoleCatalogueDto catalogue) {
		IdmRoleCatalogueRoleDto roleCatalogueRole = new IdmRoleCatalogueRoleDto();
		roleCatalogueRole.setRole(role.getId());
		roleCatalogueRole.setRoleCatalogue(catalogue.getId());
		//
		return roleCatalogueRoleService.save(roleCatalogueRole);
	}

	@Override
	public void enable(Class<? extends EntityEventProcessor<?>> processorType) {
		enableProcessor(processorType, true);
	}

	@Override
	public void disable(Class<? extends EntityEventProcessor<?>> processorType) {
		enableProcessor(processorType, false);
	}
	
	@Override
	public void enableFilter(Class<? extends FilterBuilder<?, ?>> filterType) {
		enableFilter(filterType, true);
	}
	
	@Override
	public void disableFilter(Class<? extends FilterBuilder<?, ?>> filterType) {
		enableFilter(filterType, false);
	}
	
	@Override
	public void enableModule(String moduleId) {
		moduleService.enable(moduleId);
	}
	
	@Override
	public void disableModule(String moduleId) {
		moduleService.disable(moduleId);
	}
	
	@Override
	public void setConfigurationValue(String configurationPropertyName, boolean value) {
		Assert.notNull(configurationPropertyName);
		//
		configurationService.setBooleanValue(configurationPropertyName, value);
	}
	
	@Override
	public void setConfigurationValue(String configurationPropertyName, String value) {
		Assert.notNull(configurationPropertyName);
		//
		configurationService.setValue(configurationPropertyName, value);
	}

	@Override
	public void waitForResult(Function<String, Boolean> continueFunction) {
		waitForResult(continueFunction, null, null);
	}
	
	@Override
	public void waitForResult(Function<String, Boolean> continueFunction, Integer interationWaitMilis, Integer iterationCount) {
		int maxCounter = iterationCount == null ? 50 : iterationCount;
		int waitTime = interationWaitMilis == null ? 300 : interationWaitMilis;
		//
		int counter = 0;
		while((continueFunction == null ? true : continueFunction.apply(null)) && (counter < maxCounter)) {
			counter++;
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException ex) {
				throw new CoreException(ex);
			}
		};
	}

	private void enableProcessor(Class<? extends EntityEventProcessor<?>> processorType, boolean enabled) {
		entityEventManager.setEnabled(context.getBean(processorType).getId(), enabled);
	}
	
	private void enableFilter(Class<? extends FilterBuilder<?, ?>> filterType, boolean enabled) {
		Assert.notNull(filterType);
		//
		FilterBuilder<?, ?> filter = context.getBean(filterType);
		Assert.notNull(filter);
		String enabledPropertyName = filter.getConfigurationPropertyName(ConfigurationService.PROPERTY_ENABLED);
		configurationService.setBooleanValue(enabledPropertyName, enabled);
	}

	@Override
	public IdmProcessedTaskItemDto prepareProcessedItem(IdmLongRunningTaskDto lrt) {
		IdmProcessedTaskItemDto item = new IdmProcessedTaskItemDto();
		item.setReferencedDtoType(IdmIdentityDto.class.getCanonicalName());
		item.setReferencedEntityId(UUID.randomUUID());
		item.setLongRunningTask(lrt.getId());
		item.setOperationResult(new OperationResult.Builder(OperationState.EXECUTED).build());
		return item;
	}
	@Override
	public IdmProcessedTaskItemDto prepareProcessedItem(IdmScheduledTaskDto d) {
		IdmProcessedTaskItemDto item = new IdmProcessedTaskItemDto();
		item.setReferencedDtoType(IdmIdentityDto.class.getCanonicalName());
		item.setReferencedEntityId(UUID.randomUUID());
		item.setScheduledTaskQueueOwner(d.getId());
		item.setOperationResult(new OperationResult.Builder(OperationState.EXECUTED).build());
		return item;
	}
	@Override
	public IdmProcessedTaskItemDto prepareProcessedItem(IdmScheduledTaskDto d, OperationState state) {
		IdmProcessedTaskItemDto item = new IdmProcessedTaskItemDto();
		item.setReferencedDtoType(IdmIdentityDto.class.getCanonicalName());
		item.setReferencedEntityId(UUID.randomUUID());
		item.setScheduledTaskQueueOwner(d.getId());
		item.setOperationResult(new OperationResult.Builder(state).build());
		return item;
	}

	@Override
	public IdmScheduledTaskDto createSchedulableTask() {
		IdmScheduledTaskDto d = new IdmScheduledTaskDto();
		d.setQuartzTaskName(UUID.randomUUID().toString());
		d = scheduledTaskService.saveInternal(d);
		return d;
	}

	@Override
	public IdmFormAttributeDto createEavAttribute(String code, Class<? extends Identifiable> clazz,
			PersistentType type) {
		IdmFormAttributeDto eavAttribute = new IdmFormAttributeDto();
		eavAttribute.setCode(code);
		IdmFormDefinitionDto main = formDefinitionService.findOneByMain(clazz.getName());
		eavAttribute.setFormDefinition(main.getId());
		eavAttribute.setName(code);
		eavAttribute.setConfidential(false);
		eavAttribute.setRequired(false);
		eavAttribute.setReadonly(false);
		eavAttribute.setPersistentType(type);
		return formAttributeService.save(eavAttribute);
	}

	@Override
	public void setEavValue(Identifiable owner, IdmFormAttributeDto attribute, Class<? extends Identifiable> clazz,
			Serializable value, PersistentType type) {
		UUID ownerId = UUID.fromString(owner.getId().toString());
		IdmFormDefinitionDto main = formDefinitionService.findOneByMain(clazz.getName());
		List<IdmFormValueDto> values = Lists.newArrayList(formService.getValues(ownerId, clazz, attribute));
		
		if (values.isEmpty()) {
			IdmFormValueDto newValue = new IdmFormValueDto();
			newValue.setPersistentType(type);
			newValue.setValue(value);
			newValue.setFormAttribute(attribute.getId());
			newValue.setOwnerId(owner.getId());
			values.add(newValue);
		} else {
			values.get(0).setValue(value);
		}
		
		formService.saveFormInstance(owner, main, values);
		
	}

	@Override
	public IdmAutomaticRoleAttributeDto createAutomaticRole(UUID roleId) {
		String testName = "test-auto-role-" + System.currentTimeMillis();
		if (roleId == null) {
			IdmRoleDto role = this.createRole();
			roleId = role.getId();
		}
		IdmAutomaticRoleAttributeDto automaticRole = new IdmAutomaticRoleAttributeDto();
		automaticRole.setRole(roleId);
		automaticRole.setName(testName);
		return automaticRoleAttributeService.save(automaticRole);
	}

	@Override
	public IdmAutomaticRoleAttributeRuleDto createAutomaticRoleRule(UUID automaticRoleId,
			AutomaticRoleAttributeRuleComparison comparsion, AutomaticRoleAttributeRuleType type, String attrName,
			UUID formAttrId, String value) {
		IdmAutomaticRoleAttributeRuleDto rule = new IdmAutomaticRoleAttributeRuleDto();
		rule.setComparison(comparsion);
		rule.setType(type);
		rule.setAttributeName(attrName);
		rule.setFormAttribute(formAttrId);
		rule.setValue(value);
		rule.setAutomaticRoleAttribute(automaticRoleId);
		rule = automaticRoleAttributeRuleService.save(rule);
		// disable concept must be after rule save
		IdmAutomaticRoleAttributeDto automaticRole = automaticRoleAttributeService.get(automaticRoleId);
		automaticRole.setConcept(false);
		automaticRole = automaticRoleAttributeService.save(automaticRole);
		//
		return rule;
	}
	
	@Override
	public IdmRoleTreeNodeDto createAutomaticRole(IdmRoleDto role, IdmTreeNodeDto treeNode) {
		return createRoleTreeNode(role, treeNode, false);
	}
	
	@Override
	public IdmProfileDto createProfile(IdmIdentityDto identity) {
		return profileService.findOrCreateByIdentity(identity.getId());
	}
}
