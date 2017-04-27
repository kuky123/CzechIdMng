package eu.bcvsolutions.idm.acc.dto;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.model.dto.IdmRoleDto;

/**
 * Role could assign account on target system (account template) DTO.
 * 
 * @author Svanda
 *
 */
public class SysRoleSystemDto extends AbstractDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = IdmRoleDto.class)
	private UUID role;
	@Embedded(dtoClass = SysSystemDto.class)
	private UUID system;
	//private SysSystemMappingDto systemMapping;
	
	public UUID getRole() {
		return role;
	}
	public void setRole(UUID role) {
		this.role = role;
	}
	public UUID getSystem() {
		return system;
	}
	public void setSystem(UUID system) {
		this.system = system;
	}

}