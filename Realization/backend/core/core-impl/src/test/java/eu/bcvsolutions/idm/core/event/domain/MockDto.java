package eu.bcvsolutions.idm.core.event.domain;

import java.io.Serializable;
import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;

/**
 * Test dto for testing processor generalization
 * 
 * @author Radek Tomiška
 *
 */
public class MockDto implements BaseDto {

	private static final long serialVersionUID = 1L;
	private UUID id = UUID.randomUUID();
	
	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public void setId(Serializable id) {
		this.id = DtoUtils.toUuid(id);
	}

}
