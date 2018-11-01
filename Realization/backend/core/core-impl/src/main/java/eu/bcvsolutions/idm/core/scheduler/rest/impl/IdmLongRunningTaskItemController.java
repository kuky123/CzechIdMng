package eu.bcvsolutions.idm.core.scheduler.rest.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
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

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmProcessedTaskItemDto;
import eu.bcvsolutions.idm.core.scheduler.api.dto.filter.IdmProcessedTaskItemFilter;
import eu.bcvsolutions.idm.core.scheduler.api.service.IdmProcessedTaskItemService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Default controller for Processed Task Item
 *
 * @author Marek Klement
 * @author Radek Tomiška
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/long-running-task-items")
@Api(
		value = IdmLongRunningTaskItemController.TAG,
		description = "Operations with processed task items",
		tags = { IdmLongRunningTaskItemController.TAG })
public class IdmLongRunningTaskItemController extends AbstractReadWriteDtoController<IdmProcessedTaskItemDto, IdmProcessedTaskItemFilter> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdmLongRunningTaskItemController.class);
	protected static final String TAG = "Long running task items";
	//
	private final IdmProcessedTaskItemService itemService;

	@Autowired
	public IdmLongRunningTaskItemController(IdmProcessedTaskItemService itemService) {
		super(itemService);
		//
		Assert.notNull(itemService);
		//
		this.itemService = itemService;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.SCHEDULER_READ + "')")
	@ApiOperation(
			value = "Processed task items",
			nickname = "getProcessedTaskItems",
			response = IdmProcessedTaskItemDto.class,
			tags={ IdmLongRunningTaskItemController.TAG},
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") })
			})
	public ResponseEntity<?> get(
			@ApiParam(value = "Processed task's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.SCHEDULER_READ + "')")
	@ApiOperation(value = "Search processed task's items (/search/quick alias)", nickname = "searchProcessedTaskItems", tags={ IdmLongRunningTaskItemController.TAG }, authorizations = {
			@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
					@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") }),
			@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
					@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") })
	})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	/**
	 * All endpoints will support find quick method.
	 *
	 * @param parameters
	 * @param pageable
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value= "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.SCHEDULER_READ + "')")
	@ApiOperation(value = "Search processed task's items", nickname = "searchQuickProcessedTaskItems", tags={ IdmLongRunningTaskItemController.TAG }, authorizations = {
			@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
					@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") }),
			@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
					@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_READ, description = "") })
	})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}
	
	@Override
	public Page<IdmProcessedTaskItemDto> find(IdmProcessedTaskItemFilter filter, Pageable pageable, BasePermission permission) {
		Page<IdmProcessedTaskItemDto> dtos = super.find(filter, pageable, permission);
		Map<UUID, BaseDto> loadedDtos = new HashMap<>();
		dtos.forEach(dto -> {
			loadEmbeddedEntity(loadedDtos, dto);
		});
		//
		return dtos;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.SCHEDULER_DELETE + "')")
	@ApiOperation(
			value = "Delete record",
			nickname = "deleteRecord",
			tags = { IdmLongRunningTaskItemController.TAG },
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_DELETE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_DELETE, description = "") })
			})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Records's uuid identifier", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}

	@ResponseBody
	@RequestMapping(value = "/{backendId}/queue-item", method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.SCHEDULER_CREATE + "')")
	@ApiOperation(
			value = "Create record",
			nickname = "createRecord",
			tags = { IdmLongRunningTaskItemController.TAG },
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_CREATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.SCHEDULER_CREATE, description = "") })
			})
	public ResponseEntity<?> addToQueue(
			@ApiParam(value = "Records's uuid identifier", required = true)
			@PathVariable @NotNull String backendId, @Valid @RequestBody UUID scheduledTask) {
		IdmProcessedTaskItemDto itemDto = itemService.get(backendId);
		itemService.createQueueItem(itemDto, new OperationResult(OperationState.EXECUTED), scheduledTask);
		//
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@Override
	protected IdmProcessedTaskItemFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmProcessedTaskItemFilter filter = super.toFilter(parameters);
		return filter;
	}
	
	/**
	 * Fills referenced entity to dto - prevent to load entity for each row
	 * 
	 * @param dto
	 */
	private void loadEmbeddedEntity(Map<UUID, BaseDto> loadedDtos, IdmProcessedTaskItemDto dto) {
		UUID entityId = dto.getReferencedEntityId();
		try {
			if (!loadedDtos.containsKey(entityId)) {
				loadedDtos.put(entityId, getLookupService().lookupDto(dto.getReferencedDtoType(), entityId));
			}
			dto.getEmbedded().put("referencedEntityId", loadedDtos.get(entityId));
		} catch (IllegalArgumentException ex) {
			LOG.debug("Class [{}] not found on classpath (e.g. module was uninstalled)", dto.getReferencedDtoType(), ex);
		}
	}
}
