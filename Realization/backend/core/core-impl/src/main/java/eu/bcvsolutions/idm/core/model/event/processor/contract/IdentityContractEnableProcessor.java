package eu.bcvsolutions.idm.core.model.event.processor.contract;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.domain.ContractState;
import eu.bcvsolutions.idm.core.api.domain.IdentityState;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.processor.IdentityContractProcessor;
import eu.bcvsolutions.idm.core.api.service.IdmAutomaticRoleAttributeService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.event.IdentityContractEvent.IdentityContractEventType;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent.IdentityEventType;
import eu.bcvsolutions.idm.core.model.event.processor.AbstractWorkflowEventProcessor;

/**
 * HR process - enable identity's contract process. The processes is started
 * for contracts that are both valid (meaning validFrom and validTill and disabled state) and
 * not excluded.
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Description("HR process - enable identity's contract process. The processes is started"
		+ " for contracts that are both valid (meaning validFrom and validTill and disabled state) and"
		+ " not excluded.")
public class IdentityContractEnableProcessor
		extends AbstractWorkflowEventProcessor<IdmIdentityContractDto> 
		implements IdentityContractProcessor {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IdentityContractEnableProcessor.class);
	//
	public static final String PROCESSOR_NAME = "identity-contract-enable-processor";
	//
	@Autowired private IdmIdentityService identityService;
	
	public IdentityContractEnableProcessor() {
		super(IdentityContractEventType.CREATE, IdentityContractEventType.UPDATE);
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	@Override
	protected String getDefaultWorkflowDefinitionKey() {
		// "hrEnableContract" can be configured as example, 
		// default business logic is implemented in process method, if no definition is configured
		return null;
	}
	
	/**
	 * Identity contracts, that was valid and not excluded - is ecluded now
	 */
	@Override
	public boolean conditional(EntityEvent<IdmIdentityContractDto> event) {
		// Skip HR process
		if (this.getBooleanProperty(IdmIdentityContractService.SKIP_HR_PROCESSES, event.getProperties())) {
			return false;
		}
		IdmIdentityContractDto current = event.getContent();
		IdmIdentityContractDto previous = event.getOriginalSource();
		//
		return (previous == null || !previous.isValid() || previous.getState() == ContractState.EXCLUDED) && current.isValid();
	}
	
	@Override
	public EventResult<IdmIdentityContractDto> process(EntityEvent<IdmIdentityContractDto> event) {
		if (!StringUtils.isEmpty(getWorkflowDefinitionKey())) { 
			// wf is configured - execute wf instance
			return super.process(event);
		}
		//
		IdmIdentityContractDto contract = event.getContent();
		OperationResult result = process(contract, (Boolean) event.getProperties().get(IdmAutomaticRoleAttributeService.SKIP_RECALCULATION));
		return new DefaultEventResult.Builder<>(event, this).setResult(result).build();
	}
	
	/**
	 * Check identity state after contract is enabled
	 * 
	 * @param contract
	 * @param skipRecalculation Skip automatic role recalculation
	 * @return
	 */
	public OperationResult process(IdmIdentityContractDto contract, Boolean skipRecalculation) {
		if (contract.isValid() && contract.getState() != ContractState.EXCLUDED) {
			IdmIdentityDto identity = identityService.get(contract.getIdentity());
			IdentityState newState = identityService.evaluateState(identity.getId());
			//
			// we want to enable identity with contract other than default one
			if (newState == IdentityState.VALID 
					&& (identity.isDisabled() || identity.getState() == IdentityState.CREATED)) {
				LOG.info("Change identity [{}] state [{}]", identity.getUsername(), IdentityState.VALID);
				//
				identity.setState(IdentityState.VALID);
				// is neccessary publish new event with skip recalculation automatic roles
				IdentityEvent event = new IdentityEvent(IdentityEventType.UPDATE, identity);
				event.getProperties().put(IdmAutomaticRoleAttributeService.SKIP_RECALCULATION, skipRecalculation);
		    	identityService.publish(event);
			}
		}
		return new OperationResult.Builder(OperationState.EXECUTED).build();
	}
	
	/**
	 * after save
	 */
	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER + 200;
	}

}
