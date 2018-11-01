package eu.bcvsolutions.idm.core.bulk.action.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.bulk.action.BulkActionManager;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.utils.AutowireHelper;
import eu.bcvsolutions.idm.core.scheduler.api.dto.LongRunningFutureTask;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskManager;
import eu.bcvsolutions.idm.core.security.api.service.EnabledEvaluator;

/**
 * Implementation of manager for bulk action
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Service("bulkActionManager")
public class DefaultBulkActionManager implements BulkActionManager {

	private final PluginRegistry<AbstractBulkAction<? extends BaseDto, ? extends BaseFilter>, Class<? extends BaseEntity>> pluginExecutors;
	private final LongRunningTaskManager taskManager;
	private final EnabledEvaluator enabledEvaluator;
	
	@Autowired
	public DefaultBulkActionManager(
			List<AbstractBulkAction<? extends BaseDto, ? extends BaseFilter>> actions,
			LongRunningTaskManager taskManager,
			EnabledEvaluator enabledEvaluator) {
		pluginExecutors = OrderAwarePluginRegistry.create(actions);
		//
		this.taskManager = taskManager;
		this.enabledEvaluator = enabledEvaluator;
	}
	
	@Override
	public IdmBulkActionDto processAction(IdmBulkActionDto action) {
		AbstractBulkAction<? extends BaseDto, ? extends BaseFilter> executor = getOperationForDto(action);
		//
		executor = (AbstractBulkAction<?, ?>) AutowireHelper.createBean(executor.getClass());
		//
		executor.setAction(action);
		//
		// validate before execute
		executor.validate();
		//
		LongRunningFutureTask<OperationResult> execute = taskManager.execute(executor);
		action.setLongRunningTaskId(execute.getExecutor().getLongRunningTaskId());
		action.setEntityClass(executor.getService().getEntityClass().getName());
		action.setFilterClass(executor.getService().getFilterClass().getName());
		action.setModule(executor.getModule());
		action.setFormAttributes(executor.getFormAttributes());
		//
		action.setAuthorities(executor.getAuthorities());
		return action;
	}
	
	public ResultModels prevalidate(IdmBulkActionDto action) {
		AbstractBulkAction<? extends BaseDto, ? extends BaseFilter> executor = getOperationForDto(action);
		//
		executor = (AbstractBulkAction<?, ?>) AutowireHelper.createBean(executor.getClass());
		//
		executor.setAction(action);
		//
		// Prevalidate before execute
		return executor.prevalidate();
	}
	
	@Override
	public List<IdmBulkActionDto> getAvailableActions(Class<? extends BaseEntity> entity) {
		return getEnabledActions(entity)
				.stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private AbstractBulkAction<? extends BaseDto, ? extends BaseFilter> getOperationForDto(IdmBulkActionDto actionDto) {
		Assert.notNull(actionDto);
		Assert.notNull(actionDto.getEntityClass());
		try {
			Class<?> forName = Class.forName(actionDto.getEntityClass());
			if (AbstractEntity.class.isAssignableFrom(forName)) {
				List<AbstractBulkAction<? extends BaseDto, ? extends BaseFilter>> actions = getEnabledActions((Class<? extends BaseEntity>) forName);
				//
				for (AbstractBulkAction<? extends BaseDto, ? extends BaseFilter> action : actions) {
					// skip disabled modules 
					if (!enabledEvaluator.isEnabled(action)) {
						continue;
					}
					if (action.getName().equals(actionDto.getName())) {
						return action;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("bulkActionClass", actionDto.getEntityClass()), e);
		}
		throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("bulkActionName", actionDto.getName()));
	}
	
	/**
	 * Map action to dto.
	 * 
	 * @param action
	 * @return
	 */
	private IdmBulkActionDto toDto(AbstractBulkAction<? extends BaseDto, ? extends BaseFilter> action) {
		IdmBulkActionDto actionDto = new IdmBulkActionDto();
		actionDto.setEntityClass(action.getService().getEntityClass().getName());
		actionDto.setFilterClass(action.getService().getFilterClass().getName());
		actionDto.setModule(action.getModule());
		actionDto.setName(action.getName());
		actionDto.setFormAttributes(action.getFormAttributes());
		actionDto.setAuthorities(action.getAuthorities());
		actionDto.setShowWithoutSelection(action.showWithoutSelection());
		actionDto.setShowWithSelection(action.showWithSelection());
		//
		return actionDto;
	}
	
	/**
	 * Get enabled actions
	 * 
	 * @param entity
	 * @return
	 */
	private List<AbstractBulkAction<? extends BaseDto, ? extends BaseFilter>> getEnabledActions(Class<? extends BaseEntity> entity) {
		return pluginExecutors
				.getPluginsFor(entity)
				.stream()
				.filter(action -> enabledEvaluator.isEnabled(action))
				.collect(Collectors.toList());
	}
}
