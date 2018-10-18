package eu.bcvsolutions.idm.acc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.acc.TestHelper;
import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordPolicyService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Password pre validation integration test
 * 
 * TODO: make DefaultIdmPasswordPolicyService constants protected / or public and use them here instead hard coded strings.
 * 
 * @author Patrik Stloukal
 *
 */
public class PasswordPreValidationIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private IdmPasswordPolicyService passwordPolicyService;
	@Autowired
	private IdmIdentityService idmIdentityService;
	@Autowired
	private TestHelper testHelper;
	@Autowired
	private AccAccountService accountService;
	@Autowired
	private AccIdentityAccountService accountIdentityService;
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
	public void testLenght() {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("test" + System.currentTimeMillis());
		identity.setFirstName("testFirst");
		identity.setLastName("testSecond");
		identity = idmIdentityService.save(identity);
		//
		SysSystemDto system = testHelper.createTestResourceSystem(true);
		//
		AccAccountDto acc = new AccAccountDto();
		acc.setId(UUID.randomUUID());
		acc.setUid(System.currentTimeMillis() + "");
		acc.setAccountType(AccountType.PERSONAL);
		acc.setSystem(system.getId());
		//
		acc = accountService.save(acc);
		//
		AccIdentityAccountDto account = testHelper.createIdentityAccount(system, identity);
		account.setAccount(acc.getId());
		account.setOwnership(true);
		account = accountIdentityService.save(account);
		List<String> accounts = new ArrayList<String>();
		accounts.add(acc.getId() + "");
		// password policy default
		IdmPasswordPolicyDto policyDefault = new IdmPasswordPolicyDto();
		policyDefault.setName(System.currentTimeMillis() + "test1");
		policyDefault.setDefaultPolicy(true);
		policyDefault.setMinPasswordLength(5);
		policyDefault.setMaxPasswordLength(10);
		// password policy
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName(System.currentTimeMillis() + "test2");
		policy.setDefaultPolicy(false);
		policy.setMinPasswordLength(6);
		policy.setMaxPasswordLength(11);

		policyDefault = passwordPolicyService.save(policyDefault);
		policy = passwordPolicyService.save(policy);
		system.setPasswordPolicyValidate(policy.getId());
		systemService.save(system);

		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAccounts(accounts);
		passwordChange.setAll(true);

		try {
			idmIdentityService.validatePassword(passwordChange);
		} catch (ResultCodeException ex) {
			assertEquals(6, ex.getError().getError().getParameters().get("minLength"));
			assertEquals(10, ex.getError().getError().getParameters().get("maxLength"));

			assertEquals(2, ex.getError().getError().getParameters().size());
			policyDefault.setDefaultPolicy(false);
			passwordPolicyService.save(policyDefault);

		}
	}

	@Test
	public void testMinChar() {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("test" + System.currentTimeMillis());
		identity.setFirstName("testFirst");
		identity.setLastName("testSecond");
		identity = idmIdentityService.save(identity);
		//
		SysSystemDto system = testHelper.createTestResourceSystem(true);
		//
		AccAccountDto acc = new AccAccountDto();
		acc.setId(UUID.randomUUID());
		acc.setUid(System.currentTimeMillis() + "");
		acc.setAccountType(AccountType.PERSONAL);
		acc.setSystem(system.getId());
		//
		acc = accountService.save(acc);
		//
		AccIdentityAccountDto account = testHelper.createIdentityAccount(system, identity);
		account.setAccount(acc.getId());
		account = accountIdentityService.save(account);
		account.setOwnership(true);
		List<String> accounts = new ArrayList<String>();
		accounts.add(acc.getId() + "");
		// password policy default
		IdmPasswordPolicyDto policyDefault = new IdmPasswordPolicyDto();
		policyDefault.setName(System.currentTimeMillis() + "test1");
		policyDefault.setDefaultPolicy(true);
		policyDefault.setMinUpperChar(6);
		policyDefault.setMinLowerChar(10);
		// password policy
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName(System.currentTimeMillis() + "test2");
		policy.setDefaultPolicy(false);
		policy.setMinUpperChar(5);
		policy.setMinLowerChar(11);

		policyDefault = passwordPolicyService.save(policyDefault);
		policy = passwordPolicyService.save(policy);
		system.setPasswordPolicyValidate(policy.getId());
		systemService.save(system);

		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAccounts(accounts);
		passwordChange.setAll(true);

		try {
			idmIdentityService.validatePassword(passwordChange);
		} catch (ResultCodeException ex) {
			assertEquals(6, ex.getError().getError().getParameters().get("minUpperChar"));
			assertEquals(11, ex.getError().getError().getParameters().get("minLowerChar"));

			assertEquals(2, ex.getError().getError().getParameters().size());
			policyDefault.setDefaultPolicy(false);
			passwordPolicyService.save(policyDefault);
		}
	}

	@Test
	public void testNumberSpecialChar() {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("test" + System.currentTimeMillis());
		identity.setFirstName("testFirst");
		identity.setLastName("testSecond");
		identity = idmIdentityService.save(identity);
		//
		SysSystemDto system = testHelper.createTestResourceSystem(true);
		//
		AccAccountDto acc = new AccAccountDto();
		acc.setId(UUID.randomUUID());
		acc.setUid(System.currentTimeMillis() + "");
		acc.setAccountType(AccountType.PERSONAL);
		acc.setSystem(system.getId());
		//
		acc = accountService.save(acc);
		//
		AccIdentityAccountDto account = testHelper.createIdentityAccount(system, identity);
		account.setAccount(acc.getId());
		account = accountIdentityService.save(account);
		account.setOwnership(true);
		List<String> accounts = new ArrayList<String>();
		accounts.add(acc.getId() + "");
		// password policy default
		IdmPasswordPolicyDto policyDefault = new IdmPasswordPolicyDto();
		policyDefault.setName(System.currentTimeMillis() + "test1");
		policyDefault.setDefaultPolicy(true);
		policyDefault.setMinNumber(6);
		policyDefault.setMinSpecialChar(10);
		// password policy
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName(System.currentTimeMillis() + "test2");
		policy.setDefaultPolicy(false);
		policy.setMinNumber(5);
		policy.setMinSpecialChar(11);

		policyDefault = passwordPolicyService.save(policyDefault);
		policy = passwordPolicyService.save(policy);
		system.setPasswordPolicyValidate(policy.getId());
		systemService.save(system);

		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAccounts(accounts);
		passwordChange.setAll(true);

		try {
			idmIdentityService.validatePassword(passwordChange);
		} catch (ResultCodeException ex) {
			assertEquals(6, ex.getError().getError().getParameters().get("minNumber"));
			assertEquals(11, ex.getError().getError().getParameters().get("minSpecialChar"));

			assertFalse(ex.getError().getError().getParameters().get("specialCharacterBase") == null);
			assertEquals(3, ex.getError().getError().getParameters().size());
			policyDefault.setDefaultPolicy(false);
			passwordPolicyService.save(policyDefault);
		}
	}

	@Test
	public void testAdvancedEnabled() {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("test" + System.currentTimeMillis());
		identity.setFirstName("testFirst");
		identity.setLastName("testSecond");
		identity = idmIdentityService.save(identity);
		//
		SysSystemDto system = testHelper.createTestResourceSystem(true);
		//
		AccAccountDto acc = new AccAccountDto();
		acc.setId(UUID.randomUUID());
		acc.setUid(System.currentTimeMillis() + "");
		acc.setAccountType(AccountType.PERSONAL);
		acc.setSystem(system.getId());
		//
		acc = accountService.save(acc);
		//
		AccIdentityAccountDto account = testHelper.createIdentityAccount(system, identity);
		account.setAccount(acc.getId());
		account = accountIdentityService.save(account);
		account.setOwnership(true);
		List<String> accounts = new ArrayList<String>();
		accounts.add(acc.getId() + "");
		// password policy default
		IdmPasswordPolicyDto policyDefault = new IdmPasswordPolicyDto();
		policyDefault.setName(System.currentTimeMillis() + "test1");
		policyDefault.setDefaultPolicy(true);
		policyDefault.setMinPasswordLength(10);
		policyDefault.setMaxPasswordLength(20);
		policyDefault.setPasswordLengthRequired(true);
		policyDefault.setMinUpperChar(5);
		policyDefault.setUpperCharRequired(true);
		policyDefault.setMinLowerChar(4);
		policyDefault.setLowerCharRequired(true);
		policyDefault.setEnchancedControl(true);
		policyDefault.setMinRulesToFulfill(1);
		policyDefault.setMinNumber(3);
		policyDefault.setNumberRequired(false);
		policyDefault.setMinSpecialChar(6);
		policyDefault.setSpecialCharRequired(false);
		policyDefault.setIdentityAttributeCheck("");
		// password policy
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName(System.currentTimeMillis() + "test2");
		policy.setDefaultPolicy(false);
		policy.setMinPasswordLength(9);
		policy.setMaxPasswordLength(21);
		policy.setPasswordLengthRequired(true);
		policy.setMinUpperChar(4);
		policy.setUpperCharRequired(true);
		policy.setMinLowerChar(3);
		policy.setLowerCharRequired(true);
		policy.setEnchancedControl(true);
		policy.setMinRulesToFulfill(1);
		policy.setMinNumber(5);
		policy.setNumberRequired(false);
		policy.setMinSpecialChar(4);
		policy.setSpecialCharRequired(false);
		policy.setIdentityAttributeCheck("");
		policy.setProhibitedCharacters("Test");

		policyDefault = passwordPolicyService.save(policyDefault);
		policy = passwordPolicyService.save(policy);
		system.setPasswordPolicyValidate(policy.getId());
		systemService.save(system);

		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAccounts(accounts);
		passwordChange.setAll(true);

		try {
			idmIdentityService.validatePassword(passwordChange);
		} catch (ResultCodeException ex) {
			Map<String, Object> parametrs = new HashMap<String, Object>();
			parametrs.put("minNumber", 3);
			parametrs.put("minSpecialChar", 6);
			assertEquals(10, ex.getError().getError().getParameters().get("minLength"));
			assertEquals(20, ex.getError().getError().getParameters().get("maxLength"));
			assertEquals(5, ex.getError().getError().getParameters().get("minUpperChar"));
			assertEquals(4, ex.getError().getError().getParameters().get("minLowerChar"));
			assertEquals(parametrs.toString(),
					ex.getError().getError().getParameters().get("minRulesToFulfill").toString());
			//test forbidden chars
			assertEquals("Test", ((Map<String, String>)ex.getError().getError().getParameters().get("forbiddenCharacterBase")).get(policy.getName()));
			// special char base -> 8
			assertEquals(8, ex.getError().getError().getParameters().size());
			policyDefault.setDefaultPolicy(false);
			passwordPolicyService.save(policyDefault);
		}
	}

	@Test
	public void testAdvancedEnabledSimilarAttributes() {
		IdmIdentityDto identity = new IdmIdentityDto();
		identity.setUsername("test" + System.currentTimeMillis());
		identity.setFirstName("testFirst");
		identity.setLastName("testSecond");
		identity = idmIdentityService.save(identity);
		//
		SysSystemDto system = testHelper.createTestResourceSystem(true);
		//
		AccAccountDto acc = new AccAccountDto();
		acc.setId(UUID.randomUUID());
		acc.setUid(System.currentTimeMillis() + "");
		acc.setAccountType(AccountType.PERSONAL);
		acc.setSystem(system.getId());
		//
		acc = accountService.save(acc);
		//
		AccIdentityAccountDto account = testHelper.createIdentityAccount(system, identity);
		account.setAccount(acc.getId());
		account = accountIdentityService.save(account);
		account.setOwnership(true);
		List<String> accounts = new ArrayList<String>();
		accounts.add(acc.getId() + "");
		// password policy default
		IdmPasswordPolicyDto policyDefault = new IdmPasswordPolicyDto();
		policyDefault.setName(System.currentTimeMillis() + "test1");
		policyDefault.setDefaultPolicy(true);
		policyDefault.setMinPasswordLength(10);
		policyDefault.setMaxPasswordLength(20);
		policyDefault.setPasswordLengthRequired(true);
		policyDefault.setMinUpperChar(5);
		policyDefault.setUpperCharRequired(true);
		policyDefault.setMinLowerChar(4);
		policyDefault.setLowerCharRequired(true);
		policyDefault.setEnchancedControl(true);
		policyDefault.setMinRulesToFulfill(1);
		policyDefault.setMinNumber(3);
		policyDefault.setNumberRequired(false);
		policyDefault.setMinSpecialChar(4);
		policyDefault.setSpecialCharRequired(false);
		policyDefault.setIdentityAttributeCheck("EMAIL, FIRSTNAME");
		// password policy
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName(System.currentTimeMillis() + "test2");
		policy.setDefaultPolicy(false);
		policy.setMinPasswordLength(9);
		policy.setMaxPasswordLength(21);
		policy.setPasswordLengthRequired(true);
		policy.setMinUpperChar(4);
		policy.setUpperCharRequired(true);
		policy.setMinLowerChar(3);
		policy.setLowerCharRequired(true);
		policy.setEnchancedControl(true);
		policy.setMinRulesToFulfill(1);
		policy.setMinNumber(5);
		policy.setNumberRequired(false);
		policy.setMinSpecialChar(2);
		policy.setSpecialCharRequired(false);
		policy.setIdentityAttributeCheck("USERNAME");

		policyDefault = passwordPolicyService.save(policyDefault);
		policy = passwordPolicyService.save(policy);
		system.setPasswordPolicyValidate(policy.getId());
		systemService.save(system);

		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAccounts(accounts);
		passwordChange.setAll(true);

		try {
			idmIdentityService.validatePassword(passwordChange);
		} catch (ResultCodeException ex) {
			Map<String, Object> parametrs = new HashMap<String, Object>();
			parametrs.put("minNumber", 3);
			parametrs.put("minSpecialChar", 4);
			assertEquals(10, ex.getError().getError().getParameters().get("minLength"));
			assertEquals(20, ex.getError().getError().getParameters().get("maxLength"));
			assertEquals(5, ex.getError().getError().getParameters().get("minUpperChar"));
			assertEquals(4, ex.getError().getError().getParameters().get("minLowerChar"));
			assertEquals(parametrs.toString(),
					ex.getError().getError().getParameters().get("minRulesToFulfill").toString());
			// special char base, passwordSimilarUsername, passwordSimilarLastName,
			// passwordSimilarEmail -> 11, policy's name erased -> 10
			assertEquals(10, ex.getError().getError().getParameters().size());
			policyDefault.setDefaultPolicy(false);
			passwordPolicyService.save(policyDefault);
		}
	}

}
