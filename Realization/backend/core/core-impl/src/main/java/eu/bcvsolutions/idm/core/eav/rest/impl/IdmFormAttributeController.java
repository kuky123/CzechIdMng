package eu.bcvsolutions.idm.core.eav.rest.impl;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * EAV Form definitions
 * 
 * @author Radek Tomiška
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/form-attributes")
@Api(
		value = IdmFormAttributeController.TAG, 
		description = "Operations with form attributes (eav)", 
		tags = { IdmFormAttributeController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE)
public class IdmFormAttributeController extends AbstractReadWriteDtoController<IdmFormAttributeDto, IdmFormAttributeFilter>  {

	protected static final String TAG = "Form attributes";
	
	@Autowired
	public IdmFormAttributeController(IdmFormAttributeService service) {
		super(service);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_READ + "')")
	@ApiOperation(
			value = "Search form attributes (/search/quick alias)", 
			nickname = "searchFormAttributes",
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@ResponseBody
	@RequestMapping(value= "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_READ + "')")
	@ApiOperation(
			value = "Search form attributes", 
			nickname = "searchQuickFormAttributes", 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.findQuick(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete form attributes (selectbox usage)", 
			nickname = "autocompleteFormAttributes", 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/count", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_COUNT + "')")
	@ApiOperation(
			value = "The number of entities that match the filter", 
			nickname = "countFormAttributes", 
			tags = { IdmFormAttributeController.TAG },
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_COUNT, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_COUNT, description = "") })
				})
	public long count(@RequestParam(required = false) MultiValueMap<String, Object> parameters) {
		return super.count(parameters);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_READ + "')")
	@ApiOperation(
			value = "Form attribute detail", 
			nickname = "getFormAttribute", 
			response = IdmFormAttributeDto.class, 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_CREATE + "')"
			+ " or hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_UPDATE + "')")
	@ApiOperation(
			value = "Create / update form attribute", 
			nickname = "postFormAttribute", 
			response = IdmFormAttributeDto.class, 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@Valid @RequestBody IdmFormAttributeDto dto) {
		return super.post(dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_UPDATE + "')")
	@ApiOperation(
			value = "Update form attribute",
			nickname = "putFormAttribute", 
			response = IdmFormAttributeDto.class, 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "") })
				})
	public ResponseEntity<?> put(
			@ApiParam(value = "Form attribute's uuid identifier", required = true)
			@PathVariable @NotNull String backendId, 
			@Valid @RequestBody IdmFormAttributeDto dto) {
		return super.put(backendId, dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PATCH)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_UPDATE + "')")
	@ApiOperation(
			value = "Patch form attribute", 
			nickname = "patchFormAttribute", 
			response = IdmFormAttributeDto.class, 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_UPDATE, description = "") })
				})
	public ResponseEntity<?> patch(
			@ApiParam(value = "Form attribute's uuid identifier", required = true)
			@PathVariable @NotNull String backendId,
			HttpServletRequest nativeRequest)
			throws HttpMessageNotReadableException {
		return super.patch(backendId, nativeRequest);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_DELETE + "')")
	@ApiOperation(
			value = "Delete form attribute", 
			nickname = "deleteFormAttribute",
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Form attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.FORM_ATTRIBUTE_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnFormAttribute", 
			tags = { IdmFormAttributeController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.FORM_ATTRIBUTE_READ, description = "") })
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	
	@Override
	public void deleteDto(IdmFormAttributeDto dto) {
		// attribute flagged as system attribute can't be deleted from controller
		if (dto.isUnmodifiable()) {
			throw new ResultCodeException(CoreResultCode.FORM_ATTRIBUTE_DELETE_FAILED_SYSTEM_ATTRIBUTE, ImmutableMap.of("code", dto.getCode()));
		}
		super.deleteDto(dto);
	}
	
	@Override
	protected IdmFormAttributeDto validateDto(IdmFormAttributeDto dto) {
		// check if exist id = create entity, then check if exist old entity = create entity with id
		if (dto.getId() == null || getService().isNew(dto)) {
			return super.validateDto(dto);
		}
		IdmFormAttributeDto previousDto = getDto(dto.getId());
		if (previousDto != null && previousDto.isUnmodifiable()) {
			// check explicit attributes that can't be changed
			if (!previousDto.getCode().equals(dto.getCode())) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "name", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.getPersistentType() != dto.getPersistentType()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "persistentType", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isConfidential() != dto.isConfidential()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "confidential", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isRequired() != dto.isRequired()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "required", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isReadonly() != dto.isReadonly()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "readonly", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isMultiple() != dto.isMultiple()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "multiple", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isRequired() != dto.isRequired()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "required", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isUnmodifiable() != dto.isUnmodifiable()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "unmodifiable", "class", dto.getClass().getSimpleName()));
			}
		}
		return super.validateDto(dto);
	}
}
