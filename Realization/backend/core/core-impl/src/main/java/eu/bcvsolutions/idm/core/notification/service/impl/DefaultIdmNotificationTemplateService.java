package eu.bcvsolutions.idm.core.notification.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.DisplayTool;
import org.dom4j.CDATA;
import org.dom4j.DocumentHelper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.jaxb.JaxbCharacterEscapeEncoder;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationTemplateFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationConfiguration_;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationTemplate;
import eu.bcvsolutions.idm.core.notification.jaxb.IdmNotificationTemplateType;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationTemplateRepository;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Default implementation interface {@link IdmNotificationTemplateService} basic
 * method for template engine - apache velocity. Initialization apache velocity
 * is in constructor.
 * 
 * TODO: rewrite toPredicates
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 *
 */
@Service("notificationTemplateService")
public class DefaultIdmNotificationTemplateService extends
		AbstractReadWriteDtoService<IdmNotificationTemplateDto, IdmNotificationTemplate, IdmNotificationTemplateFilter>
		implements IdmNotificationTemplateService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmNotificationTemplateService.class);
	//
	// TODO: script + common template configuration
	private static final String TEMPLATE_FILE_SUFIX = "idm.sec.core.notification.template.fileSuffix";
	private static final String TEMPLATE_DEFAULT_BACKUP_FOLDER = "templates/";
	private static final String DEFAULT_TEMPLATE_FILE_SUFIX = "**/**.xml";
	//
	private final IdmNotificationTemplateRepository repository;
	private final VelocityEngine velocityEngine;
	private final JAXBContext jaxbContext;
	private IdmNotificationConfigurationService notificationConfigurationService;
	//
	@Autowired private ApplicationContext applicationContext;
	@Autowired private ConfigurationService configurationService;
	@Autowired private SecurityService securityService;
	
	@Autowired
	public DefaultIdmNotificationTemplateService(IdmNotificationTemplateRepository repository) {
		super(repository);
		//
		this.repository = repository;
		//
		VelocityEngine velocityEngine = new VelocityEngine();
		// Initialization apache velocity
		velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, LOG);
		velocityEngine.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, Boolean.TRUE);
		velocityEngine.init();
		this.velocityEngine = velocityEngine;
		//
		// init jaxbContext
		try {
			jaxbContext = JAXBContext.newInstance(IdmNotificationTemplateType.class);
		} catch (JAXBException e) {
			// TODO throw error, or just log and continue?
			throw new ResultCodeException(CoreResultCode.XML_JAXB_INIT_ERROR, e);
		}
	}
	
	/**
	 * TODO: rewrite toPredicates and remove this method
	 */
	@Override
	protected Page<IdmNotificationTemplate> findEntities(IdmNotificationTemplateFilter filter, Pageable pageable, BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmNotificationTemplateDto getByCode(String code) {
		return toDto(repository.findOneByCode(code));
	}

	@Override
	@Transactional
	public IdmNotificationTemplateDto save(IdmNotificationTemplateDto entity, BasePermission... permission) {
		return super.save(entity);
	}

	@Override
	@Transactional
	public void delete(IdmNotificationTemplateDto entity, BasePermission... permission) {
		if (entity.isUnmodifiable()) {
			throw new ResultCodeException(CoreResultCode.NOTIFICATION_SYSTEM_TEMPLATE_DELETE_FAILED,
					ImmutableMap.of("template", entity.getName()));
		}
		super.delete(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmNotificationTemplateDto getTemplateByCode(String code) {
		return getByCode(code);
	}

	/**
	 * Return {@link VelocityContext} for create templates
	 * 
	 * @param model
	 * @return
	 */
	private VelocityContext getContext(Map<String, Object> model) {
		return new VelocityContext(model);
	}

	@Override
	public IdmMessageDto buildMessage(IdmMessageDto message, boolean showGuardedString) {
		StringWriter bodyHtml = new StringWriter();
		StringWriter bodyText = new StringWriter();
		StringWriter subject = new StringWriter();
		IdmNotificationTemplateDto template = message.getTemplate() == null ? null : get(message.getTemplate().getId());
		//
		if (template == null) {
			return message;
		}
		// get parameters from messages
		Map<String, Object> model = message.getParameters();
		//
		// create copy of parameters
		Map<String, Object> parameters = new HashMap<>();
		// iterate trough parameters and find GuardedStrings, there may be
		// templates, but no parameters
		if (model != null) {
			for (Entry<String, Object> entry : model.entrySet()) {
				if (entry.getValue() instanceof GuardedString && showGuardedString) {
					parameters.put(entry.getKey(), ((GuardedString) entry.getValue()).asString());
				} else if (entry.getValue() instanceof GuardedString) {
					parameters.put(entry.getKey(), ((GuardedString) entry.getValue()).toString());
				} else {
					parameters.put(entry.getKey(), entry.getValue());
				}
			}
		}
		// prepare html, text, subject
		String html = template.getBodyHtml();
		String text = template.getBodyText();
		String subjectString = template.getSubject();
		// Same parameters for all (html, txt, subject)
		VelocityContext velocityContext = getContext(parameters);
		// include some tools from Apache velocity -
		// http://velocity.apache.org/tools/devel/generic.html#tools
		velocityContext.put("display", new DisplayTool());
		velocityContext.put("date", new DateTool());
		//
		// html and text may not exists, evaluate only if exists
		if (html != null) {
			velocityEngine.evaluate(velocityContext, bodyHtml, template.getCode(), html);
		}
		//
		if (text != null) {
			velocityEngine.evaluate(velocityContext, bodyText, template.getCode(), text);
		}
		// subject must exists
		velocityEngine.evaluate(velocityContext, subject, template.getCode(), subjectString);
		//
		IdmMessageDto newMessage;
		// if is set model from message build with them
		if (message.getModel() != null) {
			newMessage = new IdmMessageDto
					.Builder()
					.setHtmlMessage(bodyHtml.toString())
					.setTextMessage(bodyText.toString())
					.setSubject(StringUtils.isNotEmpty(subject.toString()) ? subject.toString() : message.getModel().getStatusEnum())
					.setLevel(message.getLevel()) // level get from old message
					.setTemplate(template)
					.setParameters(model)
					.setModel(message.getModel()).build();
		} else {
			// Build IdmMessage
			newMessage = new IdmMessageDto
					.Builder()
					.setHtmlMessage(bodyHtml.toString())
					.setTextMessage(bodyText.toString())
					.setSubject(subject.toString())
					.setLevel(message.getLevel()) // level
					.setTemplate(template)
					.setParameters(model)
					.build();
		}
		//
		return newMessage;
	}

	@Override
	public IdmMessageDto buildMessage(IdmMessageDto message) {
		if (message.getTemplate() == null) {
			return message;
		}
		return this.buildMessage(message, false);
	}

	@Override
	@Transactional
	public void init() {
		for (IdmNotificationTemplateType templateType : findTemplates().values()) {
			IdmNotificationTemplateDto template = this.getByCode(templateType.getCode());
			// if template exist don't save it again => init only
			if (template != null) {
				LOG.info("Load template with code [{}], template is already initialized, skipping.", templateType.getCode());
				continue;
			}
			//
			LOG.info("Load template with code [{}], template will be initialized.", templateType.getCode());
			// save
			this.save(typeToDto(templateType, null));
		}
	}

	@Override
	public List<IdmNotificationTemplateDto> findAllSystemTemplates() {
		IdmNotificationTemplateFilter filter = new IdmNotificationTemplateFilter();
		filter.setUnmodifiable(Boolean.TRUE);
		return this.find(filter, null).getContent().stream().collect(Collectors.toList());
	}

	@Override
	public IdmNotificationTemplateDto resolveTemplate(String topic, NotificationLevel level, String notificationType) {
		// find all configuration by topic and level
		NotificationConfigurationDto configuration = getNotificationConfigurationService()
				.getConfigurationByTopicLevelNotificationType(topic, level, notificationType);
		// if configurations is empty, found a wild card configuration
		if (configuration == null) {
			configuration = getNotificationConfigurationService()
					.getConfigurationByTopicAndNotificationTypeAndLevelIsNull(topic, notificationType);
		}
		return DtoUtils.getEmbedded(configuration, IdmNotificationConfiguration_.template, IdmNotificationTemplateDto.class);
	}

	@Override
	public void backup(IdmNotificationTemplateDto dto) {
		String directory = getDirectoryForBackup();
		//
		Marshaller jaxbMarshaller = initJaxbMarshaller();
		//
		File backupFolder = new File(directory);
		if (!backupFolder.exists()) {
			boolean success = backupFolder.mkdirs();
			// if make dir after check if exist, throw error.
			if (!success) {
				LOG.error("Backup for template: {} failed, backup folder path: [{}] can't be created.", dto.getCode(),
						backupFolder.getAbsolutePath());
				throw new ResultCodeException(CoreResultCode.BACKUP_FAIL,
						ImmutableMap.of("code", dto.getCode()));
			}
		}
		//
		IdmNotificationTemplateType type = dtoToType(dto);
		//
		File file = new File(getBackupFileName(directory, dto));
		try {
			jaxbMarshaller.marshal(type, file);
		} catch (JAXBException e) {
			LOG.error("Backup for template: {} failed",
					dto.getCode());
			throw new ResultCodeException(CoreResultCode.BACKUP_FAIL,
					ImmutableMap.of("code", dto.getCode()), e);
		}
	}

	@Override
	public IdmNotificationTemplateDto redeploy(IdmNotificationTemplateDto dto) {
		IdmNotificationTemplateType foundType = findTemplates().get(dto.getCode());
		//
		if (foundType == null) {
			throw new ResultCodeException(CoreResultCode.NOTIFICATION_TEMPLATE_XML_FILE_NOT_FOUND,
					ImmutableMap.of("code", dto.getCode()));
		}
		//
		return deployNewAndBackupOld(dto, foundType);
	}

	@Override
	public List<IdmNotificationLogDto> prepareNotifications(String topic, IdmMessageDto message) {
		Assert.notNull(message);
		List<IdmNotificationLogDto> notifications = new ArrayList<>();
		//
		// find all configuration by topic and level
		List<NotificationConfigurationDto> configurations = getNotificationConfigurationService().getConfigurations(topic, message.getLevel());
		// if configurations is empty, found a wild card configuration
		if (configurations.isEmpty()) {
			configurations = getNotificationConfigurationService().getWildcardConfigurations(topic);
		}
		//
		// if configurations still empty and exists final message send only his message, this message will be sent without type
		if (configurations.isEmpty()) {
			// this state is possible send message to topic that hasn't set any configurations
			IdmNotificationLogDto notification = new IdmNotificationLogDto();
			notification.setTopic(topic);
			notification.setMessage(this.buildMessage(message, false));
			notifications.add(notification);
			return notifications;
		}
		//
		// 1. Priority - Own message in IdmMessage has biggest priority than otherwise settings
		// 2. Priority - Template from IdmMessage has second biggest priority
		// 3. Priority - Get message from configuration by topic
		//
		// html, text and subject is not empty use them
		for (NotificationConfigurationDto configuration : configurations) {
			if (configuration.isDisabled()) {
				LOG.debug("Configuration [{}] for topic [{}], level [{}], type [{}] is disabled. "
						+ "Notification will not be sent by this configuration.", 
						configuration.getId(), topic, message.getLevel(), configuration.getNotificationType());
				continue;
			}
			//			
			// sending notification to original recipients 
			if (!configuration.isRedirect()) {
				notifications.add(createFinalMessage(message, configuration));
			}
			//
			// sending notification to recipients defined in configuration
			List<IdmNotificationRecipientDto> recipients = getNotificationConfigurationService().getRecipients(configuration);
			if (!recipients.isEmpty()) {
				IdmNotificationLogDto notification = createFinalMessage(message, configuration);
				notification.setRecipients(recipients);
				notifications.add(notification);
			} else if (configuration.isRedirect()) {
				// redirect and no recipient is configured => exception
				// just for sure - validation, when configuration is saved, should solve it before => we don't want to fail some operation just because notification is not sent.
				throw new ResultCodeException(CoreResultCode.NOTIFICATION_CONFIGURATION_RECIPIENT_NOT_FOUND, ImmutableMap.of("topic", configuration.getTopic()));
			}
		}
		//
		return notifications;
	}
	
	private IdmNotificationLogDto createFinalMessage(IdmMessageDto message, NotificationConfigurationDto configuration) {
		IdmMessageDto finalMessage = null;
		if (message.getTemplate() != null) {
			// exist template in message
			finalMessage = this.buildMessage(message, false);
		} else if (configuration.getTemplate() != null) {	
			finalMessage = new IdmMessageDto(message);
			finalMessage.setTemplate(DtoUtils.getEmbedded(configuration, IdmNotificationConfiguration_.template, IdmNotificationTemplateDto.class));
			finalMessage = this.buildMessage(finalMessage, false);
		} else {
			finalMessage = message;
		}
		if (!StringUtils.isEmpty(message.getSubject())) {
			finalMessage.setSubject(message.getSubject());
		}
		if (!StringUtils.isEmpty(message.getTextMessage())) {
			finalMessage.setTextMessage(message.getTextMessage());
		}
		if (!StringUtils.isEmpty(message.getHtmlMessage())) {
			finalMessage.setHtmlMessage(message.getHtmlMessage());
		}
		//
		// send message for every found configuration
		IdmNotificationLogDto notification = new IdmNotificationLogDto();
		notification.setTopic(configuration.getTopic());
		notification.setType(configuration.getNotificationType());
		notification.setMessage(finalMessage);
		//
		return notification;
	}

	/**
	 * Create instance of JaxbMarshaller and set required properties to him.
	 * 
	 * @return
	 */
	private Marshaller initJaxbMarshaller() {
		Marshaller jaxbMarshaller = null;
		try {
			jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
			jaxbMarshaller.setProperty(ENCODING_HANDLER, new JaxbCharacterEscapeEncoder());
		} catch (JAXBException e) {
			throw new ResultCodeException(CoreResultCode.XML_JAXB_INIT_ERROR, e);
		}
		return jaxbMarshaller;
	}

	/**
	 * Transform type to dto, if second parameter is null it will be created new
	 * dto.
	 * 
	 * @param type
	 * @param template
	 * @return
	 */
	private IdmNotificationTemplateDto typeToDto(IdmNotificationTemplateType type,
			IdmNotificationTemplateDto template) {
		if (template == null) {
			template = new IdmNotificationTemplateDto();
		}
		//
		if (type == null) {
			return template;
		}
		// transform type to DTO
		template.setCode(type.getCode());
		template.setName(type.getName());
		template.setBodyHtml(type.getBodyHtml());
		template.setBodyText(type.getBodyText());
		template.setModule(type.getModuleId());
		template.setSubject(type.getSubject());
		template.setUnmodifiable(type.isSystemTemplate());
		template.setParameter(type.getParameter());
		template.setSender(type.getSender());
		return template;
	}

	/**
	 * Return simple string as CDATA for XML.
	 * 
	 * @param input
	 * @return
	 */
	public static String getStringAsCdata(String input) {
		CDATA cdata = DocumentHelper.createCDATA(input);
		return cdata.asXML();
	}

	/**
	 * Transform dto to type.
	 * 
	 * @param dto
	 * @return
	 */
	private IdmNotificationTemplateType dtoToType(IdmNotificationTemplateDto dto) {
		IdmNotificationTemplateType type = new IdmNotificationTemplateType();
		if (dto == null) {
			return type;
		}
		// transform DTO to type
		type.setCode(dto.getCode());
		type.setName(dto.getName());
		type.setBodyHtml(dto.getBodyHtml());
		type.setBodyText(dto.getBodyText());
		type.setModuleId(dto.getModule());
		type.setSubject(dto.getSubject());
		type.setSystemTemplate(dto.isUnmodifiable());
		type.setParameter(dto.getParameter());
		type.setSender(dto.getSender());
		return type;
	}

	/**
	 * Return folder for backups. If isn't folder defined in configuration
	 * properties use default folder from system property java.io.tmpdir.
	 * 
	 * @return
	 */
	private String getDirectoryForBackup() {
		String backupPath = configurationService.getValue(BACKUP_FOLDER_CONFIG);
		if (backupPath == null) {
			// if backup path null throw error, backup folder must be set
			throw new ResultCodeException(CoreResultCode.BACKUP_FOLDER_NOT_FOUND,
					ImmutableMap.of("property", BACKUP_FOLDER_CONFIG));
		}
		// apend template default backup folder
		backupPath = backupPath + "/" + TEMPLATE_DEFAULT_BACKUP_FOLDER;
		// add date folder
		DateTime date = new DateTime();
		DecimalFormat decimalFormat = new DecimalFormat("00");
		return backupPath + date.getYear() + decimalFormat.format(date.getMonthOfYear())
				+ decimalFormat.format(date.getDayOfMonth()) + "/";
	}

	/**
	 * Method return path for file. That will be save into backup directory.
	 * 
	 * @param directory
	 * @param template
	 * @return
	 */
	private String getBackupFileName(String directory, IdmNotificationTemplateDto template) {
		return directory + template.getCode() + "_" + securityService.getCurrentUsername() + "_"
				+ System.currentTimeMillis() + EXPORT_FILE_SUFIX;
	}
	
	/**
	 * Return list of {@link IdmNotificationTemplateType} from resources.
	 * {@link IdmNotificationTemplateType} are found by configured locations and by priority - last one wins.
	 * So default location should be configured first, then external, etc. 
	 * 
	 * @return <code, script>
	 */
	private Map<String, IdmNotificationTemplateType> findTemplates() {
		Unmarshaller jaxbUnmarshaller = null;
		//
		try {
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new ResultCodeException(CoreResultCode.XML_JAXB_INIT_ERROR, e);
		}
		// last script with the same is used
		// => last location has the highest priority
		Map<String, IdmNotificationTemplateType> templates = new HashMap<>();
		//
		for(String location : configurationService.getValues(TEMPLATE_FOLDER)) {
			location = location + configurationService.getValue(TEMPLATE_FILE_SUFIX, DEFAULT_TEMPLATE_FILE_SUFIX);
			Map<String, IdmNotificationTemplateType> locationTemplates = new HashMap<>();
			try {
				Resource[] resources = applicationContext.getResources(location);
				LOG.debug("Found [{}] resources on location [{}]", resources == null ? 0 : resources.length, location);
				//
				if (ArrayUtils.isNotEmpty(resources)) {
					for (Resource resource : resources) {
						try {
							IdmNotificationTemplateType templateType = (IdmNotificationTemplateType) jaxbUnmarshaller.unmarshal(resource.getInputStream());
							//
							// log error, if script with the same code was found twice in one resource
							if (locationTemplates.containsKey(templateType.getCode())) {
								LOG.error("More templates with code [{}] found on the same location [{}].",
										templateType.getCode(),
										location);
							}
							// last one wins
							locationTemplates.put(templateType.getCode(), templateType);
						} catch (JAXBException ex) {
							LOG.error("Template validation failed, file name [{}].", resource.getFilename(), ex);
						} catch (IOException ex) {
							LOG.error("Failed get input stream from, file name [{}].", resource.getFilename(), ex);
						}							
					}
					templates.putAll(locationTemplates);
				}
			} catch (IOException ex) {
				throw new ResultCodeException(CoreResultCode.DEPLOY_ERROR, ImmutableMap.of("path", location), ex);
			}
		}
		return templates;
	}

	/**
	 * Method replace all attribute from dto with type attributes, old dto will
	 * be backup to system folder.
	 * 
	 * @param oldTemplate
	 * @param newTemplate
	 * @return
	 */
	private IdmNotificationTemplateDto deployNewAndBackupOld(IdmNotificationTemplateDto oldTemplate,
			IdmNotificationTemplateType newTemplate) {
		// backup
		this.backup(oldTemplate);

		// transform new
		oldTemplate = typeToDto(newTemplate, oldTemplate);
		return this.save(oldTemplate);
	}
	
	/**
	 * Lazy init ... template is injected into senders ... senders are injected into configuration => cyclic dependency
	 * TODO: redesign notification subsystem => configuration => template => senders at bottom
	 * 
	 * @return
	 */
	private IdmNotificationConfigurationService getNotificationConfigurationService() {
		if (notificationConfigurationService == null) {
			notificationConfigurationService = applicationContext.getBean(IdmNotificationConfigurationService.class);
		}
		return notificationConfigurationService;
	}
}
