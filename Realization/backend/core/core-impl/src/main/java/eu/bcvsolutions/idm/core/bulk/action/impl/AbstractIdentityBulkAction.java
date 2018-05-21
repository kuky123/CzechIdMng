package eu.bcvsolutions.idm.core.bulk.action.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;
/**
 * Abstract bulk action for all actions on identities
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public abstract class AbstractIdentityBulkAction extends AbstractBulkAction<IdmIdentityDto> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractIdentityBulkAction.class);
	
	@Autowired
	private IdmIdentityService identityService;
	
	@Override
	public boolean supports(Class<? extends AbstractEntity> clazz) {
		return clazz.isAssignableFrom(IdmIdentity.class);
	}
	
	@Override
	public String getEntityClass() {
		return IdmIdentity.class.getName();
	}
	
	@Override
	public String getFilterClass() {
		return IdmIdentityFilter.class.getName();
	}
	
	@Override
	public OperationResult process() {
		IdmBulkActionDto action = this.getAction();
		Assert.notNull(action);
		//
		StringBuilder description = new StringBuilder();
		IdmLongRunningTaskDto longRunningTask = this.getLongRunningTaskService().get(this.getLongRunningTaskId());
		description.append(longRunningTask.getTaskDescription());
		//
		Collection<UUID> identities = null;
		if (action.getIdentifiers() != null) {
			identities = action.getIdentifiers();
			//
			description.append(System.lineSeparator());
			description.append("For filtering is used list of ID's.");
		} else {
			identities = find(transformFilter(action.getTransformedFilter()), null);
			//
			description.append(System.lineSeparator());
			description.append("For filtering is used filter:");
			description.append(System.lineSeparator());
			String filterAsString = Arrays.toString(action.getFilter().entrySet().toArray());
			description.append(filterAsString);
		}
		this.count = Long.valueOf(identities.size());
		this.counter = 0l;
		//
		// update description
		longRunningTask.setTaskDescription(description.toString());
		this.getLongRunningTaskService().save(longRunningTask);
		// we must update state for send new state to websocket
		this.updateState();
		//
		return processIdentities(identities);
	}
	
	/**
	 * Transform filter to {@link IdmIdentityFilter}
	 *
	 * @param filter
	 * @return
	 */
	protected IdmIdentityFilter transformFilter(BaseFilter filter) {
		if (filter instanceof IdmIdentityFilter) {
			return (IdmIdentityFilter) filter;
		}
		throw new ResultCodeException(CoreResultCode.BULK_ACTION_BAD_FILTER, ImmutableMap.of("filter", IdmIdentityFilter.class.getName(), "givenFilter", filter.getClass().getName()));
	}
	
	/**
	 * Process all identities by given list of ID's
	 *
	 * @param identitiesId
	 * @return
	 */
	protected OperationResult processIdentities(Collection<UUID> identitiesId) {
		for (UUID identityId : identitiesId) {
			IdmIdentityDto identity = identityService.get(identityId);
			if (identity == null) {
				LOG.warn("Identity with id [{}] not found. The identity will be skipped.", identityId);
				continue;
			}
			try {
				if (checkPermissionForIdentity(identity)) {
					OperationResult result = processIdentity(identity);
					if (result.getState() == OperationState.EXECUTED) {
						// log success result
						createSuccessLog(identity);
					} else {
						// something inside process method failed
						createFailedLog(identity, result);
					}
				} else {
					// check permission failed
					createPermissionFailedLog(identity, null);
				}
				//
				//
				this.increaseCounter();
				if (!updateState()) {
					return new OperationResult.Builder(OperationState.CANCELED).build();
				}
			} catch (Exception ex) {
				// log failed result and continue
				// TODO: log into log4j?
				createFailedLog(identity, ex, null);
				if (!updateState()) {
					return new OperationResult.Builder(OperationState.CANCELED).setCause(ex).build();
				}
			}
		}
		return new OperationResult.Builder(OperationState.EXECUTED).build();
	}
	
	/**
	 * Find id's for given filter and pageable
	 *
	 * @param filter
	 * @param pageable
	 * @return
	 */
	protected List<UUID> find(IdmIdentityFilter filter, Pageable pageable) {
		return identityService.findIds(filter, null).getContent();
	}
	
	/**
	 * Create failed log for given identity with exception for insufficient permission
	 *
	 * @param identity
	 * @param cause
	 */
	protected void createPermissionFailedLog(IdmIdentityDto identity, Throwable cause) {
		DefaultResultModel model = new DefaultResultModel(CoreResultCode.BULK_ACTION_INSUFFICIENT_PERMISSION,
				ImmutableMap.of("bulkAction", this.getAction().getName(),
						"identityId", identity.getId(),
						"identityUsername", identity.getUsername()));
		// operation state = blocked for insufficient permission
		this.createFailedLog(identity, cause, new OperationResult.Builder(OperationState.BLOCKED).setCause(cause).setModel(model).build());
	}

	/**
	 * Create failed log for given identity. With exception and model
	 *
	 * @param identity
	 * @param cause
	 * @param model
	 */
	protected void createFailedLog(IdmIdentityDto identity, Throwable cause, OperationResult result) {
		this.logItemProcessed(identity, result);
	}
	
	/**
	 * Create failed log for given identity
	 *
	 * @param identity
	 */
	protected void createFailedLog(IdmIdentityDto identity, OperationResult result) {
		this.createFailedLog(identity, null, result);
	}
	
	/**
	 * Create success log for given identity
	 *
	 * @param identity
	 */
	protected void createSuccessLog(IdmIdentityDto identity) {
		this.logItemProcessed(identity, new OperationResult.Builder(OperationState.EXECUTED).build());
	}
	
	/**
	 * Process one of identities in queue
	 *
	 * @param dto
	 * @return return operation result for current processed identity
	 */
	protected abstract OperationResult processIdentity(IdmIdentityDto dto);
	
	/**
	 * Check permission for given identity. Permission for check is get by identity service.
	 * Required permission is given by method 
	 * @param identity
	 * @return
	 */
	protected boolean checkPermissionForIdentity(IdmIdentityDto identity) {
		return PermissionUtils.hasPermission(identityService.getPermissions(identity), getPermissionForIdentity());
	}

	/**
	 * Get required permissions for process identity.
	 *
	 * @return
	 */
	protected abstract BasePermission[] getPermissionForIdentity();
	
	@Override
	public Map<String, BasePermission[]> getPermissions() {
		Map<String, BasePermission[]> permissions = super.getPermissions();
		permissions.put(IdmIdentity.class.getSimpleName(), this.getPermissionForIdentity());
		return permissions;
	}
}