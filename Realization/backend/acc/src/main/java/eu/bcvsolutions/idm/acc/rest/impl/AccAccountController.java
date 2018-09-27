package eu.bcvsolutions.idm.acc.rest.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AccountType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccAccountFilter;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.ic.api.IcConnectorObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;;

/**
 * Accounts on target system
 * 
 * @author Radek Tomiška
 *
 */
@RestController
@Enabled(AccModuleDescriptor.MODULE_ID)
@RequestMapping(value = BaseDtoController.BASE_PATH + "/accounts")
@Api(
		value = AccAccountController.TAG, 
		tags = AccAccountController.TAG, 
		description = "Account on target system",
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class AccAccountController extends AbstractReadWriteDtoController<AccAccountDto, AccAccountFilter> {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AccAccountController.class);
	protected static final String TAG = "Accounts";
	//
	@Autowired private SysSystemEntityService systemEntityService;
	
	@Autowired
	public AccAccountController(AccAccountService accountService) {
		super(accountService);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_READ + "')")
	@ApiOperation(
			value = "Search accounts (/search/quick alias)", 
			nickname = "searchAccounts",
			tags = { AccAccountController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_READ + "')")
	@RequestMapping(value= "/search/quick", method = RequestMethod.GET)
	@ApiOperation(
			value = "Search accounts", 
			nickname = "searchQuickAccounts",
			tags = { AccAccountController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@Override
	public Page<AccAccountDto> find(AccAccountFilter filter, Pageable pageable, BasePermission permission) {
		Page<AccAccountDto> dtos = super.find(filter, pageable, permission);
		Map<UUID, BaseDto> loadedDtos = new HashMap<>();
		dtos.forEach(dto -> {
			loadEmbeddedEntity(loadedDtos, dto);
		});
		return dtos;
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete accounts (selectbox usage)", 
			nickname = "autocompleteAccounts", 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_READ + "')")
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@ApiOperation(
			value = "Account detail", 
			nickname = "getAccount", 
			response = AccAccountDto.class, 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
	
	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_CREATE + "')"
			+ " or hasAuthority('" + AccGroupPermission.ACCOUNT_UPDATE + "')")
	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(
			value = "Create / update account", 
			nickname = "postAccount", 
			response = AccAccountDto.class, 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_CREATE, description = ""),
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_CREATE, description = ""),
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@RequestBody @NotNull AccAccountDto dto) {
		return super.post(dto);
	}
	
	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_UPDATE + "')")
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@ApiOperation(
			value = "Update account",
			nickname = "putAccount", 
			response = AccAccountDto.class, 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "") })
				})
	public ResponseEntity<?> put(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId,
			@RequestBody @NotNull AccAccountDto dto) {
		return super.put(backendId, dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PATCH)
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_UPDATE + "')")
	@ApiOperation(
			value = "Update account", 
			nickname = "patchAccount", 
			response = AccAccountDto.class, 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_UPDATE, description = "") })
				})
	public ResponseEntity<?> patch(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId,
			HttpServletRequest nativeRequest)
			throws HttpMessageNotReadableException {
		return super.patch(backendId, nativeRequest);
	}
	
	@Override
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_DELETE + "')")
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@ApiOperation(
			value = "Delete account", 
			nickname = "deleteAccount", 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	protected AccAccountDto validateDto(AccAccountDto dto) {
		dto = super.validateDto(dto);
		// preset entity type
		if (dto.getSystemEntity() != null) {
			SysSystemEntityDto systemEntity = systemEntityService.get(dto.getSystemEntity());
			dto.setEntityType(systemEntity.getEntityType());
		}
		if (!getService().isNew(dto)) {
			AccAccountDto previous = getDto(dto.getId());
			if(previous.isInProtection() && !dto.isInProtection()) {
				throw new ResultCodeException(AccResultCode.ACCOUNT_CANNOT_UPDATE_IS_PROTECTED, ImmutableMap.of("uid", dto.getUid()));
			}
		}
		return dto;
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + AccGroupPermission.ACCOUNT_READ + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnAccount", 
			tags = { AccAccountController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = AccGroupPermission.ACCOUNT_READ, description = "") })
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	
	@ResponseBody
	@PreAuthorize("hasAuthority('" + AccGroupPermission.SYSTEM_READ + "')")
	@RequestMapping(value = "/{backendId}/connector-object", method = RequestMethod.GET)
	@ApiOperation(
			value = "Connector object for the account. Contains only attributes for witch have a schema attribute definitons.", 
			nickname = "getConnectorObject", 
			response = IcConnectorObject.class, 
			tags = { SysSystemEntityController.TAG }, 
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = AccGroupPermission.SYSTEM_READ, description = "")}),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = AccGroupPermission.SYSTEM_READ, description = "")})
					})
	public ResponseEntity<IcConnectorObject> getConnectorObject(
			@ApiParam(value = "Account's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		AccAccountDto account = this.getDto(backendId);
		if(account == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		IcConnectorObject connectorObject = ((AccAccountService)getService())
				.getConnectorObject(account, IdmBasePermission.READ);
		if(connectorObject == null) {
			return new ResponseEntity<IcConnectorObject>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<IcConnectorObject>(connectorObject, HttpStatus.OK);
	}
	
	@Override
	protected AccAccountFilter toFilter(MultiValueMap<String, Object> parameters) {
		AccAccountFilter filter = new AccAccountFilter(parameters);
		//
		filter.setSystemId(getParameterConverter().toUuid(parameters, "systemId"));
		filter.setSystemEntityId(getParameterConverter().toUuid(parameters, "systemEntityId"));
		//
		// for first check identityId, this attribute has bigger priority than identity parameter
		UUID identityId = getParameterConverter().toUuid(parameters, "identityId");
		if (identityId == null) {
			identityId = getParameterConverter().toEntityUuid(parameters, "identity", IdmIdentity.class);
		}
		filter.setIdentityId(identityId);
		//
		filter.setUid(getParameterConverter().toString(parameters, "uid"));
		filter.setAccountType(getParameterConverter().toEnum(parameters, "accountType", AccountType.class));
		filter.setOwnership(getParameterConverter().toBoolean(parameters, "ownership"));
		filter.setSupportChangePassword(getParameterConverter().toBoolean(parameters, "supportChangePassword"));
		filter.setEntityType(getParameterConverter().toEnum(parameters, "entityType", SystemEntityType.class));
		//
		return filter;
	}
	
	/**
	 * Fills referenced entity to dto - prevent to load entity for each row
	 * 
	 * @param dto
	 */
	private void loadEmbeddedEntity(Map<UUID, BaseDto> loadedDtos, AccAccountDto dto) {
		UUID entityId = dto.getTargetEntityId();
		if (entityId == null || StringUtils.isEmpty(dto.getTargetEntityType())) {
			return; // IdM entity is not linked to account 
		}
		try {
			if (!loadedDtos.containsKey(entityId)) {
				loadedDtos.put(entityId, getLookupService().lookupDto(dto.getTargetEntityType(), entityId));
			}
			dto.getEmbedded().put("targetEntityId", loadedDtos.get(entityId));
		} catch (IllegalArgumentException ex) {
			LOG.debug("Class [{}] not found on classpath (e.g. module was uninstalled)", dto.getTargetEntityType(), ex);
		}
	}
}
