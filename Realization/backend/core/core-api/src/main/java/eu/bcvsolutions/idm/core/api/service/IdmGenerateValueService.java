package eu.bcvsolutions.idm.core.api.service;

import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmGenerateValueFilter;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Generated values service
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 */
public interface IdmGenerateValueService
		extends ReadWriteDtoService<IdmGenerateValueDto, IdmGenerateValueFilter>,
		AuthorizableService<IdmGenerateValueDto> {

	/**
	 * Generator with seq equals or lower than this constant is marked as system and is not 
	 * possible to create the generator with this seq by REST.
	 */
	short SYSTEM_SEQ_MAXIMUM = 10;
}
