package eu.bcvsolutions.idm.core.security.api.domain;

public enum IdentityBasePermission implements BasePermission {
	
	PASSWORDCHANGE; // password change
	// PASSWORDRESET; // TODO: password reset - resurrect password reset from CA
	
	@Override
	public String getName() {
		return name();
	}
	
	@Override
	public String getModule() {
		// common base permission without module
		return null;
	}
}
