package eu.bcvsolutions.idm.core.notification.api.service;

import eu.bcvsolutions.idm.core.notification.api.dto.IdmWebsocketLogDto;

/**
 * Sends notifications through websocket
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
public interface WebsocketNotificationSender extends NotificationSender<IdmWebsocketLogDto> {

}
