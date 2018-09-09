package eu.bcvsolutions.idm.core.bulk.action.impl;

import java.util.List;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.CoreModuleDescriptor;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractRemoveBulkAction;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormValueFilter;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;

/**
 * Delete given form values
 *
 * @author Roman Kučera
 *
 */

@Component("formValueDeleteBulkAction")
@Description("Delete given form values.")
@Enabled(module = CoreModuleDescriptor.MODULE_ID)
public class FormValueDeleteBulkAction extends AbstractRemoveBulkAction<IdmFormValueDto, IdmFormValueFilter> {

	public static final String NAME = "form-value-delete-bulk-action";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.IDENTITY_DELETE);
	}

	@Override
	public ReadWriteDtoService<IdmFormValueDto, IdmFormValueFilter> getService() {
		//TODO need to return service by type, which i dont know
		return null;
	}

}
