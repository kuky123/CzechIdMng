package eu.bcvsolutions.idm.core.workflow.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.StrongUuidGenerator;
import org.activiti.spring.SpringAsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.ActivitiProperties;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import eu.bcvsolutions.idm.core.notification.api.service.EmailNotificationSender;
import eu.bcvsolutions.idm.core.workflow.domain.CustomActivityBehaviorFactory;
import eu.bcvsolutions.idm.core.workflow.domain.formtype.CustomFormTypes;
import eu.bcvsolutions.idm.core.workflow.listener.CandidateToUuidEventListener;
import eu.bcvsolutions.idm.core.workflow.listener.StartProcessEventListener;
import eu.bcvsolutions.idm.core.workflow.listener.TaskSendNotificationEventListener;

/**
 * Extended Activiti engine auto configuration
 * - custom auto deployment strategy - prevent to override external resources. Based on resource name - process definition id is not available in this phase
 * - custom behavior (sending email through {@link EmailNotificationSender})
 * - support for custom form types (decisions etc.)
 * - support for custom activiti event listeners
 * 
 * @author Radek Tomiška
 *
 */
@Configuration
@AutoConfigureBefore(JpaProcessEngineAutoConfiguration.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class WorkflowConfig {
	
	/*
	 * Attribute that is in WF. Enable or disable send notification. When this
	 * attribute is empty is send notification 
	 */
	public static final String SEND_NOTIFICATION_FROM_WF_ATTRIBUTE = "sendNotification";
	
	/*
	 * Configuration attribute that allow global enable
	 * or disable bulk sending notification from WF
	 * This application property has higher priority than attribute set into task. 
	 */
	public static final String SEND_NOTIFICATION_CONFIGURATION_PROPERTY = "idm.sec.core.wf.notification.send";

	@Configuration
	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	@EnableConfigurationProperties(ActivitiProperties.class)
	public static class JpaConfiguration extends AbstractProcessEngineAutoConfiguration {
		
		private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JpaConfiguration.class);

		@Bean
		@ConditionalOnMissingBean
		public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}

		@Bean
		@ConditionalOnMissingBean
		public SpringProcessEngineConfiguration springProcessEngineConfiguration(
				DataSource dataSource,
				EntityManagerFactory entityManagerFactory, 
				PlatformTransactionManager transactionManager,
				SpringAsyncExecutor springAsyncExecutor) throws IOException {

			SpringProcessEngineConfiguration config = this.baseSpringProcessEngineConfiguration(dataSource,
					transactionManager, springAsyncExecutor);
			config.setJpaEntityManagerFactory(entityManagerFactory);
			config.setTransactionManager(transactionManager);
			config.setJpaHandleTransaction(false);
			config.setJpaCloseEntityManager(false);
			//
			// custom setting
			config.setDeploymentMode(SingleResourceAutoDeploymentStrategy.DEPLOYMENT_MODE);
			// Set UUID ID generator
			StrongUuidGenerator uuidGenerator = new StrongUuidGenerator();
			config.setIdGenerator(uuidGenerator);
			// task form types
			config.setFormTypes(new CustomFormTypes());
			//Add ours Activiti event listeners to engine configuration
			addActivitiEventListeners(config);
			//
			return config;
		}
		
		/**
		 * Initialize custom behavior
		 * 
		 * @return
		 */
		@Bean
		public ActivityBehaviorFactory activityBehaviorFactory(SpringProcessEngineConfiguration processEngineConfiguration) {
			CustomActivityBehaviorFactory customActivityBehaviorFactory = new CustomActivityBehaviorFactory();
			// Evaluate expression in workflow
			customActivityBehaviorFactory.setExpressionManager(processEngineConfiguration.getExpressionManager());
			// For catch email
			processEngineConfiguration
					.getBpmnParser()
					.setActivityBehaviorFactory(customActivityBehaviorFactory);
			return customActivityBehaviorFactory;
		}
		
		@Bean
		public CandidateToUuidEventListener candidateToUuidEventListener() {
			return new CandidateToUuidEventListener();
		}
		
		@Bean 
		public TaskSendNotificationEventListener taskSendNotificationEventListener() {
			return new TaskSendNotificationEventListener();
		}
		
		@Bean
		public StartProcessEventListener startSubprocessEventListener() {
			return new StartProcessEventListener();
		}
		
		@Override
		public List<Resource> discoverProcessDefinitionResources(ResourcePatternResolver applicationContext,
				String prefixes, List<String> suffixes, boolean checkPDs) throws IOException {
			if (checkPDs && StringUtils.isNotBlank(prefixes)) {	
				Map<String, Resource> resources = new HashMap<>();
	    		for(String prefix : prefixes.split(",")) {
	    			if(StringUtils.isBlank(prefix)) {
	    				// nothing to do
	    				continue;
	    			}
    				for(Resource resource : super.discoverProcessDefinitionResources(applicationContext, prefix.trim(), suffixes, checkPDs)) {
    					String resourceName = determineResourceName(resource);
    					if (resources.containsKey(resourceName)) {
    						// last one wins - just log
    						LOG.info("Resource [{}] was found in more locations, using resource from [{}].", resourceName, prefix);
    					}
    					resources.put(resourceName, resource);
    				}
	    			
	    		}
	    		return new ArrayList<>(resources.values());
		    }
		    return new ArrayList<Resource>();
		}
		
		/**
	     * Determines the name to be used for the provided resource.
	     *
	     * @param resource the resource to get the name for
	     * @return the name of the resource
	     */
	    private String determineResourceName(Resource resource) {
	        String resourceName = null;

	        if (resource instanceof ContextResource) {
	            resourceName = ((ContextResource) resource).getPathWithinContext();
	        } else if (resource instanceof ByteArrayResource) {
	            resourceName = resource.getDescription();
	        } else {	    
	            resourceName = resource.getFilename();
	        }
	        return resourceName;
	    }
	    
	    /**
		 * Add ours custom Activiti event listeners to engine configuration
		 * 
		 * @param processEngineConfiguration
		 */
		private void addActivitiEventListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {
			CandidateToUuidEventListener candidateToUuidEventListener = candidateToUuidEventListener();
			//
			List<ActivitiEventListener> candidateUiidList = Stream
					.of(candidateToUuidEventListener)
					.collect(Collectors.toList());
			List<ActivitiEventListener> taskSendNotificationList = Stream
					.of(taskSendNotificationEventListener(), candidateToUuidEventListener)
					.collect(Collectors.toList());
			//
			Map<String, List<ActivitiEventListener>> typedListeners = new HashMap<>();
			typedListeners.put(
					ActivitiEventType.PROCESS_STARTED.name(),
					Stream
						.of(startSubprocessEventListener())
						.collect(Collectors.toList()));
			//
			typedListeners.put(ActivitiEventType.TASK_ASSIGNED.name(), taskSendNotificationList);
			typedListeners.put(ActivitiEventType.TASK_CREATED.name(), taskSendNotificationList);
			typedListeners.put(ActivitiEventType.TASK_COMPLETED.name(), taskSendNotificationList);
			//
			typedListeners.put(ActivitiEventType.ENTITY_CREATED.name(), candidateUiidList);
			typedListeners.put(ActivitiEventType.ENTITY_INITIALIZED.name(), candidateUiidList);
			//
			processEngineConfiguration.setTypedEventListeners(typedListeners);
		}
	}
}
