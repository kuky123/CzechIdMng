package eu.bcvsolutions.idm.example.repository.filter;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.example.dto.ExampleProductDto;
import eu.bcvsolutions.idm.example.dto.filter.ExampleProductFilter;
import eu.bcvsolutions.idm.example.entity.ExampleProduct;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test for {@link TextExampleProductFilter}
 * 
 * @author Radek Tomiška
 */
@Transactional
public class ExampleProductNameFilterIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ExampleProductNameFilter filter;

	@Test
	public void testFilter() {
		ExampleProductDto productOne = getHelper().createProduct();
		getHelper().createProduct(); // other product
		//		
		ExampleProductFilter productFilter = new ExampleProductFilter();
		productFilter.setName(productOne.getName());
		List<ExampleProduct> products = filter.find(productFilter, null).getContent();
		//
		Assert.assertEquals(1, products.size());
		Assert.assertEquals(productOne.getId(), products.get(0).getId());
	}
	
	@Override
	protected eu.bcvsolutions.idm.example.TestHelper getHelper() {
		return (eu.bcvsolutions.idm.example.TestHelper) super.getHelper();
	}
}
