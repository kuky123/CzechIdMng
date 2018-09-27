package eu.bcvsolutions.idm.core.audit.rest.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.RevisionType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.audit.dto.IdmAuditDiffDto;
import eu.bcvsolutions.idm.core.api.audit.dto.IdmAuditDto;
import eu.bcvsolutions.idm.core.api.audit.dto.filter.IdmAuditFilter;
import eu.bcvsolutions.idm.core.api.audit.service.IdmAuditService;
import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * IdM audit endpoint 
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 */

@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/audits")
@Api(
		value = IdmAuditController.TAG, 
		description = "Read / search audit log", 
		tags = { IdmAuditController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmAuditController extends AbstractReadWriteDtoController<IdmAuditDto, IdmAuditFilter> {

	protected static final String TAG = "Audit";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdmAuditController.class);
	//
	private final IdmAuditService auditService;
	private final ModelMapper mapper;
	
	@Autowired
	public IdmAuditController(IdmAuditService auditService, ModelMapper mapper) {
		super(auditService);
		//
		Assert.notNull(mapper);
		//
		this.auditService = auditService;
		this.mapper = mapper;
	}
	
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@RequestMapping(value= "/search/quick", method = RequestMethod.GET)
	@ApiOperation(
			value = "Search audit logs", 
			nickname = "searchQuickAudits", 
			tags = { IdmAuditController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return this.find(parameters, pageable);
	}
	
	@Override
	public Page<IdmAuditDto> find(IdmAuditFilter filter, Pageable pageable, BasePermission permission) {
		Page<IdmAuditDto> dtos = super.find(filter, pageable, permission);
		Map<UUID, BaseDto> loadedDtos = new HashMap<>();
		dtos.forEach(dto -> {
			loadEmbeddedEntity(loadedDtos, dto);
		});
		//
		return dtos;
	}
	
	@SuppressWarnings("unchecked")
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@RequestMapping(value= "/search/entity", method = RequestMethod.GET)
	@ApiOperation(
			value = "Search audit logs related to entity", 
			nickname = "searchEntity", 
			tags = { IdmAuditController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				})
	@ApiImplicitParams({
		@ApiImplicitParam(name = "entity", allowMultiple = false, dataType = "string", paramType = "query",
				value = "Entity class - find related audit log to this class"),
        @ApiImplicitParam(name = "page", dataType = "string", paramType = "query",
                value = "Results page you want to retrieve (0..N)"),
        @ApiImplicitParam(name = "size", dataType = "string", paramType = "query",
                value = "Number of records per page."),
        @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
                value = "Sorting criteria in the format: property(,asc|desc). " +
                        "Default sort order is ascending. " +
                        "Multiple sort criteria are supported.")
	})
	public Resources<?> findEntity(
			@RequestParam(required = false) String entityClass,
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		//
		if (entityClass == null) {
			throw new ResultCodeException(CoreResultCode.AUDIT_ENTITY_CLASS_IS_NOT_FILLED);
		}
		//
		try {
			Page<IdmAuditDto> dtos = auditService.findEntityWithRelation((Class<? extends AbstractEntity>) Class.forName(entityClass), parameters, pageable);
			Map<UUID, BaseDto> loadedDtos = new HashMap<>();
			dtos.forEach(dto -> {
				loadEmbeddedEntity(loadedDtos, dto);
			});
			return toResources(dtos, getDtoClass());
		} catch (ClassNotFoundException e) {
			throw new ResultCodeException(CoreResultCode.AUDIT_ENTITY_CLASS_NOT_FOUND, ImmutableMap.of("class", entityClass), e);
		}

	}
	
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@RequestMapping(value= "/search/entities", method = RequestMethod.GET)
	@ApiOperation(
			value = "Search audited entity classes", 
			nickname = "findAllAuditedEntities", 
			tags = { IdmAuditController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				},
			notes = "Method return list of class simple name for which is audited."
					+ " Must at least one attribute withannotation {@value Audited}")
	public ResponseEntity<?> findAuditedEntity() {
		List<String> entities = auditService.getAllAuditedEntitiesNames();
		return new ResponseEntity<>(toResources(entities, null), HttpStatus.OK);
	}
	
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/{backendId}")
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@Override
	@ApiOperation(
			value = "Audit log detail", 
			nickname = "getAuditLog", 
			response = IdmAuditDto.class, 
			tags = { IdmAuditController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Audit log's identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmAuditDto audit = auditService.get(backendId);
		
		// Map with all values
		Map<String, Object> revisionValues = null;
		
		Object revision = null;
		try {
			revision = auditService.findVersion(Class.forName(audit.getType()), audit.getEntityId(), Long.valueOf(audit.getId().toString()));
		} catch (NumberFormatException | ClassNotFoundException e) {
			throw new ResultCodeException(CoreResultCode.BAD_VALUE, ImmutableMap.of("audit", audit), e);
		}
		
		revisionValues = auditService.getValuesFromVersion(revision);
		
		// create DTO and fill with values from IdmAudit
		IdmAuditDto auditDto = new IdmAuditDto();
		mapper.map(audit, auditDto);
		auditDto.setRevisionValues(revisionValues);
		
		return new ResponseEntity<IdmAuditDto>(auditDto, HttpStatus.OK);
	}
	
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/{revId}/diff/previous")
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@ApiOperation(
			value = "Audit log detail", 
			nickname = "getAuditLogPreviousVersion", 
			response = IdmAuditDto.class, 
			tags = { IdmAuditController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				},
			notes = "Returns previous version for given audit log")
	public ResponseEntity<?> previousVersion(
			@ApiParam(value = "Audit log's identifier.", required = true)
			@PathVariable @NotNull String revId) {
		IdmAuditDto currentAudit = auditService.get(revId);
		IdmAuditDto previousAudit;
		ResponseEntity<IdmAuditDto> resource = null;
		
		try {
			IdmAuditDto dto = null;
			previousAudit = auditService.findPreviousRevision(Long.valueOf(currentAudit.getId().toString()));
			
			// previous version dost'n exist
			if (previousAudit != null) {
				dto = new IdmAuditDto();
				mapper.map(previousAudit, dto);
				dto.setRevisionValues(
						auditService.getValuesFromVersion(
								auditService.findPreviousVersion(
										Class.forName(previousAudit.getType()),
										previousAudit.getEntityId(),
										Long.valueOf(previousAudit.getId().toString()))));
				resource = new ResponseEntity<IdmAuditDto>(dto, HttpStatus.OK);
			} else {
				resource = new ResponseEntity<IdmAuditDto>(HttpStatus.NOT_FOUND);
			}
			
		} catch (ClassNotFoundException e) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("audit class", currentAudit.getType()), e);
		}
		
		return resource;
	}
	
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, value = "/{firstRevId}/diff/{secondRevId}")
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.AUDIT_READ + "')")
	@ApiOperation(
			value = "Audit log diff", 
			nickname = "getAuditLogDiff", 
			response = IdmAuditDiffDto.class, 
			tags = { IdmAuditController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.AUDIT_READ, description = "") })
				},
			notes = "Returns diff between given audit logs versions")
	public ResponseEntity<?> diff(
			@ApiParam(value = "Audit log's identifier.", required = true)
			@PathVariable @NotNull String firstRevId, 
			@ApiParam(value = "Audit log's identifier.", required = true)
			@PathVariable String secondRevId) {
		IdmAuditDiffDto dto = new IdmAuditDiffDto();
		dto.setDiffValues(auditService.getDiffBetweenVersion(Long.valueOf(firstRevId), Long.valueOf(secondRevId)));
		dto.setIdFirstRevision(Long.valueOf(firstRevId));
		dto.setIdSecondRevision(Long.valueOf(secondRevId));
		
		return new ResponseEntity<IdmAuditDiffDto>(dto, HttpStatus.OK);
	}
	
	/**
	 * Fills referenced entity to dto - prevent to load entity for each row
	 * 
	 * @param dto
	 */
	private void loadEmbeddedEntity(Map<UUID, BaseDto> loadedDtos, IdmAuditDto dto) {
		if (!RevisionType.DEL.name().equals(dto.getModification())) {
			UUID entityId = dto.getEntityId();
			if (entityId == null || StringUtils.isEmpty(dto.getType())) {
				return; // just form sure - IdmAudit entity doesn't specify it as required (but it should be)
			}
			try {
				if (!loadedDtos.containsKey(entityId)) {
					loadedDtos.put(entityId, getLookupService().lookupDto(dto.getType(), entityId));
				}
				dto.getEmbedded().put("entityId", loadedDtos.get(entityId));
			} catch (IllegalArgumentException ex) {
				LOG.debug("Class [{}] not found on classpath (e.g. module was uninstalled)", dto.getType(), ex);
			}
		}
	}
}
