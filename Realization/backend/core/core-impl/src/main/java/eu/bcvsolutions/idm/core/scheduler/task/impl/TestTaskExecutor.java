package eu.bcvsolutions.idm.core.scheduler.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.quartz.DisallowConcurrentExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableStatefulExecutor;

/**
 * Test task executor implementation
 * 
 * @author Radek Tomiška
 *
 */
@Component(TestTaskExecutor.TASK_NAME)
@DisallowConcurrentExecution
@Description("Test long running task")
@ConditionalOnProperty(prefix = "idm.pub.app", name = "stage", havingValue = "development")
public class TestTaskExecutor extends AbstractSchedulableStatefulExecutor<IdmIdentityDto> {
	
	public static final String TASK_NAME = "core-test-long-running-task";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TestTaskExecutor.class);
	private static final String PARAMETER_COUNT = "count";
	private static final long DEFAULT_COUNT = 100L;
	
	@Override
	public String getName() {
		return TASK_NAME;
	}
	
	@Override
	public void init(Map<String, Object> properties) {
		super.init(properties);
		//
		count = getParameterConverter().toLong(properties, PARAMETER_COUNT);
		if (count == null) {
			count = DEFAULT_COUNT;
		}
		counter = 0L;
	}
	
	@Override
	public Page<IdmIdentityDto> getItemsToProcess(Pageable pageable) {
		List<IdmIdentityDto> identities = new ArrayList<>();
		for (int i = 0; i < count; i ++) {
			identities.add(new IdmIdentityDto(UUID.randomUUID(), "test-" + i));
		}
		return new PageImpl<>(identities);
	}

	@Override
	public Optional<OperationResult> processItem(IdmIdentityDto dto) {
		try {
			LOG.warn(".......... identity: [{}]", dto.getUsername());
			Thread.sleep(300L);
			return Optional.of(new OperationResult.Builder(OperationState.EXECUTED).build());
		} catch (Exception ex) {
			throw new CoreException(ex);
		}
	}
	
	@Override
	public List<String> getPropertyNames() {
		List<String> parameters = super.getPropertyNames();
		parameters.add(PARAMETER_COUNT);
		return parameters;
	}
	
	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = super.getProperties();
		properties.put(PARAMETER_COUNT, count);
		return properties;
	}
	
	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		IdmFormAttributeDto countAttribute = new IdmFormAttributeDto(PARAMETER_COUNT, PARAMETER_COUNT, PersistentType.INT);
		countAttribute.setDefaultValue(String.valueOf(DEFAULT_COUNT));
		countAttribute.setRequired(true);
		//
		return Lists.newArrayList(countAttribute);
	}
	
    @Override
    public boolean supportsDryRun() {
    	return true;
    }
}
