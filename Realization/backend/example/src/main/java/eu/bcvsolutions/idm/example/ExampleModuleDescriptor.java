package eu.bcvsolutions.idm.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.domain.PropertyModuleDescriptor;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.entity.IdmWebsocketLog;
import eu.bcvsolutions.idm.core.security.api.domain.GroupPermission;
import eu.bcvsolutions.idm.example.domain.ExampleGroupPermission;

/**
 * Example module descriptor
 * 
 * @author Radek Tomiška
 *
 */
@Component
@PropertySource("classpath:module-" + ExampleModuleDescriptor.MODULE_ID + ".properties")
@ConfigurationProperties(prefix = "module." + ExampleModuleDescriptor.MODULE_ID + ".build", ignoreUnknownFields = true, ignoreInvalidFields = true)
public class ExampleModuleDescriptor extends PropertyModuleDescriptor {

	public static final String MODULE_ID = "example";
	public static final String TOPIC_EXAMPLE = String.format("%s:example", MODULE_ID);
	
	@Override
	public String getId() {
		return MODULE_ID;
	}
	
	@Override
	public boolean isDocumentationAvailable() {
		return true;
	}
	
	@Override
	public List<NotificationConfigurationDto> getDefaultNotificationConfigurations() {
		List<NotificationConfigurationDto> configs = new ArrayList<>();
		configs.add(new NotificationConfigurationDto(
				TOPIC_EXAMPLE, 
				null, 
				IdmWebsocketLog.NOTIFICATION_TYPE,
				"Example notification", 
				null));
		return configs;
	}
	
	@Override
	public List<GroupPermission> getPermissions() {
		return Arrays.asList(ExampleGroupPermission.values());
	}
}
