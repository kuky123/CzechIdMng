package eu.bcvsolutions.idm.core.rest.impl;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoControllerRestTest;
import eu.bcvsolutions.idm.core.generator.identity.IdentityFormDefaultValueGenerator;

/**
 * Controller tests
 * - TODO: move filters here
 * 
 * @author Radek Tomiška
 *
 */
public class IdmGenerateValueRestTest extends AbstractReadWriteDtoControllerRestTest<IdmGenerateValueDto> {

	@Autowired private IdmGenerateValueController controller;
	
	@Override
	protected AbstractReadWriteDtoController<IdmGenerateValueDto, ?> getController() {
		return controller;
	}
	
	@Override
	protected boolean supportsPatch() {
		return false;
	}
	
	@Override
	protected boolean supportsAutocomplete() {
		return false;
	}

	@Override
	protected IdmGenerateValueDto prepareDto() {
		IdmGenerateValueDto dto = new IdmGenerateValueDto();
		dto.setDtoType(IdmIdentityDto.class.getCanonicalName());
		dto.setGeneratorType(IdentityFormDefaultValueGenerator.class.getCanonicalName());
		dto.setSeq((short) 100);
		return dto;
	}
}
