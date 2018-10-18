package eu.bcvsolutions.idm.core.notification.entity;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;
import org.hibernate.validator.constraints.NotEmpty;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.domain.Disableable;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;

/**
 * Configuration for notification routing
 * 
 * @author Radek Tomiška
 *
 */
@Entity
@Table(name = "idm_notification_configuration", indexes = {
		@Index(name = "ux_idm_not_conf", columnList = "topic,level,notification_type", unique = true),
		@Index(name = "idx_idm_not_conf_topic", columnList = "topic"),
		@Index(name = "idx_idm_not_conf_level", columnList = "level"),
		@Index(name = "idx_idm_not_conf_type", columnList = "notification_type"),
		@Index(name = "idx_idm_not_template", columnList = "template_id")
		})
public class IdmNotificationConfiguration extends AbstractEntity implements Disableable {

	private static final long serialVersionUID = 3131809487111061022L;

	@Audited
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "topic", nullable = false, length = DefaultFieldLengths.NAME)
	private String topic; // TODO: should be defined in module descriptor as supported topics
	
	@Enumerated(EnumType.STRING)
	@Column(name = "level", length = 45)
	private NotificationLevel level;
	
	@Audited
	@NotEmpty
	@Size(min = 1, max = DefaultFieldLengths.NAME)
	@Column(name = "notification_type", nullable = false, length = DefaultFieldLengths.NAME)
	private String notificationType;
	
	@Audited
	@ManyToOne(optional = true)
	@JoinColumn(name = "template_id", referencedColumnName = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	@SuppressWarnings("deprecation") // jpa FK constraint does not work in hibernate 4
	@org.hibernate.annotations.ForeignKey( name = "none" )
	private IdmNotificationTemplate template;
	
	@Audited
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	@Column(name = "description", length = DefaultFieldLengths.DESCRIPTION)
	private String description;
	
	@Audited
	@NotNull
	@Column(name = "disabled", nullable = false)
	private boolean disabled;
	
	@Audited
	@NotNull
	@Column(name = "redirect", nullable = false)
	private boolean redirect;
	
	@Audited
	@Column(name = "recipients", length = DefaultFieldLengths.LOG)
	private String recipients; // see IdmNotificationTemplateService#PARAMETER_DELIMITIER

	public IdmNotificationConfiguration() {
	}
	
	public IdmNotificationConfiguration(String topic, NotificationLevel level, String notificationType) {
		this.topic = topic;
		this.level = level;
		this.notificationType = notificationType;
	}
	
	public IdmNotificationConfiguration(NotificationConfigurationDto dto) {
		this.topic = dto.getTopic();
		this.level = dto.getLevel();
		this.notificationType = dto.getNotificationType();
		this.description = dto.getDescription();
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}
	
	public void setLevel(NotificationLevel level) {
		this.level = level;
	}
	
	public NotificationLevel getLevel() {
		return level;
	}

	public IdmNotificationTemplate getTemplate() {
		return template;
	}

	public String getDescription() {
		return description;
	}

	public void setTemplate(IdmNotificationTemplate template) {
		this.template = template;
	}

	public void setDescription(String description) {
		this.description = description;
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
