package eu.bcvsolutions.idm.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.DefaultRevisionEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import eu.bcvsolutions.idm.InitTestData;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourceWrapper;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.repository.IdmRoleRepository;
import eu.bcvsolutions.idm.core.rest.impl.IdmRoleController;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

public class RoleAuditIntegrationTest extends AbstractIntegrationTest {
	
	@Autowired
	private IdmRoleRepository roleRepository;
	
	@Autowired
	private IdmRoleController roleController;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	private IdmRole role = null;
	
	private final String testName = "test_audit role";
	
	@Before
	public void initBefore() {
		loginAsAdmin(InitTestData.TEST_ADMIN_USERNAME);
	}
	
	@After
	@Transactional
	public void deleteRole() {
		// we need to ensure "rollback" manually the same as we are starting transaction manually		
		roleRepository.delete(role);
		logout();
	}
	
	@Test
	public void testRoleController() {
		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus arg0) {
				role = constructTestRole();
				role = saveInTransaction(role, roleRepository);
				
				String nonExistRoleId = "NON_EXIST_ROLE_ID";
				
				ResponseEntity<ResourcesWrapper<ResourceWrapper<DefaultRevisionEntity>>> result = roleController.findRevisions(role.getName());
				
				assertEquals(true, result.hasBody());
				
				Exception exception = null;
				
				try {
					roleController.findRevisions(nonExistRoleId);
				} catch (ResultCodeException e) {
					exception = e;
				} catch (Exception e) {
					// do nothing
				}
				
				assertNotNull(exception);
				
				exception = null;
				
				try {
					roleController.findRevision(nonExistRoleId, Integer.MAX_VALUE);
				} catch (ResultCodeException e) {
					exception = e;
				} catch (Exception e) {
					// do nothing
				}
				
				assertNotNull(exception);
			}
		});
	}
	
	private IdmRole constructTestRole() {
		IdmRole role = new IdmRole();
		role.setName(testName);		
		return role;
	}
}