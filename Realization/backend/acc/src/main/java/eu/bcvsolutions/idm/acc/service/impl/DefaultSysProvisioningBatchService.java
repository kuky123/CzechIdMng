package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.config.domain.ProvisioningConfiguration;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBatchDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningBatch;
import eu.bcvsolutions.idm.acc.repository.SysProvisioningBatchRepository;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBatchService;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.filter.EmptyFilter;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;

/**
 * Persists provisioning operation batches
 * 
 * @author Radek Tomiška
 * @author Filip Mestanek
 *
 */
@Service("provisioningBatchService")
public class DefaultSysProvisioningBatchService
		extends AbstractReadWriteDtoService<SysProvisioningBatchDto, SysProvisioningBatch, EmptyFilter> 
		implements SysProvisioningBatchService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultSysProvisioningBatchService.class);
	//
	private final SysProvisioningBatchRepository repository;
	//
	@Autowired private ProvisioningConfiguration provisioningConfiguration;
	
	@Autowired
	public DefaultSysProvisioningBatchService(
			SysProvisioningBatchRepository repository) {
		super(repository);
		//
		Assert.notNull(repository);
		//
		this.repository = repository;
	}
	
	/**
	 * Calculates when the request should be invoked
	 * 
	 * @return Date of the next attempt. Null if there should be no next attempt 
	 */
	@Override
	public DateTime calculateNextAttempt(SysProvisioningOperationDto operation) {		
		if (operation.getCurrentAttempt() >= operation.getMaxAttempts()) {
			LOG.debug("All attemts for retry mechanism for operation [{}] was used current [{}] - max [{}].",
					operation.getId(), operation.getCurrentAttempt(), operation.getMaxAttempts());
			return null;
		}
		//
		DateTime nextAttempt;
		if (operation.getCurrentAttempt() == 0) {
			nextAttempt = new DateTime();
		} else {
			List<Integer> sequence = provisioningConfiguration.getRetrySequence();
			int indexToSequence = Math.min(operation.getCurrentAttempt() - 1, sequence.size() - 1);
			Integer secInterval = sequence.get(indexToSequence);
			//
			nextAttempt = new DateTime().plusSeconds(secInterval);
		}
		//
		LOG.trace("Next retry attempt for operation [{}] will be executed [{}].", operation.getId(), nextAttempt);
		//
		return nextAttempt;
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<SysProvisioningBatchDto> findBatchesToProcess(Boolean virtualSystem, Pageable pageable) {
		if (virtualSystem == null) {
			return toDtoPage(repository.findByOperationState(OperationState.CREATED, pageable));
		}
		return toDtoPage(repository.findByVirtualSystemAndOperationState(virtualSystem, OperationState.CREATED, pageable));
	}
	
	@Override
	@Transactional(readOnly = true)
	public Page<SysProvisioningBatchDto> findBatchesToRetry(DateTime date, Pageable pageable) {
		return toDtoPage(repository.findByNextAttemptLessThanEqual(date, pageable));
	}
	
	@Override
	@Transactional
	public SysProvisioningBatchDto findBatch(UUID systemId, UUID entityIdentifier, UUID systemEntity) {
		return findBatch(systemEntity);
	}
	
	@Override
	@Transactional
	public SysProvisioningBatchDto findBatch(UUID systemEntity) {
		List<SysProvisioningBatch> batches = repository.findAllBySystemEntity_IdOrderByCreatedAsc(systemEntity);
		//
		if (batches.isEmpty()) {
			return null;
		}
		//
		if (batches.size() == 1) {
			// consistent state
			return  toDto(batches.get(0));
		}
		//
		// merge batches together - use the first as target
		SysProvisioningBatch firstBatch = batches.get(0);
		for (int index = 1; index < batches.size(); index ++) {
			// update batch for other provisioning operations 
			SysProvisioningBatch oldBatch = batches.get(index);
			repository.mergeBatch(oldBatch, firstBatch);
			deleteById(oldBatch.getId());
		}
		//
		return toDto(firstBatch);
	}
}
