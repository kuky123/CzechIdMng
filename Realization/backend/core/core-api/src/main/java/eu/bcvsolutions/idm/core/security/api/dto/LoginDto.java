package eu.bcvsolutions.idm.core.security.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import io.swagger.annotations.ApiModelProperty;

/**
 * Identity authentication - response
 * 
 * @author Svanda
 * @author Radek Tomiška
 *
 */
public class LoginDto extends LoginRequestDto {
	
	@JsonProperty(access = Access.READ_ONLY)
	@ApiModelProperty(readOnly = true, notes = "Logged identity's authentication token.")
	private String token;
	@JsonProperty(access = Access.READ_ONLY)
	@ApiModelProperty(readOnly = true, notes = "Logged identity's authentication metadata.")
	private IdmJwtAuthenticationDto authentication;
	@JsonProperty(access = Access.READ_ONLY)
	@ApiModelProperty(readOnly = true, notes = "Logged identity's granted authorities.")
	private List<DefaultGrantedAuthorityDto> authorities;
	@JsonIgnore
	private boolean skipMustChange = false;
	@JsonProperty(access = Access.READ_ONLY)
	@ApiModelProperty(readOnly = true, notes = "Which module authenticated identity.")
	private String authenticationModule; // identifier - which module authenticated identity
	
	public LoginDto() {
	}
	
	public LoginDto(String username, GuardedString password) {
		super(username, password);
	}
	
	public LoginDto(LoginRequestDto loginRequest) {
		this(loginRequest.getUsername(), loginRequest.getPassword());
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setAuthentication(IdmJwtAuthenticationDto authenticationDto) {
		this.authentication = authenticationDto;
	}

	public IdmJwtAuthenticationDto getAuthentication() {
		return this.authentication;
	}

	public boolean isSkipMustChange() {
		return skipMustChange;
	}

	public void setSkipMustChange(boolean skipMustChange) {
		this.skipMustChange = skipMustChange;
	}

	public String getAuthenticationModule() {
		return authenticationModule;
	}

	public void setAuthenticationModule(String authenticationModule) {
		this.authenticationModule = authenticationModule;
	}

	public List<DefaultGrantedAuthorityDto> getAuthorities() {
		if (authorities == null) {
			authorities = new ArrayList<>();
		}
		return authorities;
	}

	public void setAuthorities(List<DefaultGrantedAuthorityDto> authorities) {
		this.authorities = authorities;
	}
}
