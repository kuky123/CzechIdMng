package eu.bcvsolutions.idm.core.rest.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityEventDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityEventFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmEntityEventService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Entity events
 * 
 * @author Radek Tomiška
 *
 */
@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/entity-events")
@Api(
		value = IdmEntityEventController.TAG, 
		description = "Operations with entity events", 
		tags = { IdmEntityEventController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmEntityEventController extends DefaultReadWriteDtoController<IdmEntityEventDto, IdmEntityEventFilter> {
	
	protected static final String TAG = "Entity events";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdmEntityEventController.class);
	//
	private final IdmEntityEventService service;
	//
	@Autowired private EntityEventManager manager;
	
	@Autowired
	public IdmEntityEventController(IdmEntityEventService service) {
		super(service);
		//
		this.service = service;
	}
	
	@ResponseBody
	@RequestMapping(value = "/action/bulk/delete", method = RequestMethod.DELETE)
	@ApiOperation(
			value = "Delete entity events", 
			nickname = "deleteAllEntityEvents",
			tags = { IdmEntityEventController.TAG },
			notes = "Delete all persisted events and their states.")
	public ResponseEntity<?> deleteAll() {
		service.deleteAll();
		//
		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}
	
	@Override
	public Page<IdmEntityEventDto> find(IdmEntityEventFilter filter, Pageable pageable, BasePermission permission) {
		Page<IdmEntityEventDto> results = super.find(filter, pageable, permission);
		// fill entity embedded for FE
		Map<UUID, BaseDto> loadedDtos = new HashMap<>();
		results.getContent().forEach(dto -> {
			UUID ownerId = dto.getOwnerId();
			if (!loadedDtos.containsKey(ownerId)) {
				try {
					loadedDtos.put(ownerId, getLookupService().lookupDto(dto.getOwnerType(), ownerId));
				} catch (IllegalArgumentException ex) {
					LOG.debug("Class [{}] not found on classpath (e.g. module was uninstalled)", dto.getOwnerType(), ex);
				}
			}
			dto.getEmbedded().put("ownerId", loadedDtos.get(ownerId));
		});
		return results;
	}
	
	@Override
	protected IdmEntityEventFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmEntityEventFilter filter = new IdmEntityEventFilter(parameters);
		filter.setCreatedFrom(getParameterConverter().toDateTime(parameters, "createdFrom"));
		filter.setCreatedTill(getParameterConverter().toDateTime(parameters, "createdTill"));
		filter.setOwnerType(getParameterConverter().toString(parameters, "ownerType"));
		//
		String ownerId = getParameterConverter().toString(parameters, "ownerId");
		if (StringUtils.isNotEmpty(filter.getOwnerType()) 
				&& StringUtils.isNotEmpty(ownerId)) {
			// try to find entity owner by Codeable identifier
			AbstractDto owner = manager.findOwner(filter.getOwnerType(), ownerId);
			if (owner != null) {
				filter.setOwnerId(owner.getId());
			} else {
				throw new ResultCodeException(CoreResultCode.BAD_VALUE, "Entity type [%s] with identifier [%s] does not found",
						ImmutableMap.of("entityClass", filter.getOwnerType(), "identifier", ownerId));
			}
		} else {
			filter.setOwnerId(getParameterConverter().toUuid(parameters, "ownerId"));
		}
		filter.setStates(getParameterConverter().toEnums(parameters, "states", OperationState.class));
		return filter;
	}
}
