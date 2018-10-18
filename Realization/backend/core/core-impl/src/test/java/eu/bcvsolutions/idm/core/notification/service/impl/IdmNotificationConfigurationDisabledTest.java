package eu.bcvsolutions.idm.core.notification.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationLogService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.notification.entity.IdmConsoleLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmEmailLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationLog;
import eu.bcvsolutions.idm.core.notification.repository.IdmEmailLogRepository;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationLogRepository;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for disabled notifications
 * 
 * @author Patrik Stloukal
 *
 */
public class IdmNotificationConfigurationDisabledTest extends AbstractIntegrationTest {

	private static final String TOPIC = "idm:test";

	@Autowired private NotificationManager notificationManager;
	@Autowired private IdmNotificationLogRepository idmNotificationRepository;
	@Autowired private IdmNotificationLogService notificationLogService;
	@Autowired private IdmEmailLogRepository emailLogRepository;
	@Autowired private IdmNotificationTemplateService notificationTemplateService;
	@Autowired private IdmNotificationConfigurationService notificationConfigurationService;
	
	private List<NotificationConfigurationDto> configs = new ArrayList<>();

	@Before
	public void clear() {
		loginAsAdmin();
		emailLogRepository.deleteAll();
		idmNotificationRepository.deleteAll();
	}

	@After
	@Override
	public void logout() {
		super.logout();
	}

	@Test
	@Transactional
	public void testNotDisabledErrorEmail() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), false));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(1, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testDisabledErrorEmail() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), true));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(0, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testNotDisabledErrorWebsocket() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), false));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(1, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testDisabledErrorWebsocket() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), true));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(0, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testDisabledError() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), true));
		configs.add(createNotificationConfiguration(TOPIC, level, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), true));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(0, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testOneDisabledError() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), false));
		configs.add(createNotificationConfiguration(TOPIC, level, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), true));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(1, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testNotDisabledError() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, level, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), false));
		configs.add(createNotificationConfiguration(TOPIC, level, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), false));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(2, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testNotDisabled() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity("Test_disable_notifications" + System.currentTimeMillis());

		configs.add(createNotificationConfiguration(TOPIC, null, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), false));
		configs.add(createNotificationConfiguration(TOPIC, null, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), false));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(2, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	@Test
	@Transactional
	public void testOneDisabled() {
		assertEquals(0, idmNotificationRepository.count());
		NotificationLevel level = NotificationLevel.ERROR;
		
		IdmNotificationTemplateDto template = createTestTemplate("Idm test notification", "disabled test");
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);

		configs.add(createNotificationConfiguration(TOPIC, null, IdmConsoleLog.NOTIFICATION_TYPE, template.getId(), true));
		configs.add(createNotificationConfiguration(TOPIC, null, IdmEmailLog.NOTIFICATION_TYPE, template.getId(), false));
		IdmMessageDto message = new IdmMessageDto();
		message.setTemplate(template);
		message.setLevel(level);

		notificationManager.send(TOPIC, message, identity);
		
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(1, notificationLogService.find(filter, null).getTotalElements());
		deleteNotificationConfig();
	}
	
	/**
	 * Method creates test template
	 * @param body
	 * @param subject
	 * @return
	 */
	private IdmNotificationTemplateDto createTestTemplate(String body, String subject) {
		IdmNotificationTemplateDto template = new IdmNotificationTemplateDto();
		template.setName("test_" + System.currentTimeMillis());
		template.setBodyHtml(body);
		template.setBodyText(body);
		template.setCode(subject);
		template.setSubject(subject);
		return notificationTemplateService.save(template);
	}
	
	/**
	 * Method creates notification configuration to configure sending message
	 * @param topic
	 * @param level
	 * @param notificationType
	 * @param template
	 * @param disabled
	 * @return
	 */
	private NotificationConfigurationDto createNotificationConfiguration(String topic, NotificationLevel level, String notificationType, UUID template, Boolean disabled) {
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic);
		config.setLevel(level);
		config.setNotificationType(notificationType);
		config.setTemplate(template);
		config.setDisabled(disabled);
		return notificationConfigurationService.save(config);
	}
	
	/**
	 * Method will delete all created notification configurations
	 */
	private void deleteNotificationConfig() {
		for (NotificationConfigurationDto config : configs) {
			notificationConfigurationService.delete(config);
		}
		configs = new ArrayList<>();
	}
}
