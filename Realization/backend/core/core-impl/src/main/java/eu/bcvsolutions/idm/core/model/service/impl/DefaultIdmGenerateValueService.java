package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;

import eu.bcvsolutions.idm.core.api.dto.IdmGenerateValueDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmGenerateValueFilter;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.IdmGenerateValueService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmGenerateValue;
import eu.bcvsolutions.idm.core.model.entity.IdmGenerateValue_;
import eu.bcvsolutions.idm.core.model.repository.IdmGenerateValueRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Default implementation of service for generate attributes
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class DefaultIdmGenerateValueService extends
		AbstractReadWriteDtoService<IdmGenerateValueDto, IdmGenerateValue, IdmGenerateValueFilter>
		implements IdmGenerateValueService {

	public DefaultIdmGenerateValueService(IdmGenerateValueRepository repository) {
		super(repository);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.GENERATEVALUE, getEntityClass());
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmGenerateValue> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, IdmGenerateValueFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		String dtoType = filter.getDtoType();
		if (StringUtils.isNotEmpty(dtoType)) {
			predicates.add(builder.equal(root.get(IdmGenerateValue_.dtoType), dtoType));
		}
		
		Boolean disabled = filter.getDisabled();
		if (disabled != null) {
			predicates.add(builder.equal(root.get(IdmGenerateValue_.disabled), disabled));
		}

		return predicates;
	}
}
