package eu.bcvsolutions.idm.example;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.example.dto.ExampleProductDto;
import eu.bcvsolutions.idm.example.service.api.ExampleProductService;

/**
 * Example test helper - custom test helper can be defined in modules.
 * 
 * @author Radek Tomiška
 */
@Primary
@Component("exampleTestHelper")
public class DefaultExampleTestHelper extends eu.bcvsolutions.idm.test.api.DefaultTestHelper implements TestHelper {

	@Autowired private ExampleProductService productService; 
	
	@Override
	public ExampleProductDto createProduct() {
		return createProduct(new BigDecimal(ThreadLocalRandom.current().nextDouble(0, 1000000)));
	}
	
	@Override
	public ExampleProductDto createProduct(String name) {
		return createProduct(createName(), name, new BigDecimal(ThreadLocalRandom.current().nextDouble(0, 1000000)));
	}

	@Override
	public ExampleProductDto createProduct(BigDecimal price) {
		return createProduct(createName(), createName(), price);
	}
	
	@Override
	public ExampleProductDto createProduct(String code, String name, BigDecimal price) {
		ExampleProductDto product = new ExampleProductDto();
		product.setCode(code == null ? createName() : code);
		product.setName(name == null ? createName() : name);
		product.setPrice(price);
		//
		return productService.save(product);
	}
	
	
}
