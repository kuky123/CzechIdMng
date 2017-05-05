package eu.bcvsolutions.idm.core.api.utils;

import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.EmptyFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.EntityLookupService;

/**
 * Converts rest parameter to {@link BaseFilter}.
 * 
 * @author Radek Tomiška
 */
public class FilterConverter extends ParameterConverter {

	private final ObjectMapper mapper;
	
	public FilterConverter(EntityLookupService entityLookupService, ObjectMapper mapper) {
		super(entityLookupService);
		//
		this.mapper = mapper;
	}
	
	/**
	 * Converts http get parameters to filter
	 * 
	 * @param parameters
	 * @param filterClass
	 * @return
	 */
	public <F extends BaseFilter> F toFilter(MultiValueMap<String, Object> parameters, Class<F> filterClass) {
		Assert.notNull(parameters);
		//
		return toFilter(parameters.toSingleValueMap(), filterClass);
	}
	
	/**
	 * Converts parameters in map to filter
	 * 
	 * @param parameters
	 * @param filterClass
	 * @return
	 */
	public <F extends BaseFilter> F toFilter(Map<String, Object> parameters, Class<F> filterClass) {
		if (mapper == null || parameters.isEmpty() || EmptyFilter.class.equals(filterClass)) {
			return null;
		}
		try {
			return mapper.convertValue(parameters, filterClass);
		} catch (IllegalArgumentException ex) {
			throw new ResultCodeException(CoreResultCode.BAD_FILTER, ex);
		}
	}
}
