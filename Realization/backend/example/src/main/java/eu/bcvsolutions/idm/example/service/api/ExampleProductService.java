package eu.bcvsolutions.idm.example.service.api;

import eu.bcvsolutions.idm.core.api.service.CodeableService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;
import eu.bcvsolutions.idm.example.dto.ExampleProductDto;
import eu.bcvsolutions.idm.example.dto.filter.ExampleProductFilter;

/**
 * Example product service
 * 
 * @author Radek Tomiška
 *
 */
public interface ExampleProductService extends 
		ReadWriteDtoService<ExampleProductDto, ExampleProductFilter>,
		CodeableService<ExampleProductDto>,
		AuthorizableService<ExampleProductDto> {
}
