package eu.bcvsolutions.idm.core.api.bulk.action;

import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.config.domain.RequestConfiguration;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.AcceptedException;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.ExceptionUtils;

/**
 * Abstract class for all remove operations
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 * @param <DTO>
 * @param <F>
 */

public abstract class AbstractRemoveBulkAction<DTO extends AbstractDto, F extends BaseFilter>
		extends AbstractBulkAction<DTO, F> {

	@Autowired
	private RequestConfiguration requestConfiguration;
	@Autowired
	private RequestManager requestManager;

	@Override
	protected OperationResult processDto(DTO dto) {
		try {
			if (dto instanceof Requestable
					&& requestConfiguration.isRequestModeEnabled(dto.getClass())) {
				// Request mode is enabled for that DTO
				Requestable requestable = (Requestable) dto;
				IdmRequestDto request = requestManager.deleteRequestable(requestable, false);
				 
				if (RequestState.IN_PROGRESS == request.getState()) {
					throw new AcceptedException(request.getId().toString());
				}
				if (RequestState.EXCEPTION == request.getState()) {
					throw new CoreException(ExceptionUtils.resolveException(request.getResult().getException()));
				}
				return new OperationResult.Builder(request.getResult().getState()).setCause(request.getResult().getException()).build();
			}
			this.getService().delete(dto);
			return new OperationResult.Builder(OperationState.EXECUTED).build();
		} catch (AcceptedException ex) {
			return new OperationResult.Builder(OperationState.RUNNING).setException(ex).build();
		} catch (ResultCodeException ex) {
			return new OperationResult.Builder(OperationState.EXCEPTION).setException(ex).build();
		} catch (Exception ex) {
			Throwable resolvedException = ExceptionUtils.resolveException(ex);
			if (resolvedException instanceof ResultCodeException) {
				return new OperationResult.Builder(OperationState.EXCEPTION) //
						.setException((ResultCodeException) resolvedException) //
						.build(); //
			}
			return new OperationResult.Builder(OperationState.EXCEPTION).setCause(ex).build();
		}
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 10000;
	}

}
