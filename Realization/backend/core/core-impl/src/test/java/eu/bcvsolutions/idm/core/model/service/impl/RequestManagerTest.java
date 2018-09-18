package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.AbstractCoreWorkflowIntegrationTest;
import eu.bcvsolutions.idm.core.api.domain.RequestOperationType;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemChangesDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowFilterDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowProcessInstanceDto;
import eu.bcvsolutions.idm.core.workflow.model.dto.WorkflowTaskInstanceDto;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowTaskInstanceService;
import eu.bcvsolutions.idm.test.api.TestHelper;

/**
 * Test for universal request agenda
 * 
 * @author svandav
 *
 */
public class RequestManagerTest extends AbstractCoreWorkflowIntegrationTest {

	@Autowired
	protected TestHelper helper;
	@Autowired
	private RequestManager requestManager;
	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private WorkflowTaskInstanceService workflowTaskInstanceService;
	@Autowired
	private WorkflowProcessInstanceService workflowProcessInstanceService;
	@Autowired
	private IdmRequestService requestService;
	@Autowired
	private IdmRoleGuaranteeService roleGuaranteeService;
	@Autowired
	private FormService formService;

	@Before
	public void init() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void testCreateRoleByRequest() {

		IdmRoleDto newRole = new IdmRoleDto();
		newRole.setCode(getHelper().createName());
		newRole.setName(newRole.getCode());
		newRole.setPriority(10);
		newRole.setDescription(getHelper().createName());

		IdmRequestDto request = requestManager.createRequest(newRole);
		Assert.assertNotNull(request);

		Requestable requestable = requestManager.post(request.getId(), newRole);
		Assert.assertNotNull(requestable);
		Assert.assertTrue(requestable instanceof IdmRoleDto);

		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;
		// Is not same instance
		Assert.assertTrue(newRole != roleFromRequest);
		// Has same values as new role
		Assert.assertEquals(newRole.getCode(), roleFromRequest.getCode());
		Assert.assertEquals(newRole.getName(), roleFromRequest.getName());
		Assert.assertEquals(newRole.getPriority(), roleFromRequest.getPriority());
		Assert.assertEquals(newRole.getDescription(), roleFromRequest.getDescription());

		// Role not exists yet
		Assert.assertNull(roleService.get(roleFromRequest.getId()));

		IdmRequestDto executedRequest = requestManager.executeRequest(request.getId());
		Assert.assertNotNull(executedRequest);
		Assert.assertEquals(RequestState.EXECUTED, executedRequest.getState());

		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		// Role must exists now
		Assert.assertNotNull(executedRole);
		// Has same values as new role
		Assert.assertEquals(newRole.getCode(), executedRole.getCode());
		Assert.assertEquals(newRole.getName(), executedRole.getName());
		Assert.assertEquals(newRole.getPriority(), executedRole.getPriority());
		Assert.assertEquals(newRole.getDescription(), executedRole.getDescription());

	}

