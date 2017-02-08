package eu.bcvsolutions.idm.core.security.service;

import eu.bcvsolutions.idm.core.security.dto.LoginDto;

/**
 * Authenticate identity
 * 
 * @author svandav
 *
 */
public interface LoginService {

	/**
	 * Login identity and returns assigned token
	 * 
	 * @param loginDto
	 * @return
	 */
	public LoginDto login(LoginDto loginDto);
}