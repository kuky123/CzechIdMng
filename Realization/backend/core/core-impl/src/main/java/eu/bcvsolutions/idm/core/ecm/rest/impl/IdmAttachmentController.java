package eu.bcvsolutions.idm.core.ecm.rest.impl;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.filter.IdmAttachmentFilter;
import eu.bcvsolutions.idm.core.ecm.api.entity.AttachableEntity;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.security.api.domain.IdmGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * ECM attachments
 * 
 * @author Radek Tomiška
 * @since 9.2.0
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/attachments")
@Api(
		value = IdmAttachmentController.TAG, 
		description = "Operations with attachments (metadata, upload)", 
		tags = { IdmAttachmentController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE)
public class IdmAttachmentController extends AbstractReadWriteDtoController<IdmAttachmentDto, IdmAttachmentFilter>  {

	protected static final String TAG = "Form attributes";
	//
	private final AttachmentManager attachmentManager;
	
	@Autowired
	public IdmAttachmentController(AttachmentManager attachmentManager) {
		super(attachmentManager);
		//
		this.attachmentManager = attachmentManager;
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Search form attributes (/search/quick alias)", 
			nickname = "searchAttachments",
			tags = { IdmAttachmentController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@ResponseBody
	@RequestMapping(value= "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Search form attributes", 
			nickname = "searchQuickAttachments", 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.findQuick(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Autocomplete form attributes (selectbox usage)", 
			nickname = "autocompleteAttachments", 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/count", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "The number of entities that match the filter", 
			nickname = "countAttachments", 
			tags = { IdmAttachmentController.TAG },
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public long count(@RequestParam(required = false) MultiValueMap<String, Object> parameters) {
		return super.count(parameters);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Form attribute detail", 
			nickname = "getAttachment", 
			response = IdmAttachmentDto.class, 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')"
			+ " or hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Create / update form attribute", 
			nickname = "postAttachment", 
			response = IdmAttachmentDto.class, 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = ""),
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = ""),
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "")})
				})
	public ResponseEntity<?> post(@Valid @RequestBody IdmAttachmentDto dto) {
		return super.post(dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Update form attribute",
			nickname = "putAttachment", 
			response = IdmAttachmentDto.class, 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public ResponseEntity<?> put(
			@ApiParam(value = "Form attribute's uuid identifier", required = true)
			@PathVariable @NotNull String backendId, 
			@Valid @RequestBody IdmAttachmentDto dto) {
		return super.put(backendId, dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PATCH)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Patch form attribute", 
			nickname = "patchAttachment", 
			response = IdmAttachmentDto.class, 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
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
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "Delete form attribute", 
			nickname = "deleteAttachment",
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Form attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnAttachment", 
			tags = { IdmAttachmentController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = IdmGroupPermission.APP_ADMIN, description = "") })
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Attribute's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	
	@ResponseBody
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	@ApiOperation(
			value = "Upload CSR file for given request", 
			nickname = "uploadCertificateRequestCsr", 
			response = IdmAttachmentDto.class, 
			tags = { IdmAttachmentController.TAG }, 
			notes=  "Upload file and save them as attachment.")
	public ResponseEntity<?> upload(
			@NotNull 
			@RequestParam(required = true, name = "fileName")
			String fileName,
			@RequestParam(required = true, name = "data") 
			MultipartFile data) throws IOException {
		// save attachment
		IdmAttachmentDto attachment = new IdmAttachmentDto();
		attachment.setOwnerType(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE);
		attachment.setName(fileName);
		attachment.setMimetype(StringUtils.isBlank(data.getContentType()) ? AttachableEntity.DEFAULT_MIMETYPE : data.getContentType());
		attachment.setInputData(data.getInputStream());
		attachment = attachmentManager.saveAttachment(null, attachment); // owner and version is resolved after attachment is saved
		//
		return new ResponseEntity<>(toResource(attachment), HttpStatus.CREATED);
	}
}
