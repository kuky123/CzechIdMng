package eu.bcvsolutions.idm.core.model.event.processor.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.config.domain.RequestConfiguration;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.model.event.RequestEvent.RequestEventType;

/**
 * Approve changes in the request.
 * 
 * @author svandav
 *
 */
@Component(RequestApprovalProcessor.PROCESSOR_NAME)
@Description("Approve changes in the request.")
public class RequestApprovalProcessor extends CoreEventProcessor<IdmRequestDto> {
	
	public static final String PROCESSOR_NAME = "request-approval-processor";
	public static final String CHECK_RIGHT_PROPERTY = "checkRight";

	@Autowired
	private RequestManager manager;
	@Autowired
	private RequestConfiguration requestConfiguration;
	
	public RequestApprovalProcessor() {
		super(RequestEventType.EXECUTE); 
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@SuppressWarnings("unchecked")
	@Override
	public EventResult<IdmRequestDto> process(EntityEvent<IdmRequestDto> event) {
		IdmRequestDto dto = event.getContent();
		boolean checkRight = (boolean) event.getProperties().get(CHECK_RIGHT_PROPERTY);
		//
		String ownerTyp = dto.getOwnerType();
		Assert.notNull(ownerTyp, "Owner type is rquired for start approval process!");
		Class<Requestable> ownerClass = null;
		try {
			ownerClass = (Class<Requestable>) Class.forName(ownerTyp);
		} catch (ClassNotFoundException e) {
			throw new CoreException(e);
		}
		
		String wfDefinition = requestConfiguration.getRequestApprovalProcessKey(ownerClass);
		if(Strings.isNullOrEmpty(wfDefinition)){
			throw new ResultCodeException(CoreResultCode.REQUEST_NO_WF_DEF_FOUND, ImmutableMap.of("entityType", dto.getOwnerType()));
		}
		boolean approved = manager.startApprovalProcess(dto, checkRight, event, wfDefinition);
		DefaultEventResult<IdmRequestDto> result = new DefaultEventResult<>(event, this);
		result.setSuspended(!approved);
		return result;
	}
	
	/**
	 * Before standard save
	 * 
	 * @return
	 */
	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER - 1000;
	}
}
