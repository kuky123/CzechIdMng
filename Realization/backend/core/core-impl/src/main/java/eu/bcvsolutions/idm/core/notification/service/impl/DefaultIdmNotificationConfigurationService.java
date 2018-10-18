package eu.bcvsolutions.idm.core.notification.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity_;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.ModuleService;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.BaseNotification;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationConfigurationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationSender;
import eu.bcvsolutions.idm.core.notification.entity.IdmConsoleLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationConfiguration;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationConfiguration_;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationLog;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationConfigurationRepository;

/**
 * Configuration for notification routing
 * 
 * @author Radek Tomiška
 *
 */
@Service("notificationConfigurationService")
public class DefaultIdmNotificationConfigurationService
	extends AbstractReadWriteDtoService<NotificationConfigurationDto, IdmNotificationConfiguration, IdmNotificationConfigurationFilter> 
    implements IdmNotificationConfigurationService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmNotificationConfigurationService.class);
	//
	@Autowired private ApplicationContext context;
	//
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
	
	@Override
	@Transactional
	public NotificationConfigurationDto saveInternal(NotificationConfigurationDto dto) {
		Assert.notNull(dto);
		//
		// check duplicity
		IdmNotificationConfiguration duplicitEntity = repository.findByTopicAndLevelAndNotificationType(dto.getTopic(), dto.getLevel(), dto.getNotificationType());
		if (duplicitEntity != null && !duplicitEntity.getId().equals(dto.getId())) {
			throw new ResultCodeException(CoreResultCode.NOTIFICATION_TOPIC_AND_LEVEL_EXISTS, ImmutableMap.of("topic", dto.getTopic()));
		}
		//
		// check recipient is filled when redirect is enabled
		if (dto.isRedirect()) {
			if (getRecipients(dto).isEmpty()) {
				// redirect and no recipient is configured => exception
				throw new ResultCodeException(CoreResultCode.NOTIFICATION_CONFIGURATION_RECIPIENT_NOT_FOUND, ImmutableMap.of("topic", dto.getTopic()));
			}
		}
		//
		return super.saveInternal(dto);
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
					UUID template = config.getTemplate();
					NotificationConfigurationDto notConfiguration = new NotificationConfigurationDto(config);
					notConfiguration.setTemplate(template);
					repository.save(toEntity(notConfiguration, null));
				}
			});
		});
	}
	
	@Override
	public List<NotificationSender<?>> getDefaultSenders() {
		List<NotificationSender<?>> senders = new ArrayList<>();
		senders.add(notificationSenders.getPluginFor(IdmConsoleLog.NOTIFICATION_TYPE)); // TODO: global configuration
		return Collections.unmodifiableList(senders);
	}
	
	@Override
	public List<NotificationSender<?>> getSenders(BaseNotification notification) {
		Assert.notNull(notification);
		Assert.notNull(notification.getMessage());
		//
		// default senders for unknown topics
		NotificationLevel level = notification.getMessage().getLevel();
		String topic = notification.getTopic();
		if (StringUtils.isEmpty(notification.getTopic())) {
			return getDefaultSenders();
		}
		// if configuration for given topic is found, but is disabled, 
		// then default senders are not used => noticication is disabled and not be sent.
		boolean disabled = false;
		//
		List<NotificationSender<?>> senders = new ArrayList<>();
		if (!IdmNotificationLog.NOTIFICATION_TYPE.equals(notification.getType())) {
			// concrete sender - configuration was resolved before, check for disabled is not needed now
			NotificationSender<?> sender = getSender(notification.getType());
			if (sender != null) {
				senders.add(sender);
			}
		} else {
			// notification - find all senders by topic and level by configuration
			// check configuration is enabled
			List<IdmNotificationConfiguration> configs = repository.findAllByTopicAndWildcardLevel(topic, level);
			//
			for(IdmNotificationConfiguration config : configs) {
				if (config.isDisabled()) {
					disabled = true;
					LOG.debug("Configuration for topic [{}], level [{}], type [{}] is disabled. "
							+ "Notification will not be sent by this configuration", topic, level, config.getNotificationType());
				} else {
					NotificationSender<?> sender = getSender(config.getNotificationType());
					if (sender != null) {
						senders.add(sender);
					}
				}
			}
		}
		//
		if (senders.isEmpty()) {
			if (disabled) {
				LOG.info("All configurations for topic [{}], level [{}] are disabled. "
						+ "Notification will not be sent.", topic, level);
			} else {
				// configuration not found - return default senders
				return getDefaultSenders();
			}
		}
		return senders;
	}
	
	@Override
	public NotificationSender<?> getSender(String notificationType) {
		if (!notificationSenders.hasPluginFor(notificationType)) {
			return null;
		}
		//
		// default plugin by ordered definition
		NotificationSender<?> sender = notificationSenders.getPluginFor(notificationType);
		String implName = sender.getConfigurationValue(ConfigurationService.PROPERTY_IMPLEMENTATION);
		if (StringUtils.isBlank(implName)) {
			// return default sender - configuration is empty
			return sender;
		}
		//
		try {
			// returns bean by name from filter configuration
			return (NotificationSender<?>) context.getBean(implName);
		} catch (Exception ex) {
			throw new ResultCodeException(
					CoreResultCode.NOTIFICATION_SENDER_IMPLEMENTATION_NOT_FOUND, 
					ImmutableMap.of(
						"implementation", implName,
						"notificationType", notificationType,
						"configurationProperty", sender.getConfigurationPropertyName(ConfigurationService.PROPERTY_IMPLEMENTATION)
						), ex);
		}
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

	@Override
	public Class<? extends BaseEntity> toSenderType(String notificationType) {
		if (StringUtils.isEmpty(notificationType)) {
			return null;
		}
		for(NotificationSender<?> sender : notificationSenders.getPlugins()) {
			if (sender.getType().equals(notificationType)) {
				return sender.getNotificationType();
			}
		}
		throw new CoreException(String.format("Notification type [%s] is not supported.", notificationType));
	}

	@Override
	public NotificationConfigurationDto getConfigurationByTopicLevelNotificationType(String topic, NotificationLevel level, String notificationType) {
		return toDto(this.repository.findByTopicAndLevelAndNotificationType(topic, level, notificationType));
	}
	
	@Override
	public NotificationConfigurationDto getConfigurationByTopicAndNotificationTypeAndLevelIsNull(String topic, String notificationType) {
		return toDto(this.repository.findByTopicAndNotificationTypeAndLevelIsNull(topic, notificationType));
	}
	
	@Override
	public List<NotificationConfigurationDto> getConfigurations(String topic, NotificationLevel level) {
		return toDtos(repository.findByTopicAndLevel(topic, level), false);
	}
	
	@Override
	public List<NotificationConfigurationDto> getWildcardConfigurations(String topic) {
		return toDtos(repository.findByTopicAndLevelIsNull(topic), false);
	}
	
	@Override
	public List<IdmNotificationRecipientDto> getRecipients(NotificationConfigurationDto configuration) {
		Set<String> uniqueRecipients = Sets.newHashSet();
		//
		String rawRecipients = configuration.getRecipients();
		if (StringUtils.isNotBlank(rawRecipients)) {
			for(String rawRecipient : StringUtils.split(rawRecipients, ConfigurationService.PROPERTY_MULTIVALUED_SEPARATOR)) {
				if (StringUtils.isNotBlank(rawRecipient)) {
					uniqueRecipients.add(rawRecipient.trim());
				}
			}
		}
		return uniqueRecipients
				.stream()
				.sorted()
				.map(recipient -> {
					return new IdmNotificationRecipientDto(recipient);
				})
				.collect(Collectors.toList());
	}
	
	@Override
	protected List<Predicate> toPredicates(Root<IdmNotificationConfiguration> root, CriteriaQuery<?> query, CriteriaBuilder builder, IdmNotificationConfigurationFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		//topic of notification configuration
		if (StringUtils.isNotEmpty(filter.getText())) {
			predicates.add(builder.like(builder.lower(root.get(IdmNotificationConfiguration_.topic)),( "%" + filter.getText().toLowerCase() + "%")));
		}
		//level of notification configuration
		if (filter.getLevel() != null) {
			predicates.add(builder.equal(root.get(IdmNotificationConfiguration_.level), filter.getLevel()));
		}
		//notification type of notification configuration
		if (StringUtils.isNotEmpty(filter.getNotificationType())) {
			predicates.add(builder.equal(root.get(IdmNotificationConfiguration_.notificationType), filter.getNotificationType()));
		}
		//template uuid of notification configuration
		if (filter.getTemplate() != null) {
			predicates.add(builder.equal(root.get(IdmNotificationConfiguration_.template).get(AbstractEntity_.id), filter.getTemplate()));
		}
		//disabled notification, default = false
		Boolean disabled = filter.getDisabled();
		if (disabled != null) {
			predicates.add(builder.equal(root.get(IdmNotificationConfiguration_.disabled), disabled));
		}
		//topic of notification configuration without like for searching on BE
		String topic = filter.getTopic();
		if (StringUtils.isNotEmpty(topic)) {
			predicates.add(builder.equal(root.get(IdmNotificationConfiguration_.topic), topic));
		}
		//
		return predicates;
	}

}
