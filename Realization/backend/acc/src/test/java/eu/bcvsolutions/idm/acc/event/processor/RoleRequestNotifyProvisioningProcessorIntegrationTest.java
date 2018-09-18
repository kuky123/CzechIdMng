package eu.bcvsolutions.idm.acc.event.processor;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningArchiveDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningArchiveService;
import eu.bcvsolutions.idm.core.api.config.domain.EventConfiguration;
import eu.bcvsolutions.idm.core.api.domain.ConceptRoleRequestOperation;
import eu.bcvsolutions.idm.core.api.domain.RoleRequestedByType;
import eu.bcvsolutions.idm.core.api.dto.IdmConceptRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityEventFilter;
import eu.bcvsolutions.idm.core.api.service.IdmConceptRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmEntityEventService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Execute role request
 * - one provisioning operation schould be ececuted
 * - prevent to drop and create target account, if one assigned role is deleted 
 * 
 * @author Radek Tomiška
 *
 */
public class RoleRequestNotifyProvisioningProcessorIntegrationTest extends AbstractIntegrationTest{

	@Autowired private IdmIdentityRoleService identityRoleService;
	@Autowired private SysProvisioningArchiveService provisioningArchiveService;
	@Autowired private IdmRoleRequestService roleRequestService;
	@Autowired private IdmConceptRoleRequestService conceptRoleRequestService;
	@Autowired private AccAccountService accountService;
	@Autowired private IdmEntityEventService entityEventService;
	
