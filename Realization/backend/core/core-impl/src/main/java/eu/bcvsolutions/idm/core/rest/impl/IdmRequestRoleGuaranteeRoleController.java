package eu.bcvsolutions.idm.core.rest.impl;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleGuaranteeRoleFilter;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmRoleGuaranteeRoleService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.rest.AbstractRequestDtoController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Request for role guarantee controller - by role
 * 
 * @author svandav
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/requests")
@Api(
		value = IdmRequestRoleGuaranteeRoleController.TAG, 
		description = "Operations with role guarantees by role", 
		tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmRequestRoleGuaranteeRoleController extends AbstractRequestDtoController<IdmRoleGuaranteeRoleDto, IdmRoleGuaranteeRoleFilter> {
	
	protected static final String TAG = "Request for role guarantees - by roles";
	protected static final String REQUEST_SUB_PATH = "/role-guarantee-roles";
	
	@Autowired
	public IdmRequestRoleGuaranteeRoleController(IdmRoleGuaranteeRoleService service) {
		super(service);
	}
	
	@Override
	public String getRequestSubPath(){
		return REQUEST_SUB_PATH;
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH, method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_READ + "')")
	@ApiOperation(
			value = "Search role guarantee roles (/search/quick alias)", 
			nickname = "searchRoleGuaranteeRoles", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") })
				})
	public Resources<?> find(
			@PathVariable @NotNull String requestId,
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(requestId, parameters, pageable);
	}

	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_READ + "')")
	@ApiOperation(
			value = "Search role guarantee roles", 
			nickname = "searchQuickRoleGuaranteeRoles", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") })
				})
	public Resources<?> findQuick(
			@PathVariable @NotNull String requestId,
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(requestId, parameters, pageable);
	}
	
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete role guarantee roles (selectbox usage)", 
			nickname = "autocompleteRoleGuaranteeRoles", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@PathVariable @NotNull String requestId,
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.autocomplete(requestId, parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/search/count", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_COUNT + "')")
	@ApiOperation(
			value = "The number of entities that match the filter", 
			nickname = "countRoleGuaranteeRoles", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_COUNT, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_COUNT, description = "") })
				})
	public long count(@PathVariable @NotNull String requestId, @RequestParam(required = false) MultiValueMap<String, Object> parameters) {
		return super.count(requestId, parameters);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_READ + "')")
	@ApiOperation(
			value = "Role guarantee role detail", 
			nickname = "getRoleGuaranteeRole", 
			response = IdmRoleGuaranteeRoleDto.class, 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@PathVariable @NotNull String requestId,
			@ApiParam(value = "Role guarantee's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(requestId, backendId);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH, method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_CREATE + "')"
			+ " or hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE + "')")
	@ApiOperation(
			value = "Create / update role guarantee role", 
			nickname = "postRoleGuaranteeRole", 
			response = IdmRoleGuaranteeRoleDto.class, 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@PathVariable @NotNull String requestId, @Valid @RequestBody IdmRoleGuaranteeRoleDto dto) {
		return super.post(requestId, dto);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE + "')")
	@ApiOperation(
			value = "Update role guarantee role", 
			nickname = "putRoleGuaranteeRole", 
			response = IdmRoleGuaranteeRoleDto.class, 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "") })
				})
	public ResponseEntity<?> put(
			@PathVariable @NotNull String requestId,
			@ApiParam(value = "Role guarantee's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId, 
			@Valid @RequestBody IdmRoleGuaranteeRoleDto dto) {
		return super.put(requestId, backendId, dto);
	}
	
//	@Override
//	@ResponseBody
//	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/{backendId}", method = RequestMethod.PATCH)
//	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE + "')")
//	@ApiOperation(
//			value = "Update role guarantee role", 
//			nickname = "patchRoleGuaranteeRole", 
//			response = IdmRoleGuaranteeRoleDto.class, 
//			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
//			authorizations = { 
//				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
//						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "") }),
//				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
//						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_UPDATE, description = "") })
//				})
//	public ResponseEntity<?> patch(
//			@ApiParam(value = "Role guarantee's uuid identifier.", required = true)
//			@PathVariable @NotNull String backendId,
//			HttpServletRequest nativeRequest)
//			throws HttpMessageNotReadableException {
//		return super.patch(backendId, nativeRequest);
//	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_DELETE + "')")
	@ApiOperation(
			value = "Delete role guarantee role", 
			nickname = "deleteRoleGuaranteeRole", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@PathVariable @NotNull String requestId,
			@ApiParam(value = "Role guarantee's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(requestId, backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{requestId}"+ REQUEST_SUB_PATH +  "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLEGUARANTEEROLE_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnRoleGuaranteeRole", 
			tags = { IdmRequestRoleGuaranteeRoleController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLEGUARANTEEROLE_READ, description = "") })
				})
	public Set<String> getPermissions(
			@PathVariable @NotNull String requestId,
			@ApiParam(value = "Role guarantee's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(requestId, backendId);
	}
}
