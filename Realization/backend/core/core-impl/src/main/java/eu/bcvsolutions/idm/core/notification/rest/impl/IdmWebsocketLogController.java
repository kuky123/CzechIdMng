package eu.bcvsolutions.idm.core.notification.rest.impl;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadDtoController;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmWebsocketLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.service.IdmWebsocketLogService;
import eu.bcvsolutions.idm.core.notification.domain.NotificationGroupPermission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Read websocket logs
 * 
 * @author Radek Tomiška
 * @deprecated @since 9.2.0 websocket notification will be removed
 */
//@RestController
//@RequestMapping(value = BaseDtoController.BASE_PATH + "/notification-websockets")
//@Api(
//		value = IdmWebsocketLogController.TAG, 
//		description = "Websocket history", 
//		tags = { IdmWebsocketLogController.TAG }, 
//		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
//		consumes = MediaType.APPLICATION_JSON_VALUE)
@Deprecated
public class IdmWebsocketLogController extends AbstractReadDtoController<IdmWebsocketLogDto, IdmNotificationFilter> {
	
	protected static final String TAG = "Notification logs - websocket";
	
	@Autowired
	public IdmWebsocketLogController(IdmWebsocketLogService entityLookupService) {
		super(entityLookupService);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + NotificationGroupPermission.NOTIFICATION_READ + "')")
	@ApiOperation(
			value = "Search websocket logs (/search/quick alias)", 
			nickname = "searchWebsocketLogs", 
			tags = { IdmWebsocketLogController.TAG }, 
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
			value = "Search websocket logs", 
			nickname = "searchQuickWebsocketLogs", 
			tags = { IdmWebsocketLogController.TAG }, 
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
			value = "Websocket log detail", 
			nickname = "getWebsocketLog", 
			response = IdmWebsocketLogDto.class, 
			tags = { IdmWebsocketLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = NotificationGroupPermission.NOTIFICATION_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Websocket log's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
}
