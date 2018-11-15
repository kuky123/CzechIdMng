package eu.bcvsolutions.idm.acc.event.processor;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccIdentityAccountDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.event.IdentityAccountEvent.IdentityAccountEventType;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Save identity account
 * 
 * @author Svanda
 */
@Component("accIdentityAccountSaveProcessor")
@Description("Ensures referential integrity. Cannot be disabled.")
public class IdentityAccountSaveProcessor extends CoreEventProcessor<AccIdentityAccountDto> {

	private static final String PROCESSOR_NAME = "identity-account-save-processor";
//	private static final Logger LOG = LoggerFactory.getLogger(IdentityAccountSaveProcessor.class);
	private final AccIdentityAccountService service;
	private final AccAccountService accountService;

	@Autowired
	public IdentityAccountSaveProcessor(AccIdentityAccountService service, AccAccountService accountService) {
		super(IdentityAccountEventType.CREATE, IdentityAccountEventType.UPDATE);
		//
		Assert.notNull(service);
		Assert.notNull(accountService);
		//
		this.service = service;
		this.accountService = accountService;
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<AccIdentityAccountDto> process(EntityEvent<AccIdentityAccountDto> event) {
		AccIdentityAccountDto entity = event.getContent();
		UUID account = entity.getAccount();
		AccAccountDto accountEntity = accountService.get(account);
		Assert.notNull(account, "Account cannot be null!");

		// If is account protected and new role for same account is creates, then we
		// have to deactivate account protection and delete last protected
		// identity-account
		
		// TODO write test that creating account without identityRole failed
		if (service.isNew(entity) && entity.isOwnership() && entity.getIdentityRole() != null && accountEntity.isInProtection()) {
			AccIdentityAccountDto protectedIdentityAccount = findProtectedIdentityAccount(account);
			// First we save new identity-account
			event.setContent(service.saveInternal(entity));
			// Second we delete protected identity-account
			service.delete(protectedIdentityAccount);
			// Next we set account to unprotected state
			this.deactivateProtection(accountEntity);
			accountEntity = accountService.save(accountEntity);
			return new DefaultEventResult<>(event, this);
		}
		event.setContent(service.saveInternal(entity));

		return new DefaultEventResult<>(event, this);
	}

	private AccIdentityAccountDto findProtectedIdentityAccount(UUID account) {
		List<AccIdentityAccountDto> identityAccounts = findIdentityAccounts(account);
		if (!identityAccounts.isEmpty()) {
			return identityAccounts.get(0);
		}
		return null;
	}

	private List<AccIdentityAccountDto> findIdentityAccounts(UUID account) {
		AccIdentityAccountFilter filter = new AccIdentityAccountFilter();
		filter.setAccountId(account);
		filter.setOwnership(Boolean.TRUE);

		List<AccIdentityAccountDto> identityAccounts = service.find(filter, null).getContent();
		return identityAccounts;
	}

	private void deactivateProtection(AccAccountDto accountEntity) {
		accountEntity.setInProtection(false);
		accountEntity.setEndOfProtection(null);
	}

	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER;
	}

	@Override
	public boolean isDisableable() {
		return false;
	}

}
