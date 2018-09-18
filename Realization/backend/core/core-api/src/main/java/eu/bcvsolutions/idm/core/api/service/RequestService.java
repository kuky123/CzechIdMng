package eu.bcvsolutions.idm.core.api.service;

import java.util.UUID;

import eu.bcvsolutions.idm.core.api.dto.AbstractRequestDto;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;

/**
 * Universal request service
 *
 * @author svandav
 * @since 9.1.0
 */
public interface RequestService<DTO extends AbstractRequestDto> {

	/**
	 * Start approval process for given request. Request approving will be started
	 * in new transaction.
	 * 
	 * @param requestId
	 * @param checkRight
	 *            - If is true, then will be check right for immediately execution
	 *            (if is requires)
	 */
	DTO startRequest(UUID requestId, boolean checkRight);

	/**
	 * Internal start request. Not accessible from REST.
	 * 
	 * @param requestId
	 * @param checkRight
	 *            - If is true, then will be check right for immediately execution
	 *            (if is requires)
	 */
	DTO startRequestInternal(UUID requestId, boolean checkRight);

	/**
	 * Realization of request (applying the requested changes).
	 * 
	 * @param requestId
	 * @return
	 */
	DTO executeRequest(UUID requestId);

	/**
	 * Start approval process for this request.
	 * 
	 * @param request
	 * @param checkRight
	 * @param event
	 * @param wfDefinition
	 * @return
	 */
	boolean startApprovalProcess(DTO request, boolean checkRight,
			EntityEvent<DTO> event, String wfDefinition);

	/**
	 * Set request state to CANCELED and stop workflow process (connected to this
	 * request)
	 * 
	 * @param dto
	 */
	void cancel(DTO dto);

}