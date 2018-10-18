package eu.bcvsolutions.idm.core.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyGenerateType;
import eu.bcvsolutions.idm.core.api.domain.IdmPasswordPolicyType;
import eu.bcvsolutions.idm.core.api.domain.PasswordGenerate;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;

/**
 * Password policies entity:
 * Used for validation and generate password,
 * in idm system can be two default policies.
 * One default for validation and one for generate.
 * For generate passwords is use random generator and
 * passphrase
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

@Entity
@Table(name = "idm_password_policy", indexes = {
		@Index(name = "ux_idm_pass_policy_name", columnList = "name", unique = true)
		})
public class IdmPasswordPolicy extends AbstractEntity implements Codeable, PasswordGenerate, Disableable {

	private static final long serialVersionUID = -7107125399784973455L;
	
	@Audited
	@NotEmpty
	@Size(min = 0, max = DefaultFieldLengths.NAME)
	@Column(name = "name", length = DefaultFieldLengths.NAME, nullable = false)
	private String name;
	
	@Audited
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	@Column(name = "description", length = DefaultFieldLengths.DESCRIPTION)
	private String description;

	@Audited
	@Column(name = "password_length_required", nullable = false)
	private boolean passwordLengthRequired = true;
	
	@Audited
	@Column(name = "min_password_length")
	private Integer minPasswordLength;
	
	@Audited
	@Column(name = "max_password_length")
	private Integer maxPasswordLength;

	@Audited
	@Column(name = "upper_char_required", nullable = false)
	private boolean upperCharRequired = true;
	
	@Audited
	@Column(name = "min_upper_char")
	private Integer minUpperChar;
	
	@Audited
	@Column(name = "lower_char_required", nullable = false)
	private boolean lowerCharRequired = true;

	@Audited
	@Column(name = "min_lower_char")
	private Integer minLowerChar;
	
	@Audited
	@Column(name = "number_required", nullable = false)
	private boolean numberRequired = true;
	
	@Audited
	@Column(name = "min_number")
	private Integer minNumber;
	
	@Audited
	@Column(name = "special_char_required", nullable = false)
	private boolean specialCharRequired = true;
	
	@Audited
	@Column(name = "min_special_char")
	private Integer minSpecialChar;
	
	@Audited
	@Column(name = "weak_pass_required", nullable = false)
	private boolean weakPassRequired = true;
	
	@Audited
	@Column(name = "weak_pass")
	private String weakPass;
	
	@Audited
	@Column(name = "max_password_age")
	private Integer maxPasswordAge;
	
	@Audited
	@Column(name = "min_password_age")
	private Integer minPasswordAge;
	
	@Audited
	@Column(name = "enchanced_control", nullable = false)
	private boolean enchancedControl = false;
	
	@Audited
	@Column(name = "min_rules_to_fulfill")
	private Integer minRulesToFulfill;
	
	@Audited
	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private IdmPasswordPolicyType type = IdmPasswordPolicyType.VALIDATE;
	
	@Audited
	@Enumerated(EnumType.STRING)
	@Column(name = "generate_type")
	private IdmPasswordPolicyGenerateType generateType = IdmPasswordPolicyGenerateType.RANDOM;
	
	@Audited
	@Column(name = "passphrase_words")
	private Integer passphraseWords;

	@Audited
	@Column(name = "prohibited_characters")
	private String prohibitedCharacters;

	@Audited
	@Column(name = "default_policy", nullable = false)
	private boolean defaultPolicy = false;
	
	@Audited
	@NotNull
	@Column(name = "special_char_base", nullable = false)
	private String specialCharBase;
	
	@Audited
	@NotNull
	@Column(name = "upper_char_base", nullable = false)
	private String upperCharBase;
	
	@Audited
	@NotNull
	@Column(name = "number_base", nullable = false)
	private String numberBase;
	
	@Audited
	@NotNull
	@Column(name = "lower_char_base", nullable = false)
	private String lowerCharBase;
	
	@Audited
	@Column(name = "max_history_similar")
	private Integer maxHistorySimilar;
	
	@Audited
	@Column(name = "identity_attribute_check")
	private String identityAttributeCheck;
	
	@Audited
	@NotNull
	@Column(name = "disabled", nullable = false)
	private boolean disabled = false;

	@Audited
	@Column(name = "max_unsuccessful_attempts")
	private Integer maxUnsuccessfulAttempts;

	@Audited
	@Column(name = "block_login_time")
	private Integer blockLoginTime;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "prefix")
	private String prefix;

	@Audited
	@Size(max = DefaultFieldLengths.NAME)
	@Column(name = "suffix")
	private String suffix;

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

	public Integer getMaxHistorySimilar() {
		return maxHistorySimilar;
	}

	public void setMaxHistorySimilar(Integer maxHistorySimilar) {
		this.maxHistorySimilar = maxHistorySimilar;
	}

	public void setName(String name) {
		this.name = name;
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

	public boolean isDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(boolean defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}
	
	public IdmPasswordPolicyGenerateType getGenerateType() {
		return generateType;
	}

	public void setGenerateType(IdmPasswordPolicyGenerateType generateType) {
		this.generateType = generateType;
	}

	public String getProhibitedCharacters() {
		return prohibitedCharacters;
	}

	public void setProhibitedCharacters(String prohibitedCharacters) {
		this.prohibitedCharacters = prohibitedCharacters;
	}

	public Integer getPassphraseWords() {
		return passphraseWords;
	}

	public void setPassphraseWords(Integer passphraseWords) {
		this.passphraseWords = passphraseWords;
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
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIdentityAttributeCheck() {
		return identityAttributeCheck;
	}

	public void setIdentityAttributeCheck(String identityAttributeCheck) {
		this.identityAttributeCheck = identityAttributeCheck;
	}
	
	@Override
	public boolean isDisabled() {
		return disabled;
	}

	@Override
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public String getName() {
		return name;
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

	/**
	 * Get how many rules in password policy isn't required
	 * 
	 * @param entity
	 * @return
	 */
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
}
