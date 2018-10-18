package eu.bcvsolutions.idm.core.notification.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationSender;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotification;
import eu.bcvsolutions.idm.core.security.api.domain.AbstractAuthentication;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Basic notification service
 * 
 * @author Radek Tomiška
 * @author Ondřej Kopr
 *
 * @param <N> Notification type
 */
public abstract class AbstractNotificationSender<N extends IdmNotificationDto> implements NotificationSender<N> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractNotificationSender.class);

	@Autowired(required = false)
	private SecurityService securityService;
	@Autowired
	private IdmNotificationTemplateService notificationTemplateService;
	@Autowired(required = false)
	private ConfigurationService configurationService; // optional internal dependency - e.g. checks for sender is enabled
	
	/**
	 * Returns true, if given delimiter equals this managers {@link IdmNotification} type.
	 */
	@Override
	public boolean supports(String delimiter) {
		String type = getType();
		if (StringUtils.isEmpty(type)) {
			return false;
		}
		return getType().equals(delimiter);
	}
	
	@Override
	@Transactional
	public List<N> send(IdmMessageDto message, IdmIdentityDto recipient) {
		return send(DEFAULT_TOPIC, message, recipient);
	}

	@Override
	@Transactional
	public List<N> send(IdmMessageDto message, List<IdmIdentityDto> recipients) {
		return send(DEFAULT_TOPIC, message, recipients);
	}

	@Override
	@Transactional
	public List<N> send(String topic, IdmMessageDto message, IdmIdentityDto recipient) {
		return send(topic, message, Lists.newArrayList(recipient));
	}
	
	@Override
	@Transactional
	public List<N> send(String topic, IdmMessageDto message) {
		Assert.notNull(securityService, "Security service is required for this operation");
		Assert.notNull(topic, "Message topic can not be null.");
		Assert.notNull(message, "Message can not be null.");
		//
		AbstractAuthentication auth = securityService.getAuthentication();
		IdmIdentityDto currentIdentityDto = auth == null ? null : auth.getCurrentIdentity();
		if (currentIdentityDto == null || currentIdentityDto.getId() == null) {
			 LOG.warn("No identity is currently signed, swallowing the message: [{}], parameters: [{}].",
					 message.getTextMessage(), message.getParameters());
			// system, guest, etc.
			return null;
		}

		return send(topic, message, Lists.newArrayList(currentIdentityDto));
	}

	@Override
	@Transactional
	public List<N> send(String topic, IdmMessageDto message, List<IdmIdentityDto> recipients) {
		return send(topic, message, null, recipients);
	}
	
	@Override
	@Transactional
	public List<N> send(String topic, IdmMessageDto message, IdmIdentityDto identitySender, List<IdmIdentityDto> recipients) {
		Assert.notNull(message, "Message is required");
		List<N> sendMessages = new ArrayList<>();
		//
		List<IdmNotificationRecipientDto> notificationRecipients = new ArrayList<>();
		recipients.forEach(recipient ->{
			notificationRecipients.add(new IdmNotificationRecipientDto(recipient.getId()));
	    	});
		//
		List<IdmNotificationLogDto> notifications = notificationTemplateService.prepareNotifications(topic, message);
		//
		if (notifications.isEmpty()) {
			LOG.info("Notification for topic [{}] level [{}] not found or is disabled. Message will not be sent.", topic, message.getLevel());
			// no notifications found
			return sendMessages;
		}
		//
		// iterate over all prepared notifications, set recipients and send them
		for (IdmNotificationLogDto notification : notifications) {
			//
			final IdmMessageDto notificationMessage = notification.getMessage();
			if (notificationMessage.getHtmlMessage() == null 
					&& notificationMessage.getSubject() == null 
					&& notificationMessage.getTextMessage() == null 
					&& notificationMessage.getModel() == null) {
				LOG.error("Notification has empty template and message. Message will not be sent! [topic:{}]", topic);
				continue;
			}
			// recipient can be prepared by configuration (sending message to some alias)
			if (notification.getRecipients().isEmpty()) {
				notification.setRecipients(notificationRecipients);
			}
			notification.setIdentitySender(identitySender == null ? null : identitySender.getId());
			//
			sendMessages.add(send(notification));
		}
		return sendMessages;
	}
	
	@Override
	public int getOrder() {
		return ConfigurationService.DEFAULT_ORDER;
	}
	
	@Override
	public ConfigurationService getConfigurationService() {
		return configurationService;
	}

	/**
	 * Clone notification message. Method just clone {@link IdmMessageDto}, or if object {@link IdmMessageDto}
	 * contain {@link IdmNotificationTemplateDto} it will be generate new message from templates and parameters if any.
	 * Clone message from template will not contain plain text of object {@link GuardedString}, just asterisk.
	 * For show {@link GuardedString} in plain text use method for generate from template.
	 * 
	 * @param notification
	 * @return
	 */
	protected IdmMessageDto cloneMessage(IdmNotificationDto notification) {
		IdmMessageDto message = notification.getMessage();
		if (message.getTemplate() != null) {
			return this.getMessage(notification, false);
		}
		//
		message = new IdmMessageDto(message);
		//
		// build message, message may contain some parameters
		return this.notificationTemplateService.buildMessage(message, false);
	}
	
	/**
	 * Return {@link IdmMessageDto} from notification, or generate new copy of {@link IdmMessageDto} from template.
	 * {@link IdmMessageDto} is required. For generate {@link IdmMessageDto} from template is
	 * required object {@link IdmNotificationTemplateDto}
	 * Return {@link IdmMessageDto} or new generate instance from template.
	 * If not used generating from template, the parameter showGuardedString is irrelevant. Message is in plain text,
	 * it not possible to show or hide {@link GuardedString}
	 * 
	 * @param notification - notification that contain {@link IdmMessageDto} and {@ IdmNotificationTemplateDto}
	 * @param showGuardedString - flag for hide or show {@link GuardedString}
	 * @return
	 */
	protected IdmMessageDto getMessage(IdmNotificationDto notification, boolean showGuardedString) {
		Assert.notNull(notification.getMessage());
		if (notification.getMessage().getTemplate() == null) {
			return notification.getMessage();
		}
		
		return this.notificationTemplateService.buildMessage(notification.getMessage(), showGuardedString);
	}

	/**
	 * clone recipients with resolved real email address
	 *
	 * @param notification - recipients new parent
	 * @param recipient - source recipient
	 * @return Clone of recipient without specified identifier
	 */
	protected IdmNotificationRecipientDto cloneRecipient(IdmNotificationDto notification,
			IdmNotificationRecipientDto recipient, String realRecipient) {
		return new IdmNotificationRecipientDto(notification.getId(), recipient.getIdentityRecipient(),
			realRecipient);
	}
}