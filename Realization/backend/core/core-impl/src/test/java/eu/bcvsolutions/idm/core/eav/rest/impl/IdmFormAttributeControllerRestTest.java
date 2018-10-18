package eu.bcvsolutions.idm.core.eav.rest.impl;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;

/**
 * Controller tests
 * - CRUD
 * 
 * @author Radek Tomiška
 *
 */
public class IdmFormAttributeControllerRestTest extends AbstractReadWriteDtoControllerRestTest<IdmFormAttributeDto> {

	@Autowired private IdmFormAttributeController controller;
	@Autowired private FormService formService;
	
	@Override
	protected AbstractReadWriteDtoController<IdmFormAttributeDto, ?> getController() {
		return controller;
	}

	@Override
	protected IdmFormAttributeDto prepareDto() {
		IdmFormDefinitionDto formDefinition = formService.createDefinition(IdmIdentityDto.class, getHelper().createName(), null);
		IdmFormAttributeDto dto = new IdmFormAttributeDto();
		dto.setName(getHelper().createName());
		dto.setCode(getHelper().createName());
		dto.setFormDefinition(formDefinition.getId());
		dto.setPersistentType(PersistentType.SHORTTEXT);
		//
		return dto;
	}
}
