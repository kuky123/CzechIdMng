package eu.bcvsolutions.idm.example;

import java.math.BigDecimal;

import eu.bcvsolutions.idm.example.dto.ExampleProductDto;

/**
 * Reuses core TestHelper and adds example spec. methods
 * 
 * @author Radek Tomiška
 *
 */
public interface TestHelper extends eu.bcvsolutions.idm.test.api.TestHelper {

	/**
	 * Creates test product with random name, code and price.
	 * 
	 * @return
	 */
	ExampleProductDto createProduct();
	
	/**
	 * Creates test product with random code, price and given name.
	 * 
	 * @param name
	 * @return
	 */
	ExampleProductDto createProduct(String name);
	
	/**
	 * Creates test product with random name, code and given price.
	 * 
	 * @param price
	 * @return
	 */
	ExampleProductDto createProduct(BigDecimal price);
	
	/**
	 * Creates test product with given name, code and price.
	 * 
	 * @param code
	 * @param name
	 * @param price
	 * @return
	 */
	ExampleProductDto createProduct(String code, String name, BigDecimal price);
	
}
