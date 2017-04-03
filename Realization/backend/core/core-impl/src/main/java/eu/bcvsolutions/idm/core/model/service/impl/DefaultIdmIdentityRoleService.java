package eu.bcvsolutions.idm.core.model.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.model.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.model.dto.filter.IdentityRoleFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityRoleEvent.IdentityRoleEventType;
import eu.bcvsolutions.idm.core.model.event.processor.IdentityRoleDeleteProcessor;
import eu.bcvsolutions.idm.core.model.event.processor.IdentityRoleSaveProcessor;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityContractRepository;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityRoleRepository;
import eu.bcvsolutions.idm.core.model.repository.IdmRoleRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityRoleService;

/**
 * Operations with identity roles - usable in wf
 * 
 * @author svanda
 *
 */
@Service("identityRoleService")
public class DefaultIdmIdentityRoleService extends AbstractReadWriteEntityService<IdmIdentityRole, IdentityRoleFilter>
		implements IdmIdentityRoleService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmIdentityRoleService.class);

	private final IdmIdentityRoleRepository repository;
	private final IdmRoleRepository roleRepository;
	private final IdmIdentityContractRepository identityContractRepository;
	private final EntityEventManager entityEventManager;

	@Autowired
	public DefaultIdmIdentityRoleService(
			IdmIdentityRoleRepository repository,
			IdmRoleRepository roleRepository,
			IdmIdentityContractRepository identityContractRepository,
			EntityEventManager entityEventManager) {
		super(repository);
		//
		Assert.notNull(roleRepository);
		Assert.notNull(entityEventManager);
		Assert.notNull(identityContractRepository);
		//
		this.repository = repository;
		this.roleRepository = roleRepository;
		this.entityEventManager = entityEventManager;
		this.identityContractRepository = identityContractRepository;
	}
	
	/**
	 * Publish {@link IdentityRoleEvent} only.
	 * 
	 * @see {@link IdentityRoleSaveProcessor}
	 */
	@Override
	@Transactional
	public IdmIdentityRole save(IdmIdentityRole entity) {
		Assert.notNull(entity);
		Assert.notNull(entity.getRole());
		Assert.notNull(entity.getIdentityContract());
		//
		LOG.debug("Saving role [{}] for identity [{}]", entity.getRole().getName(), entity.getIdentityContract().getIdentity().getUsername());
		return entityEventManager.process(
				new IdentityRoleEvent(isNew(entity) ? IdentityRoleEventType.CREATE : IdentityRoleEventType.UPDATE, entity)).getContent();
	}

	/**
	 * Publish {@link IdentityRoleEvent} only.
	 * 
	 * @see {@link IdentityRoleDeleteProcessor}
	 */
	@Override
	@Transactional
	public void delete(IdmIdentityRole entity) {
		Assert.notNull(entity);
		Assert.notNull(entity.getRole());
		Assert.notNull(entity.getIdentityContract());
		//
		LOG.debug("Deleting role [{}] for identity [{}]", entity.getRole().getName(), entity.getIdentityContract().getIdentity().getUsername());
		entityEventManager.process(new IdentityRoleEvent(IdentityRoleEventType.DELETE, entity));
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmIdentityRole> getRoles(IdmIdentity identity) {
		return repository.findAllByIdentityContract_Identity(identity, new Sort("role.name"));
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmIdentityRole> getRoles(IdmIdentityContract identityContract) {
		return repository.findAllByIdentityContract(identityContract, new Sort("role.name"));
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<IdmIdentityRole> getRolesByAutomaticRole(UUID roleTreeNodeId, Pageable pageable) {
		return repository.findByRoleTreeNode_Id(roleTreeNodeId, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmIdentityRole> getByIds(List<String> ids) {
		if (ids == null) {
			return null;
		}
		List<IdmIdentityRole> idmRoles = new ArrayList<>();
		for (String id : ids) {
			idmRoles.add(get(id));
		}
		return idmRoles;
	}

	@Override
	@Transactional
	public IdmIdentityRole updateByDto(String id, IdmIdentityRoleDto dto) {
		Assert.notNull(id);
		Assert.notNull(dto);

		IdmIdentityRole identityRole = repository.findOne(UUID.fromString(id));
		return this.save(toEntity(dto, identityRole));
	}

	@Override
	@Transactional
	@Deprecated
	public IdmIdentityRole addByDto(IdmIdentityRoleDto dto) {
		Assert.notNull(dto);
		//
		IdmIdentityRole identityRole = new IdmIdentityRole();
		return this.save(toEntity(dto, identityRole));
	}

	@Deprecated
	private IdmIdentityRole toEntity(IdmIdentityRoleDto identityRoleDto, IdmIdentityRole identityRole) {
		if (identityRoleDto == null || identityRole == null) {
			return null;
		}
		IdmRole role = identityRole.getRole();
		IdmIdentityContract identityContract = identityRole.getIdentityContract();
		if (identityRoleDto.getRole() != null) {
			role = roleRepository.findOne(identityRoleDto.getRole());
		}
		if (identityRoleDto.getIdentityContract() != null) {
			identityContract = identityContractRepository.findOne(identityRoleDto.getIdentityContract());
		}
		identityRole.setRole(role);
		identityRole.setIdentityContract(identityContract);
		identityRole.setValidFrom(identityRoleDto.getValidFrom());
		identityRole.setValidTill(identityRoleDto.getValidTill());
		identityRole.setOriginalCreator(identityRoleDto.getOriginalCreator());
		identityRole.setOriginalModifier(identityRoleDto.getOriginalModifier());
		return identityRole;
	}

	@Override
	public boolean isIdentityRoleValidFromNow(IdmIdentityRole identityRole) {
		LocalDate fromDate = LocalDate.MIN;
		if (identityRole.getValidFrom() != null) {
			fromDate = identityRole.getValidFrom().toDateTimeAtStartOfDay().toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}

		LocalDate tillDate = LocalDate.MAX;
		if (identityRole.getValidTill() != null) {
			tillDate = identityRole.getValidTill().toDateTimeAtStartOfDay().toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		LocalDate now = LocalDate.now();
		if ((now.isAfter(fromDate) || now.isEqual(fromDate)) && (now.isBefore(tillDate) || now.isEqual(tillDate))) {
			return true;
		}
		return false;
	}	

	public Page<IdmIdentityRole> findExpiredRoles(org.joda.time.LocalDate expirationDate, Pageable page) {
		Assert.notNull(expirationDate);
		//
		return repository.findExpiredRoles(expirationDate, page);
	}

}
