package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;
import eu.bcvsolutions.idm.core.api.utils.PasswordGenerator;
import eu.bcvsolutions.idm.core.model.domain.IdmPasswordPolicyGenerateType;
import eu.bcvsolutions.idm.core.model.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.model.dto.filter.PasswordPolicyFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmPasswordPolicy;
import eu.bcvsolutions.idm.core.model.repository.IdmPasswordPolicyRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmPasswordPolicyService;

@Service
public class DefaultIdmPasswordPolicyService extends AbstractReadWriteEntityService<IdmPasswordPolicy, PasswordPolicyFilter> implements IdmPasswordPolicyService {
	
	// TODO: better place for constant?
	private static final String MIN_LENGTH = "minLength";
	private static final String MAX_LENGTH = "maxLength";
	private static final String MIN_UPPER_CHAR = "minUpperChar";
	private static final String MIN_LOWER_CHAR = "minLowerChar";
	private static final String MIN_NUMBER = "minNumber";
	private static final String MIN_SPECIAL_CHAR = "minSpecialChar";
	private static final String COINTAIN_PROHIBITED = "prohibited";
	private static final String CONTAIN_WEAK_PASS = "weakPass";
	private static final String POLICY_NAME = "policiesNames";
	
	private PasswordGenerator passwordGenerator;
	private final IdmPasswordPolicyRepository passwordPolicyRepository;
	
	@Autowired
	public DefaultIdmPasswordPolicyService(
			AbstractEntityRepository<IdmPasswordPolicy, PasswordPolicyFilter> repository, IdmPasswordPolicyRepository passwordPolicyRepository) {
		super(repository);
		this.passwordPolicyRepository = passwordPolicyRepository;
	}
	
	@Override
	public IdmPasswordPolicy save(IdmPasswordPolicy entity) {
		if (canSaveEntity(entity)) {
			return super.save(entity);
		} else {
			throw new ResultCodeException(CoreResultCode.PASSWORD_POLICY_DEFAULT_TYPE, ImmutableMap.of("name", entity.getName()));
		}
	}
	
