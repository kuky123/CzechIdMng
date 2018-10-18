package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.bcvsolutions.idm.core.api.domain.RequestState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRequestFilter;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * @author Patrik Stloukal
 */
@Transactional
public class IdmRequestFilterTest extends AbstractIntegrationTest{
	
	@Autowired
	private DefaultIdmRequestService requestService;

	@Test
	public void dateTest() {
		UUID ownerId = UUID.randomUUID();
		String ownerType = IdmIdentityDto.class.toString();
		createRequest(ownerType, ownerId);
		
		IdmRequestFilter filter = new IdmRequestFilter();
		filter.setCreatedAfter(new DateTime().minusSeconds(10));
		filter.setCreatedBefore(new DateTime());
		List<IdmRequestDto> content = requestService.find(filter, null).getContent();
		Assert.assertEquals(1, content.size());
		
		filter.setCreatedAfter(new DateTime().minusSeconds(10));
		filter.setCreatedBefore(new DateTime().minusSeconds(9));
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(0, content.size());
		
		filter.setCreatedAfter(new DateTime().plusSeconds(10));
		filter.setCreatedBefore(new DateTime().plusSeconds(11));
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(0, content.size());
	}
	
	@Test
	public void ownerTypeTest() {
		UUID ownerId = UUID.randomUUID();
		String ownerType = IdmRoleDto.class.toString();
		createRequest(ownerType, ownerId);
		createRequest(ownerType, ownerId);
		createRequest(ownerType, ownerId);
		createRequest(IdmIdentityDto.class.toString(), ownerId);
		
		IdmRequestFilter filter = new IdmRequestFilter();
		filter.setOwnerType(ownerType);
		filter.setOwnerId(ownerId);
		List<IdmRequestDto> content = requestService.find(filter, null).getContent();
		Assert.assertEquals(3, content.size());
	}
	
	@Test
	public void ownerTest() {
		String ownerType = IdmIdentityDto.class.toString();
		IdmRequestDto request1 = createRequest(ownerType, UUID.randomUUID());
		IdmRequestDto request2 = createRequest(ownerType, UUID.randomUUID());
		IdmRequestDto request3 = createRequest(ownerType, UUID.randomUUID());
		
		IdmRequestFilter filter = new IdmRequestFilter();
		filter.setOwnerType(ownerType);
		filter.setOwnerId(request1.getOwnerId());
		List<IdmRequestDto> content = requestService.find(filter, null).getContent();
		Assert.assertEquals(1, content.size());
		Assert.assertEquals(request1.getName(), content.get(0).getName());
		
		filter.setOwnerId(request2.getOwnerId());
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(1, content.size());
		Assert.assertEquals(request2.getName(), content.get(0).getName());
		
		filter.setOwnerId(request3.getOwnerId());
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(1, content.size());
		Assert.assertEquals(request3.getName(), content.get(0).getName());
	}
	
	@Test
	public void stateTest() {
		UUID ownerId = UUID.randomUUID();
		String ownerType = IdmIdentityDto.class.toString();
		List<RequestState> states = new ArrayList<>();
		RequestState state = RequestState.APPROVED;
		states.add(state);
		
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		
		IdmRequestFilter filter = new IdmRequestFilter();
		filter.setOwnerType(ownerType);
		filter.setOwnerId(ownerId);
		filter.setStates(states);
		List<IdmRequestDto> content = requestService.find(filter, null).getContent();
		Assert.assertEquals(3, content.size());
		
		state = RequestState.CANCELED;
		states.add(state);
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		
		filter.setStates(states);
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(6, content.size());
		
		state = RequestState.EXCEPTION;
		states.add(state);
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		
		filter.setStates(states);
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(9, content.size());
		
		state = RequestState.IN_PROGRESS;
		states.add(state);
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		changeState(state, createRequest(ownerType, ownerId));
		
		filter.setStates(states);
		content = requestService.find(filter, null).getContent();
		Assert.assertEquals(12, content.size());
	}
	
	 /**
	  * Method creates IdmRequestDto
	  * @param ownerType
	  * @param ownerId
	  * @return
	  */
	private IdmRequestDto createRequest(String ownerType, UUID ownerId) {
		IdmRequestDto request = new IdmRequestDto();
		request.setName("request" + System.currentTimeMillis());
		request.setOwnerType(ownerType);
		request.setRequestType("Request");
		request.setOwnerId(ownerId);
		return requestService.save(request);
	}
	
	private IdmRequestDto changeState(RequestState state, IdmRequestDto request) {
		request.setState(state);
		return requestService.save(request);
	}
}
