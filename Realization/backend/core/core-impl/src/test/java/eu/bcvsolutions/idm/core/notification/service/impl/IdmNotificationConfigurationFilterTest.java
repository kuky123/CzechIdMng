package eu.bcvsolutions.idm.core.notification.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import eu.bcvsolutions.idm.core.CoreModuleDescriptor;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationConfigurationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Filter test for notification configuration
 * 
 * @author Patrik Stloukal
 *
 */
public class IdmNotificationConfigurationFilterTest extends AbstractIntegrationTest {
	
	@Autowired private IdmNotificationConfigurationService idmNotificationConfService;
	@Autowired private IdmNotificationTemplateService idmNotificationTemplateService;

	@Before
	public void login() {
		loginAsAdmin();
	}

	@After
	public void logout() {
		super.logout();
	}

	@Test
	public void testTopicFilter() {
		String text = "someText" + System.currentTimeMillis();
		NotificationConfigurationDto notification = createNotification(NotificationLevel.SUCCESS, CoreModuleDescriptor.MODULE_ID + ":test001", text,
				null);
		IdmNotificationConfigurationFilter filter = new IdmNotificationConfigurationFilter();
		filter.setText("core:test001");
		filter.setNotificationType(text);
		Page<NotificationConfigurationDto> result = idmNotificationConfService.find(filter, null);
		assertEquals(1, result.getTotalElements());
		assertEquals(notification.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testLevelFilter() {
		String text = "someText" + System.currentTimeMillis();
		NotificationConfigurationDto notification = createNotification(NotificationLevel.ERROR, CoreModuleDescriptor.MODULE_ID + ":test002", text,
				null);
		IdmNotificationConfigurationFilter filter = new IdmNotificationConfigurationFilter();
		filter.setLevel(NotificationLevel.ERROR);
		filter.setNotificationType(text);
		Page<NotificationConfigurationDto> result = idmNotificationConfService.find(filter, null);
		assertEquals(1, result.getTotalElements());
		assertEquals(notification.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testTemplateIdFilter() {
		String text = "someText" + System.currentTimeMillis();
		IdmNotificationTemplateDto templ = createTemplate("template " + System.currentTimeMillis(), "code",
				"testFilter");
		NotificationConfigurationDto notification = createNotification(NotificationLevel.SUCCESS, CoreModuleDescriptor.MODULE_ID + ":test003", text,
				templ.getId());
		IdmNotificationConfigurationFilter filter = new IdmNotificationConfigurationFilter();
		filter.setTemplate(templ.getId());
		filter.setNotificationType(text);
		Page<NotificationConfigurationDto> result = idmNotificationConfService.find(filter, null);
		assertEquals(1, result.getTotalElements());
		assertEquals(notification.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testNotificationTypeFilter() {
		String text = "someText" + System.currentTimeMillis();
		NotificationConfigurationDto notification = createNotification(NotificationLevel.SUCCESS, CoreModuleDescriptor.MODULE_ID + ":test004", text,
				null);
		IdmNotificationConfigurationFilter filter = new IdmNotificationConfigurationFilter();
		filter.setNotificationType(text);
		filter.setText("core:test004");
		Page<NotificationConfigurationDto> result = idmNotificationConfService.find(filter, null);
		assertEquals(1, result.getTotalElements());
		assertEquals(notification.getId(), result.getContent().get(0).getId());
	}

	/**
	 * Creates notification configuration, saves it in service and returns it
	 * 
	 * @return
	 * 
	 */
	private NotificationConfigurationDto createNotification(NotificationLevel level, String topic,
			String notificationType, UUID templateId) {
		NotificationConfigurationDto notification = new NotificationConfigurationDto();
		notification.setLevel(level);
		notification.setTopic(topic);
		notification.setNotificationType(notificationType);
		notification.setTemplate(templateId);
		notification = idmNotificationConfService.save(notification);
		return notification;
	}

	/**
	 * Creates and returns notification template
	 * 
	 * @return
	 */
	private IdmNotificationTemplateDto createTemplate(String name, String code, String subject) {
		IdmNotificationTemplateDto templ = new IdmNotificationTemplateDto();
		templ.setName(name);
		templ.setCode(code);
		templ.setSubject(subject);
		return idmNotificationTemplateService.save(templ);
	}
}
