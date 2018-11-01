package eu.bcvsolutions.idm.acc.service.impl;

import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Simple provisioning exception simulation
 * 
 * @author Radek Tomiška
 *
 */
@Component
public class TestProvisioningExceptionProcessor extends AbstractEntityEventProcessor<SysProvisioningOperationDto> {

	private boolean disabled = true;
	
	@Override
	public EventResult<SysProvisioningOperationDto> process(EntityEvent<SysProvisioningOperationDto> event) {
		throw new ProvisioningException(AccResultCode.PROVISIONING_FAILED, "test exception");
	}
	
	@Override
	public boolean isDisabled() {
		return disabled;
	}
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Before provisioning
	 */
	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER - 1;
	}

	
	
}
