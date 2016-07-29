package eu.bcvsolutions.idm.core.notification.service;

import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationLog;

/**
 * Extend default notification service for simle noticitaion log sending
 * 
 * @author Radek Tomiška <radek.tomiska@bcvsolutions.eu>
 *
 */
public interface NotificationLogService extends NotificationService {
	
	/**
	 * Sends existing notification to routing
	 * 
	 * @param notification
	 * @return
	 */
	boolean sendNotificationLog(IdmNotificationLog notificationLog);

}
