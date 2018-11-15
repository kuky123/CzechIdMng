package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.config.domain.TreeConfiguration;
import eu.bcvsolutions.idm.core.api.domain.ContractState;
import eu.bcvsolutions.idm.core.api.domain.RecursionType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmContractSliceFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityContractFilter;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity_;
import eu.bcvsolutions.idm.core.api.event.processor.IdentityContractProcessor;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmContractSliceService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.utils.RepositoryUtils;
import eu.bcvsolutions.idm.core.eav.api.service.AbstractFormableService;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract_;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode_;
import eu.bcvsolutions.idm.core.model.event.IdentityContractEvent;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityContractRepository;
import eu.bcvsolutions.idm.core.model.repository.IdmTreeNodeRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Identity contract administration
 * - supports {@link IdentityContractEvent}.
 * - identity contract is required for role assign
 * 
 * @author Radek Tomiška
 * @see IdentityContractEvent
 * @see IdentityContractProcessor
 *
 */
public class DefaultIdmIdentityContractService 
		extends AbstractFormableService<IdmIdentityContractDto, IdmIdentityContract, IdmIdentityContractFilter>
		implements IdmIdentityContractService {

	private final IdmIdentityContractRepository repository;
	private final TreeConfiguration treeConfiguration;
	private final IdmTreeNodeRepository treeNodeRepository;
	@Autowired
	private IdmContractSliceService contractSliceService;
	
	@Autowired
	public DefaultIdmIdentityContractService(
			IdmIdentityContractRepository repository,
			FormService formService,
			EntityEventManager entityEventManager,
			TreeConfiguration treeConfiguration,
			IdmTreeNodeRepository treeNodeRepository) {
		super(repository, entityEventManager, formService);
		//
		Assert.notNull(treeConfiguration);
		Assert.notNull(treeNodeRepository);
		//
		this.repository = repository;
		this.treeConfiguration = treeConfiguration;
		this.treeNodeRepository = treeNodeRepository;
	}
	
	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.IDENTITYCONTRACT, getEntityClass());
	}
	
	@Override
	protected IdmIdentityContract toEntity(IdmIdentityContractDto dto, IdmIdentityContract entity) {
		IdmIdentityContract contract = super.toEntity(dto, entity);
		if (contract != null && dto != null) {
			contract.setDisabled(dto.isDisabled()); // redundant attribute for queries
		}
		return contract;
	}
	
	@Override
	protected IdmIdentityContractDto toDto(IdmIdentityContract entity, IdmIdentityContractDto dto) {
		IdmIdentityContractDto resultDto = super.toDto(entity, dto);
		// Set attribute if that contract was created by slices
		if(resultDto != null && resultDto.getId() != null && !resultDto.isTrimmed()) {
			IdmContractSliceFilter sliceFilter = new IdmContractSliceFilter();
			sliceFilter.setParentContract(resultDto.getId());
			if(contractSliceService.find(sliceFilter, null).getTotalElements() > 0) {
				resultDto.setControlledBySlices(Boolean.TRUE);
			}else {
				resultDto.setControlledBySlices(Boolean.FALSE);
			}
		}
		return resultDto;
	}
	
	@Override
	protected List<Predicate> toPredicates(Root<IdmIdentityContract> root, CriteriaQuery<?> query, CriteriaBuilder builder, IdmIdentityContractFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		// quick
		if (StringUtils.isNotEmpty(filter.getText())) {
			Path<IdmTreeNode> wp = root.get(IdmIdentityContract_.workPosition);
			predicates.add(
					builder.or(
							builder.like(builder.lower(root.get(IdmIdentityContract_.position)), "%" + filter.getText().toLowerCase() + "%"),
							builder.like(builder.lower(wp.get(IdmTreeNode_.name)), "%" + filter.getText().toLowerCase() + "%"),
							builder.like(builder.lower(wp.get(IdmTreeNode_.code)), "%" + filter.getText().toLowerCase() + "%")
							)
					);
		}
		if (filter.getIdentity() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.identity).get(AbstractEntity_.id), filter.getIdentity()));
		}
		if (filter.getValidTill() != null) {
			predicates.add(builder.lessThanOrEqualTo(root.get(IdmIdentityContract_.validTill), filter.getValidTill()));
		}
		if (filter.getValidFrom() != null) {
			predicates.add(builder.greaterThanOrEqualTo(root.get(IdmIdentityContract_.validFrom), filter.getValidFrom()));
		}
		if (filter.getExterne() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.externe), filter.getExterne()));
		}
		if (filter.getDisabled() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.disabled), filter.getDisabled()));
		}
		if (filter.getMain() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.main), filter.getMain()));
		}
		if (filter.getValid() != null) {
			final LocalDate today = LocalDate.now();
			//
			if (filter.getValid()) {
				// EXCLUDED contracts remain valid ...
				predicates.add(
						builder.and(
								RepositoryUtils.getValidPredicate(root, builder, today),
								builder.equal(root.get(IdmIdentityContract_.disabled), Boolean.FALSE)
								)								
						);
			} else {
				predicates.add(
						builder.or(
								builder.lessThan(root.get(IdmIdentityContract_.validTill), today),
								builder.greaterThan(root.get(IdmIdentityContract_.validFrom), today),
								builder.equal(root.get(IdmIdentityContract_.disabled), Boolean.TRUE)
								)
						);
			}
		}
		if (filter.getValidNowOrInFuture() != null) {
			if (filter.getValidNowOrInFuture()) {
				predicates.add(
						builder.and(
								builder.or(
										builder.greaterThanOrEqualTo(root.get(IdmIdentityContract_.validTill), LocalDate.now()),
										builder.isNull(root.get(IdmIdentityContract_.validTill))
										),
								builder.equal(root.get(IdmIdentityContract_.disabled), Boolean.FALSE)
							));
			} else {
				predicates.add(builder.lessThan(root.get(IdmIdentityContract_.validTill), LocalDate.now()));
			}
		}
		if (filter.getState() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.state), filter.getState()));
		}
		if (StringUtils.isNotEmpty(filter.getPosition())) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.position), filter.getPosition()));
		}
		if (filter.getWorkPosition() != null) {
			predicates.add(builder.equal(root.get(IdmIdentityContract_.workPosition).get(IdmTreeNode_.id), filter.getWorkPosition()));
		}
		Boolean excluded = filter.getExcluded();
		if (excluded != null) {
			Predicate excludedPredicate = builder.equal(root.get(IdmIdentityContract_.state), ContractState.EXCLUDED);
			if (excluded) {
				predicates.add(excludedPredicate);
			} else {
				predicates.add(
						builder.or(
								builder.not(excludedPredicate),
								builder.isNull(root.get(IdmIdentityContract_.state))
								)
						);
			}
		}
		//
		return predicates;
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmIdentityContractDto> findAllByIdentity(UUID identityId) {
		return toDtos(repository.findAllByIdentity_Id(identityId, new Sort(IdmIdentityContract_.validFrom.getName())), false);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmIdentityContractDto> findAllByWorkPosition(UUID workPositionId, RecursionType recursion) {
		Assert.notNull(workPositionId);
		// TODO: use uuid only - rewrite to subquery
		IdmTreeNode workPosition = treeNodeRepository.findOne(workPositionId);
		Assert.notNull(workPosition);
		//
		return toDtos(repository.findAllByWorkPosition(workPosition, recursion == null ? RecursionType.NO : recursion), false);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<IdmIdentityContractDto> findExpiredContracts(LocalDate expiration, Pageable pageable) {
		return toDtoPage(repository.findExpiredContracts(expiration, pageable));
	}
	
	
	@Override
	@Deprecated
	public IdmIdentityContractDto prepareDefaultContract(UUID identityId) {
		return prepareMainContract(identityId);
	}

	@Override
	public IdmIdentityContractDto prepareMainContract(UUID identityId) {
		Assert.notNull(identityId);
		//
		// set identity
		IdmIdentityContractDto contract = new IdmIdentityContractDto();
		contract.setIdentity(identityId);
		contract.setMain(true);
		//
		// set default contract name
		// TODO: add boolean attribute
		contract.setPosition(DEFAULT_POSITION_NAME);
		//
		// set working position
		IdmTreeNodeDto defaultTreeNode = treeConfiguration.getDefaultNode();
		if (defaultTreeNode != null) {
			contract.setWorkPosition(defaultTreeNode.getId());
		}
		return contract;
	}
	
	/**
	 * Returns given identity's prime contract, by contract's priority:
	 * - 1. main
	 * - 2. valid (validable and not disabled)
	 * - 3. with working position with default tree type
	 * - 4. with working position with any tree type
	 * - 5. other with lowest valid from
	 * 
	 * @param identityId
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public IdmIdentityContractDto getPrimeContract(UUID identityId) {
		Assert.notNull(identityId);
		//
		// find all identity working position
		List<IdmIdentityContract> contracts = repository.findAllByIdentity_Id(identityId, null);
		if (contracts.isEmpty()) {
			return null;
		}
		Collections.sort(contracts, new PrimeIdentityContractComparator(treeConfiguration.getDefaultType()));
		// return contract with the highest priority
		return toDto(contracts.get(contracts.size() - 1));
	}
	
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
	@Override
	@Transactional(readOnly = true)
	public IdmIdentityContractDto getPrimeValidContract(UUID identityId) {
		Assert.notNull(identityId);
		//
		// find valid all identity working position
		List<IdmIdentityContract> contracts = repository.findAllValidContracts(identityId, LocalDate.now(), null);
		if (contracts.isEmpty()) {
			return null;
		}
		Collections.sort(contracts, new PrimeIdentityContractComparator(treeConfiguration.getDefaultType()));
		// return contract with the highest priority
		return toDto(contracts.get(contracts.size() - 1));
	}

	@Override
	public List<IdmIdentityContractDto> findAllValidForDate(UUID identityId, LocalDate date, Boolean onlyExterne) {
		return toDtos(this.repository.findAllValidContracts(identityId, date, onlyExterne), false);
	}

	@Override
	public IdmIdentityContractDto findLastExpiredContract(UUID identityId, LocalDate expiration) {
		List<IdmIdentityContract> contracts = repository.findExpiredContracts(expiration, null).getContent();
		IdmIdentityContract lastValidContract = contracts.stream().max(Comparator.comparing(IdmIdentityContract::getValidTill)).orElse(null);
		return lastValidContract == null ? null : toDto(lastValidContract);
	}
	/**
	 * Returns contracts sorted by priority:
	 * - 1. main
	 * - 2. valid (validable and not disabled)
	 * - 3. with working position with default tree type
	 * - 4. with working position with any tree type
	 * - 5. other with lowest valid from
	 * 
	 * @author Radek Tomiška
	 *
	 */
	private static class PrimeIdentityContractComparator implements Comparator<IdmIdentityContract> {

		private final IdmTreeTypeDto defaultTreeType;
		
		public PrimeIdentityContractComparator(IdmTreeTypeDto defaultTreeType) {
			this.defaultTreeType = defaultTreeType;
		}
		
		@Override
		public int compare(IdmIdentityContract o1, IdmIdentityContract o2) {
			CompareToBuilder builder = new CompareToBuilder();
			// main
			builder.append(o1.isMain(), o2.isMain());
			// valid 
			builder.append(!o1.isDisabled(), !o2.isDisabled());
			// not disabled
			builder.append(o1.isValid(), o2.isValid());
			// with default tree position
			if (defaultTreeType != null) {
				builder.append(o1.getWorkPosition() != null && o1.getWorkPosition().getTreeType().getId().equals(defaultTreeType.getId()), 
						o2.getWorkPosition() != null && o2.getWorkPosition().getTreeType().getId().equals(defaultTreeType.getId()));
			}			
			// with any tree position
			builder.append(o1.getWorkPosition() != null, o2.getWorkPosition() != null);
			// by valid from
			builder.append(o1.getValidFrom(), o2.getValidFrom());
			//
			return builder.toComparison();
		}
	}
}

