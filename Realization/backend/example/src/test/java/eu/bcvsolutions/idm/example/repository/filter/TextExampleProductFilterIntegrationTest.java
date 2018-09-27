package eu.bcvsolutions.idm.example.repository.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.example.dto.ExampleProductDto;
import eu.bcvsolutions.idm.example.dto.filter.ExampleProductFilter;
import eu.bcvsolutions.idm.example.entity.ExampleProduct;
import eu.bcvsolutions.idm.example.service.api.ExampleProductService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for {@link TextExampleProductFilter}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Radek Tomiška
 */
@Transactional
public class TextExampleProductFilterIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private ExampleProductService productService;
	@Autowired
	private TextExampleProductFilter textProductFilter;

	@Test
	public void testFilteringFound() {
		String textValue = getHelper().createName() + System.currentTimeMillis();
		ExampleProductDto productOne = getHelper().createProduct(getHelper().createName() + textValue.toLowerCase() + System.currentTimeMillis());
		ExampleProductDto productTwo = getHelper().createProduct(getHelper().createName(), textValue + getHelper().createName(), null);
		ExampleProductDto productThree = getHelper().createProduct();
		productThree.setDescription(getHelper().createName() + textValue.toUpperCase() + getHelper().createName());
		productService.save(productThree);
		
		ExampleProductFilter filter = new ExampleProductFilter();
		filter.setText(textValue);
		List<ExampleProduct> products = textProductFilter.find(filter, null).getContent();
		
		assertEquals(3, products.size());
		
		// list must contains all products
		ExampleProduct product = products.stream().filter(prod -> prod.getId().equals(productOne.getId())).findFirst().get();
		assertNotNull(product);
		
		product = products.stream().filter(prod -> prod.getId().equals(productTwo.getId())).findFirst().get();
		assertNotNull(product);
		
		product = products.stream().filter(prod -> prod.getId().equals(productThree.getId())).findFirst().get();
		assertNotNull(product);
	}
	
	@Test
	public void testFilteringNotFound() {
		String textValue = "textValue" + System.currentTimeMillis();
		getHelper().createProduct("123" + textValue + System.currentTimeMillis());
		ExampleProductDto productTwo = getHelper().createProduct();
		ExampleProductDto productThree = getHelper().createProduct();
		productTwo.setCode(textValue + getHelper().createName());
		productThree.setDescription(getHelper().createName() + textValue + getHelper().createName());
		
		productService.save(productTwo);
		productService.save(productThree);
		
		ExampleProductFilter filter = new ExampleProductFilter();
		filter.setText("textValue" + System.currentTimeMillis()); // different value than in variable textValue
		List<ExampleProduct> products = textProductFilter.find(filter, null).getContent();
		
		assertEquals(0, products.size());
	}
	
	@Override
	protected eu.bcvsolutions.idm.example.TestHelper getHelper() {
		return (eu.bcvsolutions.idm.example.TestHelper) super.getHelper();
	}
}
