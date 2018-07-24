package eu.bcvsolutions.idm.core.rest.impl;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
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

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestItemFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmRequestItemService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Request's items endpoint
 * 
 * @author svandav
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/request-items")
@Api(
		value = IdmRequestItemController.TAG, 
		description = "Operations with request items", 
		tags = { IdmRequestItemController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmRequestItemController extends AbstractReadWriteDtoController<IdmRequestItemDto, IdmRequestItemFilter>{

	protected static final String TAG = "Request's items";
		
	@Autowired
	public IdmRequestItemController(
			IdmRequestItemService service) {
		super(service);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_READ + "')")
	@ApiOperation(
			value = "Search request items (/search/quick alias)", 
			nickname = "searchRequestItems", 
			tags = { IdmRequestItemController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_READ + "')")
	@ApiOperation(
			value = "Search request items", 
			nickname = "searchQuickRequestItems", 
			tags = { IdmRequestItemController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.findQuick(parameters, pageable);
	}
	
	@ResponseBody
	@RequestMapping(value= "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete request items (selectbox usage)", 
			nickname = "autocompleteRequestItems", 
			tags = { IdmRequestItemController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_READ + "')")
	@ApiOperation(
			value = "Request detail item", 
			nickname = "getRequestItem", 
			response = IdmRequestItemDto.class, 
			tags = { IdmRequestItemController.TAG },
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") })
					})
	public ResponseEntity<?> get(
			@ApiParam(value = "Item's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_CREATE + "')"
			+ " or hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_UPDATE + "')")
	@ApiOperation(
			value = "Create / update request item", 
			nickname = "postRequestItem", 
			response = IdmRequestItemDto.class, 
			tags = { IdmRequestItemController.TAG },
			authorizations = { 
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_CREATE, description = ""),
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_UPDATE, description = "")}),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_CREATE, description = ""),
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_UPDATE, description = "")})
					})
	public ResponseEntity<?> post(@RequestBody @NotNull IdmRequestItemDto request) {
		if (getService().isNew(request)) { 
			request.setResult(new OperationResultDto(OperationState.CREATED));
			request.setState(RequestState.CONCEPT);
		}
		return super.post(request);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_UPDATE + "')")
	@ApiOperation(
			value = "Update request item", 
			nickname = "putRequestItem", 
			response = IdmRequestItemDto.class, 
			tags = { IdmRequestItemController.TAG },
			authorizations = { 
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_UPDATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_UPDATE, description = "") })
					})
	public ResponseEntity<?> put(
			@ApiParam(value = "Item's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId, 
			@RequestBody @NotNull IdmRequestItemDto dto) {
		return super.put(backendId, dto);
	}

	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_DELETE + "')")
	@ApiOperation(
			value = "Delete request", 
			nickname = "deleteRequest",
			tags = { IdmRequestItemController.TAG },
			authorizations = { 
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_DELETE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_DELETE, description = "") })
					})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Item's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmRequestItemService service = ((IdmRequestItemService)this.getService());
		IdmRequestItemDto dto = service.get(backendId);
		//
		checkAccess(dto, IdmBasePermission.DELETE);
		//
		// Request in Executed state can not be delete or change
		if(RequestState.EXECUTED == dto.getState()){
			throw new ResultCodeException(CoreResultCode.REQUEST_EXECUTED_CANNOT_DELETE,
					ImmutableMap.of("request", dto));
		}
		
		// Only request in Concept state, can be deleted. In others states, will be request set to Canceled state and save.
		if(RequestState.CONCEPT == dto.getState()){
			service.delete(dto);
		}else {
			dto.setState(RequestState.CANCELED);
			dto.setResult(new OperationResultDto(OperationState.CANCELED));
			service.save(dto);
		}
		
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.REQUEST_ITEM_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnRequest", 
			tags = { IdmRequestItemController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.REQUEST_ITEM_READ, description = "") })
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	

	@Override
	protected IdmRequestItemFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmRequestItemFilter filter = new IdmRequestItemFilter(parameters);
		filter.setStates(getParameterConverter().toEnums(parameters, "states", RequestState.class));
		return filter;
	}
	
}
