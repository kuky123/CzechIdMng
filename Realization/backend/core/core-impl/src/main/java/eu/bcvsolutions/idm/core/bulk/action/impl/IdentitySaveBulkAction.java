package eu.bcvsolutions.idm.core.bulk.action.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.CoreModuleDescriptor;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Bulk operation for save identity
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 */
@Enabled(CoreModuleDescriptor.MODULE_ID)
@Component(IdentitySaveBulkAction.NAME)
@Description("Bulk action save identity.")
public class IdentitySaveBulkAction extends AbstractBulkAction<IdmIdentityDto, IdmIdentityFilter> {

	public static final String NAME = "identity-save-bulk-action";
	
	public static final String ONLY_NOTIFY_CODE = "onlyNotify";
	
	@Autowired
	private IdmIdentityService identityService;
	@Autowired
	private EntityEventManager entityEventManager;
	
	@Override
	protected OperationResult processDto(IdmIdentityDto dto) {
		if (isOnlyNotify()) {
			entityEventManager.changedEntity(dto);
		} else {
			identityService.save(dto);
		}
		return new OperationResult(OperationState.EXECUTED);
	}
	
	@Override
	protected List<UUID> getAllEntities(IdmBulkActionDto action, StringBuilder description) {
		// all identities
		return identityService.findIds((Pageable) null, getPermissionForEntity()).getContent();
	}

	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		List<IdmFormAttributeDto> formAttributes = super.getFormAttributes();
		formAttributes.add(getOnlyNotifyAttribute());
		return formAttributes;
	}
	
	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.IDENTITY_READ, CoreGroupPermission.IDENTITY_UPDATE);
	}

	@Override
	public String getName() {
		return NAME;
	}
	
	/**
	 * Is set only notify event
	 *
	 * @return
	 */
	private boolean isOnlyNotify() {
		Boolean onlyNotify = this.getParameterConverter().toBoolean(getProperties(), ONLY_NOTIFY_CODE);
		return onlyNotify != null ? onlyNotify.booleanValue() : false;
	}

	/**
	 * Get {@link IdmFormAttributeDto} for checkbox only notify
	 *
	 * @return
	 */
	private IdmFormAttributeDto getOnlyNotifyAttribute() {
		IdmFormAttributeDto primaryContract = new IdmFormAttributeDto(
				ONLY_NOTIFY_CODE, 
				ONLY_NOTIFY_CODE, 
				PersistentType.BOOLEAN);
		primaryContract.setDefaultValue(Boolean.FALSE.toString());
		return primaryContract;
	}
	
	@Override
	public int getOrder() {
		return super.getOrder() + 1500;
	}

	@Override
	public ReadWriteDtoService<IdmIdentityDto, IdmIdentityFilter> getService() {
		return identityService;
	}
	
	@Override
	public boolean showWithoutSelection() {
		return true;
	}
}
