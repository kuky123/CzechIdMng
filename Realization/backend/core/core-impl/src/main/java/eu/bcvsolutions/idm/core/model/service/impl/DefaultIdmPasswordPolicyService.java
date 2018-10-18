package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyGenerateType;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyIdentityAttributes;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordValidationDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmPasswordPolicyFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordHistoryService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordService;
import eu.bcvsolutions.idm.core.api.utils.PasswordGenerator;
import eu.bcvsolutions.idm.core.model.entity.IdmPasswordPolicy;
import eu.bcvsolutions.idm.core.model.event.PasswordPolicyEvent;
import eu.bcvsolutions.idm.core.model.event.PasswordPolicyEvent.PasswordPolicyEvenType;
import eu.bcvsolutions.idm.core.model.repository.IdmPasswordPolicyRepository;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Password policy service.
 * validation and generate method.
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class DefaultIdmPasswordPolicyService
		extends AbstractReadWriteDtoService<IdmPasswordPolicyDto, IdmPasswordPolicy, IdmPasswordPolicyFilter>
		implements IdmPasswordPolicyService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmPasswordPolicyService.class);
	
	// TODO: better place for constant?
	private static final String MIN_LENGTH = "minLength";
	private static final String MAX_LENGTH = "maxLength";
	private static final String MIN_UPPER_CHAR = "minUpperChar";
	private static final String MIN_LOWER_CHAR = "minLowerChar";
	private static final String MIN_NUMBER = "minNumber";
	private static final String MIN_SPECIAL_CHAR = "minSpecialChar";
	private static final String COINTAIN_PROHIBITED = "prohibited";
	// private static final String CONTAIN_WEAK_PASS = "weakPass"; // TODO
	private static final String MIN_RULES_TO_FULFILL = "minRulesToFulfill";
	private static final String MIN_RULES_TO_FULFILL_COUNT = "minRulesToFulfillCount";
	private static final String POLICY_NAME = "policiesNames";
	private static final String PASSWORD_SIMILAR_USERNAME = "passwordSimilarUsername";
	private static final String PASSWORD_SIMILAR_EMAIL = "passwordSimilarEmail";
	private static final String PASSWORD_SIMILAR_FIRSTNAME = "passwordSimilarFirstName";
	private static final String PASSWORD_SIMILAR_LASTNAME = "passwordSimilarLastName";
	private static final String PASSWORD_SIMILAR_USERNAME_PREVALIDATE = "passwordSimilarUsernamePreValidate";
	private static final String PASSWORD_SIMILAR_EMAIL_PREVALIDATE = "passwordSimilarEmailPreValidate";
	private static final String PASSWORD_SIMILAR_FIRSTNAME_PREVALIDATE = "passwordSimilarFirstNamePreValidate";
	private static final String PASSWORD_SIMILAR_LASTNAME_PREVALIDATE = "passwordSimilarLastNamePreValidate";
	private static final String POLICY_NAME_PREVALIDATION = "policiesNamesPreValidation";
	private static final String SPECIAL_CHARACTER_BASE = "specialCharacterBase";
	private static final String FORBIDDEN_CHARACTER_BASE = "forbiddenCharacterBase";
	private static final String MAX_HISTORY_SIMILAR = "maxHistorySimilar";
	
	private PasswordGenerator passwordGenerator;
	private final IdmPasswordPolicyRepository repository;
	private final SecurityService securityService;
	private final EntityEventManager entityEventProcessorService;
	private final IdmPasswordService passwordService;
	private final IdmPasswordHistoryService passwordHistoryService;
	
	@Autowired
	public DefaultIdmPasswordPolicyService(
			IdmPasswordPolicyRepository repository,
			EntityEventManager entityEventProcessorService,
			SecurityService securityService,
			IdmPasswordService passwordService,
			IdmPasswordHistoryService passwordHistoryService) {
		super(repository);
		//
		Assert.notNull(entityEventProcessorService);
		Assert.notNull(repository);
		Assert.notNull(securityService);
		Assert.notNull(passwordService);
		Assert.notNull(passwordHistoryService);
		//
		this.entityEventProcessorService = entityEventProcessorService;
		this.repository = repository;
		this.securityService = securityService;
		this.passwordService = passwordService;
		this.passwordHistoryService = passwordHistoryService;
	}
	
	@Override
	protected Page<IdmPasswordPolicy> findEntities(IdmPasswordPolicyFilter filter, Pageable pageable, BasePermission... permission) {
		if (filter == null) {
			return getRepository().findAll(pageable);
		}
		return repository.find(filter, pageable);
	}
	
	@Override
	@Transactional
	public IdmPasswordPolicyDto save(IdmPasswordPolicyDto dto, BasePermission... permission) {
		Assert.notNull(dto);
		//
		// TODO: this should be moved to save internal, can be bypassed by event publishing
		if (!ObjectUtils.isEmpty(permission)) {
			IdmPasswordPolicy persistEntity = null;
			if (dto.getId() != null) {
				persistEntity = this.getEntity(dto.getId());
				if (persistEntity != null) {
					// check access on previous entity - update is needed
					checkAccess(persistEntity, IdmBasePermission.UPDATE);
				}
			}
			checkAccess(toEntity(dto, persistEntity), permission); // TODO: remove one checkAccess?
		}
		// Check, if max attempts attribute is defined, then time of blocking must have defined too
		Integer maxAttempts = dto.getMaxUnsuccessfulAttempts();
		Integer blockLogin = dto.getBlockLoginTime();
		if (maxAttempts != null && maxAttempts.intValue() > 0 && (blockLogin == null || blockLogin <= 0)) {
			throw new ResultCodeException(CoreResultCode.PASSWORD_POLICY_BLOCK_TIME_IS_REQUIRED, ImmutableMap.of("definition", dto.getName()));
		}
		//
		LOG.debug("Saving entity [{}]", dto.getName());
		if (isNew(dto)) {
			// throw event with create
			return entityEventProcessorService.process(new PasswordPolicyEvent(PasswordPolicyEvenType.CREATE, dto)).getContent();
		}
		// else throw event with update
		return entityEventProcessorService.process(new PasswordPolicyEvent(PasswordPolicyEvenType.UPDATE, dto)).getContent();
	}
	
	@Override
	@Transactional
	public void delete(IdmPasswordPolicyDto dto, BasePermission... permission) {
		checkAccess(this.getEntity(dto.getId()), permission);
		//
		LOG.debug("Delete entity [{}]", dto.getName());
		//
		entityEventProcessorService.process(new PasswordPolicyEvent(PasswordPolicyEvenType.DELETE, dto));
	}
	
	@Override
	public void validate(IdmPasswordValidationDto passwordValidationDto) {
		this.validate(passwordValidationDto, this.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE));
	}
	
	@Override
	public void validate(IdmPasswordValidationDto passwordValidationDto, IdmPasswordPolicyDto passwordPolicy) {
		List<IdmPasswordPolicyDto> passwordPolicyList = new ArrayList<IdmPasswordPolicyDto>();
		
		if (passwordPolicy != null) {
			passwordPolicyList.add(passwordPolicy);
		}
		
		this.validate(passwordValidationDto, passwordPolicyList);
	}

	@Override
	public void validate(IdmPasswordValidationDto passwordValidationDto, List<IdmPasswordPolicyDto> passwordPolicyList) {
		validate(passwordValidationDto, passwordPolicyList, false);
	}

	@Override
	public IdmPasswordPolicyDto getDefaultPasswordPolicy(IdmPasswordPolicyType type) {
		IdmPasswordPolicy defaultPolicy = repository.findOneDefaultType(type);
		return this.toDto(defaultPolicy);
	}

	@Override
	public String generatePassword(IdmPasswordPolicyDto passwordPolicy) {
		Assert.notNull(passwordPolicy);
		Assert.doesNotContain(passwordPolicy.getType().name(), IdmPasswordPolicyType.VALIDATE.name(), "Bad type.");
		String generateRandom = null;
		
		// generate password with passphrase or random
		if (passwordPolicy.getGenerateType() == IdmPasswordPolicyGenerateType.PASSPHRASE) {
			generateRandom = this.getPasswordGenerator().generatePassphrase(passwordPolicy);
		} else {
			generateRandom = this.getPasswordGenerator().generateRandom(passwordPolicy);
		}

		StringBuilder result = new StringBuilder();
		// prepare prefix and suffix
		String prefix = passwordPolicy.getPrefix();
		String suffix = passwordPolicy.getSuffix();

		if (StringUtils.isNotEmpty(prefix)) {
			result.append(prefix);
		}

		// append default generated password
		result.append(generateRandom);

		if (StringUtils.isNotEmpty(suffix)) {
			result.append(suffix);
		}

		return result.toString();
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
		IdmPasswordPolicyDto defaultPasswordPolicy = this.getDefaultPasswordPolicy(IdmPasswordPolicyType.GENERATE);
		
		// if default password policy for generating not exist
		// generate random string
		if (defaultPasswordPolicy == null) {
			return this.getPasswordGenerator().generateRandom();
		}
		
		return this.generatePassword(defaultPasswordPolicy);
	}
	
	@Override
	public Integer getMaxPasswordAge(List<IdmPasswordPolicyDto> policyList) {
		Assert.notNull(policyList);
		//
		if (policyList.isEmpty()) {
			return null;
		}
		//
		Integer passwordAge = new Integer(Integer.MIN_VALUE);
		for (IdmPasswordPolicyDto idmPasswordPolicy : policyList) {
			if (idmPasswordPolicy.getMaxPasswordAge() != 0 && 
					idmPasswordPolicy.getMaxPasswordAge() > passwordAge) {
				passwordAge = idmPasswordPolicy.getMaxPasswordAge();
			}
		}
		//
		if (passwordAge.equals(Integer.MIN_VALUE)) {
			return null;
		} 
		//
		return passwordAge;
	}
	
	public void preValidate(IdmPasswordValidationDto passwordValidationDto) {
		IdmPasswordPolicyDto defaultPolicy = this.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE);
		if (defaultPolicy == null) {
			defaultPolicy = new IdmPasswordPolicyDto();
		}
		List<IdmPasswordPolicyDto> passwordPolicyList = new ArrayList<IdmPasswordPolicyDto>();
		passwordPolicyList.add(defaultPolicy);
		preValidate(passwordValidationDto, passwordPolicyList);
	}

	public void preValidate(IdmPasswordValidationDto passwordValidationDto,
			List<IdmPasswordPolicyDto> passwordPolicyList) {
		passwordValidationDto.setPassword("");
		validate(passwordValidationDto, passwordPolicyList, true);
	}

	private void validate(IdmPasswordValidationDto passwordValidationDto, List<IdmPasswordPolicyDto> passwordPolicyList,
			boolean prevalidation) {
		Assert.notNull(passwordPolicyList);
		Assert.notNull(passwordValidationDto);
		
		// default password policy is used when list of password policies is empty, or for get maximum equals password
		IdmPasswordPolicyDto defaultPolicy = this.getDefaultPasswordPolicy(IdmPasswordPolicyType.VALIDATE);

		// if list is empty, get default password policy
		if (passwordPolicyList.isEmpty() && !prevalidation) {
			if (defaultPolicy != null) {
				passwordPolicyList.add(defaultPolicy);
			}
		}

		// if list with password policies is empty, validate is always true
		if (passwordPolicyList.isEmpty()) {
			// this state means that system idm hasn't default password policy
			return;
		}

		IdmPasswordDto oldPassword = passwordValidationDto.getOldPassword() != null
				? passwordService.get(passwordValidationDto.getOldPassword())
				: null;
		String password = passwordValidationDto.getPassword().asString();

		DateTime now = new DateTime();

		Map<String, Object> errors = new HashMap<>();
		Set<Character> prohibitedChar = new HashSet<>();
		List<String> policyNames = new ArrayList<String>();
		Map<String, Object> specialCharBase = new HashMap<>();
		Map<String, Object> forbiddenCharBase = new HashMap<>();

		for (IdmPasswordPolicyDto passwordPolicy : passwordPolicyList) {
			if (passwordPolicy.isDisabled()) {
				continue;
			}
			boolean validateNotSuccess = false;

			// check if can change password for minimal age for change
			// if loged user is admin, skip this
			if (oldPassword != null && !securityService.isAdmin() && !prevalidation) {
				if (passwordPolicy.getMinPasswordAge() != null && oldPassword.getValidFrom()
						.plusDays(passwordPolicy.getMinPasswordAge()).compareTo(now.toLocalDate()) >= 1) {
					throw new ResultCodeException(CoreResultCode.PASSWORD_CANNOT_CHANGE, ImmutableMap.of(("date"),
							oldPassword.getValidFrom().plusDays(passwordPolicy.getMinPasswordAge())));
				}
			}

			// minimum rules to fulfill
			Map<String, Object> notPassRules = new HashMap<>();

			int minRulesToFulfill = passwordPolicy.getMinRulesToFulfill() == null ? 0
					: passwordPolicy.getMinRulesToFulfill().intValue();

			// check to max password length
			if (!isNull(passwordPolicy.getMaxPasswordLength()) && (password.length() > passwordPolicy.getMaxPasswordLength() ||  prevalidation)) {
				if (!passwordPolicy.isPasswordLengthRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MAX_LENGTH,
							Math.min(convertToInt(errors.get(MAX_LENGTH)), passwordPolicy.getMaxPasswordLength()));
				} else if (!(errors.containsKey(MAX_LENGTH)
						&& compareInt(passwordPolicy.getMaxPasswordLength(), errors.get(MAX_LENGTH)))) {
					errors.put(MAX_LENGTH, passwordPolicy.getMaxPasswordLength());
				}
				validateNotSuccess = true;
			}
			// check to minimal password length
			if (!isNull(passwordPolicy.getMinPasswordLength())
					&& password.length() < passwordPolicy.getMinPasswordLength()) {
				if (!passwordPolicy.isPasswordLengthRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MIN_LENGTH,
							Math.max(convertToInt(errors.get(MIN_LENGTH)), passwordPolicy.getMinPasswordLength()));
				} else if (!(errors.containsKey(MIN_LENGTH)
						&& compareInt(errors.get(MIN_LENGTH), passwordPolicy.getMinPasswordLength()))) {
					errors.put(MIN_LENGTH, passwordPolicy.getMinPasswordLength());
				}
				validateNotSuccess = true;
			}
			// check to prohibited characters
			if (!Strings.isNullOrEmpty(passwordPolicy.getProhibitedCharacters())
					&& !password.matches("[^" + passwordPolicy.getProhibitedCharacters() + "]*")) {
				for (char character : passwordPolicy.getProhibitedCharacters().toCharArray()) {
					if (password.indexOf(character) >= 0) {
						prohibitedChar.add(character);
					}
				}
				validateNotSuccess = true;
			}
			// check to minimal numbers
			if (!isNull(passwordPolicy.getMinNumber()) && !password.matches("(.*["
					+ Pattern.quote(passwordPolicy.getNumberBase()) + "].*){" + passwordPolicy.getMinNumber() + ",}")) {
				if (!passwordPolicy.isNumberRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MIN_NUMBER,
							Math.max(convertToInt(errors.get(MIN_NUMBER)), passwordPolicy.getMinNumber()));
				} else if (!(errors.containsKey(MIN_NUMBER)
						&& compareInt(errors.get(MIN_NUMBER), passwordPolicy.getMinNumber()))) {
					errors.put(MIN_NUMBER, passwordPolicy.getMinNumber());
				}
				validateNotSuccess = true;
			}
			// check to minimal lower characters
			if (!isNull(passwordPolicy.getMinLowerChar())
					&& !password.matches("(.*[" + Pattern.quote(passwordPolicy.getLowerCharBase()) + "].*){"
							+ passwordPolicy.getMinLowerChar() + ",}")) {
				if (!passwordPolicy.isLowerCharRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MIN_LOWER_CHAR,
							Math.max(convertToInt(errors.get(MIN_LOWER_CHAR)), passwordPolicy.getMinLowerChar()));
				} else if (!(errors.containsKey(MIN_LOWER_CHAR)
						&& compareInt(errors.get(MIN_LOWER_CHAR), passwordPolicy.getMinLowerChar()))) {
					errors.put(MIN_LOWER_CHAR, passwordPolicy.getMinLowerChar());
				}
				validateNotSuccess = true;
			}
			// check to minimal upper character
			if (!isNull(passwordPolicy.getMinUpperChar())
					&& !password.matches("(.*[" + Pattern.quote(passwordPolicy.getUpperCharBase()) + "].*){"
							+ passwordPolicy.getMinUpperChar() + ",}")) {
				if (!passwordPolicy.isUpperCharRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MIN_UPPER_CHAR,
							Math.max(convertToInt(errors.get(MIN_UPPER_CHAR)), passwordPolicy.getMinUpperChar()));
				} else if (!(errors.containsKey(MIN_UPPER_CHAR)
						&& compareInt(errors.get(MIN_UPPER_CHAR), passwordPolicy.getMinUpperChar()))) {
					errors.put(MIN_UPPER_CHAR, passwordPolicy.getMinUpperChar());
				}
			}
			// check to minimal special character and add special character base
			if (!isNull(passwordPolicy.getMinSpecialChar())
					&& !password.matches("(.*[" + Pattern.quote(passwordPolicy.getSpecialCharBase()) + "].*){"
							+ passwordPolicy.getMinSpecialChar() + ",}")) {
				if (!passwordPolicy.isSpecialCharRequired() && passwordPolicy.isEnchancedControl()) {
					notPassRules.put(MIN_SPECIAL_CHAR,
							Math.max(convertToInt(errors.get(MIN_SPECIAL_CHAR)), passwordPolicy.getMinSpecialChar()));
					specialCharBase.put(passwordPolicy.getName(), passwordPolicy.getSpecialCharBase());
				} else if (!(errors.containsKey(MIN_SPECIAL_CHAR)
						&& compareInt(errors.get(MIN_SPECIAL_CHAR), passwordPolicy.getMinSpecialChar()))) {
					errors.put(MIN_SPECIAL_CHAR, passwordPolicy.getMinSpecialChar());
					specialCharBase.put(passwordPolicy.getName(), passwordPolicy.getSpecialCharBase());
				}
				validateNotSuccess = true;
			}
			
			if (passwordPolicy.getProhibitedCharacters() != null) {
				forbiddenCharBase.put(passwordPolicy.getName(), passwordPolicy.getProhibitedCharacters());
			}

			if (!notPassRules.isEmpty() && passwordPolicy.isEnchancedControl()) {
				int notRequiredRules = passwordPolicy.getNotRequiredRules();
				int missingRules = notRequiredRules - notPassRules.size();
				if (missingRules - minRulesToFulfill < 0) {
					errors.put(MIN_RULES_TO_FULFILL_COUNT, minRulesToFulfill - missingRules); 
					errors.put(MIN_RULES_TO_FULFILL, notPassRules);
				}
			}

			// if not success we want password policy name
			if (validateNotSuccess && !errors.isEmpty() && !prevalidation) {
				policyNames.add(passwordPolicy.getName());
			}

			// check to similar identity attributes, enhanced control
			if (prevalidation) {
				enhancedControlForSimilar(passwordPolicy, prevalidation, errors);
			} else {
				enhancedControlForSimilar(passwordPolicy, passwordValidationDto, errors);
			}

			// TODO: weak words
		}
		
		if (!specialCharBase.isEmpty() && prevalidation) {
			errors.put(SPECIAL_CHARACTER_BASE, specialCharBase); 
		}
		
		if (!forbiddenCharBase.isEmpty() && prevalidation) {
			errors.put(FORBIDDEN_CHARACTER_BASE, forbiddenCharBase); 
		}

		if (!policyNames.isEmpty() && !prevalidation) {
			String name = prevalidation ? POLICY_NAME_PREVALIDATION : POLICY_NAME;
			errors.put(name, String.join(", ", policyNames));
		}

		if (!prohibitedChar.isEmpty()) {
			errors.put(COINTAIN_PROHIBITED, prohibitedChar.toString());
		}
		
		// password history. Skip when doesn't exists settings, or identity isn't saved
		// in some case (tests) are save identity in one transaction and id doesn't exist
		if (!prevalidation && defaultPolicy != null) {
			Integer maxHistorySimilar = defaultPolicy.getMaxHistorySimilar();
			IdmIdentityDto identity = passwordValidationDto.getIdentity();
			if (maxHistorySimilar != null && identity != null && identity.getId() != null) {
				boolean checkHistory = passwordHistoryService.checkHistory(passwordValidationDto.getIdentity().getId(), maxHistorySimilar, passwordValidationDto.getPassword());
				
				if (checkHistory) {
					errors.put(MAX_HISTORY_SIMILAR, maxHistorySimilar);
				}
			}
		}
		
		if (!errors.isEmpty()) {
			// TODO: password policy audit
			if(prevalidation) {
				throw new ResultCodeException(CoreResultCode.PASSWORD_PREVALIDATION, errors);
			}
			throw new ResultCodeException(CoreResultCode.PASSWORD_DOES_NOT_MEET_POLICY, errors);
		}
	}

	/**
	 * Method sets to which attribute of identity cannot be similar to password - pre-validation
	 * 
	 * @param passwordPolicy
	 * @param prevalidation
	 * @param errors
	 * @return
	 */
	private Map<String, Object> enhancedControlForSimilar(IdmPasswordPolicyDto passwordPolicy, boolean prevalidation,
			Map<String, Object> errors) {
		if (passwordPolicy.isEnchancedControl()) {
			String[] attributes = passwordPolicy.getIdentityAttributeCheck().split(", ");
			for (int index = 0; index < attributes.length; index++) {
				if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.EMAIL.name())) {
					errors.put(PASSWORD_SIMILAR_EMAIL_PREVALIDATE, "");
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.FIRSTNAME.name())) {
					errors.put(PASSWORD_SIMILAR_FIRSTNAME_PREVALIDATE, "");
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.LASTNAME.name())) {
					errors.put(PASSWORD_SIMILAR_LASTNAME_PREVALIDATE, "");
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.USERNAME.name())) {
					errors.put(PASSWORD_SIMILAR_USERNAME_PREVALIDATE, "");
				}
			}
		}
		return errors;
	}

	/**
	 * Method sets to which attribute of identity is similar to password - password validation
	 * 
	 * @param passwordPolicy
	 * @param passwordValidationDto
	 * @param errors
	 * @return
	 */
	private Map<String, Object> enhancedControlForSimilar(IdmPasswordPolicyDto passwordPolicy,
			IdmPasswordValidationDto passwordValidationDto, Map<String, Object> errors) {
		String password = passwordValidationDto.getPassword().asString();

		if (passwordPolicy.isEnchancedControl()) {
			String[] attributes = passwordPolicy.getIdentityAttributeCheck().split(", ");
			IdmIdentityDto identity = passwordValidationDto.getIdentity();
			for (int index = 0; index < attributes.length; index++) {
				if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.EMAIL.name())) {
					if (identity.getEmail() != null
							&& identity.getEmail().toLowerCase().matches("(?i).*" + password.toLowerCase() + ".*")) {
						errors.put(PASSWORD_SIMILAR_EMAIL, identity.getEmail());
					}
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.FIRSTNAME.name())) {
					if (identity.getFirstName() != null && identity.getFirstName().toLowerCase()
							.matches("(?i).*" + password.toLowerCase() + ".*")) {
						errors.put(PASSWORD_SIMILAR_FIRSTNAME, identity.getFirstName());
					}
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.LASTNAME.name())) {
					if (identity.getLastName() != null
							&& identity.getLastName().toLowerCase().matches("(?i).*" + password.toLowerCase() + ".*")) {
						errors.put(PASSWORD_SIMILAR_LASTNAME, identity.getLastName());
					}
				} else if (attributes[index].equals(IdmPasswordPolicyIdentityAttributes.USERNAME.name())) {
					if (identity.getUsername() != null
							&& identity.getUsername().toLowerCase().matches("(?i).*" + password.toLowerCase() + ".*")) {
						errors.put(PASSWORD_SIMILAR_USERNAME, identity.getUsername());
					}
				}
			}
		}
		return errors;
	}
	
	/**
	 * Method compare integer in o1 with o2
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
	private boolean compareInt(Object o1, Object o2) {
		Integer i1 = Integer.valueOf(o1.toString());
		Integer i2 = Integer.valueOf(o2.toString());
		
		return i1 > i2;
	}
	
	/**
	 * Convert integer value in object to int. If Object is null,
	 * return 0.
	 * 
	 * @param object
	 * @return
	 */
	private int convertToInt(Object object) {
		if (object == null) {
			return 0;
		}
		return NumberUtils.toInt(object.toString());
	}
	
	/**
	 * Method check if given {@link Integer} is null.
	 * 
	 * @param number
	 * @return true if Integer is null.
	 */
	private boolean isNull(Integer number) {
		return number == null;
	}
	
	@Override
	public IdmPasswordPolicyDto getByCode(String code) {
		return findOneByName(code);
	}

	@Override
	public IdmPasswordPolicyDto findOneByName(String name) {
		return this.toDto(this.repository.findOneByName(name));
	}
}
