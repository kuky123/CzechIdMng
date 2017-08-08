package eu.bcvsolutions.idm.core.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Test entity event processors init
 *  
 * @author Radek Tomiška
 *
 */
@Configuration
public class TestEntityEventProcessorConfiguration {
	
	@Bean
	public EntityEventProcessor<?> testEntityEventProcessorOne() {
		return new TestProcessor(1);
	}
	
	@Bean
	public EntityEventProcessor<?> testEntityEventProcessorTwo() {
		return new TestProcessor(2);
	}
	
	@Bean
	public EntityEventProcessor<?> testEntityEventProcessorThree() {
		return new TestProcessor(3);
	}
	
	@Bean
	public EntityEventProcessor<?> testEntityEventProcessorFour() {
		return new TestProcessor(4);
	}
	
	@Bean
	public EntityEventProcessor<?> testTwoEntityEventProcessorOne() {
		return new TestProcessorTwo(2);
	}
	
	@Bean
	public EntityEventProcessor<?> testTwoEntityEventProcessorTwo() {
		return new TestProcessorTwo(2);
	}
	
	private class TestProcessor extends AbstractEntityEventProcessor<TestContent> {
		
		private final Integer order;
		
		public TestProcessor(Integer order) {
			this.order = order;
		}
		
		@Override
		public EventResult<TestContent> process(EntityEvent<TestContent> event) {
			event.getContent().setText(order.toString());			
			DefaultEventResult.Builder<TestContent> result = new DefaultEventResult.Builder<>(event, this);
			if (order.equals(event.getContent().getClose())) {
				result.setClosed(true);
			}
			if (order.equals(event.getContent().getSuspend())) {
				result.setSuspended(true);
			}
			return result.build();
		}

		@Override
		public int getOrder() {
			return order;
		}
	}
	
	private class TestProcessorTwo extends AbstractEntityEventProcessor<TestContentTwo> {
		
		private Integer order;
		
		public TestProcessorTwo(Integer order) {
			super(CoreEventType.EAV_SAVE);
			this.order = order;
		}
		
		@Override
		public EventResult<TestContentTwo> process(EntityEvent<TestContentTwo> event) {
			event.getContent().setText(order.toString());			
			DefaultEventResult.Builder<TestContentTwo> result = new DefaultEventResult.Builder<>(event, this);
			return result.build();
		}

		@Override
		public int getOrder() {
			return order;
		}
	}
}
