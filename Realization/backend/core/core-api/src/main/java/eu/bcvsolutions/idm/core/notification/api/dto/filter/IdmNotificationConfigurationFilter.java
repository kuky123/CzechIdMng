package eu.bcvsolutions.idm.core.notification.api.dto.filter;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.filter.QuickFilter;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;

/**
 * Filter for notification configuration
 * 
 * @author Patrik Stloukal
 *
 */
public class IdmNotificationConfigurationFilter extends QuickFilter {

	private NotificationLevel level;
	private String notificationType;
	private UUID template;
	private Boolean disabled;
	private String topic; // this is for equals, use getText for like

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public NotificationLevel getLevel() {
		return level;
	}

	public void setLevel(NotificationLevel level) {
		this.level = level;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public UUID getTemplate() {
		return template;
	}

	public void setTemplate(UUID templateId) {
		this.template = templateId;
	}
}
