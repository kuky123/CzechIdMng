package eu.bcvsolutions.idm.core.notification.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Strings;

import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.notification.api.dto.FlashMessage;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmWebsocketLogDto;
import eu.bcvsolutions.idm.core.notification.api.service.IdmWebsocketLogService;
import eu.bcvsolutions.idm.core.notification.api.service.WebsocketNotificationSender;
import eu.bcvsolutions.idm.core.notification.entity.IdmWebsocketLog;

/**
 * Send messages through websocket
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
public class DefaultWebsocketNotificationSender extends AbstractNotificationSender<IdmWebsocketLogDto> implements WebsocketNotificationSender {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultWebsocketNotificationSender.class);
	private final IdmWebsocketLogService websocketLogService;
	private final SimpMessagingTemplate websocket;
	private final IdmIdentityService identityService;
    
    @Autowired
    public DefaultWebsocketNotificationSender(
    		IdmWebsocketLogService websocketLogService,
			SimpMessagingTemplate websocket, IdmIdentityService identityService) {
    	Assert.notNull(websocket);
    	Assert.notNull(websocketLogService);
		Assert.notNull(identityService);
    	//
		this.websocket = websocket;
		this.websocketLogService = websocketLogService;
		this.identityService = identityService;
	}

	@Override
	public String getType() {
		return IdmWebsocketLog.NOTIFICATION_TYPE;
	}
	
	@Override
	public Class<? extends BaseEntity> getNotificationType() {
		return websocketLogService.getEntityClass();
	}
	
	@Override
	@Transactional
	public IdmWebsocketLogDto send(IdmNotificationDto notification) {
		Assert.notNull(notification, "Notification is required!");
		//
		LOG.info("Adding websocket notification to queue [{}]", notification);
		IdmWebsocketLogDto log = createLog(notification);
		// send flashmessage
		FlashMessage message = toFlashMessage(log);
		for (IdmNotificationRecipientDto recipient : log.getRecipients()) {
			if(Strings.isNullOrEmpty(recipient.getRealRecipient())){
				LOG.warn("Real recipient is empty for notification [{}]", notification);
			}else{
				websocket.convertAndSendToUser(
						recipient.getRealRecipient(),
						"/queue/messages", // TODO: configurable
						message);
			}
		}
		return log;
	}

	/**
	 * Returns recipient's username, if identity is defined and realRecipient is defined, then realRecipient is returned (has higher priority).
	 *  
	 * @param recipient
	 * @return
	 */
	public String getUsername(IdmNotificationRecipientDto recipient) {
		if (recipient == null) {
			return null;
		}
		if (StringUtils.isNotBlank(recipient.getRealRecipient())) {
			return recipient.getRealRecipient();
		}
		
		if (recipient.getIdentityRecipient() != null) {
			return identityService.get(recipient.getIdentityRecipient()).getUsername();
		}
		return null;
	}	
	
	/**
	 * Persists new websocket record from given notification
	 * 
	 * @param notification
	 * @return
	 */
	private IdmWebsocketLogDto createLog(IdmNotificationDto notification) {
		Assert.notNull(notification);
		Assert.notNull(notification.getMessage());
		//
		// we can only create log, if notification is instance of IdmNotificationLog
		if (notification instanceof IdmWebsocketLogDto) {
			notification.setSent(new DateTime());
			return websocketLogService.save((IdmWebsocketLogDto) notification);
		}
		//
		IdmWebsocketLogDto log = new IdmWebsocketLogDto();
		log.setSent(new DateTime());
		log.setTopic(notification.getTopic());
		// parent message
		if (notification.getId() != null) {
			log.setParent(notification.getId());
		}
		// clone message
		log.setMessage(cloneMessage(notification));
		// clone recipients - resolve real email
		notification.getRecipients().forEach(recipient -> {
			log.getRecipients().add(cloneRecipient(log, recipient));
		});
		log.setIdentitySender(notification.getIdentitySender());
		log.setType(IdmWebsocketLog.NOTIFICATION_TYPE);
		return websocketLogService.save(log);
	}

	/**
	 * clone recipients with resolved real email address
	 *
	 * @param notification - recipients new parent
	 * @param recipient - source recipient
	 * @return Clone of recipient without specified identifier
	 */
	protected IdmNotificationRecipientDto cloneRecipient(IdmNotificationDto notification, IdmNotificationRecipientDto recipient) {
		return super.cloneRecipient(notification, recipient, getUsername(recipient));
	}
	
	private FlashMessage toFlashMessage(IdmWebsocketLogDto log) {
		IdmMessageDto message = this.getMessage(log, true);
		//
		FlashMessage flashMessage = new FlashMessage();
		flashMessage.setId(log.getId());
		flashMessage.setKey(log.getTopic());
		flashMessage.setTitle(message.getSubject());
		flashMessage.setMessage(message.getTextMessage()); // default message
		flashMessage.setDate(log.getCreated());
		flashMessage.setLevel(message.getLevel() == null ? null : message.getLevel().toString().toLowerCase());
		flashMessage.setModel(message.getModel());
		// TODO: other params
		flashMessage.setPosition("tr");
		return flashMessage;
	}
}
