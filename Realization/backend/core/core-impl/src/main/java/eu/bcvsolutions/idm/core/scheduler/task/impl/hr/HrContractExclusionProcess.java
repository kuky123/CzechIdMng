package eu.bcvsolutions.idm.core.scheduler.task.impl.hr;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.core.api.domain.ContractState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityContractFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.model.event.processor.contract.IdentityContractExclusionProcessor;

/**
 * HR process - identity's contract exclusion. The processes is started for
 * contracts that are both valid (meaning validFrom and validTill) and excluded.
 * 
 * "hrContractExclusion" can be configured as process workflow. 
 * 
 * @author Jan Helbich
 * @author Radek Tomiška
 * @since 7.5.1
 *
 */
@Service
@Description("HR process - contract exclusion")
@DisallowConcurrentExecution
public class HrContractExclusionProcess extends AbstractHrProcess {

	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdentityContractExclusionProcessor identityContractExclusionProcessor;

	public HrContractExclusionProcess() {
	}
	
	public HrContractExclusionProcess(boolean skipAutomaticRoleRecalculation) {
		super(skipAutomaticRoleRecalculation);
	}
	
	@Override
	public boolean continueOnException() {
		return true;
	}
	
	@Override
	public boolean requireNewTransaction() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Find all identity contracts, that are both valid and disabled.
	 */
	@Override
	public Page<IdmIdentityContractDto> getItemsToProcess(Pageable pageable) {
		IdmIdentityContractFilter filter = new IdmIdentityContractFilter();
		filter.setValid(Boolean.TRUE);
		filter.setState(ContractState.EXCLUDED);
		return identityContractService.find(filter, pageable);
	}
	
	@Override
	public Optional<OperationResult> processItem(IdmIdentityContractDto dto) {
		if (!StringUtils.isEmpty(getWorkflowName())) { 
			// wf is configured - execute wf instance
			return super.processItem(dto);
		}
		return Optional.of(identityContractExclusionProcessor.process(dto, isSkipAutomaticRoleRecalculation()));
	}

}
