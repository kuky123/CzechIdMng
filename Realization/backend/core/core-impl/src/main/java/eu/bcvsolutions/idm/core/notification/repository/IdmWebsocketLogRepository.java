package eu.bcvsolutions.idm.core.notification.repository;

import org.springframework.data.repository.NoRepositoryBean;

import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.notification.entity.IdmWebsocketLog;

/**
 * Repository for sent messages through websocket
 * 
 * @author Radek Tomiška 
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
@Deprecated
@NoRepositoryBean
public interface IdmWebsocketLogRepository extends AbstractEntityRepository<IdmWebsocketLog> {
	
}
