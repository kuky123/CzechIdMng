package eu.bcvsolutions.idm.acc.dto.filter;

import java.util.UUID;

import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.core.api.dto.filter.QuickFilter;

/**
 * Filter for accounts
 * 
 * @author Radek Tomiška
 *
 */
public class AccountFilter extends QuickFilter {
	
	private UUID systemEntityId;	
	private UUID systemId;	
	private UUID identityId;	
	private String uid;
	private AccountType accountType;
	
	public UUID getSystemEntityId() {
		return systemEntityId;
	}
	
	public void setSystemEntityId(UUID systemEntityId) {
		this.systemEntityId = systemEntityId;
	}

	public UUID getSystemId() {
		return systemId;
	}

	public void setSystemId(UUID systemId) {
		this.systemId = systemId;
	}

	public UUID getIdentityId() {
		return identityId;
	}

	public void setIdentityId(UUID identityId) {
		this.identityId = identityId;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
}
