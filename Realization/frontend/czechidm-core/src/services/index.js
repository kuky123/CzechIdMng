/**
 * Services register
 *
 * import { RestApiService } from './services' can be used in redux managers (managers layer)
 *
 * @author Radek Tomiška
 */
import RestApiService from './RestApiService';
import AbstractService from './AbstractService';
import FormableEntityService from './FormableEntityService';
import AuthenticateService from './AuthenticateService';
import IdentityService from './IdentityService';
import WorkflowProcessDefinitionService from './WorkflowProcessDefinitionService';
import TreeNodeService from './TreeNodeService';
import TreeTypeService from './TreeTypeService';
import LocalizationService from './LocalizationService';
import RoleService from './RoleService';
import WorkflowTaskInstanceService from './WorkflowTaskInstanceService';
import IdentityRoleService from './IdentityRoleService';
import IdentityContractService from './IdentityContractService';
import WorkflowProcessInstanceService from './WorkflowProcessInstanceService';
import WorkflowHistoricProcessInstanceService from './WorkflowHistoricProcessInstanceService';
import WorkflowHistoricTaskInstanceService from './WorkflowHistoricTaskInstanceService';
import NotificationService from './NotificationService';
import ConfigurationService from './ConfigurationService';
import EmailService from './EmailService';
import BackendModuleService from './BackendModuleService';
import RoleCatalogueService from './RoleCatalogueService';
import RoleCatalogueRoleService from './RoleCatalogueRoleService';
import RoleCompositionService from './RoleCompositionService';
import AuditService from './AuditService';
import ScriptService from './ScriptService';
import NotificationConfigurationService from './NotificationConfigurationService';
import WebsocketService from './WebsocketService';
import PasswordPolicyService from './PasswordPolicyService';
import EntityEventProcessorService from './EntityEventProcessorService';
import LongRunningTaskService from './LongRunningTaskService';
import SchedulerService from './SchedulerService';
import NotificationTemplateService from './NotificationTemplateService';
import RoleRequestService from './RoleRequestService';
import ConceptRoleRequestService from './ConceptRoleRequestService';
import RoleTreeNodeService from './RoleTreeNodeService';
import FormDefinitionService from './FormDefinitionService';
import FormAttributeService from './FormAttributeService';
import FormValueService from './FormValueService';
import AuthorizationPolicyService from './AuthorizationPolicyService';
import ScriptAuthorityService from './ScriptAuthorityService';
import ContractGuaranteeService from './ContractGuaranteeService';
import ContractPositionService from './ContractPositionService';
import NotificationRecipientService from './NotificationRecipientService';
import SmsService from './SmsService';
import RecaptchaService from './RecaptchaService';
import LoggingEventService from './LoggingEventService';
import LoggingEventExceptionService from './LoggingEventExceptionService';
import ConfidentialStorageValueService from './ConfidentialStorageValueService';
import AutomaticRoleAttributeService from './AutomaticRoleAttributeService';
import AutomaticRoleAttributeRuleService from './AutomaticRoleAttributeRuleService';
import LongRunningTaskItemService from './LongRunningTaskItemService';
import AutomaticRoleRequestService from './AutomaticRoleRequestService';
import AutomaticRoleAttributeRuleRequestService from './AutomaticRoleAttributeRuleRequestService';
import EntityEventService from './EntityEventService';
import EntityStateService from './EntityStateService';
import ContractSliceService from './ContractSliceService';
import ContractSliceGuaranteeService from './ContractSliceGuaranteeService';
import RoleGuaranteeService from './RoleGuaranteeService';
import RoleGuaranteeRoleService from './RoleGuaranteeRoleService';
import RequestService from './RequestService';
import RequestItemService from './RequestItemService';
import AbstractRequestFormableService from './AbstractRequestFormableService';
import AbstractRequestService from './AbstractRequestService';
import ProfileService from './ProfileService';
import GenerateValueService from './GenerateValueService';
import AttachmentService from './AttachmentService';

const ServiceRoot = {
  RestApiService,
  AbstractService,
  FormableEntityService,
  AuthenticateService,
  IdentityService,
  WorkflowProcessDefinitionService,
  TreeNodeService,
  TreeTypeService,
  LocalizationService,
  RoleService,
  WorkflowTaskInstanceService,
  IdentityRoleService,
  IdentityContractService,
  WorkflowProcessInstanceService,
  WorkflowHistoricProcessInstanceService,
  WorkflowHistoricTaskInstanceService,
  NotificationService,
  ConfigurationService,
  EmailService,
  BackendModuleService,
  RoleCatalogueService,
  RoleCatalogueRoleService,
  RoleCompositionService,
  AuditService,
  ScriptService,
  NotificationConfigurationService,
  WebsocketService,
  PasswordPolicyService,
  EntityEventProcessorService,
  LongRunningTaskService,
  SchedulerService,
  NotificationTemplateService,
  RoleRequestService,
  ConceptRoleRequestService,
  RoleTreeNodeService,
  FormDefinitionService,
  FormAttributeService,
  FormValueService,
  AuthorizationPolicyService,
  ScriptAuthorityService,
  ContractGuaranteeService,
  ContractPositionService,
  NotificationRecipientService,
  SmsService,
  RecaptchaService,
  LoggingEventService,
  LoggingEventExceptionService,
  ConfidentialStorageValueService,
  AutomaticRoleAttributeService,
  AutomaticRoleAttributeRuleService,
  LongRunningTaskItemService,
  AutomaticRoleRequestService,
  AutomaticRoleAttributeRuleRequestService,
  EntityEventService,
  EntityStateService,
  ContractSliceService,
  ContractSliceGuaranteeService,
  RoleGuaranteeService,
  RoleGuaranteeRoleService,
  RequestService,
  RequestItemService,
  AbstractRequestFormableService,
  AbstractRequestService,
  ProfileService,
  GenerateValueService,
  AttachmentService
};

ServiceRoot.version = '0.0.1';
module.exports = ServiceRoot;
