package eu.bcvsolutions.idm.core.scheduler.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import eu.bcvsolutions.idm.core.api.domain.DefaultFieldLengths;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.scheduler.api.service.SchedulableTaskExecutor;

/**
 * Scheduled task with unique id 
 * * could be scheduled 
 * * could run manually through long running task service
 * 
 * Lookout: task definition and instance is mixed together!
 * 
 * @author Radek Tomiška
 */
public class Task implements BaseDto {

	private static final long serialVersionUID = 1L;
	//
	@JsonDeserialize(as = String.class)
	private String id; // quartz job name
	private String module;
	@NotEmpty
	private String instanceId;
	private Class<? extends SchedulableTaskExecutor<?>> taskType; // task executor class
	@Size(max = DefaultFieldLengths.DESCRIPTION)
	private String description;
	@JsonProperty(access=Access.READ_ONLY)
	private List<AbstractTaskTrigger> triggers;
	private Map<String, String> parameters;
	private IdmFormDefinitionDto formDefinition;
	private boolean supportsDryRun;

	public Task() {
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(Serializable id) {
		this.id = id == null ? null : id.toString();
	}
	
	public String getModule() {
		return module;
	}
	
	public void setModule(String module) {
		this.module = module;
	}
	
	public Class<? extends SchedulableTaskExecutor<?>> getTaskType() {
		return taskType;
	}
	
	public void setTaskType(Class<? extends SchedulableTaskExecutor<?>> taskType) {
		this.taskType = taskType;
	}

	public List<AbstractTaskTrigger> getTriggers() {
		if (triggers == null) {
			triggers = new ArrayList<>();
		}
		return triggers;
	}

	public void setTriggers(List<AbstractTaskTrigger> triggers) {
		this.triggers = triggers;
	}

	public Map<String, String> getParameters() {
		if (parameters == null) {
			parameters = new LinkedHashMap<String, String>();
		}
		return parameters;
	}
	
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * Adds task parameter. Returs previous values associated
	 * with the provided key.
     *
	 * @param key
	 * @param value
	 * @return previous value
	 */
	public String addParameter(String key, String value) {
		return getParameters().put(key, value);
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getInstanceId() {
		return instanceId;
	}
	
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	
	/**
	 * Given task supports dry run mode.
	 * 
	 * @param supportsDryRun
	 * @since 7.8.3
	 */
	public void setSupportsDryRun(boolean supportsDryRun) {
		this.supportsDryRun = supportsDryRun;
	}
	
	/**
	 * Returns true, if given task supports dry run mode.
	 * 
	 * @return
	 * @since 7.8.3
	 */
	public boolean isSupportsDryRun() {
		return supportsDryRun;
	}
	
	/**
	 * Eav form definition for configuration
	 * 
	 * @since 9.2.0
	 * @param formDefinition
	 */
	public void setFormDefinition(IdmFormDefinitionDto formDefinition) {
		this.formDefinition = formDefinition;
	}
	
	/**
	 * Eav form definition for configuration
	 * 
	 * @since 9.2.0
	 * @return
	 */
	public IdmFormDefinitionDto getFormDefinition() {
		return formDefinition;
	}
}
