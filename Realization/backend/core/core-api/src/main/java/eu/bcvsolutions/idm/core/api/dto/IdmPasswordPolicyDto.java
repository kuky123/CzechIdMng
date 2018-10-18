package eu.bcvsolutions.idm.core.api.dto;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.hateoas.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyGenerateType;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.api.domain.PasswordGenerate;
import eu.bcvsolutions.idm.core.api.utils.PasswordGenerator;

/**
 * Dto for password policy.
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Relation(collectionRelation = "passwordPolicies")
public class IdmPasswordPolicyDto extends AbstractDto implements PasswordGenerate, Disableable, Codeable {

	private static final long serialVersionUID = -7102038216963911330L;
	//
	@NotEmpty
	private String name;
	private String description;
	private boolean passwordLengthRequired = true;
	private Integer minPasswordLength;
	private Integer maxPasswordLength;
	private boolean upperCharRequired = true;
	private Integer minUpperChar;
	private boolean lowerCharRequired = true;
	private Integer minLowerChar;
	private boolean numberRequired = true;
	private Integer minNumber;
	private boolean specialCharRequired = true;
	private Integer minSpecialChar;
	private boolean weakPassRequired = true;
	private String weakPass;
	private Integer maxPasswordAge;
	private Integer minPasswordAge;
	private boolean enchancedControl = false;
	private Integer minRulesToFulfill;
	private IdmPasswordPolicyType type = IdmPasswordPolicyType.VALIDATE;
	private IdmPasswordPolicyGenerateType generateType = IdmPasswordPolicyGenerateType.RANDOM;
	private Integer passphraseWords;
	private String prohibitedCharacters;
	private boolean defaultPolicy = false;
	@NotEmpty
	private String specialCharBase = PasswordGenerator.SPECIAL_CHARACTERS;
	@NotEmpty
	private String upperCharBase = PasswordGenerator.UPPER_CHARACTERS;
	@NotEmpty
	private String numberBase = PasswordGenerator.NUMBERS;
	@NotEmpty
	private String lowerCharBase = PasswordGenerator.LOWER_CHARACTERS;
	private Integer maxHistorySimilar;
	private String identityAttributeCheck;
	private boolean disabled = false;
	private Integer maxUnsuccessfulAttempts;
	private Integer blockLoginTime;
	private String prefix;
	private String suffix;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPasswordLengthRequired() {
		return passwordLengthRequired;
	}

	public void setPasswordLengthRequired(boolean passwordLengthRequired) {
		this.passwordLengthRequired = passwordLengthRequired;
	}

	public Integer getMinPasswordLength() {
		return minPasswordLength;
	}

	public void setMinPasswordLength(Integer minPasswordLength) {
		this.minPasswordLength = minPasswordLength;
	}

	public Integer getMaxPasswordLength() {
		return maxPasswordLength;
	}

	public void setMaxPasswordLength(Integer maxPasswordLength) {
		this.maxPasswordLength = maxPasswordLength;
	}

	public boolean isUpperCharRequired() {
		return upperCharRequired;
	}

	public void setUpperCharRequired(boolean upperCharRequired) {
		this.upperCharRequired = upperCharRequired;
	}

	public Integer getMinUpperChar() {
		return minUpperChar;
	}

	public void setMinUpperChar(Integer minUpperChar) {
		this.minUpperChar = minUpperChar;
	}

	public boolean isLowerCharRequired() {
		return lowerCharRequired;
	}

	public void setLowerCharRequired(boolean lowerCharRequired) {
		this.lowerCharRequired = lowerCharRequired;
	}

	public Integer getMinLowerChar() {
		return minLowerChar;
	}

	public void setMinLowerChar(Integer minLowerChar) {
		this.minLowerChar = minLowerChar;
	}

	public boolean isNumberRequired() {
		return numberRequired;
	}

	public void setNumberRequired(boolean numberRequired) {
		this.numberRequired = numberRequired;
	}

	public Integer getMinNumber() {
		return minNumber;
	}

	public void setMinNumber(Integer minNumber) {
		this.minNumber = minNumber;
	}

	public boolean isSpecialCharRequired() {
		return specialCharRequired;
	}

	public void setSpecialCharRequired(boolean specialCharRequired) {
		this.specialCharRequired = specialCharRequired;
	}

	public Integer getMinSpecialChar() {
		return minSpecialChar;
	}

	public void setMinSpecialChar(Integer minSpecialChar) {
		this.minSpecialChar = minSpecialChar;
	}

	public boolean isWeakPassRequired() {
		return weakPassRequired;
	}

	public void setWeakPassRequired(boolean weakPassRequired) {
		this.weakPassRequired = weakPassRequired;
	}

	public String getWeakPass() {
		return weakPass;
	}

	public void setWeakPass(String weakPass) {
		this.weakPass = weakPass;
	}

	public Integer getMaxPasswordAge() {
		return maxPasswordAge;
	}

	public void setMaxPasswordAge(Integer maxPasswordAge) {
		this.maxPasswordAge = maxPasswordAge;
	}

	public Integer getMinPasswordAge() {
		return minPasswordAge;
	}

	public void setMinPasswordAge(Integer minPasswordAge) {
		this.minPasswordAge = minPasswordAge;
	}

	public boolean isEnchancedControl() {
		return enchancedControl;
	}

	public void setEnchancedControl(boolean enchancedControl) {
		this.enchancedControl = enchancedControl;
	}

	public Integer getMinRulesToFulfill() {
		return minRulesToFulfill;
	}

	public void setMinRulesToFulfill(Integer minRulesToFulfill) {
		this.minRulesToFulfill = minRulesToFulfill;
	}

	public IdmPasswordPolicyType getType() {
		return type;
	}

	public void setType(IdmPasswordPolicyType type) {
		this.type = type;
	}

	public IdmPasswordPolicyGenerateType getGenerateType() {
		return generateType;
	}

	public void setGenerateType(IdmPasswordPolicyGenerateType generateType) {
		this.generateType = generateType;
	}

	public Integer getPassphraseWords() {
		return passphraseWords;
	}

	public void setPassphraseWords(Integer passphraseWords) {
		this.passphraseWords = passphraseWords;
	}

	public String getProhibitedCharacters() {
		return prohibitedCharacters;
	}

	public void setProhibitedCharacters(String prohibitedCharacters) {
		this.prohibitedCharacters = prohibitedCharacters;
	}

	public boolean isDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(boolean defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public String getSpecialCharBase() {
		return specialCharBase;
	}

	public void setSpecialCharBase(String specialCharBase) {
		this.specialCharBase = specialCharBase;
	}

	public String getUpperCharBase() {
		return upperCharBase;
	}

	public void setUpperCharBase(String upperCharBase) {
		this.upperCharBase = upperCharBase;
	}

	public String getNumberBase() {
		return numberBase;
	}

	public void setNumberBase(String numberBase) {
		this.numberBase = numberBase;
	}

	public String getLowerCharBase() {
		return lowerCharBase;
	}

	public void setLowerCharBase(String lowerCharBase) {
		this.lowerCharBase = lowerCharBase;
	}

	public Integer getMaxHistorySimilar() {
		return maxHistorySimilar;
	}

	public void setMaxHistorySimilar(Integer maxHistorySimilar) {
		this.maxHistorySimilar = maxHistorySimilar;
	}

	public String getIdentityAttributeCheck() {
		return identityAttributeCheck;
	}

	public void setIdentityAttributeCheck(String identityAttributeCheck) {
		this.identityAttributeCheck = identityAttributeCheck;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	@Override
	@JsonIgnore
	public String getCode() {
		return getName();
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	@JsonIgnore
	public int getNotRequiredRules() {
		int rules = 0;
		if (!this.isLowerCharRequired()) {
			rules++;
		}
		if (!this.isNumberRequired()) {
			rules++;
		}
		if (!this.isPasswordLengthRequired()) {
			rules++;
		}
		if (!this.isSpecialCharRequired()) {
			rules++;
		}
		if (!this.isUpperCharRequired()) {
			rules++;
		}
		return rules;
	}

	public Integer getMaxUnsuccessfulAttempts() {
		return maxUnsuccessfulAttempts;
	}

	public void setMaxUnsuccessfulAttempts(Integer maxUnsuccessfulAttempts) {
		this.maxUnsuccessfulAttempts = maxUnsuccessfulAttempts;
	}

	public Integer getBlockLoginTime() {
		return blockLoginTime;
	}

	public void setBlockLoginTime(Integer blockLoginTime) {
		this.blockLoginTime = blockLoginTime;
	}
}
