package eu.bcvsolutions.idm.core.config.domain;

import java.text.MessageFormat;

import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.core.api.config.domain.AbstractConfiguration;
import eu.bcvsolutions.idm.core.api.config.domain.RequestConfiguration;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;

/**
 * Configuration for requests.
 * 
 * @author svandav
 *
 */
@Service(value = "requestConfiguration")
public class DefaultRequestConfiguration extends AbstractConfiguration implements RequestConfiguration {

	@Override
	public String getRequestApprovalProcessKey(Class<? extends Requestable> entityType) {
		if (entityType == null) {
			return null;
		}
		String entityNameCamel = entityType.getSimpleName();
		StringBuilder entityNameBuilder = new StringBuilder();

		for (String word : entityNameCamel.split(CAMEL_SPLIT_REGEX)) {
			if(word.equalsIgnoreCase("dto")) {
				continue;
			}
			entityNameBuilder.append(word.toLowerCase());
			entityNameBuilder.append('-');
		}
		String entityName = entityNameBuilder.toString();
		entityName = entityName.substring(0, entityName.length() - 1);

		return getConfigurationService().getValue(MessageFormat.format("{0}.{1}.wf", PROPERTY_WF_PREFIX, entityName), DEFAULT_APROVAL_PROCESS_KEY);
	}

	@Override
	public boolean isRequestModeEnabled(Class<? extends BaseDto> entityType) {
		if (entityType == null) {
			return false;
		}
		if (!Requestable.class.isAssignableFrom(entityType)) {
			return false;
		}
		
		// TODO: All requests are controlled by role's property for now!
		// On FE too!
		entityType = IdmRoleDto.class;
		//
		
		String entityNameCamel = entityType.getSimpleName();
		StringBuilder entityNameBuilder = new StringBuilder();
		// TODO: Use @SpinalCase utility + replace last -dto
		for (String word : entityNameCamel.split(CAMEL_SPLIT_REGEX)) {
			if(word.equalsIgnoreCase("dto")) {
				continue;
			}
			entityNameBuilder.append(word.toLowerCase());
			entityNameBuilder.append('-');
		}
		String entityName = entityNameBuilder.toString();
		entityName = entityName.substring(0, entityName.length() - 1);

		return getConfigurationService().getBooleanValue(MessageFormat.format("{0}.{1}.enabled", PROPERTY_PUBLIC_PREFIX, entityName), false);
	}

}
