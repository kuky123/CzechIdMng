package eu.bcvsolutions.idm.core.model.repository.filter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.repository.filter.AbstractFilterBuilder;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityRepository;

/**
 * Email filter for {@link IdmIdentityDto}
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
@Component
@Description("Filter by identity's email")
public class EmailIdentityFilter extends AbstractFilterBuilder<IdmIdentity, IdmIdentityFilter> {

	@Autowired
	public EmailIdentityFilter(IdmIdentityRepository repository) {
		super(repository);
	}

	@Override
	public String getName() {
		return IdmIdentityFilter.PARAMETER_EMAIL;
	}

	@Override
	public javax.persistence.criteria.Predicate getPredicate(Root<IdmIdentity> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, IdmIdentityFilter filter) {
		String email = filter.getEmail();
		if (StringUtils.isEmpty(email)) {
			return null;
		}
		return builder.equal(root.get(IdmIdentity_.email), email);
	}
}
