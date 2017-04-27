package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import eu.bcvsolutions.idm.core.api.domain.Auditable;
import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Disableable;

/**
 * Dto for identity
 * 
 * @author Radek Tomiška
 *
 */
public class IdmIdentityDto extends AbstractDto implements Disableable {

	private static final long serialVersionUID = 1L;
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	private String username;
	@Size(max = DefaultFieldLengths.NAME)
	private String firstName;
	@NotEmpty
	@Size(max = DefaultFieldLengths.NAME)
	private String lastName;
	@Email
	@Size(max = DefaultFieldLengths.EMAIL_ADDRESS)
	private String email;
	@Size(max = 30)
	private String phone;
	@Size(max = 100)
	private String titleBefore;
	@Size(max = 100)
	private String titleAfter;
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	private String description;
	@NotNull
	private boolean disabled;
	
	public IdmIdentityDto() {
	}
	
	public IdmIdentityDto(String username) {
		this.username = username;
	}
	
	public IdmIdentityDto(UUID id, String username) {
		super(id);
		this.username = username;
	}
	
	public IdmIdentityDto(Auditable auditable, String username) {
		super(auditable);
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getTitleBefore() {
		return titleBefore;
	}

	public void setTitleBefore(String titleBefore) {
		this.titleBefore = titleBefore;
	}

	public String getTitleAfter() {
		return titleAfter;
	}

	public void setTitleAfter(String titleAfter) {
		this.titleAfter = titleAfter;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
}