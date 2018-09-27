package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.config.domain.EventConfiguration;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.domain.PriorityType;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.EntityEventProcessorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityEventDto;
import eu.bcvsolutions.idm.core.api.dto.IdmEntityStateDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.filter.EntityEventProcessorFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmEntityEventFilter;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.DefaultEventContext;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EmptyEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.EventType;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.service.ConfigurationService;
import eu.bcvsolutions.idm.core.api.service.EntityStateManager;
import eu.bcvsolutions.idm.core.api.service.IdmEntityEventService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.event.ConditionalContent;
import eu.bcvsolutions.idm.core.event.TestContent;
import eu.bcvsolutions.idm.core.event.TestContentTwo;
import eu.bcvsolutions.idm.core.event.TestEntityEventProcessorConfiguration;
import eu.bcvsolutions.idm.core.event.domain.MockDto;
import eu.bcvsolutions.idm.core.event.domain.MockOwner;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityEvent.IdentityEventType;
import eu.bcvsolutions.idm.core.model.event.processor.ObserveDtoProcessor;
import eu.bcvsolutions.idm.core.model.event.processor.event.EntityEventDeleteExecutedProcessor;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Entity events integration tests
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultEntityEventManagerIntergationTest extends AbstractIntegrationTest {

	@Autowired private ApplicationContext context;
	@Autowired private ConfigurationService configurationService;
	@Autowired private IdmEntityEventService entityEventService;
	@Autowired private EntityStateManager entityStateManager;
	@Autowired
	@Qualifier("testTwoEntityEventProcessorOne")
	private EntityEventProcessor<?> testTwoEntityEventProcessorOne;
	//
	private DefaultEntityEventManager manager; 
	
	@Before
	public void init() {
		manager = context.getAutowireCapableBeanFactory().createBean(DefaultEntityEventManager.class);
	}
	
	@Test
	@Transactional
	public void testInit() {
		MockOwner mockOwner = new MockOwner();
		//
		IdmEntityEventDto entityEventDto = manager.prepareEvent(mockOwner, null);
		entityEventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		entityEventDto.setEventType(CoreEventType.CREATE.name());
		entityEventDto.setPriority(PriorityType.HIGH);
		entityEventDto = manager.saveEvent(entityEventDto);
		//
		EventResult<?> result = new DefaultEventResult.Builder<>(
				manager.toEvent(entityEventDto), new EmptyEntityEventProcessor<>())
				.setResults(Lists.newArrayList(
						new OperationResult
							.Builder(OperationState.RUNNING)
							.build()
						))
				.build();
		List<IdmEntityStateDto> states = manager.saveStates(manager.toEvent(entityEventDto), null, result);
		//
		manager.init();
		//
		entityEventDto = entityEventService.get(entityEventDto);
		states = entityStateManager.findStates(mockOwner, null).getContent();
		//
		Assert.assertEquals(1, states.size());
		Assert.assertEquals(OperationState.CANCELED, entityEventDto.getResult().getState());
		Assert.assertEquals(OperationState.CANCELED, states.get(0).getResult().getState());
	}
	
	@Test
	public void testFindProcessors() {
		EntityEventProcessorFilter filter = null;
		List<EntityEventProcessorDto> processors = manager.find(filter);
		int size = processors.size();
		//
		assertTrue(size > 11);
		//
		filter = new EntityEventProcessorFilter();
		filter.setContentClass(Serializable.class);
	    processors = manager.find(filter);
		//
	    assertEquals(size, processors.size());
	    //
	    filter.setContentClass(TestContent.class);
	    processors = manager.find(filter);
	    //
	    assertEquals(11, processors.size());
	}
	
	@Test
	public void testEventGreenLine() {
		EntityEvent<TestContent> event = new CoreEvent<>(CoreEventType.CREATE, new TestContent());
		EventContext<TestContent> context = manager.process(event);
		//
		assertEquals(4, context.getResults().size());
		assertEquals(4, context.getProcessedOrder().intValue());
		assertEquals("4", context.getLastResult().getEvent().getContent().getText());
	}
	
	@Test
	public void testCloseEvent() {
		EntityEvent<TestContent> event = new CoreEvent<>(CoreEventType.CREATE, new TestContent());
		event.getContent().setClose(2);
		EventContext<TestContent> context = manager.process(event);
		//
		assertEquals(2, context.getResults().size());
		assertEquals(2, context.getProcessedOrder().intValue());
		assertEquals("2", context.getLastResult().getEvent().getContent().getText());
	}
	
	@Test
	public void testSuspendEvent() {
		EntityEvent<TestContent> event = new CoreEvent<>(CoreEventType.CREATE, new TestContent());
		event.getContent().setSuspend(1);
		EventContext<TestContent> context = manager.process(event);
		//
		assertEquals(1, context.getResults().size());
		assertEquals(1, context.getProcessedOrder().intValue());
		assertEquals("1", context.getLastResult().getEvent().getContent().getText());
		//
		event.getContent().setSuspend(3);
		context = manager.process(event);
		//
		assertEquals(3, context.getResults().size());
		assertEquals(3, context.getProcessedOrder().intValue());
		assertEquals("3", context.getLastResult().getEvent().getContent().getText());
		//
		event.getContent().setSuspend(null);
		context = manager.process(event);
		//
		assertEquals(4, context.getResults().size());
		assertEquals(4, context.getProcessedOrder().intValue());
		assertEquals("4", context.getLastResult().getEvent().getContent().getText());
	}
	
	@Test
	public void testStartEventInMiddle() {
		DefaultEventContext<TestContent> initContext = new DefaultEventContext<>();
		initContext.setProcessedOrder(2);
		EntityEvent<TestContent> event = new CoreEvent<>(CoreEventType.CREATE, new TestContent(), null, initContext);
		EventContext<TestContent> context = manager.process(event);
		//
		assertEquals(2, context.getResults().size());
		assertEquals(4, context.getProcessedOrder().intValue());
		assertEquals("4", context.getLastResult().getEvent().getContent().getText());
	}
	
	@Test 
	public void testOriginalSource() {
		IdmIdentityDto createdIdentity = getHelper().createIdentity((GuardedString) null);
		// process change
		IdmIdentityDto updateIdentity = getHelper().getService(IdmIdentityService.class).get(createdIdentity.getId());
		updateIdentity.setFirstName("newFirst");
		updateIdentity.setLastName("newLast");
		EntityEvent<IdmIdentityDto> event = new IdentityEvent(IdentityEventType.UPDATE, updateIdentity);
		EventContext<IdmIdentityDto> context = manager.process(event);
		IdmIdentityDto originalIdentity = context.getLastResult().getEvent().getOriginalSource();
		IdmIdentityDto savedIdentity = context.getLastResult().getEvent().getContent();
		// check
		assertEquals(createdIdentity.getUsername(), originalIdentity.getUsername());
		assertEquals(createdIdentity.getFirstName(), originalIdentity.getFirstName());
		assertEquals(createdIdentity.getLastName(), originalIdentity.getLastName());
		assertEquals(updateIdentity.getUsername(), savedIdentity.getUsername());
		assertEquals(updateIdentity.getFirstName(), savedIdentity.getFirstName());
		assertEquals(updateIdentity.getLastName(), savedIdentity.getLastName());
	}
	
	@Test 
	public void testProcessorSameOrder() {
		EntityEvent<TestContentTwo> event = new CoreEvent<>(CoreEventType.EAV_SAVE, new TestContentTwo());
		EventContext<TestContentTwo> context = manager.process(event);
		//
		assertEquals(2, context.getResults().size());
		assertEquals(2, context.getProcessedOrder().intValue());
	}

	@Test
	public void testConfigPropertyEventTypeOverwrite() {
		String eventTypeName = System.nanoTime() + "_test_type";
		EventType type = (EventType) () -> eventTypeName;
		EntityEvent<TestContentTwo> event = new CoreEvent<>(type, new TestContentTwo());
		EventContext<TestContentTwo> context = manager.process(event);
		assertEquals(0, context.getResults().size());

		String configPropName = testTwoEntityEventProcessorOne.getConfigurationPropertyName(EntityEventProcessor.PROPERTY_EVENT_TYPES);
		configurationService.setValue(configPropName, eventTypeName);

		EntityEvent<TestContentTwo> event2 = new CoreEvent<>(type, new TestContentTwo());
		EventContext<TestContentTwo> context2 = manager.process(event2);
		assertEquals(2, context2.getResults().size());
	}
	
	@Test
	public void testConditionalProcessor() {
		EntityEvent<ConditionalContent> event = new CoreEvent<>(CoreEventType.CREATE, new ConditionalContent(false));
		EventContext<ConditionalContent> context = manager.process(event);
		//
		assertEquals(0, context.getResults().size());
		//
		event = new CoreEvent<>(CoreEventType.CREATE, new ConditionalContent(true));
		context = manager.process(event);
		//
		assertEquals(1, context.getResults().size());
	}
	
	@Test
	public void testSameOrderBeansOrder() {
		EntityEvent<TestContent> event = new CoreEvent<>(TestEntityEventProcessorConfiguration.EVENT_TYPE_ORDER, new TestContent());
		EventContext<TestContent> context = manager.process(event);
		//
		// Look out: processors are executed in random order in configured order is same
		assertEquals(7, context.getResults().size());
	}
	
	@Test
	public void testMultiThreadEventProcessing() {
		List<IdmEntityEventDto> events = new ArrayList<>();
		try {
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, false);
			getHelper().disable(EntityEventDeleteExecutedProcessor.class);
			int count = 250; // 15s 
			//
			// create events
			String eventType = getHelper().createName();
			for (int i = 0; i < count; i++) {
				MockOwner mockOwner = new MockOwner();
				IdmEntityEventDto entityEvent = new IdmEntityEventDto();
				entityEvent.setOwnerType(mockOwner.getClass().getCanonicalName());
				entityEvent.setEventType(eventType);
				entityEvent.setOwnerId((UUID) mockOwner.getId());
				entityEvent.setContent(mockOwner);
				entityEvent.setInstanceId(configurationService.getInstanceId());
				entityEvent.setResult(new OperationResultDto(OperationState.CREATED));
				entityEvent.setPriority(PriorityType.NORMAL);
				events.add(entityEventService.save(entityEvent));
			}
			//
			IdmEntityEventFilter filter = new IdmEntityEventFilter();
			filter.setOwnerType(MockOwner.class.getCanonicalName());
			filter.setEventType(eventType);
			filter.setStates(Lists.newArrayList(OperationState.CREATED, OperationState.RUNNING));
			Assert.assertEquals(count, entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements());
			//
			// execute
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, true);
			//
			// wait for executed events
			getHelper().waitForResult(res -> {
				return entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements() != 0;
			}, 1000, Integer.MAX_VALUE);
			//
			// check what happened
			filter.setStates(Lists.newArrayList(OperationState.EXECUTED));
			Assert.assertEquals(count, entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements());			
		} finally {
			events.forEach(e -> entityEventService.delete(e));
			getHelper().setConfigurationValue(EventConfiguration.PROPERTY_EVENT_ASYNCHRONOUS_ENABLED, false);
			getHelper().enable(EntityEventDeleteExecutedProcessor.class);
		}
	}
	
	@Test
	public void testRemoveDuplicateEventsForTheSameOwner() {
		List<IdmEntityEventDto> events = new ArrayList<>();
		MockOwner mockOwner = new MockOwner();
		try {
			manager.disable(EntityEventDeleteExecutedProcessor.PROCESSOR_NAME);
			int count = 10;
			//
			// create events
			for (int i = 0; i < count; i++) {
				IdmEntityEventDto entityEvent = new IdmEntityEventDto();
				entityEvent.setOwnerType(mockOwner.getClass().getCanonicalName());
				entityEvent.setEventType("empty");
				entityEvent.setOwnerId((UUID) mockOwner.getId());
				entityEvent.setContent(mockOwner);
				entityEvent.setInstanceId(configurationService.getInstanceId());
				entityEvent.setResult(new OperationResultDto(OperationState.CREATED));
				entityEvent.setPriority(PriorityType.NORMAL);
				events.add(entityEventService.save(entityEvent));
			}
			//
			IdmEntityEventFilter filter = new IdmEntityEventFilter();
			filter.setOwnerType(manager.getOwnerType(mockOwner));
			filter.setOwnerId(mockOwner.getId());
			filter.setStates(Lists.newArrayList(OperationState.CREATED));
			Assert.assertEquals(count, entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements());
			//
			// execute
			manager.processCreated();
			//
			// check what happened
			filter.setStates(Lists.newArrayList(OperationState.EXECUTED));
			Assert.assertEquals(1, entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements());
		} finally {
			getHelper().enable(EntityEventDeleteExecutedProcessor.class);
			entityEventService.delete(events.get(9)); // the last one
			IdmEntityEventFilter filter = new IdmEntityEventFilter();
			filter.setOwnerType(manager.getOwnerType(mockOwner));
			filter.setOwnerId(mockOwner.getId());
			Assert.assertEquals(0, entityEventService.find(filter, new PageRequest(0, 1)).getTotalElements());
		}
	}
	
	@Test
	@Transactional
	public void testEnableDisableProcessorById() {
		EntityEventProcessorDto processor = manager.get(EntityEventDeleteExecutedProcessor.PROCESSOR_NAME);
		//
		try {
			Assert.assertNotNull(processor);
			Assert.assertFalse(processor.isDisabled());
			//
			manager.disable(processor.getId());
			//
			processor = manager.get(EntityEventDeleteExecutedProcessor.PROCESSOR_NAME);
			Assert.assertTrue(processor.isDisabled());
		} finally {
			manager.enable(processor.getId());
		}
	}
	
	@Test
	public void testNextProcessorIsNotExecutedAfterException() {
		EntityEvent<TestContent> event = new CoreEvent<>(CoreEventType.CREATE, new TestContent());
		event.getContent().setException(3);
		try {
			manager.process(event);
		} catch (CoreException ex) {
			// ok
		}
		Assert.assertEquals("2", event.getContent().getText());
	}
	
	/**
	 * Child event has to be persisted automatically, when parent event is persisted
	 */
	@Test
	@Transactional
	public void testPersistChildEventAutomatically() {
		try {
			manager.disable(EntityEventDeleteExecutedProcessor.PROCESSOR_NAME);
			MockOwner mockOwner = new MockOwner();
			//
			// root
			IdmEntityEventDto eventDto = manager.prepareEvent(mockOwner, null);
			eventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
			eventDto.setEventType(CoreEventType.CREATE.name());
			eventDto.setPriority(PriorityType.HIGH);
			final IdmEntityEventDto rootEventDto = manager.saveEvent(eventDto);
			//
			// child
			CoreEvent<MockOwner> childEvent = new CoreEvent<MockOwner>(CoreEventType.CREATE, mockOwner);
			//
			// process - sync
			manager.process(childEvent, manager.toEvent(rootEventDto));
			//
			// two event should be persisted
			IdmEntityEventFilter filter = new IdmEntityEventFilter();
			filter.setOwnerType(manager.getOwnerType(mockOwner));
			filter.setOwnerId(mockOwner.getId());
			List<IdmEntityEventDto> content = entityEventService.find(filter, null).getContent();
			Assert.assertEquals(3, content.size());
			Assert.assertTrue(content.stream().allMatch(e -> e.getResult().getState() == OperationState.EXECUTED));
			Assert.assertTrue(content.stream().anyMatch(e -> e.getRootId() == null));
			Assert.assertTrue(content.stream().anyMatch(e -> rootEventDto.getId().equals(e.getRootId()) && e.getEventType().equals(CoreEventType.CREATE.name())));
			Assert.assertTrue(content.stream().anyMatch(e -> rootEventDto.getId().equals(e.getRootId()) && e.getEventType().equals(CoreEventType.NOTIFY.name())));
			// check child event
			IdmEntityEventDto childEventDto = content
					.stream()
					.filter(e -> rootEventDto.getId().equals(e.getRootId()) && e.getEventType().equals(CoreEventType.CREATE.name()))
					.findFirst()
					.get();
			
			Assert.assertEquals(rootEventDto.getId(), childEventDto.getParent());
			Assert.assertEquals(rootEventDto.getPriority(), childEventDto.getPriority());
			Assert.assertEquals(rootEventDto.getEventType(), childEventDto.getParentEventType());
		} finally {
			manager.enable(EntityEventDeleteExecutedProcessor.PROCESSOR_NAME);
		}
	}
	
	@Test
	@Transactional
	public void testGetEvent() {
		MockOwner mockOwner = new MockOwner();
		//
		IdmEntityEventDto entityEventDto = manager.prepareEvent(mockOwner, null);
		entityEventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		entityEventDto.setEventType(CoreEventType.CREATE.name());
		entityEventDto.setPriority(PriorityType.HIGH);
		entityEventDto = manager.saveEvent(entityEventDto);
		//
		CoreEvent<MockOwner> event = new CoreEvent<MockOwner>(CoreEventType.CREATE, mockOwner);
		event.setId(entityEventDto.getId());
		IdmEntityEventDto persistedEventDto = manager.getEvent(event);
		//
		Assert.assertNotNull(persistedEventDto);
		Assert.assertEquals(OperationState.RUNNING, persistedEventDto.getResult().getState());
		Assert.assertEquals(PriorityType.HIGH, persistedEventDto.getPriority());
		Assert.assertEquals(mockOwner.getId(), persistedEventDto.getOwnerId());
		Assert.assertEquals(manager.getOwnerType(mockOwner), persistedEventDto.getOwnerType());
		Assert.assertEquals(CoreEventType.CREATE.name(), persistedEventDto.getEventType());
	}
	
	@Test
	public void testGetEventId() {
		MockOwner mockOwner = new MockOwner();
		CoreEvent<MockOwner> event = new CoreEvent<MockOwner>(CoreEventType.CREATE, mockOwner);
		event.setId(mockOwner.getId());
		//
		Assert.assertEquals(mockOwner.getId(), manager.getEventId(event));
	}
	
	@Test
	@Transactional
	public void testSaveEvent() {
		MockOwner mockOwner = new MockOwner();
		//
		IdmEntityEventDto entityEventDto = manager.prepareEvent(mockOwner, null);
		entityEventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		entityEventDto.setEventType(CoreEventType.CREATE.name());
		entityEventDto.setPriority(PriorityType.HIGH);
		entityEventDto = manager.saveEvent(entityEventDto);
		//
		IdmEntityEventDto persistedEventDto = entityEventService.get(entityEventDto.getId());
		Assert.assertNotNull(persistedEventDto);
		Assert.assertEquals(OperationState.RUNNING, persistedEventDto.getResult().getState());
		Assert.assertEquals(PriorityType.HIGH, persistedEventDto.getPriority());
		Assert.assertEquals(mockOwner.getId(), persistedEventDto.getOwnerId());
		Assert.assertEquals(manager.getOwnerType(mockOwner), persistedEventDto.getOwnerType());
		Assert.assertEquals(CoreEventType.CREATE.name(), persistedEventDto.getEventType());
	}
	
	@Test
	@Transactional
	public void testSaveResult() {
		MockOwner mockOwner = new MockOwner();
		//
		IdmEntityEventDto entityEventDto = manager.prepareEvent(mockOwner, null);
		entityEventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		entityEventDto.setEventType(CoreEventType.CREATE.name());
		entityEventDto.setPriority(PriorityType.HIGH);
		entityEventDto = manager.saveEvent(entityEventDto);
		//
		manager.saveResult(entityEventDto.getId(), new OperationResultDto.Builder(OperationState.CANCELED).build());
		//
		IdmEntityEventDto persistedEventDto = entityEventService.get(entityEventDto.getId());
		//
		Assert.assertEquals(OperationState.CANCELED, persistedEventDto.getResult().getState());
	}
	
	@Test
	@Transactional
	public void testSaveStates() {
		MockOwner mockOwner = new MockOwner();
		//
		IdmEntityEventDto entityEventDto = manager.prepareEvent(mockOwner, null);
		entityEventDto.setResult(new OperationResultDto.Builder(OperationState.RUNNING).build());
		entityEventDto.setEventType(CoreEventType.CREATE.name());
		entityEventDto.setPriority(PriorityType.HIGH);
		entityEventDto = manager.saveEvent(entityEventDto);
		//
		List<IdmEntityStateDto> states = manager.saveStates(manager.toEvent(entityEventDto), null, null);
		Assert.assertEquals(1, states.size());
		Assert.assertTrue(states.stream().anyMatch(s -> s.getResult().getState() == OperationState.EXECUTED));
		//
		states = manager.saveStates(
				manager.toEvent(entityEventDto), 
				states, 
				new DefaultEventResult.Builder<>(manager.toEvent(entityEventDto), new EmptyEntityEventProcessor<>()).build());
		//
		Assert.assertEquals(1, states.size());
		Assert.assertTrue(states.stream().anyMatch(s -> s.getResult().getState() == OperationState.EXECUTED));
		//
		EventResult<?> result = new DefaultEventResult.Builder<>(
				manager.toEvent(entityEventDto), new EmptyEntityEventProcessor<>())
				.setResults(Lists.newArrayList(
						new OperationResult
							.Builder(OperationState.EXCEPTION)
							.build(),
						new OperationResult
							.Builder(OperationState.CANCELED)
							.build())
						)
				.build();
		//
		states = manager.saveStates(manager.toEvent(entityEventDto), null, result);
		//
		Assert.assertEquals(2, states.size());
		Assert.assertTrue(states.stream().anyMatch(s -> s.getResult().getState() == OperationState.EXCEPTION));
		Assert.assertTrue(states.stream().anyMatch(s -> s.getResult().getState() == OperationState.CANCELED));
		//
		result = new DefaultEventResult.Builder<>(
				manager.toEvent(entityEventDto), new EmptyEntityEventProcessor<>())
				.setResults(Lists.newArrayList(
						new OperationResult
							.Builder(OperationState.BLOCKED)
							.build())
						)
				.build();
		states = manager.saveStates(manager.toEvent(entityEventDto), states, result);
		//
		Assert.assertEquals(1, states.size());
		Assert.assertTrue(states.stream().anyMatch(s -> s.getResult().getState() == OperationState.BLOCKED));
	}
	
	@Test
	@Transactional
	public void testFindOwner() {
		IdmIdentityDto owner = getHelper().createIdentity((GuardedString) null);
		//
		IdmEntityEventDto eventDto = new IdmEntityEventDto();
		eventDto.setOwnerId(owner.getId());
		eventDto.setOwnerType(manager.getOwnerType(owner));
		AbstractDto findOwner = manager.findOwner(eventDto);
		Assert.assertEquals(owner.getId(), findOwner.getId());
		//
		findOwner = manager.findOwner(manager.getOwnerType(owner), owner.getUsername());
		Assert.assertEquals(owner.getId(), findOwner.getId());
		//
		findOwner = manager.findOwner(manager.getOwnerType(owner), owner.getId());
		Assert.assertEquals(owner.getId(), findOwner.getId());
	}
	
	@Test
	public void testBaseDtoProcessing() {
		MockDto mockDto = new MockDto();
		
		ObserveDtoProcessor.listenContent(mockDto.getId());
		
		CoreEvent<BaseDto> event = new CoreEvent<BaseDto>(CoreEventType.NOTIFY, mockDto);
		
		EventContext<BaseDto> processed = manager.process(event);
		
		Assert.assertNotNull(processed.getLastResult());
		Boolean observed = (Boolean) processed.getLastResult().getEvent().getProperties().get(ObserveDtoProcessor.PROPERTY_OBSERVED);
		//
		Assert.assertNotNull(observed);
		Assert.assertTrue(observed);
	}
}
