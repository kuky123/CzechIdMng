package eu.bcvsolutions.idm.core.rest.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityStateFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmEntityStateService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmGroupPermission;
import io.swagger.annotations.Api;

/**
 * Entity states
 * 
 * @author Radek Tomiška
 *
 */
@PreAuthorize("hasAuthority('" + IdmGroupPermission.APP_ADMIN + "')")
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/entity-states")
@Api(
		value = IdmEntityStateController.TAG, 
		description = "Operations with entity states", 
		tags = { IdmEntityStateController.TAG }, 
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmEntityStateController extends DefaultReadWriteDtoController<IdmEntityStateDto, IdmEntityStateFilter> {
	
	protected static final String TAG = "Entity states";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdmEntityStateController.class);
	//
	@Autowired private EntityEventManager manager;
	
	@Autowired
	public IdmEntityStateController(IdmEntityStateService service) {
		super(service);
	}
	
	@Override
	public Page<IdmEntityStateDto> find(IdmEntityStateFilter filter, Pageable pageable, BasePermission permission) {
		Page<IdmEntityStateDto> results = super.find(filter, pageable, permission);
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
	protected IdmEntityStateFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmEntityStateFilter filter = new IdmEntityStateFilter(parameters);
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
		filter.setEventId(getParameterConverter().toUuid(parameters, "eventId"));
		return filter;
	}
}
