package eu.bcvsolutions.idm.core.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationConfiguration;

/**
 * Configuration for notification routing
 * 
 * @author Radek Tomiška
 *
 */
public interface IdmNotificationConfigurationRepository extends AbstractEntityRepository<IdmNotificationConfiguration> {
	
	/**
	 * Finds all configurations by channel
	 * 
	 * @param notificationType channel
	 * @return
	 */
	List<IdmNotificationConfiguration> findAllByNotificationType(@Param("notificationType") String notificationType);
	
	/**
	 * Finds all channels by topic and level. Returns even configuration with no level specified (wildcard configuration).
	 * 
	 * @param topic
	 * @param level
	 * @return
	 * @deprecated @since 9.2.0 use {@link #findAllByTopicAndWildcardLevel(String, NotificationLevel)} - disabled configurations can be returned.
	 */
	@Deprecated
	@Query(value = "select distinct(e.notificationType) from #{#entityName} e where e.topic = :topic and (e.level is null or e.level = :level)")
	List<String> findTypes(@Param("topic") String topic, @Param("level") NotificationLevel level);
	
	/**
	 * Finds all channels by topic and level (supports wildcard). 
	 * Returns even configuration with no level specified (wildcard configuration).
	 * Configuration can be disabled. Check it before usage.
	 * 
	 * @param topic
	 * @param level
	 * @return
	 * @since 9.2.0
	 */
	@Query(value = "select e from #{#entityName} e where e.topic = :topic and (e.level is null or e.level = :level)")
	List<IdmNotificationConfiguration> findAllByTopicAndWildcardLevel(String topic, NotificationLevel level);
	
	IdmNotificationConfiguration findByTopicAndLevelAndNotificationType(String topic, NotificationLevel level, String notificationType);
	
	List<IdmNotificationConfiguration> findByTopicAndLevel(String topic, NotificationLevel level);
	
	List<IdmNotificationConfiguration> findByTopicAndLevelIsNull(String topic);
	
	IdmNotificationConfiguration findByTopicAndNotificationTypeAndLevelIsNull(String topic, String notificationType);
	
	Long countByTopic(@Param("topic") String topic);
}
