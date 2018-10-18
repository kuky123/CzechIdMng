package eu.bcvsolutions.idm.core.notification.api.service;

import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmWebsocketLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;

/**
 * Websocket log service
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
public interface IdmWebsocketLogService extends 
		ReadWriteDtoService<IdmWebsocketLogDto, IdmNotificationFilter> {

}
