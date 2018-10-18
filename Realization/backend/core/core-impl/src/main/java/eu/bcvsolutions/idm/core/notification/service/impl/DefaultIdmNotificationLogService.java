package eu.bcvsolutions.idm.core.notification.service.impl;

import java.util.Collections;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.notification.api.domain.NotificationState;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationLogService;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotification;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationLog;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationLogRepository;

/**
 * Notification log service
 * 
 * @author Radek Tomiška
 *
 */
@Service("notificationLogService")
public class DefaultIdmNotificationLogService 
		extends AbstractNotificationLogService<IdmNotificationLogDto, IdmNotificationLog, IdmNotificationFilter> 
		implements IdmNotificationLogService {

	@Autowired
	public DefaultIdmNotificationLogService(IdmNotificationLogRepository repository, ModelMapper modelMapper) {
		super(repository);		
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmNotificationRecipientDto> getRecipientsForNotification(String backendId) {
		IdmNotificationLogDto entity = get(backendId);
		//
		if (entity == null) {
			return Collections.<IdmNotificationRecipientDto>emptyList();
		}
		//
		return entity.getRecipients();
	}
	
	@Override
	protected IdmNotificationLogDto toDto(IdmNotificationLog entity, IdmNotificationLogDto dto) {
		dto = super.toDto(entity, dto);
		if (dto == null) {
			return null;
		}
		if (!entity.getType().equals(IdmNotificationLog.NOTIFICATION_TYPE)) {
			return dto;
		}
		// sent state by related notifications
		boolean hasSent = false;
		boolean hasNotSent = false;
		for(IdmNotification relatedNotification : entity.getRelatedNotifications()) {
			hasSent = hasSent || (relatedNotification.getSent() != null);
			hasNotSent = hasNotSent || (relatedNotification.getSent() == null);
		}
		if (hasSent && !hasNotSent) {
			dto.setState(NotificationState.ALL);
		} else if(hasSent && hasNotSent) {
			dto.setState(NotificationState.PARTLY);
		} else {
			dto.setState(NotificationState.NOT);
		}	
		return dto;
	}
}
