package eu.bcvsolutions.idm.core.api.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.bcvsolutions.idm.core.api.domain.AutomaticRoleAttributeRuleType;
import eu.bcvsolutions.idm.core.api.dto.AbstractIdmAutomaticRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmAutomaticRoleAttributeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmContractPositionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAutomaticRoleFilter;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Automatic role by attribute
 * 
 * @author Radek Tomiška
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

public interface IdmAutomaticRoleAttributeService
		extends ReadWriteDtoService<IdmAutomaticRoleAttributeDto, IdmAutomaticRoleFilter>,
		AuthorizableService<IdmAutomaticRoleAttributeDto> {
	
	/**
	 * Property in event. If is value TRUE, then will be recalculation skipped.
	 */
	String SKIP_RECALCULATION = "skip_recalculation";
	
	/**
	 * Prepare role request for delete automatic roles.
	 * Beware after was method marked as deprecated, returns null instead role request
	 * 
	 * @param identityRole
	 * @param automaticRoles
	 * @return
	 * @deprecated Role request isn't used anymore, please use {@link IdmAutomaticRoleAttributeService#removeAutomaticRoles(IdmIdentityRoleDto)}
	 */
	@Deprecated
	IdmRoleRequestDto prepareRemoveAutomaticRoles(IdmIdentityRoleDto identityRole, Set<AbstractIdmAutomaticRoleDto> automaticRoles);
	
	/**
	 * Remove identity role (must be automatic role). This method doesn't use standard role request 
	 * and remove {@link IdmIdentityRoleDto} directly.
	 * In this method skip check changed authorities by processor {@link IdentityRoleDeleteAuthoritiesProcessor}.
	 *
	 * @param contract
	 */
	void removeAutomaticRoles(IdmIdentityRoleDto identityRole);
	
	/**
	 * Remove automatic role from contract. This method doesn't use standard role request 
	 * and remove {@link IdmIdentityRoleDto} directly.
	 * In this method skip check changed authorities by processor {@link IdentityRoleDeleteAuthoritiesProcessor}.
	 * 
	 * @param contractId
	 * @param automaticRoles
	 */
	void removeAutomaticRoles(UUID contractId, Set<AbstractIdmAutomaticRoleDto> automaticRoles);
	
	/**
	 * Return all rules that pass/not pass (this is controlled by boolean parameter 'pass'),
	 * automatic role will be search only for contract id.
	 *
	 * @param pass
	 * @param type
	 * @param identityId
	 * @param contractId
	 * @return
	 */
	Set<AbstractIdmAutomaticRoleDto> getRulesForContract(boolean pass, AutomaticRoleAttributeRuleType type, UUID contractId);
	
	/**
	 * Return all id's of {@link IdmIdentityContractDto} that passed or not passed (defined in parameter passed) by given automatic role by attribute.
	 *
	 * @param automaticRoleId
	 * @param passed
	 * @param pageable
	 * @return
	 */
	Page<UUID> getContractsForAutomaticRole(UUID automaticRoleId, boolean passed, Pageable pageable);

	/**
	 * Prepare add automatic role to contract.
	 * Beware after was method marked as deprecated, returns null instead role request
	 * 
	 * @param contract
	 * @param automaticRoles
	 * @return
	 * @deprecated Role request isn't used anymore, please use {@link IdmAutomaticRoleAttributeService#addAutomaticRoles(IdmIdentityContractDto, Set)
	 */
	@Deprecated
	IdmRoleRequestDto prepareAddAutomaticRoles(IdmIdentityContractDto contract,
			Set<AbstractIdmAutomaticRoleDto> automaticRoles);
	
	/**
	 * Add automatic role to contract. This method doesn't use standard role request 
	 * and add {@link IdmIdentityRoleDto} directly.
	 * In this method skip check changed authorities by processor {@link IdentityRoleAddAuthoritiesProcessor}.
	 * 
	 * @param contract
	 * @param automaticRoles
	 */
	void addAutomaticRoles(IdmIdentityContractDto contract, Set<AbstractIdmAutomaticRoleDto> automaticRoles);
	
	/**
	 * Add automatic role to contract position. This method doesn't use standard role request 
	 * and add {@link IdmIdentityRoleDto} directly.
	 * In this method skip check changed authorities by processor {@link IdentityRoleAddAuthoritiesProcessor}.
	 * 
	 * @param contract
	 * @param automaticRoles
	 */
	void addAutomaticRoles(IdmContractPositionDto contractPosition, Set<AbstractIdmAutomaticRoleDto> automaticRoles);
	
	/**
	 * Process new automatic roles for contract given in parameter.
	 * New automatic role in parameter passedAutomaticRoles will be add by request to given identity contract
	 * and not passed automatic role given in parameter notPassedAutomaticRoles will be removed.
	 *
	 * @param contractId
	 * @param passedAutomaticRoles
	 * @param notPassedAutomaticRoles
	 */
	void processAutomaticRolesForContract(UUID contractId, Set<AbstractIdmAutomaticRoleDto> passedAutomaticRoles, Set<AbstractIdmAutomaticRoleDto> notPassedAutomaticRoles);

	
	/**
	 * Recalculate this automatic role and rules and assign new role to identity or remove.
	 * 
	 * @param automaticRoleId
	 * @return 
	 */
	IdmAutomaticRoleAttributeDto recalculate(UUID automaticRoleId);
	
	/**
	 * Find all automatic role that is not in concept state. {@link AutomaticRoleAttributeRuleType}
	 *
	 * @param type
	 * @param page
	 * @return
	 */
	Page<IdmAutomaticRoleAttributeDto> findAllToProcess(AutomaticRoleAttributeRuleType type, Pageable page);
}
