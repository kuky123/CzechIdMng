package eu.bcvsolutions.idm.core.rest.impl;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
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
import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.ValueGeneratorDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmGenerateValueFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmGenerateValueService;
import eu.bcvsolutions.idm.core.api.service.ValueGeneratorManager;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Generate values controller
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/generate-values")
@Api(
		value = IdmGenerateValueController.TAG, 
		description = "Operations with generate values", 
		tags = { IdmGenerateValueController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmGenerateValueController extends AbstractReadWriteDtoController<IdmGenerateValueDto, IdmGenerateValueFilter> {

	protected static final String TAG = "Generate values";

	private final ValueGeneratorManager valueGeneratorManager;

	@Autowired
	public IdmGenerateValueController(IdmGenerateValueService entityService, ValueGeneratorManager valueGeneratorManager) {
		super(entityService);
		//
		Assert.notNull(valueGeneratorManager);
		//
		this.valueGeneratorManager = valueGeneratorManager;
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "Search generate values (/search/quick alias)", 
			nickname = "searchGenerateValues", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@ResponseBody
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "Search generate values", 
			nickname = "searchQuickGenerateValues", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/count", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_COUNT + "')")
	@ApiOperation(
			value = "The number of entities that match the filter", 
			nickname = "countGenerateValues", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_COUNT, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_COUNT, description = "") })
				})
	public long count(@RequestParam(required = false) MultiValueMap<String, Object> parameters) {
		return super.count(parameters);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "Generate value detail", 
			nickname = "getGenerateValues", 
			response = IdmGenerateValueDto.class, 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Generate value uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_CREATE + "')"
			+ " or hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_UPDATE + "')")
	@ApiOperation(
			value = "Create / update generate value", 
			nickname = "postGenerateValue", 
			response = IdmGenerateValueDto.class, 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@Valid @RequestBody IdmGenerateValueDto dto) {
		return super.post(dto);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_UPDATE + "')")
	@ApiOperation(
			value = "Update generate value", 
			nickname = "putGenerateValues", 
			response = IdmGenerateValueDto.class, 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_UPDATE, description = "") })
				})
	public ResponseEntity<?> put(
			@ApiParam(value = "Generate value uuid identifier.", required = true)
			@PathVariable @NotNull String backendId, 
			@Valid @RequestBody IdmGenerateValueDto dto) {
		return super.put(backendId, dto);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_DELETE + "')")
	@ApiOperation(
			value = "Delete generate value", 
			nickname = "deleteGenerateValue", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Generate value uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnGenerateValue", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Generate value uuid.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	
	/**
	 * Returns all registered entities for generate values
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/search/supported-types")
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "Get all supported dto types", 
			nickname = "getSupportedTypes", 
			tags = { IdmGenerateValueController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				})
	public ResponseEntity<?> getSupportedTypes() {
		return new ResponseEntity<>(toResources(valueGeneratorManager.getSupportedTypes(), null), HttpStatus.OK);
	}
	
	/**
	 * Returns all registered and enabled generators
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/search/available-generators")
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.GENERATE_VALUE_READ + "')")
	@ApiOperation(
			value = "Get all supported generator", 
			nickname = "getGenerators", 
			tags = { IdmAuthorizationPolicyController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.GENERATE_VALUE_READ, description = "") })
				},
			notes = "Returns all registered and enabled generators.")
	public Resources<ValueGeneratorDto> getAvailableGenerators() {
		return new Resources<>(valueGeneratorManager.getAvailableGenerators(null));
	}
	
	@Override
	public void deleteDto(IdmGenerateValueDto dto) {
		// generator flagged as system can't be deleted from controller
		if (dto.isUnmodifiable()) {
			throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_DELETE_FAILED, ImmutableMap.of("record", dto.getGeneratorType()));
		}
		super.deleteDto(dto);
	}
	
	@Override
	protected IdmGenerateValueDto validateDto(IdmGenerateValueDto dto) {
		if (dto.getId() == null || getService().isNew(dto)) {
			return super.validateDto(dto);
		}
		//
		IdmGenerateValueDto previousDto = getDto(dto.getId());
		if (previousDto != null && previousDto.isUnmodifiable()) {
			// check explicit attributes that can't be changed
			if (previousDto.getSeq() != dto.getSeq()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "seq", "class", dto.getClass().getSimpleName()));
			}
			if (!previousDto.getDtoType().equals(dto.getDtoType())) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "dtoType", "class", dto.getClass().getSimpleName()));
			}
			if (!previousDto.getGeneratorType().equals(dto.getGeneratorType())) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "getGeneratorType", "class", dto.getClass().getSimpleName()));
			}
			if (previousDto.isUnmodifiable() != dto.isUnmodifiable()) {
				throw new ResultCodeException(CoreResultCode.UNMODIFIABLE_ATTRIBUTE_CHANGE, ImmutableMap.of("name", "unmodifiable", "class", dto.getClass().getSimpleName()));
			}
		}
		//
		return super.validateDto(dto);
	}
}
