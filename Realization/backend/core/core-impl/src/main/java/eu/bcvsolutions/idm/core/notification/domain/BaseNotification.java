package eu.bcvsolutions.idm.core.notification.domain;

import java.util.List;

import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.notification.entity.IdmMessage;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationRecipient;

/**
 * Common message properties for notification system
 * 
 * @author Radek Tomiška 
 *
 */
public interface BaseNotification {

	/**
	 * Notification type - email, notification, websocket etc.
	 * 
	 * @return
	 */
	String getType();
	
	/**
	 * Notification topic
	 * 
	 * @return
	 */
	String getTopic();

	/**
	 * Notification topic
	 * 
	 * @param topic
	 */
	void setTopic(String topic);

	/**
	 * Notification sender - could be filled, when notification is send from
	 * some identity
	 * 
	 * @param identitySender
	 */
	void setIdentitySender(IdmIdentity identitySender);

	/**
	 * Notification sender - could be filled, when notification is send from
	 * some identity
	 * 
	 * @return
	 */
	IdmIdentity getIdentitySender();

	/**
	 * Notification recipients
	 * 
	 * @param recipients
	 */
	void setRecipients(List<IdmNotificationRecipient> recipients);

	/**
	 * Notification recipients
	 * 
	 * @return
	 */
	List<IdmNotificationRecipient> getRecipients();

	/**
	 * Sent message
	 * 
	 * @param message
	 */
	void setMessage(IdmMessage message);

	/**
	 * Sent message
	 * 
	 * @return
	 */
	IdmMessage getMessage();
}
