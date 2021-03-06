package eu.bcvsolutions.idm.core.bulk.action.impl;

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

import eu.bcvsolutions.idm.core.api.bulk.action.AbstractRemoveBulkAction;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;

/**
 * Delete given roles
 *
 * @author svandav
 *
 */
@Component("roleDeleteBulkAction")
@Description("Delete given roles.")
public class RoleDeleteBulkAction extends AbstractRemoveBulkAction<IdmRoleDto, IdmRoleFilter> {

	public static final String NAME = "role-delete-bulk-action";

	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private IdmIdentityRoleService identityRoleService;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.ROLE_DELETE);
	}

	@Override
	public ResultModels prevalidate() {
		IdmBulkActionDto action = getAction();
		List<UUID> entities = getEntities(action, new StringBuilder());
		ResultModels result = new ResultModels();

		Map<ResultModel, Long> models = new HashMap<>();
		entities.forEach(roleId -> {
			IdmIdentityRoleFilter identityRoleFilter = new IdmIdentityRoleFilter();
			identityRoleFilter.setRoleId(roleId);
			IdmRoleDto role = getService().get(roleId);

			long count = identityRoleService.find(identityRoleFilter, new PageRequest(0, 1)).getTotalElements();
			if (count > 0) {
				models.put(new DefaultResultModel(CoreResultCode.ROLE_DELETE_BULK_ACTION_NUMBER_OF_IDENTITIES,
						ImmutableMap.of("role", role.getCode(), "count", count)), count);
			}
		});

		// Sort by count
		List<Entry<ResultModel, Long>> collect = models //
				.entrySet() //
				.stream() //
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue())) //
				.limit(5) //
				.collect(Collectors.toList()); //
		collect.forEach(entry -> {
			result.addInfo(entry.getKey());
		});

		return result;
	}

	@Override
	public ReadWriteDtoService<IdmRoleDto, IdmRoleFilter> getService() {
		return roleService;
	}
}
