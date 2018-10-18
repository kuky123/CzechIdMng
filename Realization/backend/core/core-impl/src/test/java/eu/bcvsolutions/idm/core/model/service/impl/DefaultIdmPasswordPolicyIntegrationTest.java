package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyGenerateType;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordValidationDto;
import eu.bcvsolutions.idm.core.api.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.api.exception.ErrorModel;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordPolicyService;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;
import eu.bcvsolutions.idm.test.api.TestHelper;

/**
 * Basic test for validation and generate password by IdmPasswordPolicyService
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class DefaultIdmPasswordPolicyIntegrationTest extends AbstractIntegrationTest {
	
	private static final int ATTEMPTS = 20;
	
	@Autowired
	private IdmPasswordPolicyService passwordPolicyService;
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private TestHelper testHelper;

	@Before
	public void init() {
		loginAsAdmin();
	}
	
	@After 
	public void logout() {
		super.logout();
	}
	
	@Test
	public void testGenerateRandomPasswordLength() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_01");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMinPasswordLength(5);
		policy.setMaxPasswordLength(12);
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			assertTrue(password.length() >= 5);
			assertTrue(password.length() <= 12);
		}
	}
	
	@Test
	public void testGeneratePasshrase() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_02");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.PASSPHRASE);
		policy.setPassphraseWords(5);
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			assertEquals(5, password.split(" ").length);
		}
	}
	
	@Test
	public void testFailGenerateRandom() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_03");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(5);
		
		String password = passwordPolicyService.generatePassword(policy);
		assertTrue(password.length() <= 5);
		
		policy.setMinNumber(2);
		policy.setMinSpecialChar(2);
		policy.setMinLowerChar(2);
		try {
			password = passwordPolicyService.generatePassword(policy);
			fail("Password cant be generate");
		} catch (Exception e) {
			// nothing
		}
		
		policy.setMinLowerChar(1);
		password = passwordPolicyService.generatePassword(policy);
		assertEquals(5, password.length());
	}
	
	@Test
	public void testOnlyMinimalLength() {
		// maximal password length must be always set!!
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_04");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMinPasswordLength(20);
		policy.setMaxPasswordLength(25);
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			assertTrue(password.length() >= 20);
		}
	}
	
	@Test
	public void testOnlyMaximalLength() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_05");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(20);
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			assertTrue(password.length() <= 20);
		}
	}
	
	@Test
	public void testGenerateOnlyNumbers() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_06");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(10);
		policy.setMinPasswordLength(1);
		policy.setLowerCharBase("");
		policy.setSpecialCharBase("");
		policy.setNumberBase("0123456789");
		policy.setUpperCharBase("");
		
		for (int index = 0; index < ATTEMPTS; index++) {
			try {
				String password = passwordPolicyService.generatePassword(policy);
				Long.parseLong(password);
			} catch (Exception e) {
				fail("Password must cointains only numbers: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void testGenerateOnlyAlpha() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_07");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(10);
		policy.setMinPasswordLength(1);
		policy.setSpecialCharBase("");
		policy.setNumberBase("");
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			if (!password.matches("[a-zA-Z]+")) {
				fail("Password must cointain only aplha characters, password: " + password);
			}
			
		}
	}
	
	@Test
	public void testGenerateOnlyOneSpecial() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_08");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(1);
		policy.setMinPasswordLength(1);
		policy.setSpecialCharBase("@");
		policy.setNumberBase("");
		policy.setLowerCharBase("");
		policy.setUpperCharBase("");
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			if (!password.equals("@")) {
				fail("Password must cointain only @ character, password: " + password);
			}
			
		}
	}
	
	@Test
	public void testGenerateComplexPassword() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_09");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(10);
		policy.setMinPasswordLength(5);
		policy.setSpecialCharBase("!");
		policy.setNumberBase("123");
		
		policy.setMinNumber(3);
		policy.setMinLowerChar(2);
		policy.setMinSpecialChar(2);
		policy.setMinUpperChar(1);
		
		for (int index = 0; index < ATTEMPTS * 5; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			
			assertTrue(password.length() >= 8);
			
			assertTrue(StringUtils.countMatches(password, "!") >= 2);
			
			assertTrue(StringUtils.containsNone(password, "@#$%^&*()"));
			
			assertTrue(StringUtils.containsNone(password, "0456789"));
			
			assertTrue(password.matches(".*[A-Z].*{1,}"));
			
			assertTrue(password.matches(".*[a-z].*{2,}"));
		}
	}
	
	@Test
	public void testGenerateProhibited() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_10");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(2);
		policy.setMinPasswordLength(2);
		policy.setSpecialCharBase("");
		policy.setLowerCharBase("a");
		policy.setUpperCharBase("");
		policy.setNumberBase("123");
		policy.setProhibitedCharacters("asd2!@#%3$");
		
		for (int index = 0; index < ATTEMPTS; index++) {
			String password = passwordPolicyService.generatePassword(policy);
			
			assertTrue(password.length() == 2);
			
			assertTrue(StringUtils.countMatches(password, "1") == 2);
			
			assertTrue(StringUtils.containsNone(password, "asd2!@#%3$"));
		}
	}
	
	@Test
	public void testValidateLength() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_11");
		policy.setType(IdmPasswordPolicyType.VALIDATE);
		policy.setMaxPasswordLength(10);
		policy.setMinPasswordLength(5);
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("12345");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("123456");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("1234567");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("12345678");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("123456789");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("1234567890");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password validation length. Error message: " + e.getStackTrace().toString());
		}
		
		
		try {
			password.setPassword("1234");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation length.");
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation length.");
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("123456789123");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation length.");
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testValidateMinNumbers() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_12");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(3);
		policy.setMinPasswordLength(1);
		policy.setMinNumber(2);
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("123");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("12");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("12a");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password validation numbers. " + e.getMessage());
		}
		
		try {
			password.setPassword("1");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation numbers. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("1234");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation numbers. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("test");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation numbers. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testValidateSpecialChar() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_13");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(3);
		policy.setMinPasswordLength(1);
		policy.setMinSpecialChar(2);
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("!@");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("!@#");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("!@a");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password validation special chars. " + e.getMessage());
		}
		
		try {
			password.setPassword("!");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation special chars. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("!@#$");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation special chars. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("test");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validation special chars. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testValidationProhibitedChars() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_14");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(5);
		policy.setMinPasswordLength(1);
		policy.setProhibitedCharacters("12abcDEF!@");
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("test");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("ABde");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password validate prohibited characters. " + policy);
		}
		
		try {
			password.setPassword("tEst");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validate prohibited characters. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("eddD");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validate prohibited characters. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("5416");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validate prohibited characters. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("test!");
			this.passwordPolicyService.validate(password, policy);
			fail("Password validate prohibited characters. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testValidateBase() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_15");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(5);
		policy.setMinPasswordLength(1);
		policy.setNumberBase("123");
		policy.setMinNumber(3);
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("123");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("1234");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("111");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password base validation. " + policy);
		}
		
		try {
			password.setPassword("124");
			this.passwordPolicyService.validate(password, policy);
			fail("Password base validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("456");
			this.passwordPolicyService.validate(password, policy);
			fail("Password base validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testValidateComplex() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_16");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMaxPasswordLength(20);
		policy.setMinPasswordLength(6);
		
		policy.setMinNumber(3);
		policy.setMinLowerChar(3);
		policy.setMinSpecialChar(3);
		policy.setMinUpperChar(3);
		
		policy.setSpecialCharBase("@#");
		policy.setNumberBase("0");
		
		policy.setProhibitedCharacters("*/^mn");
		
		IdmPasswordValidationDto password = new IdmPasswordValidationDto();
		
		try {
			password.setPassword("000abc@@@DEF");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("F0a@0Ec0b@@D");
			this.passwordPolicyService.validate(password, policy);
			
			password.setPassword("#3aBb@C3A1#0c00");
			this.passwordPolicyService.validate(password, policy);
		} catch (Exception e) {
			fail("Password complex validation. " + policy);
		}
		
		try {
			password.setPassword("001abc@@@DEF");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("000abc##$DEF");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("000abc)()DEF");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("#3aBb@C3A1#0c00idheff");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("#3aBmb@C3A1#0c00");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("#3aBb@C3A1n#0c00");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
		
		try {
			password.setPassword("#3mBb*@C3A1n#0c00");
			this.passwordPolicyService.validate(password, policy);
			fail("Password complex validation. " + policy);
		} catch (Exception e) {
			// nothing, success
		}
	}
	
	@Test
	public void testCreateAndFoundPasswordPolicy() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_17_saved");
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setDefaultPolicy(false);
		policy.setMaxPasswordLength(20);
		policy.setMinPasswordLength(6);
		
		policy = this.passwordPolicyService.save(policy);
		
		IdmPasswordPolicyDto foundPolicyByName = this.passwordPolicyService.findOneByName("test_17_saved");
		assertEquals(policy.getName(), foundPolicyByName.getName());
		assertEquals(policy.getType(), foundPolicyByName.getType());
		assertEquals(policy.getMaxPasswordLength(), foundPolicyByName.getMaxPasswordLength());
		assertEquals(policy.getMinPasswordLength(), foundPolicyByName.getMinPasswordLength());
		
		IdmPasswordPolicyDto foundPolicyById = this.passwordPolicyService.get(policy.getId());
		assertEquals(policy.getName(), foundPolicyById.getName());
		assertEquals(policy.getType(), foundPolicyById.getType());
		assertEquals(policy.getMaxPasswordLength(), foundPolicyById.getMaxPasswordLength());
		assertEquals(policy.getMinPasswordLength(), foundPolicyById.getMinPasswordLength());
	}
	
	@Test
	public void testCreateTwoDefaultPolicy() {
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_18_default");
		policy.setType(IdmPasswordPolicyType.VALIDATE);
		policy.setDefaultPolicy(true);
		
		policy = saveInTransaction(policy, passwordPolicyService);
		
		IdmPasswordPolicyDto defaultValidatePolicy = this.passwordPolicyService.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE);
		assertEquals(policy.getId(), defaultValidatePolicy.getId());
		assertEquals(policy.getName(), defaultValidatePolicy.getName());
		assertEquals(policy.getType(), defaultValidatePolicy.getType());
		
		IdmPasswordPolicyDto policyNew = new IdmPasswordPolicyDto();
		policyNew.setName("test_19_default");
		policyNew.setType(IdmPasswordPolicyType.VALIDATE);
		policyNew.setDefaultPolicy(true);
		
		policyNew = saveInTransaction(policyNew, passwordPolicyService);
		
		defaultValidatePolicy = this.passwordPolicyService.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE);
		assertEquals(policyNew.getId(), defaultValidatePolicy.getId());
		assertEquals(policyNew.getName(), defaultValidatePolicy.getName());
		assertEquals(policyNew.getType(), defaultValidatePolicy.getType());
	}
	
	@Test
	public void testHistoryPassword() {
		String firstPassword = "test-password-first-" + System.currentTimeMillis();
		String secondPassword = "test-password-second-" + System.currentTimeMillis();
		String thridPassword = "test-password-third" + System.currentTimeMillis();
		IdmIdentityDto identity = testHelper.createIdentity();
		
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_default_policy_" + System.currentTimeMillis());
		policy.setType(IdmPasswordPolicyType.VALIDATE);
		policy.setDefaultPolicy(true);
		policy.setMaxHistorySimilar(2);
		policy = passwordPolicyService.save(policy);
		
		PasswordChangeDto passwordChange = new PasswordChangeDto();
		passwordChange.setIdm(true);
		passwordChange.setAll(true);
		passwordChange.setNewPassword(new GuardedString(firstPassword));
		

		identityService.passwordChange(identity, passwordChange);

		// we must login as no admin
		loginAsNoAdmin(identity.getUsername());
		try {
			// same password as before
			passwordChange.setNewPassword(new GuardedString(firstPassword));
			passwordChange.setOldPassword(new GuardedString(firstPassword));
			identityService.passwordChange(identity, passwordChange);
		} catch (ResultCodeException e) {
			checkMaxHistorySimilarError(e, 2);
		} catch (Exception ex) {
			fail("Bad exception: " + ex.toString());
		}
		
		// new password
		passwordChange.setNewPassword(new GuardedString(secondPassword));
		passwordChange.setOldPassword(new GuardedString(firstPassword));
		identityService.passwordChange(identity, passwordChange);
		
		try {
			// same password as first
			passwordChange.setNewPassword(new GuardedString(firstPassword));
			passwordChange.setOldPassword(new GuardedString(secondPassword));
			identityService.passwordChange(identity, passwordChange);
		} catch (ResultCodeException e) {
			checkMaxHistorySimilarError(e, 2);
		} catch (Exception ex) {
			fail("Bad exception: " + ex.toString());
		}
		
		
		try {
			// same password as the second
			passwordChange.setNewPassword(new GuardedString(secondPassword));
			passwordChange.setOldPassword(new GuardedString(secondPassword));
			identityService.passwordChange(identity, passwordChange);
		} catch (ResultCodeException e) {
			checkMaxHistorySimilarError(e, 2);
		} catch (Exception ex) {
			fail("Bad exception: " + ex.toString());
		}
		
		// new password
		passwordChange.setNewPassword(new GuardedString(thridPassword));
		passwordChange.setOldPassword(new GuardedString(secondPassword));
		identityService.passwordChange(identity, passwordChange);
		
		try {
			// same password as the second
			passwordChange.setNewPassword(new GuardedString(secondPassword));
			passwordChange.setOldPassword(new GuardedString(thridPassword));
			identityService.passwordChange(identity, passwordChange);
		} catch (ResultCodeException e) {
			checkMaxHistorySimilarError(e, 2);
		} catch (Exception ex) {
			fail("Bad exception: " + ex.toString());
		}
		
		try {
			// same password as the second
			passwordChange.setNewPassword(new GuardedString(thridPassword));
			passwordChange.setOldPassword(new GuardedString(thridPassword));
			identityService.passwordChange(identity, passwordChange);
		} catch (ResultCodeException e) {
			checkMaxHistorySimilarError(e, 2);
		} catch (Exception ex) {
			fail("Bad exception: " + ex.toString());
		}
		
		// new password (the first one)
		passwordChange.setNewPassword(new GuardedString(firstPassword));
		passwordChange.setOldPassword(new GuardedString(thridPassword));
		identityService.passwordChange(identity, passwordChange);
		
		passwordPolicyService.delete(policy);
	}

	@Test
	public void testExistingSuffix() {
		String suffix = "test-suffix-" + System.currentTimeMillis();
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_policy" + System.currentTimeMillis());
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setMinNumber(4);
		policy.setMinPasswordLength(4);
		policy.setMaxPasswordLength(4);
		policy.setSuffix(suffix);
		policy = passwordPolicyService.save(policy);
		
		String generatePassword = passwordPolicyService.generatePassword(policy);
		
		assertTrue(generatePassword.endsWith(suffix));
		String password = StringUtils.remove(generatePassword, suffix);
		// password must be only integer
		Integer.valueOf(password);
		assertEquals(4, password.length());
		assertEquals(suffix.length() + 4, generatePassword.length());
	}

	@Test
	public void testExistingPrefix() {
		String prefix = "test-prefix-" + System.currentTimeMillis();
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_policy" + System.currentTimeMillis());
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setPrefix(prefix);
		policy.setMinLowerChar(5);
		policy.setMinPasswordLength(5);
		policy.setMaxPasswordLength(5);
		policy = passwordPolicyService.save(policy);
		
		String generatePassword = passwordPolicyService.generatePassword(policy);
		
		assertTrue(generatePassword.startsWith(prefix));
		String password = StringUtils.remove(generatePassword, prefix);
		assertEquals(5, password.length());
		assertEquals(prefix.length() + 5, generatePassword.length());
	}

	@Test
	public void testExistingPrefixAndSuffix() {
		String prefix = "test-prefix-" + System.currentTimeMillis();
		String suffix = "test-suffix-" + System.currentTimeMillis();
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_policy" + System.currentTimeMillis());
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.RANDOM);
		policy.setPrefix(prefix);
		policy.setSuffix(suffix);
		policy.setMaxPasswordLength(10);
		policy.setMinPasswordLength(10);
		policy = passwordPolicyService.save(policy);
		
		String generatePassword = passwordPolicyService.generatePassword(policy);
		
		assertTrue(generatePassword.startsWith(prefix));
		assertTrue(generatePassword.endsWith(suffix));
		assertEquals(suffix.length() + prefix.length() + 10, generatePassword.length());
		
		String password = StringUtils.remove(generatePassword, prefix);
		password = StringUtils.remove(password, suffix);
		assertEquals(10, password.length());
	}

	@Test
	public void testPrefixAndSuffixWithPassphrase() {
		String prefix = "test-prefix-" + System.currentTimeMillis();
		String suffix = "test-suffix-" + System.currentTimeMillis();
		IdmPasswordPolicyDto policy = new IdmPasswordPolicyDto();
		policy.setName("test_policy" + System.currentTimeMillis());
		policy.setType(IdmPasswordPolicyType.GENERATE);
		policy.setGenerateType(IdmPasswordPolicyGenerateType.PASSPHRASE);
		policy.setPrefix(prefix);
		policy.setSuffix(suffix);
		policy.setPassphraseWords(2);
		policy = passwordPolicyService.save(policy);
		
		String generatePassword = passwordPolicyService.generatePassword(policy);
		
		assertTrue(generatePassword.startsWith(prefix));
		assertTrue(generatePassword.endsWith(suffix));
	}

	private void checkMaxHistorySimilarError(ResultCodeException exception, int maxHistorySettingOriginal) {
		ErrorModel error = exception.getError().getError();
		assertTrue(error.getMessage().contains("Password does not match password policy"));
		assertEquals(CoreResultCode.PASSWORD_DOES_NOT_MEET_POLICY.getCode(), error.getStatusEnum());
		Map<String, Object> parameters = error.getParameters();
		assertEquals(1, parameters.size());
		assertTrue(parameters.containsKey("maxHistorySimilar"));
		Object parameterAsObject = parameters.get("maxHistorySimilar");
		int maxHistorySetting = Integer.valueOf(parameterAsObject.toString());
		assertEquals(maxHistorySettingOriginal, maxHistorySetting);
	}
}
