package eu.bcvsolutions.idm.core.audit.entity.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.audite.dto.filter.AuditEntityFilter;
import eu.bcvsolutions.idm.core.audite.dto.filter.AuditIdentityFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmAudit;
import eu.bcvsolutions.idm.core.model.entity.IdmAudit_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmIdentityFormValue;
import eu.bcvsolutions.idm.core.model.entity.eav.IdmIdentityFormValue_;

/**
 * Idm audit service for Identity and their relations
 * TODO:
 * envers has bug with search deleted entities
 * https://github.com/spring-projects/spring-data-envers/issues/21
 * 
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

@Service("auditIdentityService")
public class IdmAuditIdentityService extends AbstractAuditEntityService {

	private List<Class<?>> relation;

	public IdmAuditIdentityService() {
		initRelation();

	}

	@Override
	public boolean supports(Class<? extends AbstractEntity> delimiter) {
		return delimiter.isAssignableFrom(IdmIdentity.class);
	}

	@Override
	public List<Class<?>> getRelationship() {
		return relation;
	}

	private void initRelation() {
		relation = new ArrayList<>();
		relation.add(IdmIdentityContract.class);
		relation.add(IdmIdentityRole.class);
		relation.add(IdmIdentityFormValue.class);
	}

	@Override
	public List<IdmAudit> findRevisionBy(AuditEntityFilter filter) {
		AuditIdentityFilter identityFilter = (AuditIdentityFilter) filter;
		// in identities can be more UUID, we search for all
		List<Object[]> identities = findIdentityByAttribute(identityFilter);
		//
		// get identity ids
		List<UUID> identityIds = getEntityIdFromList(identities);
		//
		// get contracts
		List<Object[]> contracts = findRealationByEntityId(IdmIdentityContract.class,
				IdmIdentityContract_.identity.getName(), identityIds, identityFilter);
		//
		// get roles
		List<Object[]> roles = findRolesForIdentities(getEntityIdFromList(contracts), identityFilter);
		//
		// get eav attributes
		List<Object[]> eavAttributes = findRealationByEntityId(IdmIdentityFormValue.class,
				IdmIdentityFormValue_.owner.getName(), identityIds, identityFilter);
		//
		List<IdmAudit> revisions = new ArrayList<>();
		revisions.addAll(getRevisionFromList(identities));
		revisions.addAll(getRevisionFromList(contracts));
		revisions.addAll(getRevisionFromList(roles));
		revisions.addAll(getRevisionFromList(eavAttributes));
		//
		return sortByTimestamp(revisions);
	}

	/**
	 * Method sort revisions by timestamp
	 * 
	 * @param revisions
	 * @return
	 */
	private List<IdmAudit> sortByTimestamp(List<IdmAudit> revisions) {
		Collections.sort(revisions, new Comparator<IdmAudit>() {
			public int compare(IdmAudit o1, IdmAudit o2) {
				return o2.getRevisionDate().compareTo(o1.getRevisionDate());
			}
		});
		return revisions;
	}

	/**
	 * Find relation defined by clazz for list ids defined by entityId
	 * 
	 * @param clazz
	 * @param relationAtrributeName
	 * @param entityId
	 * @param filter
	 * @return
	 */
	private List<Object[]> findRealationByEntityId(Class clazz, String relationAtrributeName, List<UUID> entityId,
			AuditIdentityFilter filter) {
		List<Object[]> result = new ArrayList<>();
		//
		for (UUID id : entityId) {
			AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(clazz, false, true)
					.add(AuditEntity.relatedId(relationAtrributeName).eq(id));
			//
			if (filter.getModifier() != null) {
				query.add(AuditEntity.revisionProperty(IdmAudit_.modifier.getName()).like(filter.getModifier()));
			}
			//
			List<Object[]> resultTemp = query.getResultList();
			if (!resultTemp.isEmpty()) {
				result.addAll(resultTemp);
			}
		}
		return result;
	}

	/**
	 * Return roles defined for contracts.
	 * 
	 * @param contracts
	 * @param filter
	 * @return
	 */
	private List<Object[]> findRolesForIdentities(List<UUID> contracts, AuditIdentityFilter filter) {
		List<Object[]> result = new ArrayList<>();
		for (UUID contract : contracts) {
			AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(IdmIdentityRole.class, false, true)
					.add(AuditEntity.relatedId(IdmIdentityRole_.identityContract.getName()).eq(contract));
			//
			if (filter.getModifier() != null) {
				query.add(AuditEntity.revisionProperty(IdmAudit_.modifier.getName()).like(filter.getModifier()));
			}
			//
			List<Object[]> resultTemp = query.getResultList();
			if (!resultTemp.isEmpty()) {
				result.addAll(resultTemp);
			}
		}
		return result;
	}

	/**
	 * Find identity by attribute (username)
	 * 
	 * @param filter
	 * @return
	 */
	private List<Object[]> findIdentityByAttribute(AuditIdentityFilter filter) {
		AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(IdmIdentity.class, false, true);
		query.add(AuditEntity.revisionProperty(IdmAudit_.type.getName()).eq(IdmIdentity.class.getCanonicalName()));
		//
		// known bug from envers that traversing a historic entity retrieved with rev_type == DEL doesn't work
		/*String[] types = new String[3];
		types[0] = RevisionType.ADD.name();
		types[1] = RevisionType.DEL.name();
		types[2] = RevisionType.MOD.name();
		query.add(AuditEntity.revisionType().eq(RevisionType.DEL));*/
		//
		// TODO: search by another attribute
		if (filter.getUsername() != null) {
			query.add(AuditEntity.property(IdmIdentity_.username.getName()).eq(filter.getUsername()));
		}
		if (filter.getId() != null) {
			query.add(AuditEntity.property(IdmIdentity_.id.getName()).eq(filter.getId()));
		}
		if (filter.getFrom() != null) {
			query.add(AuditEntity.revisionProperty(IdmAudit_.timestamp.getName()).ge(filter.getFrom().getMillis()));
		}
		if (filter.getTill() != null) {
			query.add(AuditEntity.revisionProperty(IdmAudit_.timestamp.getName()).le(filter.getTill().getMillis()));
		}
		if (filter.getModifier() != null) {
			query.add(AuditEntity.revisionProperty(IdmAudit_.modifier.getName()).like(filter.getModifier()));
		}
		//
		return query.getResultList();
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
		//
		filter.setId(id != null ? UUID.fromString(id.toString()) : null);
		filter.setUsername(username != null ? username.toString() : null);
		filter.setFrom(from != null ? new DateTime(from) : null);
		filter.setTill(till != null ? new DateTime(till) : null);
		filter.setModifier(modifier != null ? modifier.toString() : null);
		//
		return filter;
	}
}