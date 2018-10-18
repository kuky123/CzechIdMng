package eu.bcvsolutions.idm.core.notification.rest.impl;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;

/**
 * Controller tests
 * - TODO: move filters here
 * 
 * @author Radek Tomiška
 *
 */
public class IdmNotificationLogRestTest extends AbstractReadWriteDtoControllerRestTest<IdmNotificationLogDto> {

	@Autowired private IdmNotificationLogController controller;
	
	@Override
	protected AbstractReadWriteDtoController<IdmNotificationLogDto, ?> getController() {
		return controller;
	}
	
	@Override
	protected boolean supportsAutocomplete() {
		return false;
	}
	
	@Override
	protected boolean supportsPatch() {
		return false;
	}
	
	@Override
	protected boolean supportsPut() {
		return false;
	}
	
	@Override
	protected boolean supportsDelete() {
		return false;
	}

	@Override
	protected IdmNotificationLogDto prepareDto() {
		IdmIdentityDto recipient = getHelper().createIdentity((GuardedString) null);
		IdmNotificationLogDto dto = new IdmNotificationLogDto();
		dto.setMessage(new IdmMessageDto.Builder(NotificationLevel.SUCCESS).setMessage(getHelper().createName()).build());
		dto.getRecipients().add(new IdmNotificationRecipientDto(recipient.getId()));
		//
		return dto;
	}
}
