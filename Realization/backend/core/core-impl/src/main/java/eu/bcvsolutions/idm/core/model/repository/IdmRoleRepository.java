package eu.bcvsolutions.idm.core.model.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;

/**
 * Roles repository
 * 
 * @author Radek Tomiška 
 *
 */
public interface IdmRoleRepository extends AbstractEntityRepository<IdmRole> {
	
	@Query(value = "select e from #{#entityName} e where e.code = :code")
	IdmRole findOneByCode(@Param("code") String code);
}
