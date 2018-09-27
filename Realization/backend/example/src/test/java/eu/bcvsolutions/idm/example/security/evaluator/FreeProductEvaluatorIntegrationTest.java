package eu.bcvsolutions.idm.example.security.evaluator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.example.domain.ExampleGroupPermission;
import eu.bcvsolutions.idm.example.dto.ExampleProductDto;
import eu.bcvsolutions.idm.example.dto.filter.ExampleProductFilter;
import eu.bcvsolutions.idm.example.entity.ExampleProduct;
import eu.bcvsolutions.idm.example.service.api.ExampleProductService;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Security evaluator integration test for example products
 * 
 * @author Radek Tomiška
 *
 */
@Transactional
public class FreeProductEvaluatorIntegrationTest extends AbstractIntegrationTest {

	@Autowired private ExampleProductService service; 
	
	@Test
	public void testPermission() {
		String productName = getHelper().createName(); // same na for both (other product can be created by other test)
		ExampleProductDto freeProduct = getHelper().createProduct(getHelper().createName(), productName, BigDecimal.ZERO);
		getHelper().createProduct(getHelper().createName(), productName, BigDecimal.ONE); // other paid product
		ExampleProductFilter filter = new ExampleProductFilter();
		filter.setName(productName);
		//
		// identity, with permission to read free products
		IdmIdentityDto identity = getHelper().createIdentity();
		List<ExampleProductDto> products = null;
		IdmRoleDto role = getHelper().createRole();
		getHelper().createAuthorizationPolicy(
				role.getId(),
				ExampleGroupPermission.EXAMPLEPRODUCT,
				ExampleProduct.class,
				FreeProductEvaluator.class,
				IdmBasePermission.READ);
		//
		// check created identity doesn't have compositions
		try {			
			getHelper().login(identity.getUsername(), identity.getPassword());
			products = service.find(filter, null, IdmBasePermission.READ).getContent();
			Assert.assertTrue(products.isEmpty());	
		} finally {
			logout();
		}
		//
		// assign role to identity
		getHelper().createIdentityRole(identity, role);
		//
		try {
			getHelper().login(identity.getUsername(), identity.getPassword());
			//
			// evaluate	access
			products = service.find(filter, null, IdmBasePermission.READ).getContent();
			Assert.assertEquals(1, products.size());	
			Assert.assertEquals(freeProduct.getId(), products.get(0).getId());
			//
			Set<String> permissions = service.getPermissions(freeProduct);
			Assert.assertEquals(1, permissions.size());
			Assert.assertEquals(IdmBasePermission.READ.name(), permissions.iterator().next());
		} finally {
			logout();
		}
	}
	
	@Override
	protected eu.bcvsolutions.idm.example.TestHelper getHelper() {
		return (eu.bcvsolutions.idm.example.TestHelper) super.getHelper();
	}
}
