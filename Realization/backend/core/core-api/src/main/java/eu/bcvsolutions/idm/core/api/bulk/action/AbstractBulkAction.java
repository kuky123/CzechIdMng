package eu.bcvsolutions.idm.core.api.bulk.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.Codeable;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.scheduler.api.dto.IdmLongRunningTaskDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Abstract parent for all bulk actions
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 * @param <DTO>
 */
public abstract class AbstractBulkAction<DTO extends AbstractDto, F extends BaseFilter>
		extends AbstractLongRunningTaskExecutor<OperationResult>
		implements IdmBulkAction<DTO, F> {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractBulkAction.class);
	
	private IdmBulkActionDto action;
	@Autowired
	private NotificationManager notificationManager;
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private ConfigurationService configurationService;
	
	@Override
	public IdmBulkActionDto getAction() {
		return action;
	}

	@Override
	public void setAction(IdmBulkActionDto action) {
		this.action = action;
	}
	
	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		return new ArrayList<>();
	}

	@Override
	public boolean supports(Class<? extends BaseEntity> clazz) {
		return clazz.isAssignableFrom(getService().getEntityClass());
	}

	@Override
	public void validate() {
		Assert.notNull(action, "Action can't be null");
		//
		if (!showWithoutSelection() && action.getIdentifiers().isEmpty() && action.getFilter() == null) {
			throw new ResultCodeException(CoreResultCode.BULK_ACTION_ENTITIES_ARE_NOT_SPECIFIED);
		}
		//
		if (!action.getIdentifiers().isEmpty() && action.getFilter() != null) {
			throw new ResultCodeException(CoreResultCode.BULK_ACTION_ONLY_ONE_FILTER_CAN_BE_APPLIED);
		}
		//
		for (IdmFormAttributeDto attribute : this.getFormAttributes()) {
			if (attribute.isRequired()) {
				if (action.getProperties().isEmpty()) {
					// this state is also possible
					throw new ResultCodeException(CoreResultCode.BULK_ACTION_REQUIRED_PROPERTY, ImmutableMap.of("attributeCode", attribute.getCode()));
				}
				Object value = action.getProperties().get(attribute.getCode());
				if (value == null) {
					throw new ResultCodeException(CoreResultCode.BULK_ACTION_REQUIRED_PROPERTY, ImmutableMap.of("attributeCode", attribute.getCode()));
				} 
				if (attribute.isMultiple()) {
					Collection<?> multivaluedValue= (Collection<?>)value;
					if (multivaluedValue.isEmpty()) {
						throw new ResultCodeException(CoreResultCode.BULK_ACTION_REQUIRED_PROPERTY, ImmutableMap.of("attributeCode", attribute.getCode()));
					}
				}
			}
		}
	}
	
	@Override
	public ResultModels prevalidate() {
		return new ResultModels();
	}
	
	@Override
	public Map<String, Object> getProperties() {
		if (this.getAction() == null) {
			return super.getProperties();
		}
		return this.getAction().getProperties();
	}
	
	@Override
	public List<String> getAuthorities() {
		return this.getAuthoritiesForEntity();
	}
	
	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}
	
	@Override
	protected OperationResult end(OperationResult result, Exception ex) {
		OperationResult end = null;
		if (result != null && result.getException() != null) {
			end = super.end(result, (Exception) result.getException());
		}
		end = super.end(result, ex);
		// send message
		IdmLongRunningTaskDto task = getLongRunningTaskService().get(getLongRunningTaskId());
		IdmBulkActionDto action = getAction();
		//
		if (task.getCreatorId() != null) {
			IdmIdentityDto identityDto = identityService.get(task.getCreatorId());
			if (identityDto != null) {
				// TODO: coreModuleDescriptor is in impl
				notificationManager.send("core:bulkActionEnd",
						new IdmMessageDto.Builder()
						.addParameter("action", action)
						.addParameter("task", task)
						.addParameter("owner", identityDto)
						.addParameter("result", end)
						.addParameter("detailUrl", configurationService.getFrontendUrl(String.format("scheduler/all-tasks/%s/detail", task.getId())))
						.addParameter("processItemslUrl", configurationService.getFrontendUrl(String.format("scheduler/all-tasks/%s/items", task.getId())))
						.build(),
						identityDto);
			}
		}
		//
		return end;
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
		List<UUID> entities = getEntities(action, description);
		//
		this.count = Long.valueOf(entities.size());
		this.counter = 0l;
		//
		// update description
		longRunningTask.setTaskDescription(description.toString());
		this.getLongRunningTaskService().save(longRunningTask);
		//
		return processEntities(entities);
	}

	/**
	 * Obtains the all UUIDs of entities to processing
	 * 
	 * @param action
	 * @param description
	 * @return
	 */
	protected List<UUID> getEntities(IdmBulkActionDto action, StringBuilder description) {
		List<UUID> entities = null;
		if (!action.getIdentifiers().isEmpty()) {
			entities = new ArrayList<UUID>(action.getIdentifiers());
			//
			description.append(System.lineSeparator());
			description.append("For filtering is used list of ID's.");
		} else if (action.getTransformedFilter() != null) {
			// is necessary find entities with given base permission
			List<UUID> content = getService().findIds(this.transformFilter(action.getTransformedFilter()), null,
					getPermissionForEntity()).getContent();

			// it is necessary create new arraylist because return list form find is unmodifiable
			entities = new ArrayList<UUID>(content);
			//
			description.append(System.lineSeparator());
			description.append("For filtering is used filter:");
			description.append(System.lineSeparator());
			String filterAsString = Arrays.toString(action.getFilter().entrySet().toArray());
			description.append(filterAsString);
		} else if (showWithoutSelection()) {
			entities = getAllEntities(action, description);
		} else {
			throw new ResultCodeException(CoreResultCode.BULK_ACTION_ENTITIES_ARE_NOT_SPECIFIED);
		}
		//
		// remove given ids
		if (!action.getRemoveIdentifiers().isEmpty()) {
			entities.removeAll(action.getRemoveIdentifiers());
		}
		return entities;
	}
	
	/**
	 * Returns all entities to be process, if {@link #showWithoutSelection()} is enabled.
	 * All entities are processed, if no filter and no identifiers was given.
	 * Override this method, if {@link #showWithoutSelection()} is enabled for your action.
	 * 
	 * @param action
	 * @param description
	 * @return
	 * @throws ResultCodeException if action supports {@link #showWithoutSelection()} and this method is not implemented.
	 */
	protected List<UUID> getAllEntities(IdmBulkActionDto action, StringBuilder description) {
		throw new ResultCodeException(CoreResultCode.BULK_ACTION_ENTITIES_ARE_NOT_SPECIFIED);
	}

	/**
	 * Process all identities by given list of ID's
	 *
	 * @param identitiesId
	 * @return
	 */
	protected OperationResult processEntities(Collection<UUID> entitiesId) {
		for (UUID entityId : entitiesId) {
			this.increaseCounter();
			DTO entity = getService().get(entityId);
			if (entity == null) {
				LOG.warn("Entity with id [{}] not found. The Entity will be skipped.", entityId);
				continue;
			}
			try {
				if (checkPermissionForEntity(entity)) {
					OperationResult result = processDto(entity);
					this.logItemProcessed(entity, result);
				} else {
					// check permission failed
					createPermissionFailedLog(entity);
				}
				//
				//
				if (!updateState()) {
					return new OperationResult.Builder(OperationState.CANCELED).build();
				}
			} catch (Exception ex) {
				// log failed result and continue
				// TODO: log into log4j?
				this.logItemProcessed(entity, new OperationResult.Builder(OperationState.EXCEPTION).setCause(ex).build());
				if (!updateState()) {
					return new OperationResult.Builder(OperationState.CANCELED).setCause(ex).build();
				}
			}
		}
		return new OperationResult.Builder(OperationState.EXECUTED).build();
	}

	/**
	 * Create success log for given identity
	 *
	 * @param identity
	 */
	protected void createSuccessLog(DTO dto) {
		this.logItemProcessed(dto, new OperationResult.Builder(OperationState.EXECUTED).build());
	}
	
	/**
	 * Create failed log for given dto with exception for insufficient permission
	 *
	 * @param dto
	 * @param cause
	 */
	protected void createPermissionFailedLog(DTO dto) {
		String entityCode = "";
		if (dto.getClass().isAssignableFrom(Codeable.class)) {
			Codeable codeAbleDto = (Codeable) dto;
			entityCode = codeAbleDto.getCode();
		}
		DefaultResultModel model = new DefaultResultModel(CoreResultCode.BULK_ACTION_INSUFFICIENT_PERMISSION,
				ImmutableMap.of("bulkAction", this.getAction().getName(),
						"entityId", dto.getId(),
						"entityCode", entityCode));
		// operation state = blocked for insufficient permission
		this.logItemProcessed(dto, new OperationResult.Builder(OperationState.NOT_EXECUTED).setModel(model).build());
	}

	/**
	 * Transform filter to {@link IdmIdentityFilter}
	 *
	 * @param filter
	 * @return
	 */
	protected F transformFilter(BaseFilter filter) {
		if (this.getService().getFilterClass().isAssignableFrom(filter.getClass())) {
			return this.getService().getFilterClass().cast(filter);
		}
		throw new ResultCodeException(CoreResultCode.BULK_ACTION_BAD_FILTER, ImmutableMap.of("filter", IdmIdentityFilter.class.getName(), "givenFilter", filter.getClass().getName()));
	}

	/**
	 * Check permission for given entity. Permission for check is get by service.
	 * Required permission is given by method {@link #getPermissionForEntity}
	 * @param identity
	 * @return
	 */
	protected boolean checkPermissionForEntity(BaseDto entity) {
		return PermissionUtils.hasPermission(getService().getPermissions(entity),
				getPermissionForEntity());
	}
	
	protected BasePermission[] getPermissionForEntity() {
		return PermissionUtils.toPermissions(getAuthoritiesForEntity()).toArray(new BasePermission[] {});
	}

	/**
	 * Get required permissions for process entity.
	 * Returns empty permissions by default, override if needed.
	 *
	 * @return
	 */
	protected List<String> getAuthoritiesForEntity() {
		return new ArrayList<>();
	}
	
	/**
	 * Process one of DTO in queue
	 *
	 * @param dto
	 * @return return operation result for current processed DTO
	 */
	protected abstract OperationResult processDto(DTO dto);
}
