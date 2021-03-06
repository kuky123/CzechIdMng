package eu.bcvsolutions.idm.core.scheduler.api.dto;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.quartz.SimpleTrigger;

/**
 * Simple time task trigger
 */
public class SimpleTaskTrigger extends AbstractTaskTrigger {

	private static final long serialVersionUID = 1L;
	
	@NotNull
	private DateTime fireTime;
	
	public SimpleTaskTrigger() {
	}
	
	/**
	 * Creates a new instance using trigger and state
	 * 
	 * @param trigger trigger
	 * @param state state
	 */
	public SimpleTaskTrigger(String taskId, SimpleTrigger trigger, TaskTriggerState state) {
		super(taskId, trigger, state);
		
		this.fireTime = new DateTime(trigger.getStartTime());
	}
	
	public DateTime getFireTime() {
		return fireTime;
	}
	
	public void setFireTime(DateTime fireTime) {
		this.fireTime = fireTime;
	}
}
