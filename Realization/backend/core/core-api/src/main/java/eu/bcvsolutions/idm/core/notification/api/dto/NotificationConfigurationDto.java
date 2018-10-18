package eu.bcvsolutions.idm.core.notification.api.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;

/**
 * Notification configuration
 * 
 * Lookout: IdM prefix is missing ... but it's too late 
 *
 * @author Radek Tomiška
 */
@Relation(collectionRelation = "notificationConfigurations")
public class NotificationConfigurationDto extends AbstractDto implements Disableable {

	private static final long serialVersionUID = 1L;
	//
	private String topic;
	private NotificationLevel level;
	private String notificationType;
	private String description;
	@Embedded(dtoClass = IdmNotificationTemplateDto.class)
	private UUID template;
	private boolean disabled;
	private boolean redirect;
	private String recipients;

	public NotificationConfigurationDto() {
	}

	public NotificationConfigurationDto(
			String topic, 
			NotificationLevel level, 
			String notificationType,
			String description, 
			UUID template) {
		this.topic = topic;
		this.level = level;
		this.notificationType = notificationType;
		this.description = description;
		this.template = template;
	}

	public NotificationConfigurationDto(NotificationConfigurationDto other) {
		topic = other.getTopic();
		level = other.getLevel();
		notificationType = other.getNotificationType();
		description = other.getDescription();
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public NotificationLevel getLevel() {
		return level;
	}

	public void setLevel(NotificationLevel level) {
		this.level = level;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public UUID getTemplate() {
		return template;
	}

	public void setTemplate(UUID notificationTemplate) {
		this.template = notificationTemplate;
	}

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	@Override
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
	public void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}
	
	public boolean isRedirect() {
		return redirect;
	}
	
	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}
	
	public String getRecipients() {
		return recipients;
	}
}