	@Test
	public void testAssignSubRolesByRequestAsync() {
		try {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, true);
			// prepare role composition
			IdmRoleDto superior = getHelper().createRole();
			IdmRoleDto subOne = getHelper().createRole();
			IdmRoleDto subTwo = getHelper().createRole();
			IdmRoleDto subOneSub = getHelper().createRole();
			IdmRoleDto subOneSubSub = getHelper().createRole();
			getHelper().createRoleComposition(superior, subOne);
			getHelper().createRoleComposition(superior, subTwo);
			getHelper().createRoleComposition(subOne, subOneSub);
			getHelper().createRoleComposition(subOneSub, subOneSubSub);
			//
			IdmRoleDto other = getHelper().createRole();
			IdmRoleDto otherOne = getHelper().createRole();
			getHelper().createRoleComposition(other, otherOne);
			//
			// create test system with mapping and link her to the sub roles
			SysSystemDto system = getHelper().createTestResourceSystem(true);
			getHelper().createRoleSystem(subOneSubSub, system);
			getHelper().createRoleSystem(otherOne, system);
			//
			// assign superior role
			IdmIdentityDto identity = getHelper().createIdentity();
			//
			final IdmRoleRequestDto roleRequestOne = getHelper().createRoleRequest(identity, superior);
			//
			getHelper().executeRequest(roleRequestOne, false);
			//
			// wait for executed events
			final IdmEntityEventFilter eventFilter = new IdmEntityEventFilter();
			eventFilter.setOwnerId(roleRequestOne.getId());
			getHelper().waitForResult(res -> {
				return entityEventService.find(eventFilter, new PageRequest(0, 1)).getTotalElements() != 0;
			}, 1000, Integer.MAX_VALUE);
			//
			// check after create
			List<IdmIdentityRoleDto> assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
			Assert.assertEquals(5, assignedRoles.size());
			IdmIdentityRoleDto directRole = assignedRoles.stream().filter(ir -> ir.getDirectRole() == null).findFirst().get();
			Assert.assertEquals(superior.getId(), directRole.getRole());
			//
			// check created account
			AccAccountDto account = accountService.getAccount(identity.getUsername(), system.getId());
			Assert.assertNotNull(account);
			Assert.assertNotNull(getHelper().findResource(account.getRealUid()));
			//
			// check provisioning archive
			SysProvisioningOperationFilter archiveFilter = new SysProvisioningOperationFilter();
			archiveFilter.setEntityIdentifier(identity.getId());
			//
			// TODO: add better waiting for child events + count of all sub events?
//			List<SysProvisioningArchiveDto> executedOperations = provisioningArchiveService.find(archiveFilter, null).getContent();
//			Assert.assertEquals(1, executedOperations.size());
//			Assert.assertTrue(executedOperations.stream().anyMatch(o -> o.getOperationType() == ProvisioningEventType.CREATE));
			//
			// remove one role and add other
			IdmIdentityContractDto contract = getHelper().getPrimeContract(identity.getId());
			final IdmRoleRequestDto roleRequest = new IdmRoleRequestDto();
			roleRequest.setApplicant(identity.getId());
			roleRequest.setRequestedByType(RoleRequestedByType.MANUALLY);
			roleRequest.setExecuteImmediately(true);
			final IdmRoleRequestDto roleRequestTwo = roleRequestService.save(roleRequest);
			// remove
			IdmConceptRoleRequestDto conceptRoleRequest = new IdmConceptRoleRequestDto();
			conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
			conceptRoleRequest.setIdentityContract(contract.getId());
			conceptRoleRequest.setRole(superior.getId());
			conceptRoleRequest.setIdentityRole(directRole.getId());
			conceptRoleRequest.setOperation(ConceptRoleRequestOperation.REMOVE);
			conceptRoleRequestService.save(conceptRoleRequest);
			// add
			conceptRoleRequest = new IdmConceptRoleRequestDto();
			conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
			conceptRoleRequest.setIdentityContract(contract.getId());
			conceptRoleRequest.setRole(other.getId());
			conceptRoleRequest.setOperation(ConceptRoleRequestOperation.ADD);
			conceptRoleRequestService.save(conceptRoleRequest);
			// execute
			getHelper().executeRequest(roleRequestTwo, false);	
			//
			// wait for executed events
			eventFilter.setOwnerId(roleRequestTwo.getId());
			getHelper().waitForResult(res -> {
				return entityEventService.find(eventFilter, new PageRequest(0, 1)).getTotalElements() != 0;
			}, 1000, Integer.MAX_VALUE);
			//
			// check after role request is executed
			assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
			Assert.assertEquals(2, assignedRoles.size());
			//
			// check updated account
			AccAccountDto updatedAccount = accountService.getAccount(identity.getUsername(), system.getId());
			Assert.assertNotNull(updatedAccount);
			Assert.assertNotNull(getHelper().findResource(updatedAccount.getRealUid()));
			Assert.assertEquals(account.getCreated(), updatedAccount.getCreated());
			Assert.assertEquals(account.getRealUid(), updatedAccount.getRealUid());
			//
			// check provisioning archive
//			executedOperations = provisioningArchiveService.find(archiveFilter, null).getContent();
//			Assert.assertEquals(2, executedOperations.size());
//			Assert.assertTrue(executedOperations.stream().anyMatch(o -> o.getOperationType() == ProvisioningEventType.CREATE));
//			Assert.assertTrue(executedOperations.stream().anyMatch(o -> o.getOperationType() == ProvisioningEventType.UPDATE));
		} finally {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	/**
	 * Backward compatibily - request executed synchronously works the same as async with one exception
	 *  - account management is executed for every identity role => added identity roles are executed before deletions 
	 *  => prevent to remove account from target system is implemented this way
	 */
	@Test
	public void testAssignSubRolesByRequestSync() {
		// prepare role composition
		IdmRoleDto superior = getHelper().createRole();
		IdmRoleDto subOne = getHelper().createRole();
		IdmRoleDto subTwo = getHelper().createRole();
		IdmRoleDto subOneSub = getHelper().createRole();
		IdmRoleDto subOneSubSub = getHelper().createRole();
		getHelper().createRoleComposition(superior, subOne);
		getHelper().createRoleComposition(superior, subTwo);
		getHelper().createRoleComposition(subOne, subOneSub);
		getHelper().createRoleComposition(subOneSub, subOneSubSub);
		//
		IdmRoleDto other = getHelper().createRole();
		IdmRoleDto otherOne = getHelper().createRole();
		getHelper().createRoleComposition(other, otherOne);
		//
		// create test system with mapping and link her to the sub roles
		SysSystemDto system = getHelper().createTestResourceSystem(true);
		getHelper().createRoleSystem(subOneSubSub, system);
		getHelper().createRoleSystem(otherOne, system);
		//
		// assign superior role
		IdmIdentityDto identity = getHelper().createIdentity();
		//
		final IdmRoleRequestDto roleRequestOne = getHelper().createRoleRequest(identity, superior);
		//
		getHelper().executeRequest(roleRequestOne, false);
		//
		// check after create
		List<IdmIdentityRoleDto> assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
		Assert.assertEquals(5, assignedRoles.size());
		IdmIdentityRoleDto directRole = assignedRoles.stream().filter(ir -> ir.getDirectRole() == null).findFirst().get();
		Assert.assertEquals(superior.getId(), directRole.getRole());
		//
		// check created account
		AccAccountDto account = accountService.getAccount(identity.getUsername(), system.getId());
		Assert.assertNotNull(account);
		Assert.assertNotNull(getHelper().findResource(account.getRealUid()));
		//
		// check provisioning archive
		SysProvisioningOperationFilter archiveFilter = new SysProvisioningOperationFilter();
		archiveFilter.setEntityIdentifier(identity.getId());
		//
		List<SysProvisioningArchiveDto> executedOperations = provisioningArchiveService.find(archiveFilter, null).getContent();
		Assert.assertEquals(1, executedOperations.size());
		Assert.assertTrue(executedOperations.stream().anyMatch(o -> o.getOperationType() == ProvisioningEventType.CREATE));
		//
		// remove one role and add other
		IdmIdentityContractDto contract = getHelper().getPrimeContract(identity.getId());
		final IdmRoleRequestDto roleRequest = new IdmRoleRequestDto();
		roleRequest.setApplicant(identity.getId());
		roleRequest.setRequestedByType(RoleRequestedByType.MANUALLY);
		roleRequest.setExecuteImmediately(true);
		final IdmRoleRequestDto roleRequestTwo = roleRequestService.save(roleRequest);
		// remove
		IdmConceptRoleRequestDto conceptRoleRequest = new IdmConceptRoleRequestDto();
		conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
		conceptRoleRequest.setIdentityContract(contract.getId());
		conceptRoleRequest.setRole(superior.getId());
		conceptRoleRequest.setIdentityRole(directRole.getId());
		conceptRoleRequest.setOperation(ConceptRoleRequestOperation.REMOVE);
		conceptRoleRequestService.save(conceptRoleRequest);
		// add
		conceptRoleRequest = new IdmConceptRoleRequestDto();
		conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
		conceptRoleRequest.setIdentityContract(contract.getId());
		conceptRoleRequest.setRole(other.getId());
		conceptRoleRequest.setOperation(ConceptRoleRequestOperation.ADD);
		conceptRoleRequestService.save(conceptRoleRequest);
		// execute
		getHelper().executeRequest(roleRequestTwo, false);	
		//
		// check after role request is executed
		assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
		Assert.assertEquals(2, assignedRoles.size());
		//
		// check updated account
		AccAccountDto updatedAccount = accountService.getAccount(identity.getUsername(), system.getId());
		Assert.assertNotNull(updatedAccount);
		Assert.assertNotNull(getHelper().findResource(updatedAccount.getRealUid()));
		Assert.assertEquals(account.getCreated(), updatedAccount.getCreated());
		Assert.assertEquals(account.getRealUid(), updatedAccount.getRealUid());
	}
	
	/**
	 * Request will be executed immediate even if asynchronous events are enabled
	 */
	@Test
	public void testAssignSubRolesByRequestImmediate() {
		try {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, true);
			// prepare role composition
			IdmRoleDto superior = getHelper().createRole();
			IdmRoleDto subOne = getHelper().createRole();
			IdmRoleDto subTwo = getHelper().createRole();
			IdmRoleDto subOneSub = getHelper().createRole();
			IdmRoleDto subOneSubSub = getHelper().createRole();
			getHelper().createRoleComposition(superior, subOne);
			getHelper().createRoleComposition(superior, subTwo);
			getHelper().createRoleComposition(subOne, subOneSub);
			getHelper().createRoleComposition(subOneSub, subOneSubSub);
			//
			IdmRoleDto other = getHelper().createRole();
			IdmRoleDto otherOne = getHelper().createRole();
			getHelper().createRoleComposition(other, otherOne);
			//
			// create test system with mapping and link her to the sub roles
			SysSystemDto system = getHelper().createTestResourceSystem(true);
			getHelper().createRoleSystem(subOneSubSub, system);
			getHelper().createRoleSystem(otherOne, system);
			//
			// assign superior role
			IdmIdentityDto identity = getHelper().createIdentity();
			//
			final IdmRoleRequestDto roleRequestOne = getHelper().createRoleRequest(identity, superior);
			//
			getHelper().executeRequest(roleRequestOne, false, true);
			//
			// check after create
			List<IdmIdentityRoleDto> assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
			Assert.assertEquals(5, assignedRoles.size());
			IdmIdentityRoleDto directRole = assignedRoles.stream().filter(ir -> ir.getDirectRole() == null).findFirst().get();
			Assert.assertEquals(superior.getId(), directRole.getRole());
			//
			// check created account
			AccAccountDto account = accountService.getAccount(identity.getUsername(), system.getId());
			Assert.assertNotNull(account);
			Assert.assertNotNull(getHelper().findResource(account.getRealUid()));
			//
			// check provisioning archive
			SysProvisioningOperationFilter archiveFilter = new SysProvisioningOperationFilter();
			archiveFilter.setEntityIdentifier(identity.getId());
			//
			List<SysProvisioningArchiveDto> executedOperations = provisioningArchiveService.find(archiveFilter, null).getContent();
			Assert.assertEquals(1, executedOperations.size());
			Assert.assertTrue(executedOperations.stream().anyMatch(o -> o.getOperationType() == ProvisioningEventType.CREATE));
			//
			// remove one role and add other
			IdmIdentityContractDto contract = getHelper().getPrimeContract(identity.getId());
			final IdmRoleRequestDto roleRequest = new IdmRoleRequestDto();
			roleRequest.setApplicant(identity.getId());
			roleRequest.setRequestedByType(RoleRequestedByType.MANUALLY);
			roleRequest.setExecuteImmediately(true);
			final IdmRoleRequestDto roleRequestTwo = roleRequestService.save(roleRequest);
			// remove
			IdmConceptRoleRequestDto conceptRoleRequest = new IdmConceptRoleRequestDto();
			conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
			conceptRoleRequest.setIdentityContract(contract.getId());
			conceptRoleRequest.setRole(superior.getId());
			conceptRoleRequest.setIdentityRole(directRole.getId());
			conceptRoleRequest.setOperation(ConceptRoleRequestOperation.REMOVE);
			conceptRoleRequestService.save(conceptRoleRequest);
			// add
			conceptRoleRequest = new IdmConceptRoleRequestDto();
			conceptRoleRequest.setRoleRequest(roleRequestTwo.getId());
			conceptRoleRequest.setIdentityContract(contract.getId());
			conceptRoleRequest.setRole(other.getId());
			conceptRoleRequest.setOperation(ConceptRoleRequestOperation.ADD);
			conceptRoleRequestService.save(conceptRoleRequest);
			// execute
			getHelper().executeRequest(roleRequestTwo, false, true);	
			//
			// check after role request is executed
			assignedRoles = identityRoleService.findAllByIdentity(identity.getId());
			Assert.assertEquals(2, assignedRoles.size());
			//
			// check updated account
			AccAccountDto updatedAccount = accountService.getAccount(identity.getUsername(), system.getId());
			Assert.assertNotNull(updatedAccount);
			Assert.assertNotNull(getHelper().findResource(updatedAccount.getRealUid()));
			Assert.assertEquals(account.getCreated(), updatedAccount.getCreated());
			Assert.assertEquals(account.getRealUid(), updatedAccount.getRealUid());
		} finally {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, false);
		}
	}
	
	protected TestHelper getHelper() {
		return (TestHelper) super.getHelper();
	}
}
