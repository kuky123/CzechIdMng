package eu.bcvsolutions.idm.core.notification.api.service;

import java.util.List;

import eu.bcvsolutions.idm.core.api.service.CodeableService;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.Recoverable;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationTemplateFilter;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;

/**
 * Interface with basic method for apache velocity templates engine
 * Initialization velocity engine in constructor.
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public interface IdmNotificationTemplateService extends 
		ReadWriteDtoService<IdmNotificationTemplateDto, IdmNotificationTemplateFilter>,
		Recoverable<IdmNotificationTemplateDto>,
		CodeableService<IdmNotificationTemplateDto> {

	String PARAMETER_DELIMITIER = ConfigurationService.PROPERTY_MULTIVALUED_SEPARATOR;
	/**
	 * Folder for scanning / initializing default templates
	 */
	String TEMPLATE_FOLDER = "idm.sec.core.notification.template.folder";

	/**
	 * Return {@link IdmNotificationTemplateDto} by given code.
	 * 
	 * @param code
	 * @return
	 * @deprecated @since 7.7.0, use {@link #getByCode(String)}
	 */
	@Deprecated
	IdmNotificationTemplateDto getTemplateByCode(String code);

	/**
	 * Return {@link IdmMessageDto}, generate by {@link IdmNotificationTemplateDto}
	 * from {@link IdmMessageDto}. For generate new message will be used parameters
	 * given in {@link IdmMessageDto}. Object instance of {@link GuardedString} is
	 * show or hide by parameter showGuardedString
	 * 
	 * @param message
	 * @param showGuardedString
	 * @return
	 */
	IdmMessageDto buildMessage(IdmMessageDto message, boolean showGuardedString);

	/**
	 * Return {@link IdmMessageDto} generate by
	 * {@link IdmNotificationTemplateDto} from {@link IdmMessageDto}. Object
	 * instance of {@link GuardedString} will be replace by asterix. For
	 * generate new message will be used parameters given in
	 * {@link IdmMessageDto}.
	 * 
	 * @param message
	 * @return
	 */
	IdmMessageDto buildMessage(IdmMessageDto message);

	/**
	 * Method find all system templates.
	 * 
	 * @return
	 */
	List<IdmNotificationTemplateDto> findAllSystemTemplates();

	/**
	 * Method find template for topic, level and notification type.
	 * If template isn't found this method return null.
	 * 
	 * @param topic
	 * @param level
	 * @param notificationType
	 * @return
	 */
	IdmNotificationTemplateDto resolveTemplate(String topic, NotificationLevel level, String notificationType);
	
	/**
	 * Method returns all founded {@link IdmNotificationLogDto} for this topic
	 * and given {@link IdmMessageDto}. Method resolve wildcards, template and text message in
	 * {@link IdmMessageDto}.
	 * 
	 * @param topic
	 * @param message
	 * @param notificationType e.g. email, sms
	 * @return
	 */
	List<IdmNotificationLogDto> prepareNotifications(String topic, IdmMessageDto message);
}
