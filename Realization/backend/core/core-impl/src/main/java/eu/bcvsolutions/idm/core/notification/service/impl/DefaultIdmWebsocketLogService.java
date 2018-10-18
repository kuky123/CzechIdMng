package eu.bcvsolutions.idm.core.notification.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.notification.api.dto.IdmWebsocketLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmWebsocketLogService;
import eu.bcvsolutions.idm.core.notification.entity.IdmWebsocketLog;
import eu.bcvsolutions.idm.core.notification.repository.IdmWebsocketLogRepository;

/**
 * Websocket log service
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
public class DefaultIdmWebsocketLogService 
		extends AbstractNotificationLogService<IdmWebsocketLogDto, IdmWebsocketLog, IdmNotificationFilter> 
		implements IdmWebsocketLogService {

	
	@Autowired
	public DefaultIdmWebsocketLogService(IdmWebsocketLogRepository repository) {
		super(repository);		
	}

}
