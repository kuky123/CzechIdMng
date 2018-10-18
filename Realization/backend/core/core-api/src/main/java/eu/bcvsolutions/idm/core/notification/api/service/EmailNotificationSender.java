package eu.bcvsolutions.idm.core.notification.api.service;

import eu.bcvsolutions.idm.core.notification.api.dto.IdmEmailLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;

/**
 * Sending emails to queue (email will be sent asynchronously)
 * 
 * @author Radek Tomiška 
 *
 */
public interface EmailNotificationSender extends NotificationSender<IdmEmailLogDto> {
	
	/**
	 * Method send email and save notification log
	 * 
	 * @param message
	 * @param emails
	 * @return
	 */
	IdmEmailLogDto send(IdmMessageDto message, String[] emails);
	
	/**
	 * Method send email and save notification to log
	 * 
	 * @param message
	 * @param emails
	 * @return
	 */
	IdmEmailLogDto send(IdmMessageDto message, String email);
}
