/**
 * Data managers register
 *
 * import { IdentityManager } from './redux/data' can be used in react components (ui layer)
 *
 * @author Radek Tomiška
 */
import EntityManager from './EntityManager';
import FormableEntityManager from './FormableEntityManager';
import DataManager from './DataManager';
import IdentityManager from './IdentityManager';
import TreeNodeManager from './TreeNodeManager';
import TreeTypeManager from './TreeTypeManager';
import RoleManager from './RoleManager';
import WorkflowTaskInstanceManager from './WorkflowTaskInstanceManager';
import IdentityRoleManager from './IdentityRoleManager';
import IdentityContractManager from './IdentityContractManager';
import WorkflowProcessInstanceManager from './WorkflowProcessInstanceManager';
import WorkflowHistoricProcessInstanceManager from './WorkflowHistoricProcessInstanceManager';
import WorkflowHistoricTaskInstanceManager from './WorkflowHistoricTaskInstanceManager';
import WorkflowProcessDefinitionManager from './WorkflowProcessDefinitionManager';
import NotificationManager from './NotificationManager';
import ConfigurationManager from './ConfigurationManager';
import EmailManager from './EmailManager';
import BackendModuleManager from './BackendModuleManager';
import RoleCatalogueManager from './RoleCatalogueManager';
import RoleCatalogueRoleManager from './RoleCatalogueRoleManager';
import RoleCompositionManager from './RoleCompositionManager';
import AuditManager from './AuditManager';
import ScriptManager from './ScriptManager';
import NotificationConfigurationManager from './NotificationConfigurationManager';
import WebsocketManager from './WebsocketManager';
import PasswordPolicyManager from './PasswordPolicyManager';
import EntityEventProcessorManager from './EntityEventProcessorManager';
import LongRunningTaskManager from './LongRunningTaskManager';
import SchedulerManager from './SchedulerManager';
import NotificationTemplateManager from './NotificationTemplateManager';
import RoleRequestManager from './RoleRequestManager';
import ConceptRoleRequestManager from './ConceptRoleRequestManager';
import RoleTreeNodeManager from './RoleTreeNodeManager';
import FormDefinitionManager from './FormDefinitionManager';
import FormAttributeManager from './FormAttributeManager';
import FormValueManager from './FormValueManager';
import AuthorizationPolicyManager from './AuthorizationPolicyManager';
import ScriptAuthorityManager from './ScriptAuthorityManager';
import ContractGuaranteeManager from './ContractGuaranteeManager';
import ContractPositionManager from './ContractPositionManager';
import ContractSliceGuaranteeManager from './ContractSliceGuaranteeManager';
import NotificationRecipientManager from './NotificationRecipientManager';
import SmsManager from './SmsManager';
import LoggingEventManager from './LoggingEventManager';
import LoggingEventExceptionManager from './LoggingEventExceptionManager';
import ConfidentialStorageValueManager from './ConfidentialStorageValueManager';
import AutomaticRoleAttributeManager from './AutomaticRoleAttributeManager';
import AutomaticRoleAttributeRuleManager from './AutomaticRoleAttributeRuleManager';
import LongRunningTaskItemManager from './LongRunningTaskItemManager';
import AutomaticRoleRequestManager from './AutomaticRoleRequestManager';
import AutomaticRoleAttributeRuleRequestManager from './AutomaticRoleAttributeRuleRequestManager';
import EntityEventManager from './EntityEventManager';
import EntityStateManager from './EntityStateManager';
import ContractSliceManager from './ContractSliceManager';
import RoleGuaranteeManager from './RoleGuaranteeManager';
import RoleGuaranteeRoleManager from './RoleGuaranteeRoleManager';
import RequestManager from './RequestManager';
import RequestItemManager from './RequestItemManager';
import AbstractRequestFormableManager from './AbstractRequestFormableManager';
import AbstractRequestManager from './AbstractRequestManager';
import ProfileManager from './ProfileManager';
import GenerateValueManager from './GenerateValueManager';

const ManagerRoot = {
  EntityManager,
  FormableEntityManager,
  DataManager,
  IdentityManager,
  TreeNodeManager,
  TreeTypeManager,
  RoleManager,
  WorkflowTaskInstanceManager,
  IdentityRoleManager,
  IdentityContractManager,
  WorkflowProcessInstanceManager,
  WorkflowHistoricProcessInstanceManager,
  WorkflowHistoricTaskInstanceManager,
  WorkflowProcessDefinitionManager,
  NotificationManager,
  ConfigurationManager,
  EmailManager,
  BackendModuleManager,
  RoleCatalogueManager,
  RoleCatalogueRoleManager,
  RoleCompositionManager,
  AuditManager,
  ScriptManager,
  NotificationConfigurationManager,
  WebsocketManager,
  PasswordPolicyManager,
  EntityEventProcessorManager,
  LongRunningTaskManager,
  SchedulerManager,
  NotificationTemplateManager,
  RoleRequestManager,
  ConceptRoleRequestManager,
  RoleTreeNodeManager,
  FormDefinitionManager,
  FormAttributeManager,
  FormValueManager,
  AuthorizationPolicyManager,
  ScriptAuthorityManager,
  ContractGuaranteeManager,
  ContractPositionManager,
  NotificationRecipientManager,
  SmsManager,
  LoggingEventManager,
  LoggingEventExceptionManager,
  ConfidentialStorageValueManager,
  AutomaticRoleAttributeManager,
  AutomaticRoleAttributeRuleManager,
  LongRunningTaskItemManager,
  AutomaticRoleRequestManager,
  AutomaticRoleAttributeRuleRequestManager,
  EntityEventManager,
  EntityStateManager,
  ContractSliceManager,
  ContractSliceGuaranteeManager,
  RoleGuaranteeManager,
  RoleGuaranteeRoleManager,
  RequestManager,
  RequestItemManager,
  AbstractRequestFormableManager,
  AbstractRequestManager,
  ProfileManager,
  GenerateValueManager
};

ManagerRoot.version = '0.0.1';
module.exports = ManagerRoot;
