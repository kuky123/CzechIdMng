package eu.bcvsolutions.idm.example.dto.filter;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.dto.filter.DataFilter;
import eu.bcvsolutions.idm.example.dto.ExampleProductDto;

/**
 * Filter for example products
 * 
 * @author Radek Tomiška
 *
 */
public class ExampleProductFilter extends DataFilter {
	
	/**
	 * Product name
	 */
	public static final String PARAMETER_NAME = "name";
	
	public ExampleProductFilter() {
		this(new LinkedMultiValueMap<>());
	}
	
	public ExampleProductFilter(MultiValueMap<String, Object> data) {
		super(ExampleProductDto.class, data);
	}
	
	public String getName() {
		return (String) data.getFirst(PARAMETER_NAME);
	}

	public void setName(String username) {
		data.set(PARAMETER_NAME, username);
	}
}
