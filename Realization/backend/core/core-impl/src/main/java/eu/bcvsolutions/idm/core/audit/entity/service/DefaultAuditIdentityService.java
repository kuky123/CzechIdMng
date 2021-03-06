package eu.bcvsolutions.idm.core.audit.entity.service;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.audit.dto.filter.IdmAuditFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.audit.dto.filter.AuditEntityFilter;
import eu.bcvsolutions.idm.core.audit.dto.filter.AuditIdentityFilter;
import eu.bcvsolutions.idm.core.audit.entity.IdmAudit;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;

/**
 * Default audit service for Identity and their relations
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

@Service("auditIdentityService")
public class DefaultAuditIdentityService extends AbstractAuditEntityService {

	public DefaultAuditIdentityService() {
	}

	@Override
	public boolean supports(Class<? extends AbstractEntity> delimiter) {
		return delimiter.isAssignableFrom(IdmIdentity.class);
	}

	@Override
	public Page<IdmAudit> findRevisionBy(AuditEntityFilter filter, Pageable pageable) {
		AuditIdentityFilter identityFilter = (AuditIdentityFilter) filter;
		//
		List<String> identitiesIds = Collections.emptyList();
		if (identityFilter.getUsername() != null) {
			// in identities can be more UUID, we search for all
			identitiesIds = getAuditRepository().findDistinctOwnerIdByOwnerTypeAndOwnerCode(IdmIdentity.class.getName(), identityFilter.getUsername());
			// remove null values
			identitiesIds.removeAll(Collections.singleton(null));
			// no entity found for this username return empty list
			if (identitiesIds.isEmpty()) {
				return new PageImpl<>(Collections.emptyList());
			}
		}
		//
		IdmAuditFilter auditFilter = new IdmAuditFilter();
		auditFilter.setFrom(identityFilter.getFrom());
		auditFilter.setTill(identityFilter.getTill());
		auditFilter.setOwnerType(IdmIdentity.class.getName());
		auditFilter.setChangedAttributes(identityFilter.getChangedAttributes());
		//
		// set id as owner id
		if (identityFilter.getId() != null) {
			auditFilter.setOwnerId(identityFilter.getId().toString());
		}
		//
		if (!identitiesIds.isEmpty()) {
			auditFilter.setOwnerIds(identitiesIds);
		}
		//
		return this.getAuditRepository().find(auditFilter, pageable);
	}

	@Override
	public AuditEntityFilter getFilter(MultiValueMap<String, Object> parameters) {
		// TODO: refactor use mapper? FilterConverter
		AuditIdentityFilter filter = new AuditIdentityFilter();
		//
		Object id = parameters.getFirst("id");
		Object username = parameters.getFirst("username");
		Object from = parameters.getFirst("from");
		Object till = parameters.getFirst("till");
		Object modifier = parameters.getFirst("modifier");
		Object changedAttributes = parameters.getFirst("changedAttributes");
		//
		filter.setId(EntityUtils.toUuid(id));
		filter.setUsername(username != null ? username.toString() : null);
		filter.setFrom(from != null ? new DateTime(from) : null);
		filter.setTill(till != null ? new DateTime(till) : null);
		filter.setModifier(modifier != null ? modifier.toString() : null);
		filter.setChangedAttributes(changedAttributes != null ? changedAttributes.toString() : null);
		//
		return filter;
	}
}