	/**
	 * method check if dont exist another default type of same type
	 * 
	 * @param entity
	 * @return false if there is any another default type 
	 * and entity can be save. True if entity can't be save
	 */
	private boolean canSaveEntity(IdmPasswordPolicy entity) {
		// create filter and found default password policy
		PasswordPolicyFilter filter = new PasswordPolicyFilter();
		filter.setDefaultPolicy(true);
		filter.setType(entity.getType());
		List<IdmPasswordPolicy> result = this.find(filter, null).getContent();
		
		if (result.isEmpty()) {
			return true;
		}
		
		if (result.get(0).getId().equals(entity.getId())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean validate(String password, IdmPasswordPolicy passwordPolicy) {
		List<IdmPasswordPolicy> passwordPolicyList = new ArrayList<IdmPasswordPolicy>();
		passwordPolicyList.add(passwordPolicy);
		
		return this.validate(password, passwordPolicyList);
	}

	@Override
	public boolean validate(String password) {
		return this.validate(password, new ArrayList<>());
	}

	@Override
	public boolean validate(String password, List<IdmPasswordPolicy> passwordPolicyList) {
		Assert.notNull(passwordPolicyList);
		
		if (passwordPolicyList.isEmpty()) {
			passwordPolicyList.add(this.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE));
		}
		
		Map<String, Object> errors = new HashMap<>();
		Set<Character> prohibitedChar = new HashSet<>();
		List<String> policyNames = new ArrayList<String>();
		
		for (IdmPasswordPolicy passwordPolicy : passwordPolicyList) {
			boolean validateNotSuccess = false;
			
			if (password.length() > passwordPolicy.getMaxPasswordLength()) {
				if (!(errors.containsKey(MAX_LENGTH) && compareInt(errors.get(MAX_LENGTH), passwordPolicy.getMaxPasswordLength()))) {
					errors.put(MAX_LENGTH, passwordPolicy.getMaxPasswordLength());
				}
				validateNotSuccess = true;
			}
			if (password.length() < passwordPolicy.getMinPasswordLength()) {
				if (!(errors.containsKey(MIN_LENGTH) && compareInt(errors.get(MIN_LENGTH), passwordPolicy.getMinPasswordLength()))) {
					errors.put(MIN_LENGTH, passwordPolicy.getMinPasswordLength());
				}
				validateNotSuccess = true;
			}
			if (!password.matches("[^" + passwordPolicy.getProhibitedCharacters() + "]*")) {
				for (char character : passwordPolicy.getProhibitedCharacters().toCharArray()) {
					if (password.indexOf(character) >= 0) {
						prohibitedChar.add(character);
					}
				}
				validateNotSuccess = true;
			}
			if (passwordPolicy.getMinNumber() != 0 && !password.matches("(.*[" + Pattern.quote(passwordPolicy.getNumberBase()) + "].*){" + passwordPolicy.getMinNumber() + ",}")) {
				if (!(errors.containsKey(MIN_NUMBER) && compareInt(errors.get(MIN_NUMBER), passwordPolicy.getMinNumber()))) {
					errors.put(MIN_NUMBER, passwordPolicy.getMinNumber());
				}
			}
			if (passwordPolicy.getMinLowerChar() != 0 && !password.matches("(.*[" + Pattern.quote(passwordPolicy.getLowerCharBase()) + "].*){" + passwordPolicy.getMinLowerChar() + ",}")) {
				if (!(errors.containsKey(MIN_LOWER_CHAR) && compareInt(errors.get(MIN_LOWER_CHAR), passwordPolicy.getMinLowerChar()))) {
					errors.put(MIN_LOWER_CHAR, passwordPolicy.getMinLowerChar());
				}
				validateNotSuccess = true;
			}
			if (passwordPolicy.getMinUpperChar() != 0 && !password.matches("(.*[" + Pattern.quote(passwordPolicy.getUpperCharBase()) + "].*){" + passwordPolicy.getMinUpperChar() + ",}")) {
				if (!(errors.containsKey(MIN_UPPER_CHAR) && compareInt(errors.get(MIN_UPPER_CHAR), passwordPolicy.getMinUpperChar()))) {
					errors.put(MIN_UPPER_CHAR, passwordPolicy.getMinUpperChar());
				}
			}
			if (passwordPolicy.getMinSpecialChar() != 0 && !password.matches("(.*[" + Pattern.quote(passwordPolicy.getSpecialCharBase()) + "].*){" + passwordPolicy.getMinSpecialChar() + ",}")) {
				if (!(errors.containsKey(MIN_SPECIAL_CHAR) && compareInt(errors.get(MIN_SPECIAL_CHAR), passwordPolicy.getMinSpecialChar()))) {
					errors.put(MIN_SPECIAL_CHAR, passwordPolicy.getMinSpecialChar());
				}
				validateNotSuccess = true;
			}
			
			if (validateNotSuccess) {
				policyNames.add(passwordPolicy.getName());
			}
			
			// TODO: weak words
			
			// TODO: history similiar
		}
		
		if (!policyNames.isEmpty()) {
			errors.put(POLICY_NAME, String.join(", ", policyNames));
		}
		
		if (!prohibitedChar.isEmpty()) {
			errors.put(COINTAIN_PROHIBITED, prohibitedChar.toString());
		}
		
		if (!errors.isEmpty()) {
			throw new ResultCodeException(CoreResultCode.PASSWORD_DOES_NOT_MEET_POLICY, errors);
		}
		
		return true;
	}

	@Override
	public IdmPasswordPolicy getDefaultPasswordPolicy(IdmPasswordPolicyType type) {
		IdmPasswordPolicy policy = passwordPolicyRepository.findDefaultType(type, new PageRequest(0, 1)).get(0);
		if (policy == null) {
			throw new ResultCodeException(CoreResultCode.PASSWORD_POLICY_DEFAULT_TYPE_NOT_EXIST);
		}
		return policy;
	}

	@Override
	public String generatePassword(IdmPasswordPolicy passwordPolicy) {
		Assert.notNull(passwordPolicy);
		Assert.doesNotContain(passwordPolicy.getType().name(), IdmPasswordPolicyType.VALIDATE.name(), "Bad type.");
		if (passwordPolicy.getGenerateType().equals(IdmPasswordPolicyGenerateType.PASSPHRASE)) {
			return this.getPasswordGenerator().generatePassphrase(passwordPolicy);
		}
		// TODO: use random generate?
		return this.getPasswordGenerator().generateRandom(passwordPolicy);
	}
	
	@Override
	public PasswordGenerator getPasswordGenerator() {
		if (this.passwordGenerator == null) {
			passwordGenerator = new PasswordGenerator();
		}
		return passwordGenerator;
	}

	@Override
	public String generatePasswordByDefault() {
		PasswordPolicyFilter filter = new PasswordPolicyFilter();
		filter.setDefaultPolicy(true);
		filter.setType(IdmPasswordPolicyType.GENERATE);
		List<IdmPasswordPolicy> list = this.find(filter, null).getContent();
		
		if (list.isEmpty() || list.size() > 1) {
			throw new ResultCodeException(CoreResultCode.PASSWORD_POLICY_DEFAULT_TYPE_NOT_EXIST);
		}
		
		return this.generatePassword(list.get(0));
	}
	
	/**
	 * Method compare integer in o1 with o2
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	private boolean compareInt(Object o1, Object o2) {
		Integer i1 = Integer.parseInt(o1.toString());
		Integer i2 = Integer.parseInt(o2.toString());
		
		return i1 > i2;
	}
}