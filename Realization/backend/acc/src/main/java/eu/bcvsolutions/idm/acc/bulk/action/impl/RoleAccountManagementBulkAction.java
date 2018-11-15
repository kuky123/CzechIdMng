package eu.bcvsolutions.idm.acc.bulk.action.impl;

import java.text.MessageFormat;
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
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole_;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Bulk operation for evaluate the account management on the all identities of
 * given role.
 * 
 * @author svandav
 *
 */

@Enabled(AccModuleDescriptor.MODULE_ID)
@Component("roleSaveBulkAction")
@Description("Bulk operation to evaluate the account management for all identities of given role.")
public class RoleAccountManagementBulkAction extends AbstractBulkAction<IdmRoleDto, IdmRoleFilter> {

	public static final String NAME = "role-acm-bulk-action";

	@Autowired
	private IdmRoleService roleService;
	@Autowired
	private EntityEventManager entityEventManager;
	@Autowired
	private IdmIdentityRoleService identityRoleService;

	@Override
	protected OperationResult processDto(IdmRoleDto dto) {
		Assert.notNull(dto, "Role is required!");
		Assert.notNull(dto.getId(), "Id of role is required!");
		StringBuilder message = new StringBuilder();

		IdmIdentityRoleFilter identityRoleFilter = new IdmIdentityRoleFilter();
		identityRoleFilter.setRoleId(dto.getId());

		// Load all identity roles for this roleId.
		// Without check on IdentityRole UPDATE permissions. This operation is
		// controlled by UPDATE right on this role!
		List<IdmIdentityRoleDto> allIdentityRoles = identityRoleService.find(identityRoleFilter, null).getContent();

		allIdentityRoles.forEach(identityRole -> {
			IdmIdentityContractDto contract = DtoUtils.getEmbedded(identityRole, IdmIdentityRole_.identityContract,
					IdmIdentityContractDto.class);
			if (contract != null) {
				IdmIdentityDto identity = DtoUtils.getEmbedded(contract, IdmIdentityContract_.identity,
						IdmIdentityDto.class);
				message.append('\n');
				message.append(
						MessageFormat.format("[{0}], identity [{1}]", identityRole.getId(), identity.getUsername()));

			}
			entityEventManager.changedEntity(identityRole);
		});

		OperationResult operationResult = new OperationResult(OperationState.EXECUTED);
		operationResult.setCause(MessageFormat.format(
				"For the role [{0}], [{1}] of identity roles were processed/notified. UUIDs:\n {2}", dto.getCode(),
				allIdentityRoles.size(), message.toString()));
		return operationResult;
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
				models.put(new DefaultResultModel(AccResultCode.ROLE_ACM_BULK_ACTION_NUMBER_OF_IDENTITIES,
						ImmutableMap.of("role", role.getCode(), "count", count)), count);
			}
		});

		boolean someIdentitiesFound = models.values() //
				.stream() //
				.filter(count -> count > 0) //
				.findFirst() //
				.isPresent(); //

		if (!someIdentitiesFound) {
			result.addInfo(new DefaultResultModel(AccResultCode.ROLE_ACM_BULK_ACTION_NONE_IDENTITIES));
		} else {
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
		}

		return result;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.ROLE_READ, CoreGroupPermission.ROLE_UPDATE);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 100;
	}

	@Override
	public ReadWriteDtoService<IdmRoleDto, IdmRoleFilter> getService() {
		return roleService;
	}

}