	@Test
	public void testChangeRoleByRequest() {

		// Create role
		IdmRoleDto changedRole = getHelper().createRole();

		// Create request
		IdmRequestDto request = requestManager.createRequest(changedRole);
		Assert.assertNotNull(request);
		Assert.assertEquals(request.getOwnerType(), changedRole.getClass().getName());
		Assert.assertEquals(request.getOwnerId(), changedRole.getId());

		// Change role (without save)
		changedRole.setDescription(getHelper().createName());
		changedRole.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), changedRole);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());

		Assert.assertTrue(requestable instanceof IdmRoleDto);
		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;
		// Is not same instance
		Assert.assertTrue(changedRole != roleFromRequest);
		// Has same values as new role
		Assert.assertEquals(changedRole.getPriority(), roleFromRequest.getPriority());
		Assert.assertEquals(changedRole.getDescription(), roleFromRequest.getDescription());

		IdmRoleDto currentRole = roleService.get(changedRole.getId());
		Assert.assertNotEquals(changedRole.getPriority(), currentRole.getPriority());
		Assert.assertNotEquals(changedRole.getDescription(), currentRole.getDescription());

		// Start request
		IdmRequestDto executedRequest = requestManager.executeRequest(request.getId());
		Assert.assertNotNull(executedRequest);
		Assert.assertEquals(RequestState.EXECUTED, executedRequest.getState());

		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		Assert.assertNotNull(executedRole);
		// Has same values as new role
		Assert.assertEquals(changedRole.getCode(), executedRole.getCode());
		Assert.assertEquals(changedRole.getName(), executedRole.getName());
		Assert.assertEquals(changedRole.getPriority(), executedRole.getPriority());
		Assert.assertEquals(changedRole.getDescription(), executedRole.getDescription());

	}

	@Test
	public void testDeleteRoleByRequest() {

		// Create role
		IdmRoleDto role = getHelper().createRole();

		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);
		Assert.assertEquals(request.getOwnerType(), role.getClass().getName());
		Assert.assertEquals(request.getOwnerId(), role.getId());

		// Create request item
		Requestable requestable = requestManager.delete(request.getId(), role);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());
		IdmRequestItemDto requestItem = DtoUtils.getEmbedded((AbstractDto) requestable, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.REMOVE, requestItem.getOperation());

		Assert.assertTrue(requestable instanceof IdmRoleDto);
		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;

		// Is not deleted yet
		IdmRoleDto currentRole = roleService.get(role.getId());
		Assert.assertNotNull(currentRole);

		// Start request
		IdmRequestDto executedRequest = requestManager.executeRequest(request.getId());
		Assert.assertNotNull(executedRequest);
		Assert.assertEquals(RequestState.EXECUTED, executedRequest.getState());

		// Role have to be deleted
		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		Assert.assertNull(executedRole);
	}

	@Test
	public void testChangeRoleWithGuaranteesApprove() {

		// Create role with guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleDto changedRole = getHelper().createRole();
		getHelper().createRoleGuarantee(changedRole, guarantee);

		// Create request
		IdmRequestDto request = requestManager.createRequest(changedRole);
		Assert.assertNotNull(request);
		Assert.assertEquals(request.getOwnerType(), changedRole.getClass().getName());
		Assert.assertEquals(request.getOwnerId(), changedRole.getId());

		// Change role (without save)
		changedRole.setDescription(getHelper().createName());
		changedRole.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), changedRole);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());

		Assert.assertTrue(requestable instanceof IdmRoleDto);
		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;
		// Is not same instance
		Assert.assertTrue(changedRole != roleFromRequest);
		// Has same values as new role
		Assert.assertEquals(changedRole.getPriority(), roleFromRequest.getPriority());
		Assert.assertEquals(changedRole.getDescription(), roleFromRequest.getDescription());

		IdmRoleDto currentRole = roleService.get(changedRole.getId());
		Assert.assertNotEquals(changedRole.getPriority(), currentRole.getPriority());
		Assert.assertNotEquals(changedRole.getDescription(), currentRole.getDescription());

		// Start request
		IdmRequestDto executedRequest = requestManager.startRequest(request.getId(), true);
		Assert.assertNotNull(executedRequest);
		// Role has guarantee, approval process must be started
		Assert.assertEquals(RequestState.IN_PROGRESS, executedRequest.getState());

		loginAsNoAdmin(guarantee.getUsername());
		try {
			completeTasksFromUsers(guarantee.getId().toString(), "approve");
		} catch (ResultCodeException e) {
			fail("User has permission to approve task. Error message: " + e.getLocalizedMessage());
		} catch (Exception e) {
			fail("Some problem: " + e.getLocalizedMessage());
		}
		// Reload the request (after approving)
		executedRequest = requestService.get(executedRequest.getId());
		Assert.assertEquals(RequestState.EXECUTED, executedRequest.getState());

		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		// Role must exists now
		Assert.assertNotNull(executedRole);
		// Has same values as new role
		Assert.assertEquals(changedRole.getCode(), executedRole.getCode());
		Assert.assertEquals(changedRole.getName(), executedRole.getName());
		Assert.assertEquals(changedRole.getPriority(), executedRole.getPriority());
		Assert.assertEquals(changedRole.getDescription(), executedRole.getDescription());

	}

	@Test
	public void testChangeRoleWithGuaranteesDisapprov() {

		// Create role with guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleDto changedRole = getHelper().createRole();
		getHelper().createRoleGuarantee(changedRole, guarantee);

		// Create request
		IdmRequestDto request = requestManager.createRequest(changedRole);
		Assert.assertNotNull(request);
		Assert.assertEquals(request.getOwnerType(), changedRole.getClass().getName());
		Assert.assertEquals(request.getOwnerId(), changedRole.getId());

		// Change role (without save)
		changedRole.setDescription(getHelper().createName());
		changedRole.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), changedRole);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());

		Assert.assertTrue(requestable instanceof IdmRoleDto);
		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;
		// Is not same instance
		Assert.assertTrue(changedRole != roleFromRequest);
		// Has same values as new role
		Assert.assertEquals(changedRole.getPriority(), roleFromRequest.getPriority());
		Assert.assertEquals(changedRole.getDescription(), roleFromRequest.getDescription());

		IdmRoleDto currentRole = roleService.get(changedRole.getId());
		Assert.assertNotEquals(changedRole.getPriority(), currentRole.getPriority());
		Assert.assertNotEquals(changedRole.getDescription(), currentRole.getDescription());

		// Start request
		IdmRequestDto executedRequest = requestManager.startRequest(request.getId(), true);
		Assert.assertNotNull(executedRequest);
		// Role has guarantee, approval process must be started
		Assert.assertEquals(RequestState.IN_PROGRESS, executedRequest.getState());

		loginAsNoAdmin(guarantee.getUsername());
		try {
			completeTasksFromUsers(guarantee.getId().toString(), "disapprove");
		} catch (ResultCodeException e) {
			fail("User has permission to approve task. Error message: " + e.getLocalizedMessage());
		} catch (Exception e) {
			fail("Some problem: " + e.getLocalizedMessage());
		}
		// Reload the request (after disapproving)
		executedRequest = requestService.get(executedRequest.getId());
		Assert.assertEquals(RequestState.DISAPPROVED, executedRequest.getState());

		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		Assert.assertNotNull(executedRole);
		// Has different values as new role
		Assert.assertNotEquals(changedRole.getPriority(), executedRole.getPriority());
		Assert.assertNotEquals(changedRole.getDescription(), executedRole.getDescription());

	}

	@Test
	public void testChangeRoleWithGuaranteesSkipApproving() {

		// Create role with guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleDto changedRole = getHelper().createRole();
		getHelper().createRoleGuarantee(changedRole, guarantee);

		// Create request
		IdmRequestDto request = requestManager.createRequest(changedRole);
		Assert.assertNotNull(request);
		Assert.assertEquals(request.getOwnerType(), changedRole.getClass().getName());
		Assert.assertEquals(request.getOwnerId(), changedRole.getId());

		// Change role (without save)
		changedRole.setDescription(getHelper().createName());
		changedRole.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), changedRole);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());

		Assert.assertTrue(requestable instanceof IdmRoleDto);
		IdmRoleDto roleFromRequest = (IdmRoleDto) requestable;
		// Is not same instance
		Assert.assertTrue(changedRole != roleFromRequest);
		// Has same values as new role
		Assert.assertEquals(changedRole.getPriority(), roleFromRequest.getPriority());
		Assert.assertEquals(changedRole.getDescription(), roleFromRequest.getDescription());

		IdmRoleDto currentRole = roleService.get(changedRole.getId());
		Assert.assertNotEquals(changedRole.getPriority(), currentRole.getPriority());
		Assert.assertNotEquals(changedRole.getDescription(), currentRole.getDescription());

		// Skip approving
		request.setExecuteImmediately(true);
		request = requestService.save(request);

		// Start request
		IdmRequestDto executedRequest = requestManager.startRequest(request.getId(), true);
		Assert.assertNotNull(executedRequest);
		// Role has guarantee, but approval process wasn't started because the request
		// skipped approving.
		Assert.assertEquals(RequestState.EXECUTED, executedRequest.getState());

		IdmRoleDto executedRole = roleService.get(roleFromRequest.getId());
		// Role must exists now
		Assert.assertNotNull(executedRole);
		// Has same values as new role
		Assert.assertEquals(changedRole.getCode(), executedRole.getCode());
		Assert.assertEquals(changedRole.getName(), executedRole.getName());
		Assert.assertEquals(changedRole.getPriority(), executedRole.getPriority());
		Assert.assertEquals(changedRole.getDescription(), executedRole.getDescription());

	}

	@Test
	public void testBasicItemIntegrity() {

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);

		// null -> DELETE
		// Create delete request item
		Requestable requestable = requestManager.delete(request.getId(), role);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());
		IdmRequestItemDto requestItem = DtoUtils.getEmbedded((AbstractDto) requestable, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.REMOVE, requestItem.getOperation());
		Assert.assertTrue(requestable instanceof IdmRoleDto);

		// DELETE -> CHANGE
		// Create change request item
		role.setDescription(getHelper().createName());
		Requestable requestablePost = requestManager.post(request.getId(), requestable);
		IdmRequestItemDto changeRequestItem = DtoUtils.getEmbedded((AbstractDto) requestablePost,
				Requestable.REQUEST_ITEM_FIELD, IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.UPDATE, changeRequestItem.getOperation());
		// In one request can exists only one item for same owner -> change item must be
		// same (updated) as in delete cause.
		Assert.assertEquals(requestItem.getId(), changeRequestItem.getId());

		// CHANGE -> null
		// Create delete request item (again)
		requestable = requestManager.delete(request.getId(), requestablePost);
		Assert.assertNotNull(requestable);
		Assert.assertNull(requestable.getRequestItem());
		// Previous item was deleted
		List<IdmRequestItemDto> items = requestManager.findRequestItems(request.getId(), null);
		Assert.assertEquals(0, items.size());

		// null -> DELETE
		// Create delete request item
		requestable = requestManager.delete(request.getId(), requestable);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());
		requestItem = DtoUtils.getEmbedded((AbstractDto) requestable, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.REMOVE, requestItem.getOperation());

		// DELETE -> null
		// Again delete same DTO. In this situation should be previous item (delete)
		// deleted.
		requestable = requestManager.delete(request.getId(), requestable);
		Assert.assertNotNull(requestable);
		Assert.assertNull(requestable.getRequestItem());
		Assert.assertEquals(0, requestManager.findRequestItems(request.getId(), null).size());

		IdmRoleDto newRole = new IdmRoleDto();
		newRole.setCode(getHelper().createName());
		newRole.setName(newRole.getCode());
		// null -> ADD
		Requestable newRequestable = requestManager.post(request.getId(), newRole);
		Assert.assertNotNull(newRequestable);
		Assert.assertNotNull(newRequestable.getRequestItem());
		IdmRequestItemDto newRequestItem = DtoUtils.getEmbedded((AbstractDto) newRequestable,
				Requestable.REQUEST_ITEM_FIELD, IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.ADD, newRequestItem.getOperation());
		// One item must exists now
		Assert.assertEquals(1, requestManager.findRequestItems(request.getId(), null).size());
		// ADD -> null
		// Delete of DTO, which is not created in DB causes his deleting.
		requestable = requestManager.delete(request.getId(), newRequestable);
		Assert.assertNotNull(requestable);
		Assert.assertNull(requestable.getRequestItem());
		Assert.assertEquals(0, requestManager.findRequestItems(request.getId(), null).size());

	}

	@Test
	public void testDeleteRequestIntegrity() {

		// Log as admin, but not user 'admin' (we don't want to skip WF)
		IdmIdentityDto adminUser = getHelper().createIdentity();
		loginAsAdmin(adminUser.getUsername());

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);

		// Create guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleGuaranteeDto roleGuarantee = new IdmRoleGuaranteeDto();
		roleGuarantee.setRole(role.getId());
		roleGuarantee.setGuarantee(guarantee.getId());
		Requestable requestablePost = requestManager.post(request.getId(), roleGuarantee);
		IdmRequestItemDto changeRequestItem = DtoUtils.getEmbedded((AbstractDto) requestablePost,
				Requestable.REQUEST_ITEM_FIELD, IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.ADD, changeRequestItem.getOperation());
		Assert.assertTrue(requestablePost instanceof IdmRoleGuaranteeDto);

		// Change role (without save)
		role.setDescription(getHelper().createName());
		role.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), role);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());
		changeRequestItem = DtoUtils.getEmbedded((AbstractDto) requestable, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.UPDATE, changeRequestItem.getOperation());
		Assert.assertTrue(requestable instanceof IdmRoleDto);
		// Request should be in concept state
		request = requestService.get(request.getId());
		Assert.assertEquals(RequestState.CONCEPT, request.getState());

		// Two items should be created
		Assert.assertEquals(2, requestManager.findRequestItems(request.getId(), null).size());

		// Delete the request
		requestService.delete(request);
		IdmRequestDto requestDeleted = requestService.get(request.getId());
		Assert.assertNull(requestDeleted);

		// All items should be deleted
		Assert.assertEquals(0, requestManager.findRequestItems(request.getId(), null).size());
	}

	@Test
	public void testDeleteRequestIntegrityWithWf() {

		// Log as admin, but not user 'admin' (we don't want to skip WF)
		IdmIdentityDto adminUser = getHelper().createIdentity();
		loginAsAdmin(adminUser.getUsername());

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);

		// Create guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleGuaranteeDto roleGuarantee = new IdmRoleGuaranteeDto();
		roleGuarantee.setRole(role.getId());
		roleGuarantee.setGuarantee(guarantee.getId());
		Requestable requestablePost = requestManager.post(request.getId(), roleGuarantee);
		IdmRequestItemDto changeRequestItem = DtoUtils.getEmbedded((AbstractDto) requestablePost,
				Requestable.REQUEST_ITEM_FIELD, IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.ADD, changeRequestItem.getOperation());
		Assert.assertTrue(requestablePost instanceof IdmRoleGuaranteeDto);

		// Change role (without save)
		role.setDescription(getHelper().createName());
		role.setPriority(1000);
		// Create request item
		Requestable requestable = requestManager.post(request.getId(), role);
		Assert.assertNotNull(requestable);
		Assert.assertNotNull(requestable.getRequestItem());
		changeRequestItem = DtoUtils.getEmbedded((AbstractDto) requestable, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.UPDATE, changeRequestItem.getOperation());
		Assert.assertTrue(requestable instanceof IdmRoleDto);
		// Start request
		IdmRequestDto executedRequest = requestManager.startRequest(request.getId(), true);
		executedRequest = requestService.get(executedRequest.getId());
		Assert.assertEquals(RequestState.IN_PROGRESS, executedRequest.getState());
		String processId = executedRequest.getWfProcessId();
		Assert.assertNotNull(processId);
		// Wf process is in progress
		WorkflowProcessInstanceDto processInstace = workflowProcessInstanceService.get(processId);
		Assert.assertNotNull(processInstace);

		// Two items should be created
		Assert.assertEquals(2, requestManager.findRequestItems(request.getId(), null).size());
		// Delete the request
		requestService.delete(executedRequest);
		IdmRequestDto requestDeleted = requestService.get(executedRequest.getId());
		Assert.assertNull(requestDeleted);
		// Process should be deleted (canceled)
		processInstace = workflowProcessInstanceService.get(processId);
		Assert.assertNull(processInstace);

	}

	@Test
	public void testFind() {

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Create guarantee One
		IdmIdentityDto guaranteeOne = getHelper().createIdentity();
		IdmRoleGuaranteeDto roleGuaranteeOne = getHelper().createRoleGuarantee(role, guaranteeOne);

		IdmRoleGuaranteeFilter guaranteeFilter = new IdmRoleGuaranteeFilter();
		guaranteeFilter.setRole(role.getId());

		Assert.assertEquals(1, roleGuaranteeService.find(guaranteeFilter, null).getTotalElements());

		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);
		List<IdmRoleGuaranteeDto> guarantees = requestManager
				.find(IdmRoleGuaranteeDto.class, request.getId(), guaranteeFilter, null).getContent();
		Assert.assertEquals(1, guarantees.size());

		// Create guarantee
		IdmIdentityDto guarantee = getHelper().createIdentity();
		IdmRoleGuaranteeDto roleGuaranteeTwo = new IdmRoleGuaranteeDto();
		roleGuaranteeTwo.setRole(role.getId());
		roleGuaranteeTwo.setGuarantee(guarantee.getId());
		IdmRoleGuaranteeDto requestablePost = requestManager.post(request.getId(), roleGuaranteeTwo);
		IdmRequestItemDto changeRequestItem = DtoUtils.getEmbedded(requestablePost, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.ADD, changeRequestItem.getOperation());

		// Find via standard service returns still only one result
		Assert.assertEquals(1, roleGuaranteeService.find(guaranteeFilter, null).getTotalElements());
		// Find via request manager returns two results
		guarantees = requestManager.find(IdmRoleGuaranteeDto.class, request.getId(), guaranteeFilter, null)
				.getContent();
		Assert.assertEquals(2, guarantees.size());

		// Create new request
		IdmRequestDto requestTwo = requestManager.createRequest(role);
		Assert.assertNotNull(requestTwo);
		guarantees = requestManager.find(IdmRoleGuaranteeDto.class, requestTwo.getId(), guaranteeFilter, null)
				.getContent();
		Assert.assertEquals(1, guarantees.size());

		// Change the role-guarantee (use new guarantee)
		IdmIdentityDto guaranteeTwo = getHelper().createIdentity();
		roleGuaranteeOne.setGuarantee(guaranteeTwo.getId());
		IdmRoleGuaranteeDto roleGuaranteeOneRequest = requestManager.post(requestTwo.getId(), roleGuaranteeOne);
		changeRequestItem = DtoUtils.getEmbedded(roleGuaranteeOneRequest, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		Assert.assertEquals(RequestOperationType.UPDATE, changeRequestItem.getOperation());

		// Role guarantee One are equals (same ID), but must have different guarantee
		IdmRoleGuaranteeDto currentRoleGuaranteeOne = roleGuaranteeService.get(roleGuaranteeOne);
		Assert.assertEquals(currentRoleGuaranteeOne, roleGuaranteeOneRequest);
		Assert.assertNotEquals(currentRoleGuaranteeOne.getGuarantee(), roleGuaranteeOneRequest.getGuarantee());

		guarantees = roleGuaranteeService.find(guaranteeFilter, null).getContent();
		// Find via standard service returns still only one result
		Assert.assertEquals(1, guarantees.size());
		// Find via request manager returns one result too (item was only updated in the
		// request)
		List<IdmRoleGuaranteeDto> guaranteesRequest = requestManager
				.find(IdmRoleGuaranteeDto.class, requestTwo.getId(), guaranteeFilter, null).getContent();
		Assert.assertEquals(1, guaranteesRequest.size());
		// Role guarantee One are equals (same ID), but must have different guarantee
		Assert.assertEquals(guarantees.get(0), guaranteesRequest.get(0));
		Assert.assertNotEquals(guarantees.get(0).getGuarantee(), guaranteesRequest.get(0).getGuarantee());

	}

	@Test
	public void testRoleWithEAV() {

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Get definition
		IdmFormInstanceDto formInstance = formService.getFormInstance(role);
		// None values yet
		Assert.assertEquals(0, formInstance.getValues().size());
		IdmFormDefinitionDto formDefinition = formInstance.getFormDefinition();

		// Create form attributes
		IdmFormAttributeDto attributeShortText = new IdmFormAttributeDto();
		attributeShortText.setCode(getHelper().createName());
		attributeShortText.setName(attributeShortText.getCode());
		attributeShortText.setPersistentType(PersistentType.SHORTTEXT);
		attributeShortText.setFormDefinition(formDefinition.getId());
		attributeShortText = formService.saveAttribute(attributeShortText);

		IdmFormAttributeDto attributeBoolean = new IdmFormAttributeDto();
		attributeBoolean.setCode(getHelper().createName());
		attributeBoolean.setName(attributeBoolean.getCode());
		attributeBoolean.setPersistentType(PersistentType.BOOLEAN);
		attributeBoolean.setFormDefinition(formDefinition.getId());
		attributeBoolean = formService.saveAttribute(attributeBoolean);

		IdmFormAttributeDto attributeConfidential = new IdmFormAttributeDto();
		attributeConfidential.setCode(getHelper().createName());
		attributeConfidential.setName(attributeConfidential.getCode());
		attributeConfidential.setPersistentType(PersistentType.SHORTTEXT);
		attributeConfidential.setConfidential(true);
		attributeConfidential.setFormDefinition(formDefinition.getId());
		attributeConfidential = formService.saveAttribute(attributeConfidential);

		IdmFormAttributeDto attributeInt = new IdmFormAttributeDto();
		attributeInt.setCode(getHelper().createName());
		attributeInt.setName(attributeInt.getCode());
		attributeInt.setPersistentType(PersistentType.INT);
		attributeInt.setFormDefinition(formDefinition.getId());
		attributeInt = formService.saveAttribute(attributeInt);

		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);
		IdmFormInstanceDto formInstanceRequest = requestManager.getFormInstance(request.getId(), role, formDefinition);
		// None values yet
		Assert.assertEquals(0, formInstanceRequest.getValues().size());

		IdmFormValueDto valueShortText = new IdmFormValueDto(attributeShortText);
		valueShortText.setValue(getHelper().createName());
		formInstanceRequest.getValues().add(valueShortText);
		IdmFormValueDto valueBoolean = new IdmFormValueDto(attributeBoolean);
		valueBoolean.setValue(true);
		formInstanceRequest.getValues().add(valueBoolean);
		IdmFormValueDto valueConfidential = new IdmFormValueDto(attributeConfidential);
		String confidentialValueString = getHelper().createName();
		valueConfidential.setValue(confidentialValueString);
		formInstanceRequest.getValues().add(valueConfidential);
		IdmFormValueDto valueInt = new IdmFormValueDto(attributeInt);
		valueInt.setValue(111);
		formInstanceRequest.getValues().add(valueInt);
		formDefinition = formService.getDefinition(IdmRoleDto.class);

		requestManager.saveFormInstance(request.getId(), role, formDefinition, formInstanceRequest.getValues());
		formInstanceRequest = requestManager.getFormInstance(request.getId(), role, formDefinition);
		// Four values in request
		Assert.assertEquals(4, formInstanceRequest.getValues().size());
		formInstance = formService.getFormInstance(role);
		// None values via standard service
		Assert.assertEquals(0, formInstance.getValues().size());

		long numberOfNewValue = formInstanceRequest.getValues().stream()
				.filter(value -> RequestOperationType.ADD == DtoUtils
						.getEmbedded(value, Requestable.REQUEST_ITEM_FIELD, IdmRequestItemDto.class).getOperation())
				.count();
		Assert.assertEquals(4, numberOfNewValue);

		// Check confidential value in request
		IdmFormValueDto confidentialValueDto = formInstanceRequest.getValues().stream()
				.filter(value -> valueConfidential.getFormAttribute().equals(value.getFormAttribute())).findFirst()
				.get();
		// Cannot be same, because this value is confidential
		Assert.assertNotEquals(confidentialValueString, confidentialValueDto.getValue());

		request = requestManager.startRequest(request.getId(), true);
		Assert.assertEquals(RequestState.EXECUTED, request.getState());
		formInstance = formService.getFormInstance(role);
		// Four values via standard service
		Assert.assertEquals(4, formInstance.getValues().size());

		// Check short text
		long countShortText = formInstance.getValues().stream()
				.filter(value -> valueShortText.getFormAttribute().equals(value.getFormAttribute()))
				.filter(value -> valueShortText.getValue().equals(value.getValue())).count();
		Assert.assertEquals(1, countShortText);
		// Check int
		long countInt = formInstance.getValues().stream()
				.filter(value -> valueInt.getFormAttribute().equals(value.getFormAttribute()))
				.filter(value -> valueInt.getValue().equals(value.getValue())).count();
		Assert.assertEquals(1, countInt);
		// Check boolean
		long countBoolean = formInstance.getValues().stream()
				.filter(value -> valueBoolean.getFormAttribute().equals(value.getFormAttribute()))
				.filter(value -> valueBoolean.getValue().equals(value.getValue())).count();
		Assert.assertEquals(1, countBoolean);
		// Check confidential
		IdmFormValueDto confidentialValue = formInstance.getValues().stream()
				.filter(value -> valueConfidential.getFormAttribute().equals(value.getFormAttribute())).findFirst()
				.get();
		// Cannot be same, because this value is confidential
		Assert.assertNotEquals(confidentialValueString, confidentialValue.getValue());
		Serializable confidValue = formService.getConfidentialPersistentValue(confidentialValue);
		Assert.assertEquals(confidentialValueString, confidValue);

		// Delete attributes
		formService.deleteValues(role, attributeShortText);
		formService.deleteAttribute(attributeShortText);
		formService.deleteValues(role, attributeBoolean);
		formService.deleteAttribute(attributeBoolean);
		formService.deleteValues(role, attributeConfidential);
		formService.deleteAttribute(attributeConfidential);
		formService.deleteValues(role, attributeInt);
		formService.deleteAttribute(attributeInt);

	}

	@Test
	public void testGetChangesEAV() {

		// Create role
		IdmRoleDto role = getHelper().createRole();
		// Get definition
		IdmFormInstanceDto formInstance = formService.getFormInstance(role);
		// None values yet
		Assert.assertEquals(0, formInstance.getValues().size());
		IdmFormDefinitionDto formDefinition = formInstance.getFormDefinition();

		// Create form attributes
		IdmFormAttributeDto attributeShortText = new IdmFormAttributeDto();
		attributeShortText.setCode(getHelper().createName());
		attributeShortText.setName(attributeShortText.getCode());
		attributeShortText.setPersistentType(PersistentType.SHORTTEXT);
		attributeShortText.setFormDefinition(formDefinition.getId());
		attributeShortText = formService.saveAttribute(attributeShortText);

		IdmFormAttributeDto attributeBoolean = new IdmFormAttributeDto();
		attributeBoolean.setCode(getHelper().createName());
		attributeBoolean.setName(attributeBoolean.getCode());
		attributeBoolean.setPersistentType(PersistentType.BOOLEAN);
		attributeBoolean.setFormDefinition(formDefinition.getId());
		attributeBoolean = formService.saveAttribute(attributeBoolean);

		IdmFormAttributeDto attributeConfidential = new IdmFormAttributeDto();
		attributeConfidential.setCode(getHelper().createName());
		attributeConfidential.setName(attributeConfidential.getCode());
		attributeConfidential.setPersistentType(PersistentType.SHORTTEXT);
		attributeConfidential.setConfidential(true);
		attributeConfidential.setFormDefinition(formDefinition.getId());
		attributeConfidential = formService.saveAttribute(attributeConfidential);

		IdmFormAttributeDto attributeInt = new IdmFormAttributeDto();
		attributeInt.setCode(getHelper().createName());
		attributeInt.setName(attributeInt.getCode());
		attributeInt.setPersistentType(PersistentType.INT);
		attributeInt.setFormDefinition(formDefinition.getId());
		attributeInt = formService.saveAttribute(attributeInt);

		// Create request
		IdmRequestDto request = requestManager.createRequest(role);
		Assert.assertNotNull(request);
		IdmFormInstanceDto formInstanceRequest = requestManager.getFormInstance(request.getId(), role, formDefinition);
		// None values yet
		Assert.assertEquals(0, formInstanceRequest.getValues().size());

		IdmFormValueDto valueShortText = new IdmFormValueDto(attributeShortText);
		valueShortText.setValue(getHelper().createName());
		formInstanceRequest.getValues().add(valueShortText);
		IdmFormValueDto valueBoolean = new IdmFormValueDto(attributeBoolean);
		valueBoolean.setValue(true);
		formInstanceRequest.getValues().add(valueBoolean);
		IdmFormValueDto valueConfidential = new IdmFormValueDto(attributeConfidential);
		String confidentialValueString = getHelper().createName();
		valueConfidential.setValue(confidentialValueString);
		formInstanceRequest.getValues().add(valueConfidential);
		IdmFormValueDto valueInt = new IdmFormValueDto(attributeInt);
		valueInt.setValue(111);
		formInstanceRequest.getValues().add(valueInt);
		formDefinition = formService.getDefinition(IdmRoleDto.class);

		requestManager.saveFormInstance(request.getId(), role, formDefinition, formInstanceRequest.getValues());
		formInstanceRequest = requestManager.getFormInstance(request.getId(), role, formDefinition);
		// Four values in request
		Assert.assertEquals(4, formInstanceRequest.getValues().size());
		formInstance = formService.getFormInstance(role);
		// None values via standard service
		Assert.assertEquals(0, formInstance.getValues().size());

		formInstanceRequest.getValues().forEach(value -> {
			IdmRequestItemDto item = DtoUtils.getEmbedded(value, Requestable.REQUEST_ITEM_FIELD,
					IdmRequestItemDto.class);
			IdmRequestItemChangesDto changes = requestManager.getChanges(item);
			Assert.assertNotNull(changes);
			List<IdmRequestItemAttributeDto> attributes = changes.getAttributes();
			attributes.forEach(attribute -> {
				Assert.assertEquals(RequestOperationType.ADD, attribute.getValue().getChange());
			});
			IdmRequestItemAttributeDto attributeDto = attributes.stream()
					.filter(attribute -> "stringValue".equals(attribute.getName())).findFirst().get();
			Assert.assertEquals(value.getStringValue(), attributeDto.getValue().getValue());
			attributeDto = attributes.stream().filter(attribute -> "booleanValue".equals(attribute.getName()))
					.findFirst().get();
			Assert.assertEquals(value.getBooleanValue(), attributeDto.getValue().getValue());
			attributeDto = attributes.stream().filter(attribute -> "doubleValue".equals(attribute.getName()))
					.findFirst().get();
			Assert.assertEquals(value.getDoubleValue(), attributeDto.getValue().getValue());
			attributeDto = attributes.stream().filter(attribute -> "longValue".equals(attribute.getName())).findFirst()
					.get();
			Assert.assertEquals(value.getLongValue(), attributeDto.getValue().getValue());
			attributeDto = attributes.stream().filter(attribute -> "shortTextValue".equals(attribute.getName()))
					.findFirst().get();
			Assert.assertEquals(value.getShortTextValue(), attributeDto.getValue().getValue());

		});

		request = requestManager.startRequest(request.getId(), true);
		Assert.assertEquals(RequestState.EXECUTED, request.getState());
		formInstance = formService.getFormInstance(role);
		// Four values via standard service
		Assert.assertEquals(4, formInstance.getValues().size());

		// All changes was applied, check on none changes
		formInstanceRequest = requestManager.getFormInstance(request.getId(), role, formDefinition);
		formInstanceRequest.getValues().forEach(value -> {
			IdmRequestItemDto item = DtoUtils.getEmbedded(value, Requestable.REQUEST_ITEM_FIELD,
					IdmRequestItemDto.class);
			IdmRequestItemChangesDto changes = requestManager.getChanges(item);
			Assert.assertNotNull(changes);
			List<IdmRequestItemAttributeDto> attributes = changes.getAttributes();
			attributes.forEach(attribute -> {
				Assert.assertEquals(attribute.getValue().getOldValue(), attribute.getValue().getValue());
			});
		});

		// Make changes
		final UUID attributeShortTextId = attributeShortText.getId();
		IdmFormValueDto changedValueShortText = new IdmFormValueDto(attributeShortText);
		changedValueShortText.setValue(getHelper().createName());
		// Create new request
		IdmRequestDto requestChange = requestManager.createRequest(role);
		Assert.assertNotNull(requestChange);
		// Create request items
		requestManager.saveFormInstance(requestChange.getId(), role, formDefinition,
				Lists.newArrayList(changedValueShortText));
		formInstanceRequest = requestManager.getFormInstance(requestChange.getId(), role, formDefinition);
		// One change in the request
		Assert.assertEquals(4, formInstanceRequest.getValues().size());
		IdmFormValueDto changedValueShortTextRequest = formInstanceRequest.getValues().stream()
				.filter(value -> value.getFormAttribute().equals(attributeShortTextId)).findFirst().get();
		IdmRequestItemDto item = DtoUtils.getEmbedded(changedValueShortTextRequest, Requestable.REQUEST_ITEM_FIELD,
				IdmRequestItemDto.class);
		IdmRequestItemChangesDto changes = requestManager.getChanges(item);
		Assert.assertNotNull(changes);
		List<IdmRequestItemAttributeDto> attributes = changes.getAttributes();
		IdmRequestItemAttributeDto attributeDto = attributes.stream()
				.filter(attribute -> "stringValue".equals(attribute.getName())).findFirst().get();
		Assert.assertNotEquals(attributeDto.getValue().getOldValue(), attributeDto.getValue().getValue());
		Assert.assertEquals(changedValueShortText.getStringValue(), attributeDto.getValue().getValue());

		// Delete attributes
		formService.deleteValues(role, attributeShortText);
		formService.deleteAttribute(attributeShortText);
		formService.deleteValues(role, attributeBoolean);
		formService.deleteAttribute(attributeBoolean);
		formService.deleteValues(role, attributeConfidential);
		formService.deleteAttribute(attributeConfidential);
		formService.deleteValues(role, attributeInt);
		formService.deleteAttribute(attributeInt);

	}

	/**
	 * Complete all tasks from user given in parameters. Complete will be done by
	 * currently logged user.
	 * 
	 * @param approverUser
	 * @param decision
	 */
	private void completeTasksFromUsers(String approverUser, String decision) {
		WorkflowFilterDto taskFilter = new WorkflowFilterDto();
		taskFilter.setCandidateOrAssigned(approverUser);
		List<WorkflowTaskInstanceDto> tasks = workflowTaskInstanceService.find(taskFilter, null).getContent();
		//
		for (WorkflowTaskInstanceDto task : tasks) {
			workflowTaskInstanceService.completeTask(task.getId(), decision);
		}
	}
}
