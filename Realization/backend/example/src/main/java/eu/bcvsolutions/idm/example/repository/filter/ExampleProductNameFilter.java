package eu.bcvsolutions.idm.example.repository.filter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.repository.filter.AbstractFilterBuilder;
import eu.bcvsolutions.idm.example.dto.filter.ExampleProductFilter;
import eu.bcvsolutions.idm.example.entity.ExampleProduct;
import eu.bcvsolutions.idm.example.entity.ExampleProduct_;
import eu.bcvsolutions.idm.example.repository.ExampleProductRepository;

/**
 * Example product filter - by name, equals.
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Description("Example product filter - by name, equal.")
public class ExampleProductNameFilter extends AbstractFilterBuilder<ExampleProduct, ExampleProductFilter> {
	
	@Autowired
	public ExampleProductNameFilter(ExampleProductRepository repository) {
		super(repository);
	}
	
	@Override
	public String getName() {
		return ExampleProductFilter.PARAMETER_NAME;
	}
	
	@Override
	public Predicate getPredicate(Root<ExampleProduct> root, CriteriaQuery<?> query, CriteriaBuilder builder, ExampleProductFilter filter) {
		if (StringUtils.isEmpty(filter.getName())) {
			return null;
		}	
		return builder.equal(root.get(ExampleProduct_.name), filter.getName());
	}
}