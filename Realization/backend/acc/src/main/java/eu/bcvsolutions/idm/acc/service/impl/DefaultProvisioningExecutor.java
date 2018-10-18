package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.config.domain.ProvisioningConfiguration;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBatchDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningOperation_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysProvisioningOperationRepository;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningExecutor;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBatchService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningOperationService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.DefaultResultModel;
import eu.bcvsolutions.idm.core.api.dto.ResultModel;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.security.api.service.SecurityService;

/**
 * Entry point to all provisioning operations.
 * 
 * @author Radek Tomiška
 *
 */
@Service("provisioningExecutor")
public class DefaultProvisioningExecutor implements ProvisioningExecutor {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultProvisioningExecutor.class);
	private final EntityEventManager entityEventManager;
	private final SysProvisioningOperationService provisioningOperationService;
	private final SysProvisioningBatchService batchService;
	private final NotificationManager notificationManager;
	private final SysSystemService systemService;
	private final SecurityService securityService;
	private final ProvisioningConfiguration provisioningConfiguration;
	private final SysSystemEntityService systemEntityService;

	@Autowired
	public DefaultProvisioningExecutor(
			SysProvisioningOperationRepository repository,
			EntityEventManager entityEventManager,
			SysProvisioningOperationService provisioningOperationService,
			SysProvisioningBatchService batchService,
			NotificationManager notificationManager,
			SysSystemService systemService,
			SecurityService securityService,
			ProvisioningConfiguration provisioningConfiguration,
			SysSystemEntityService systemEntityService) {
		Assert.notNull(entityEventManager);
		Assert.notNull(provisioningOperationService);
		Assert.notNull(batchService);
		Assert.notNull(notificationManager);
		Assert.notNull(systemService);
		Assert.notNull(securityService);
		Assert.notNull(provisioningConfiguration);
		Assert.notNull(systemEntityService);
		//
		this.entityEventManager = entityEventManager;
		this.provisioningOperationService = provisioningOperationService;
		this.batchService = batchService;
		this.notificationManager = notificationManager;
		this.systemService = systemService;
		this.securityService = securityService;
		this.provisioningConfiguration = provisioningConfiguration;
		this.systemEntityService = systemEntityService;
	}
	
	/**
	 * Persist new operation
	 * 
	 * @param provisioningOperation
	 * @return
	 */
	private SysProvisioningOperationDto persistOperation(SysProvisioningOperationDto provisioningOperation) {
		Assert.notNull(provisioningOperation);
		Assert.notNull(provisioningOperation.getSystemEntity());
		Assert.notNull(provisioningOperation.getProvisioningContext());
		// get system from service, in provisioning operation may not exist
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		Assert.notNull(system);
		provisioningOperation.getEmbedded().put(SysProvisioningOperation_.system.getName(), system); // make sure system will be in embedded
		//
		// save new operation to provisioning log / queue
		String uid = systemEntityService.getByProvisioningOperation(provisioningOperation).getUid();
		// look out - system entity uid can be changed - we need to use system entity id
		SysProvisioningBatchDto batch = batchService.findBatch(provisioningOperation.getSystemEntity());
		if (batch == null) {
			// new batch
			batch = batchService.save(new SysProvisioningBatchDto(provisioningOperation));
			provisioningOperation.setResult(new OperationResult.Builder(OperationState.CREATED).build());
		} else {
			SysProvisioningOperationFilter filter = new SysProvisioningOperationFilter();
			filter.setBatchId(batch.getId());
			List<SysProvisioningOperationDto> activeOperations = provisioningOperationService
					.find(filter, new PageRequest(0, 1, new Sort(Direction.DESC, SysProvisioningOperation_.created.getName())))
					.getContent();
			if (activeOperations.isEmpty()) {
				// batch is completed (no operations in queue)
				provisioningOperation.setResult(new OperationResult.Builder(OperationState.CREATED).build());			
			} else {
				// put to queue, if previous
				ResultModel resultModel = new DefaultResultModel(AccResultCode.PROVISIONING_IS_IN_QUEUE, 
						ImmutableMap.of(
								"name", uid, 
								"system", system.getName(),
								"operationType", provisioningOperation.getOperationType(),
								"objectClass", provisioningOperation.getProvisioningContext().getConnectorObject().getObjectClass()));
				LOG.debug(resultModel.toString());				
				provisioningOperation.setResult(new OperationResult.Builder(OperationState.NOT_EXECUTED).setModel(resultModel).build());
				if (activeOperations.get(0).getResultState() == OperationState.RUNNING) { // the last operation = the first operation and it's running
					// Retry date will be set for the second operation in the queue (the first is running). 
					// Other operations will be executed automatically by batch.
					provisioningOperation.setMaxAttempts(provisioningConfiguration.getRetryMaxAttempts());
					batch.setNextAttempt(batchService.calculateNextAttempt(provisioningOperation));
					batch = batchService.save(batch);
				}
				if (securityService.getCurrentId() != null) { // TODO: check logged identity and account owner
					notificationManager.send(
							AccModuleDescriptor.TOPIC_PROVISIONING,
							new IdmMessageDto.Builder(NotificationLevel.WARNING)
								.setModel(provisioningOperation.getResult().getModel())
								.build());
				}
			}
		}
		provisioningOperation.setBatch(batch.getId());
		provisioningOperation = provisioningOperationService.save(provisioningOperation);
		//
		return provisioningOperation;
	}

	@Override
	@Transactional(noRollbackFor = ProvisioningException.class)
	public void execute(SysProvisioningOperationDto provisioningOperation) {
		//
		// execute - after original transaction is commited
		// only if system supports synchronous processing
		SysSystemDto system = systemService.get(provisioningOperation.getSystem());
		Assert.notNull(system);
		if (!system.isQueue()) {
			entityEventManager.publishEvent(provisioningOperation);
			return;
		}
		// put to queue
		if (provisioningOperationService.isNew(provisioningOperation)) {
			provisioningOperation = persistOperation(provisioningOperation);
		}
	}
	
	@Override
	@Transactional(noRollbackFor = ProvisioningException.class)
	public SysProvisioningOperationDto executeSync(SysProvisioningOperationDto provisioningOperation) {
		return executeInternal(provisioningOperation);
	}
	
	/**
	 * We need to wait to transaction commit, when provisioning is executed - all accounts have to be prepared.
	 * 
	 * @param provisioningOperation
	 * @return
	 */
	@TransactionalEventListener
	@Transactional(noRollbackFor = ProvisioningException.class, propagation = Propagation.REQUIRES_NEW)
	public SysProvisioningOperationDto executeInternal(SysProvisioningOperationDto provisioningOperation) {
		Assert.notNull(provisioningOperation);
		Assert.notNull(provisioningOperation.getSystemEntity());
		Assert.notNull(provisioningOperation.getProvisioningContext());
		//
		if (provisioningOperationService.isNew(provisioningOperation)) {
			provisioningOperation = persistOperation(provisioningOperation);
			if (OperationState.NOT_EXECUTED == provisioningOperation.getResult().getState()) {
				return provisioningOperation;
			}
		}
		//
		CoreEvent<SysProvisioningOperationDto> event = new CoreEvent<SysProvisioningOperationDto>(provisioningOperation.getOperationType(), provisioningOperation);
		try {
			EventContext<SysProvisioningOperationDto> context = entityEventManager.process(event);		
			return context.getContent();
		} catch (ProvisioningException ex) { // TODO: result code exceptions? or all exceptions?
			return provisioningOperationService.handleFailed(provisioningOperation, ex);
		}
	}
	
	@Override
	@Transactional
	public SysProvisioningOperationDto cancel(SysProvisioningOperationDto provisioningOperation) {
		// Cancel single request
		CoreEvent<SysProvisioningOperationDto> event = new CoreEvent<SysProvisioningOperationDto>(ProvisioningEventType.CANCEL, provisioningOperation);
		EventContext<SysProvisioningOperationDto> context = entityEventManager.process(event);
		return context.getContent();
	}

	@Override
	public OperationResult execute(SysProvisioningBatchDto batch) {
		Assert.notNull(batch);
		batch = batchService.get(batch.getId());
		//	
		OperationResult result = null;
		List<SysProvisioningOperationDto> operations = provisioningOperationService.getByTimelineAndBatchId(batch.getId());
		if (operations.isEmpty()) {
			// reset next attempt time if is filled
			if (batch.getNextAttempt() != null) {
				batch.setNextAttempt(null);
				batchService.save(batch);
			}
			return new OperationResult.Builder(OperationState.EXECUTED).build();
		}
		for (SysProvisioningOperationDto provisioningOperation : operations) {
			// operation is already running - not complete or executed manually between
			if (provisioningOperation.getResultState() == OperationState.RUNNING) {
				LOG.debug("Previous operation [{}] still running, next operations will be executed after previous operation ends (with next retry run)",
						provisioningOperation.getId());
				//
				ResultModel resultModel = new DefaultResultModel(AccResultCode.PROVISIONING_IS_IN_QUEUE, 
						ImmutableMap.of(
								"name", provisioningOperation.getSystemEntityUid(), 
								"system", provisioningOperation.getSystem(),
								"operationType", provisioningOperation.getOperationType(),
								"objectClass", provisioningOperation.getProvisioningContext().getConnectorObject().getObjectClass()));
				result = new OperationResult.Builder(OperationState.NOT_EXECUTED).setModel(resultModel).build();
				break;
			}			
			// It not possible to get operation from embedded, because missing request
			SysProvisioningOperationDto operation = executeInternal(provisioningOperation); // not run in transaction
			result = operation.getResult();
			if (OperationState.EXECUTED != result.getState()) {
				// stop processing next requests
				return result;
			}
		}
		// last processed request state (previous requests will be OperationState.EXECUTED)
		return result;
	}

	@Override
	@Transactional
	public void cancel(SysProvisioningBatchDto batch) {
		Assert.notNull(batch);
		//
		for (SysProvisioningOperationDto operation : provisioningOperationService.getByTimelineAndBatchId(batch.getId())) {
			// It not possible get operation from embedded, missing request
			cancel(operation);
		}
	}
	
	@Override
	public ProvisioningConfiguration getConfiguration() {
		return provisioningConfiguration;
	}
}
