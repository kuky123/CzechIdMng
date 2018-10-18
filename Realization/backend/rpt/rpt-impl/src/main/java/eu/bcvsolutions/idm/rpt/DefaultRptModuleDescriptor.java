package eu.bcvsolutions.idm.rpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.domain.PropertyModuleDescriptor;
import eu.bcvsolutions.idm.core.api.domain.ResultCode;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.entity.IdmEmailLog;
import eu.bcvsolutions.idm.core.security.api.domain.GroupPermission;
import eu.bcvsolutions.idm.rpt.api.domain.RptGroupPermission;
import eu.bcvsolutions.idm.rpt.api.domain.RptResultCode;

/**
 * Report module descriptor
 * 
 * @author Radek Tomiška
 *
 */
@Component
@PropertySource("classpath:module-" + DefaultRptModuleDescriptor.MODULE_ID + ".properties")
@ConfigurationProperties(prefix = "module." + DefaultRptModuleDescriptor.MODULE_ID + ".build", ignoreUnknownFields = true, ignoreInvalidFields = true)
public class DefaultRptModuleDescriptor extends PropertyModuleDescriptor implements RptModuleDescriptor {
	
	@Override
	public boolean isDocumentationAvailable() {
		return true;
	}
	
	@Override
	public List<GroupPermission> getPermissions() {
		return Arrays.asList(RptGroupPermission.values());
	}
	
	@Override
	public List<NotificationConfigurationDto> getDefaultNotificationConfigurations() {
		List<NotificationConfigurationDto> configs = new ArrayList<>();
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_REPORT_GENERATE_SUCCESS, 
				null, 
				IdmEmailLog.NOTIFICATION_TYPE,
				"Send notification, after report is successfully generated.", 
				getNotificationTemplateId("reportGenerateSuccess")));
		//
		configs.add(new NotificationConfigurationDto(
				TOPIC_REPORT_GENERATE_FAILED,
				null, 
				IdmEmailLog.NOTIFICATION_TYPE,
				"Send notification, after report generation failed.", 
				getNotificationTemplateId("reportGenerateFailed")));
		//
		return configs;
	}

	@Override
	public List<ResultCode> getResultCodes() {
		return Arrays.asList(RptResultCode.values());
	}
}
