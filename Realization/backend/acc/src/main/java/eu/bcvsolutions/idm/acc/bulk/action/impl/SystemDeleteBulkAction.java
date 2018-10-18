package eu.bcvsolutions.idm.acc.bulk.action.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemFilter;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningOperationService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractRemoveBulkAction;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;

/**
 * Delete given systems
 *
 * @author svandav
 *
 */
@Component("systemDeleteBulkAction")
@Description("Delete given systems")
public class SystemDeleteBulkAction extends AbstractRemoveBulkAction<SysSystemDto, SysSystemFilter> {

	public static final String NAME = "system-delete-bulk-action";

	@Autowired
	private SysSystemService systemService;
	@Autowired
	private AccAccountService accountService;
	@Autowired
	private SysProvisioningOperationService provisioningOperationService;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(AccGroupPermission.SYSTEM_DELETE);
	}

	@Override
	public ResultModels prevalidate() {
		IdmBulkActionDto action = getAction();
		List<UUID> entities = getEntities(action, new StringBuilder());
		ResultModels result = new ResultModels();

		Map<ResultModel, Long> models = new HashMap<>();
		entities.forEach(systemId -> {
			AccAccountFilter accountFilter = new AccAccountFilter();
			accountFilter.setSystemId(systemId);
			
			SysSystemDto system = getService().get(systemId);
			
			long count = accountService.find(accountFilter, new PageRequest(0, 1)).getTotalElements();
			if (count > 0) {
				models.put(new DefaultResultModel(AccResultCode.SYSTEM_DELETE_BULK_ACTION_NUMBER_OF_ACCOUNTS,
						ImmutableMap.of("system", system.getCode(), "count", count)), count);
			}
			
			SysProvisioningOperationFilter operationFilter = new SysProvisioningOperationFilter();
			operationFilter.setSystemId(system.getId());
			long countEntities = provisioningOperationService.find(operationFilter, new PageRequest(0, 1)).getTotalElements();
			if (countEntities > 0) {
				models.put(new DefaultResultModel(AccResultCode.SYSTEM_DELETE_BULK_ACTION_NUMBER_OF_PROVISIONINGS,
						ImmutableMap.of("system", system.getCode(), "count", countEntities)), countEntities);
			}
		});

		// Sort by count
		List<Entry<ResultModel, Long>> collect = models //
				.entrySet() //
				.stream() //
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue())) //
				// .limit(5) //
				.collect(Collectors.toList()); //
		collect.forEach(entry -> {
			result.addInfo(entry.getKey());
		});

		return result;
	}

	@Override
	public ReadWriteDtoService<SysSystemDto, SysSystemFilter> getService() {
		return systemService;
	}
}
