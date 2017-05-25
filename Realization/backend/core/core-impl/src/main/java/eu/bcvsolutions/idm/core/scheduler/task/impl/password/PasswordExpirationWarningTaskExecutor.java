package eu.bcvsolutions.idm.core.scheduler.task.impl.password;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.core.CoreModuleDescriptor;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordDto;
import eu.bcvsolutions.idm.core.api.dto.filter.PasswordFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.model.service.api.IdmPasswordService;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.service.api.NotificationManager;
import eu.bcvsolutions.idm.core.scheduler.service.impl.AbstractSchedulableStatefulExecutor;

/**
 * Sends warning before password expires.
 * 
 * @author Radek Tomiška
 *
 */
@Service
@DisallowConcurrentExecution
@Description("Sends warning before password expires.")
public class PasswordExpirationWarningTaskExecutor extends AbstractSchedulableStatefulExecutor<IdmPasswordDto> {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PasswordExpirationWarningTaskExecutor.class);
	private static final String PARAMETER_DAYS_BEFORE = "days before";
	private static final Long DEFAULT_DAYS_BEFORE = 7L;
	//
	@Autowired private IdmPasswordService passwordService;
	@Autowired private NotificationManager notificationManager;
	@Autowired private LookupService lookupService;
	@Autowired private ConfigurationService configurationService;
	//
	private LocalDate expiration;
	private Long daysBefore;
	
	@Override
	public void init(Map<String, Object> properties) {
		super.init(properties);
		//
		daysBefore = getParameterConverter().toLong(properties, PARAMETER_DAYS_BEFORE);
		if (daysBefore == null) {
			daysBefore = DEFAULT_DAYS_BEFORE;
		}
		expiration = new LocalDate().plusDays(daysBefore.intValue());
		LOG.debug("Send warning to identities with password expiration less than [{}]", expiration);
	}

	@Override
	public Page<IdmPasswordDto> getItemsToProcess(Pageable pageable) {
		PasswordFilter filter = new PasswordFilter();
		filter.setValidTill(expiration);
		return passwordService.find(filter, pageable);
	}

	@Override
	public Optional<OperationResult> processItem(IdmPasswordDto dto) {
		IdmIdentityDto identity = (IdmIdentityDto) lookupService.lookupDto(IdmIdentityDto.class, dto.getIdentity());
		LOG.info("Sending warning notification to identity [{}], password expires in [{}]",  identity.getUsername(), dto.getValidTill());
		try {
			// TODO: move into configuration or something
			DateTimeFormatter dateFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");
			//
			notificationManager.send(
					CoreModuleDescriptor.TOPIC_PASSWORD_EXPIRATION, 
					new IdmMessageDto
						.Builder(NotificationLevel.WARNING)
						.addParameter("expiration", dateFormat.print(dto.getValidTill()))
						.addParameter("identity", identity)
						.addParameter("url", configurationService.getFrontendUrl(String.format("password/change?username=%s", identity.getUsername())))
						.addParameter("daysBefore", daysBefore)
						.build(), 
					identity);
			return Optional.of(new OperationResult.Builder(OperationState.EXECUTED).build());
		} catch (Exception ex) {
			LOG.error("Sending warning notification to identity [{}], password expires in [{}] failed", dto.getIdentity(), dto.getValidTill(), ex);
			return Optional.of(new OperationResult.Builder(OperationState.EXCEPTION)
					.setCause(ex)
					// TODO: set model
					.build());
		}
	}
	
	@Override
	public List<String> getParameterNames() {
		List<String> parameters = super.getParameterNames();
		parameters.add(PARAMETER_DAYS_BEFORE);
		return parameters;
	}
}