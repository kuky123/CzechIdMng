package eu.bcvsolutions.idm.core.notification.rest.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationState;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationLogService;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.notification.domain.NotificationGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Read and send notifications
 * 
 * @author Radek Tomiška
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/notifications")
@Api(
		value = IdmNotificationLogController.TAG, 
		description = "Operations with notifications, history",
		tags = { IdmNotificationLogController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmNotificationLogController
		extends AbstractReadWriteDtoController<IdmNotificationLogDto, IdmNotificationFilter> {

	protected static final String TAG = "Notification logs - all";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdmNotificationLogController.class);
	private final NotificationManager notificationManager;
	private final IdmNotificationLogService notificationLogService;
	private final IdmNotificationConfigurationService notificationConfigurationService;

	@Autowired
	public IdmNotificationLogController(
			IdmNotificationLogService notificationLogService, 
			NotificationManager notificationManager,
			IdmNotificationConfigurationService notificationConfigurationService) {
		super(notificationLogService);
		//
		Assert.notNull(notificationManager);
		Assert.notNull(notificationConfigurationService);
		//
		this.notificationManager = notificationManager;
		this.notificationLogService = notificationLogService;
		this.notificationConfigurationService = notificationConfigurationService;
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@ApiOperation(
			value = "Search notification logs (/search/quick alias)", 
			nickname = "searchNotificationLogs", 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@ResponseBody
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@ApiOperation(
			value = "Search notification logs", 
			nickname = "searchQuickNotificationLogs", 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@ApiOperation(
			value = "Notification log detail", 
			nickname = "getNotificationLog", 
			response = IdmNotificationLogDto.class, 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Notification log's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
	
	/**
	 * Send notification
	 * 
	 * @param dto
	 */
	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_CREATE + "')"
			+ " or hasAuthority('" + NotificationGroupPermission.NOTIFICATION_UPDATE + "')")
	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(
			value = "Send notification", 
			nickname = "postNotificationLog", 
			response = IdmNotificationLogDto.class, 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_CREATE, description = ""),
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_CREATE, description = ""),
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@RequestBody @NotNull IdmNotificationLogDto dto) {
		return super.post(dto);
	}

	/**
	 * Send notification
	 */
	@Override
	public IdmNotificationLogDto postDto(IdmNotificationLogDto notification) {		
		LOG.debug("Notification log [{}] was created and notification will be send.", notification);
		List<IdmNotificationLogDto> results = notificationManager.send(
				notification.getTopic(), 
				notification.getMessage(), 
				notification.getIdentitySender() == null ? null : (IdmIdentityDto) getLookupService().lookupDto(IdmIdentityDto.class, notification.getIdentitySender()),
				notification.getRecipients()
					.stream()
					.map(recipient -> {
						return (IdmIdentityDto) getLookupService().lookupDto(IdmIdentityDto.class, recipient.getIdentityRecipient());
					}).collect(Collectors.toList())
				);
		// TODO: parent notification should be returned ...
		if (results.isEmpty()) {
			throw new ResultCodeException(CoreResultCode.NOTIFICATION_NOT_SENT, ImmutableMap.of("topic", notification.getTopic()));
		}
		return getDto(results.get(0).getId());
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnNotification", 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "")})
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}

	@ResponseBody
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@RequestMapping(value = "/{backendId}/recipients", method = RequestMethod.GET)
	@ApiOperation(
			value = "Notification recipients", 
			nickname = "getNotificationRecipients", 
			tags = { IdmNotificationLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") })
				})
	public List<IdmNotificationRecipientDto> getRecipients(@PathVariable @NotNull String backendId) {
		return notificationLogService.getRecipientsForNotification(backendId);
	}
	
	@Override
	protected IdmNotificationFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setText(getParameterConverter().toString(parameters, "text"));
		filter.setSender(getParameterConverter().toString(parameters, "sender"));
		filter.setRecipient(getParameterConverter().toString(parameters, "recipient"));
		filter.setState(getParameterConverter().toEnum(parameters, "state", NotificationState.class));
		filter.setFrom(getParameterConverter().toDateTime(parameters, "from"));
		filter.setTill(getParameterConverter().toDateTime(parameters, "till"));
		filter.setSent(getParameterConverter().toBoolean(parameters, "sent"));
		filter.setNotificationType(notificationConfigurationService.toSenderType(getParameterConverter().toString(parameters, "notificationType")));
		filter.setParent(getParameterConverter().toUuid(parameters, "parent"));
		return filter;
	}
}
