package eu.bcvsolutions.idm.acc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.core.api.domain.PropertyModuleDescriptor;
import eu.bcvsolutions.idm.core.api.domain.ResultCode;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.entity.IdmConsoleLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmEmailLog;
import eu.bcvsolutions.idm.core.security.api.domain.GroupPermission;

/**
 * Account management module descriptor
 * 
 * TODO: module dependencies - core, ic
 * 
 * @author Radek Tomiška
 *
 */
@Component
@PropertySource("classpath:module-" + AccModuleDescriptor.MODULE_ID + ".properties")
@ConfigurationProperties(prefix = "module." + AccModuleDescriptor.MODULE_ID + ".build", ignoreUnknownFields = true, ignoreInvalidFields = true)
public class AccModuleDescriptor extends PropertyModuleDescriptor {

	public static final String MODULE_ID = "acc";
	public static final String TOPIC_PROVISIONING = String.format("%s:provisioning", MODULE_ID);
	public static final String TOPIC_NEW_PASSWORD = String.format("%s:newPassword", MODULE_ID);
	@Deprecated // use CoreModuleDescriptor.TOPIC_PASSWORD_CHANGED
	public static final String TOPIC_NEW_PASSWORD_ALL_SYSTEMS = String.format("%s:newPasswordAllSystems", MODULE_ID);
	public static final String TOPIC_PROVISIONING_BREAK_WARNING = String.format("%s:provisioningBreakWarning", MODULE_ID);
	public static final String TOPIC_PROVISIONING_BREAK_DISABLE = String.format("%s:provisioningBreakDisable", MODULE_ID);

	@Override
	public String getId() {
		return MODULE_ID;
	}
	
	@Override
	public List<GroupPermission> getPermissions() {
		return Arrays.asList(AccGroupPermission.values());
	}
	
	@Override
	public List<NotificationConfigurationDto> getDefaultNotificationConfigurations() {
		List<NotificationConfigurationDto> configs = new ArrayList<>();
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_PROVISIONING,
				null,
				IdmConsoleLog.NOTIFICATION_TYPE, 
				"Notification with new provisioning", 
				getNotificationTemplateId("provisioningSuccess"))
				);
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_PROVISIONING_BREAK_WARNING,
				null,
				IdmEmailLog.NOTIFICATION_TYPE,
				"This message contains warning with information about provisioning operation.",
				getNotificationTemplateId("provisioningWarning"))
				);
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_PROVISIONING_BREAK_DISABLE,
				null,
				IdmEmailLog.NOTIFICATION_TYPE,
				"This message contains information about blocked system for provisioning operation.",
				getNotificationTemplateId("provisioningDisable"))
				);
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_NEW_PASSWORD, 
				null, 
				IdmEmailLog.NOTIFICATION_TYPE,
				"This message contains new password and information about new account.", 
				getNotificationTemplateId("newPassword"))
				);
		//
		return configs;
	}

	@Override
	public boolean isDocumentationAvailable() {
		return true;
	}

	@Override
	public List<ResultCode> getResultCodes() {
		return Arrays.asList(AccResultCode.values());
	}
}
