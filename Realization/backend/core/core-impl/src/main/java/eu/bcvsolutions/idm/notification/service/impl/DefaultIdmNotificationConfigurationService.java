package eu.bcvsolutions.idm.notification.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.filter.EmptyFilter;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;
import eu.bcvsolutions.idm.core.api.service.ModuleService;
import eu.bcvsolutions.idm.notification.domain.BaseNotification;
import eu.bcvsolutions.idm.notification.entity.IdmNotificationConfiguration;
import eu.bcvsolutions.idm.notification.entity.IdmNotificationLog;
import eu.bcvsolutions.idm.notification.repository.IdmNotificationConfigurationRepository;
import eu.bcvsolutions.idm.notification.service.api.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.notification.service.api.NotificationSender;

/**
 * Configuration for notification routing
 * 
 * @author Radek Tomiška
 *
 */
@Service
public class DefaultIdmNotificationConfigurationService 
		extends AbstractReadWriteEntityService<IdmNotificationConfiguration, EmptyFilter> 
		implements IdmNotificationConfigurationService {
	
	private final IdmNotificationConfigurationRepository repository;
	private final PluginRegistry<NotificationSender<?>, String> notificationSenders;
	private final ModuleService moduleService;
	
	@Autowired
	public DefaultIdmNotificationConfigurationService(
			IdmNotificationConfigurationRepository repository,
			List<? extends NotificationSender<?>> notificationSenders,
			ModuleService moduleService) {
		super(repository);		
		//
		Assert.notEmpty(notificationSenders);
		Assert.notNull(moduleService);
		//
		this.repository = repository;
		this.notificationSenders = OrderAwarePluginRegistry.create(notificationSenders);
		this.moduleService = moduleService;
	}
	
	/**
	 * Inits default notification configuration from all module descriptors.
	 */
	@Override
	@Transactional
	public void initDefaultTopics() {
		moduleService.getInstalledModules().forEach(module -> {
			Set<String> topicToCreate = new HashSet<>();
			module.getDefaultNotificationConfigurations().forEach(config -> {
				String topic = config.getTopic();
				Long count = repository.countByTopic(topic);
				if (topicToCreate.contains(topic) || count == 0) {
					topicToCreate.add(topic);
					repository.save(new IdmNotificationConfiguration(config));
				}
			});
		});
	}
	
	@Override
	public List<NotificationSender<?>> getDefaultSenders() {
		List<NotificationSender<?>> senders = new ArrayList<>();
		senders.add(notificationSenders.getPluginFor("console")); // TODO: logger sender, configuration, nothing?
		return Collections.unmodifiableList(senders);
	}
	
	@Override
	public List<NotificationSender<?>> getSenders(BaseNotification notification) {
		Assert.notNull(notification);
		Assert.notNull(notification.getMessage());
		//
		String topic = notification.getTopic();
		if (StringUtils.isEmpty(notification.getTopic())) {
			return getDefaultSenders();
		}
		List<NotificationSender<?>> senders = new ArrayList<>();
		repository.findTypes(topic, notification.getMessage().getLevel()).forEach(type -> {
			if (notificationSenders.hasPluginFor(type)) {
				senders.add(notificationSenders.getPluginFor(type));
			}
		});
		//
		if (senders.isEmpty()) {
			return getDefaultSenders();
		}
		return senders;
	}
	
	@Override
	public Set<String> getSupportedNotificationTypes() {
		Set<String> types = new HashSet<>();
		notificationSenders.getPlugins().forEach(sender -> {
			String type = sender.getType();
			if (!IdmNotificationLog.NOTIFICATION_TYPE.equals(type)) { // we does not want NotificationManager's type (just notification envelope).
				types.add(sender.getType());
			}
		});
		return types;
	}

}