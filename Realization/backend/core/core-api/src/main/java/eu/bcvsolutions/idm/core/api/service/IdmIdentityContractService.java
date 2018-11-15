package eu.bcvsolutions.idm.core.api.service;

import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import eu.bcvsolutions.idm.core.api.domain.RecursionType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityContractFilter;
import eu.bcvsolutions.idm.core.api.script.ScriptEnabled;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * Operations with working positions
 * 
 * @author Radek Tomiška
 *
 */
public interface IdmIdentityContractService extends
		EventableDtoService<IdmIdentityContractDto, IdmIdentityContractFilter>,
		AuthorizableService<IdmIdentityContractDto>,
		ScriptEnabled {
	
	String DEFAULT_POSITION_NAME = "Default"; // TODO: to configuration manager?
	
	/**
	 * Property in event. If is value TRUE, then will be creation of the default position skipped.
	 */
	String SKIP_CREATION_OF_DEFAULT_POSITION = "skip_creation_of_default_position";
	
	/**
	 * Property in event - when is true, then all dependent HR processes (hrEnableContract, hrEndContract, hrExclusionContract) will be not executed.
	 */
	String SKIP_HR_PROCESSES = "skip_hr_processes";
	
	/**
	 * Returns working positions for given identity
	 * 
	 * @param identityId
	 * @return
	 */
	List<IdmIdentityContractDto> findAllByIdentity(UUID identityId);
	
	/**
	 * Returns all identity contract, where fits conttract's work position with given work position by recursionType.
	 * 
	 * @param workPositionId
	 * @param recursion
	 * @return
	 */
	List<IdmIdentityContractDto> findAllByWorkPosition(UUID workPositionId, RecursionType recursion);
	
	/**
	 * Returns expired contracts
	 * 
	 * @param expiration date to compare
	 * @param pageable
	 * @return
	 */	
	Page<IdmIdentityContractDto> findExpiredContracts(LocalDate expiration, Pageable pageable);
	
	/**
	 * Constructs main contract for given identity by configuration.
	 * 
	 * @see {@link IdmTreeTypeService#getDefaultTreeType()}
	 * @param identityId
	 * @return
	 * @deprecated see {@link #prepareMainContract(UUID)}
	 */
	@Deprecated
	IdmIdentityContractDto prepareDefaultContract(UUID identityId);	
	
	/**
	 * Constructs main contract for given identity by configuration.
	 * 
	 * @see {@link IdmTreeTypeService#getDefaultTreeType()}
	 * @param identityId
	 * @return
	 */
	IdmIdentityContractDto prepareMainContract(UUID identityId);
	
	/**
	 * Returns given identity's prime contract.
	 * If no main contract is defined, then returns the first contract with working position defined (default tree type has higher priority).
	 * 
	 * @param identityId
	 * @return
	 */
	IdmIdentityContractDto getPrimeContract(UUID identityId);
	
	/**
	 * Method get valid {@link IdmIdentityContractDto} for date and {@link IdmIdentityDto} id given in parameter.
	 * Parameter onlyExterne if it's true search only contracts where is {@link IdmIdentityDto} marked as externe, this param can be null - search all contracts.
	 * 
	 * @param identityId
	 * @param date
	 * @param onlyExterne
	 * @return
	 */
	List<IdmIdentityContractDto> findAllValidForDate(UUID identityId, LocalDate date, Boolean onlyExterne);

	
	/**
	 * Returns given valid identity's prime contract, by contract's priority:
	 * - 1. main
	 * - 2. valid (validable and not disabled)
	 * - 3. with working position with default tree type
	 * - 4. with working position with any tree type
	 * - 5. other with lowest valid from
	 * 
	 * @param identityId
	 * @return
	 */
	IdmIdentityContractDto getPrimeValidContract(UUID identityId);

	/**
	 * If given identity has any expired contract, it returns the latest expired (according to validTill).
	 * Otherwise returns null.
	 * 
	 * @param identityId
	 * @param expiration
	 * @return
	 */
	IdmIdentityContractDto findLastExpiredContract(UUID identityId, LocalDate expiration);

}
