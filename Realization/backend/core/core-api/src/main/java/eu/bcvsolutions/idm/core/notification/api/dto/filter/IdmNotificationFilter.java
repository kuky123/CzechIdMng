package eu.bcvsolutions.idm.core.notification.api.dto.filter;

import java.util.UUID;

import org.joda.time.DateTime;

import eu.bcvsolutions.idm.core.api.dto.filter.QuickFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationState;

/**
 * Filter for notifications (notification, email ...)
 * 
 * TODO: add topic
 * 
 * @author Radek Tomiška
 *
 */
public class IdmNotificationFilter extends QuickFilter {

	private String sender; // senders username
	private String recipient; // recipients username
	private NotificationState state;
	private DateTime from;
	private DateTime till;
	private Boolean sent;
	private Class<? extends BaseEntity> notificationType;
	private UUID parent;

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public DateTime getFrom() {
		return from;
	}

	public void setFrom(DateTime from) {
		this.from = from;
	}

	public DateTime getTill() {
		return till;
	}

	public void setTill(DateTime till) {
		this.till = till;
	}
	
	public NotificationState getState() {
		return state;
	}
	
	public void setState(NotificationState state) {
		this.state = state;
	}
	
	public Boolean getSent() {
		return sent;
	}
	
	public void setSent(Boolean sent) {
		this.sent = sent;
	}
	
	public Class<? extends BaseEntity> getNotificationType() {
		return notificationType;
	}
	
	public void setNotificationType(Class<? extends BaseEntity> notificationType) {
		this.notificationType = notificationType;
	}
	
	public void setParent(UUID parent) {
		this.parent = parent;
	}
	
	public UUID getParent() {
		return parent;
	}
}
