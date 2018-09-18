package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem_;
import eu.bcvsolutions.idm.acc.repository.AccIdentityAccountRepository;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemAttributeRepository;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemRepository;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Role could assign identity account on target system.
 * 
 * @author Radek Tomiška
 *
 */
@Service
public class DefaultSysRoleSystemService extends AbstractReadWriteDtoService<SysRoleSystemDto, SysRoleSystem, SysRoleSystemFilter> implements SysRoleSystemService {

	private final SysRoleSystemRepository repository;
	private final SysRoleSystemAttributeRepository roleSystemAttributeRepository;
	private final AccIdentityAccountRepository identityAccountRepository;
	private final IdmRoleService roleService;
	@Autowired
	private RequestManager requestManager;
	
	@Autowired
	public DefaultSysRoleSystemService(
			SysRoleSystemRepository repository,
			SysRoleSystemAttributeRepository roleSystemAttributeRepository,
			AccIdentityAccountRepository identityAccountRepository,
			IdmRoleService roleService) {
		super(repository);
		//
		Assert.notNull(roleSystemAttributeRepository);
		Assert.notNull(identityAccountRepository);
		Assert.notNull(roleService);
		//
		this.repository = repository;
		this.roleSystemAttributeRepository = roleSystemAttributeRepository;
		this.identityAccountRepository = identityAccountRepository;
		this.roleService = roleService;
	}
	
	@Override
	protected Page<SysRoleSystem> findEntities(SysRoleSystemFilter filter, Pageable pageable, BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}
	
	@Override
	@Transactional
	public void delete(SysRoleSystemDto roleSystem, BasePermission... permission) {
		Assert.notNull(roleSystem);
		// delete attributes
		SysRoleSystem roleSystemEntity = this.getEntity(roleSystem.getId());
		roleSystemAttributeRepository.deleteByRoleSystem(this.getEntity(roleSystem.getId()));
		//
		// clear identityAccounts - only link on roleSystem
		identityAccountRepository.clearRoleSystem(roleSystemEntity);
		//
		// Cancel requests and request items using that deleting DTO
		requestManager.onDeleteRequestable(roleSystem);
		
		super.delete(roleSystem, permission);
	}
	
	@Override
	public SysRoleSystemDto save(SysRoleSystemDto dto, BasePermission... permission) {
		Assert.notNull(dto, "RoleSystem cannot be null!");
		Assert.notNull(dto.getRole(), "Role cannot be null!");
		Assert.notNull(dto.getSystem(), "System cannot be null!");
		
		SysRoleSystemFilter filter = new SysRoleSystemFilter();
	    filter.setRoleId(dto.getRole());
	    filter.setSystemId(dto.getSystem());
	    
	    List<SysRoleSystemDto> roleSystems = this.find(filter, null).getContent();
		boolean isDuplicated = roleSystems.stream().filter(roleSystem -> {
			return !roleSystem.getId().equals(dto.getId());
		}).findFirst().isPresent();
		
		if(isDuplicated){
			IdmRoleDto roleDto = roleService.get(dto.getRole());
			SysSystemDto systemDto = DtoUtils.getEmbedded(dto, SysRoleSystem_.system);
			throw new ResultCodeException(AccResultCode.ROLE_SYSTEM_ALREADY_EXISTS,
					ImmutableMap.of("role", roleDto.getCode(), "system", systemDto.getName()));
		}
	    
		return super.save(dto, permission);
	}
	
}
