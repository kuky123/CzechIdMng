package eu.bcvsolutions.idm.core.notification.api.dto;

import org.springframework.hateoas.core.Relation;

/**
 * Web socket log message dto
 *
 * @author Peter Sourek
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Relation(collectionRelation = "websocketLogs")
public class IdmWebsocketLogDto extends IdmNotificationLogDto {

    private static final long serialVersionUID = 1L;

    public IdmWebsocketLogDto(IdmNotificationDto notification) {
        super(notification);
    }

    public IdmWebsocketLogDto() {

    }
}
