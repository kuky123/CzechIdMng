package eu.bcvsolutions.idm.acc.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.acc.dto.SchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.entity.SysSchemaAttribute;
import eu.bcvsolutions.idm.acc.repository.SysSchemaAttributeRepository;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;

/**
 * Default schema attributes
 * 
 * @author svandav
 *
 */
@Service
public class DefaultSysSchemaAttributeService extends AbstractReadWriteEntityService<SysSchemaAttribute, SchemaAttributeFilter>
		implements SysSchemaAttributeService {

	@Autowired
	private SysSchemaAttributeRepository repository;

	@Override
	protected AbstractEntityRepository<SysSchemaAttribute, SchemaAttributeFilter> getRepository() {
		return repository;
	}
}
