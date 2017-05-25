package eu.bcvsolutions.idm.core.workflow.listener;

import java.util.Map;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiProcessStartedEvent;
import org.activiti.engine.task.IdentityLinkType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;

/**
 * Listener call after process started. Add automatically implementor as starter and applicant as owner to new subprocess.
 * @author svandav
 *
 */

@Component
public class StartSubprocessEventListener implements ActivitiEventListener {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartSubprocessEventListener.class);

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Override
	public void onEvent(ActivitiEvent event) {
		switch (event.getType()) {

		case PROCESS_STARTED:
			log.debug("StartSubprocesEventListener - recieve event [{}]", event.getType());
			ActivitiProcessStartedEvent eventStarted = ((ActivitiProcessStartedEvent)event);
			if (eventStarted.getNestedProcessInstanceId()  == null) {
				// Only superprocess have nested process null;
				return;
			}
			// Manual load bean ... autowired is not possible, because this listeners are create before runtimeService 
			RuntimeService runtimeService = beanFactory.getBean(RuntimeService.class);
			
			// To subprocess set process instance ID as variable (we need use id in subprocess)
			runtimeService.setVariable(event.getProcessInstanceId(), WorkflowProcessInstanceService.PROCESS_INSTANCE_ID, event.getProcessInstanceId());
			
			@SuppressWarnings("unchecked") Map<String, Object> variables = eventStarted.getVariables();
			variables.forEach((k, v) -> {
				if (WorkflowProcessInstanceService.APPLICANT_IDENTIFIER.equals(k)) {
					String value = v == null ? null : v.toString();
					// Set applicant as owner of process
					runtimeService.addUserIdentityLink(event.getProcessInstanceId(), value, IdentityLinkType.OWNER);
					log.debug("StartSubprocesEventListener - set process owner [{}]", value);
				} else if (WorkflowProcessInstanceService.IMPLEMENTER_IDENTIFIER.equals(k)) {
					String value = v == null ? null : v.toString();
					// Set current logged user (implementer) as starter of
					// process
					runtimeService.addUserIdentityLink(event.getProcessInstanceId(), value, IdentityLinkType.STARTER);
					log.debug("StartSubprocesEventListener - set process starter [{}]", value);
				}
			});

			break;

		default:
			log.debug("StartSubprocesEventListener - receive not required event [{}]", event.getType());
		}
	}

	@Override
	public boolean isFailOnException() {
		// We can throw exception
		return true;
	}
}