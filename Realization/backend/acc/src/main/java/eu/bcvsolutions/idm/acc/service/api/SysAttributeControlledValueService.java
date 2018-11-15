package eu.bcvsolutions.idm.acc.service.api;

import java.io.Serializable;
import java.util.List;

import eu.bcvsolutions.idm.acc.dto.SysAttributeControlledValueDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysAttributeControlledValueFilter;
import eu.bcvsolutions.idm.core.api.service.EventableDtoService;

/**
 * Service - Controlled value for attribute DTO. Is using in the provisioning
 * merge.
 * 
 * @author Vít Švanda
 *
 */
public interface SysAttributeControlledValueService extends EventableDtoService<SysAttributeControlledValueDto, SysAttributeControlledValueFilter> {

	void setControlledValues(SysSystemAttributeMappingDto attributeMapping,
			List<Serializable> controlledAttributeValues);
	
	void addHistoricValue(SysSystemAttributeMappingDto attributeMapping,
			Serializable value);

}
