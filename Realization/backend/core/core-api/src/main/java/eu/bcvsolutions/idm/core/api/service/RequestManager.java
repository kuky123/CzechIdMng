package eu.bcvsolutions.idm.core.api.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import eu.bcvsolutions.idm.core.api.domain.Requestable;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemChangesDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestItemDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.service.FormValueService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Manager for automatic role request
 * 
 * @author svandav
 * @since 9.1.0
 */
public interface RequestManager extends RequestService<IdmRequestDto> {

	/**
	 * Create or update given DTO for given request.
	 * 
	 * @param requestId
	 * @param dto
	 * @param permission
	 * @return
	 */
	<R extends Requestable> R post(Serializable requestId, R dto, BasePermission... permission);

	/**
	 * Delete given DTO for given request. Returns that DTO, but marked as DELETED.
	 * 
	 * @param requestId
	 * @param dto
	 * @param permission
	 * @return
	 */
	<R extends Requestable> R delete(Serializable requestId, R dto, BasePermission... permission);

	/**
	 * Returns given DTO for given request. First try to find that DTO as item in
	 * the request. If does not exists returns DTO from standard service.
	 * 
	 * @param requestId
	 * @param dtoId
	 * @param dtoClass
	 * @param permission
	 * @return
	 */
	<R extends Requestable> R get(UUID requestId, UUID dtoId, Class<R> dtoClass, BasePermission... permission);

	/**
	 * Executes filter and on result applies DTOs from this request's items.
	 * 
	 * @param dtoClass
	 * @param requestId
	 * @param filter
	 * @param pageable
	 * @param permission
	 * @return
	 */
	<R extends Requestable> Page<R> find(Class<? extends R> dtoClass, Serializable requestId, BaseFilter filter, Pageable pageable,
			IdmBasePermission... permission);

	/**
	 * Create new request for given DTO.
	 * 
	 * @param dto
	 * @param permission
	 * @return
	 */
	<R extends Requestable> IdmRequestDto createRequest(R dto, BasePermission... permission);

	/**
	 * Save form values for given owner to this request
	 * 
	 * @param requestId
	 * @param owner
	 * @param formDefinition
	 * @param newValues
	 * @param permission
	 * @return
	 */
	<R extends Requestable> IdmFormInstanceDto saveFormInstance(UUID requestId, R owner, IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> newValues, BasePermission... permission);

	/**
	 * Read form values and applies data from request on the result.
	 * 
	 * @param requestId
	 * @param owner
	 * @param formDefinition
	 * @param permission
	 * @return
	 */
	<R extends Requestable> IdmFormInstanceDto getFormInstance(UUID requestId, R owner, IdmFormDefinitionDto formDefinition,
			BasePermission... permission);

	/**
	 * Returns current changes between DTOs in the request and in the DB.
	 * 
	 * @param item
	 * @param permission
	 * @return
	 */
	IdmRequestItemChangesDto getChanges(IdmRequestItemDto item, BasePermission... permission);

	/**
	 * Returns confidential storage key for given request item
	 * 
	 * @param itemId
	 * @return
	 */
	public static String getConfidentialStorageKey(UUID itemId) {
		Assert.notNull(itemId);
		//
		return FormValueService.CONFIDENTIAL_STORAGE_VALUE_PREFIX + ":" + itemId;
	}

	/**
	 * Cancel requests and request items using that deleting DTO
	 * 
	 * @param requestable
	 */
	<R extends Requestable> void onDeleteRequestable(R requestable);

	/**
	 * Delete given requestable DTO. Creates and executes request.
	 * 
	 * @param dto
	 * @param executeImmediately
	 */
	<R extends Requestable> IdmRequestDto deleteRequestable(R dto, boolean executeImmediately);

	/**
	 * Get DTO from the request item. Place for additional conversion (EAV attribute
	 * for example)
	 *
	 * @param item
	 * @param type
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	<R extends Requestable> R convertItemToDto(IdmRequestItemDto item, Class<? extends R> type)
			throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException;

	/**
	 * Find/filter all DTOs with UUID fields when values are equals to values in
	 * filter (predicates)
	 * 
	 * @param requestables
	 * @param dtoClass
	 * @param predicates
	 * @return
	 */
	<R extends Requestable> List<R> filterDtosByPredicates(List<R> requestables, Class<? extends R> dtoClass,
			List<RequestPredicate> predicates);

	/**
	 * Find request's items
	 * 
	 * @param requestId
	 * @param dtoClass
	 * @return
	 */
	List<IdmRequestItemDto> findRequestItems(UUID requestId, Class<? extends Requestable> dtoClass);

	/**
	 * Wrapper class for request's predicates
	 * 
	 * @author svandav
	 *
	 */
	public class RequestPredicate {

		private UUID value;
		private String field;

		public RequestPredicate(UUID value, String field) {
			super();
			this.value = value;
			this.field = field;
		}

		public UUID getValue() {
			return value;
		}

		public void setValue(UUID value) {
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

}
