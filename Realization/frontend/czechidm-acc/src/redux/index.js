import SystemManager from './SystemManager';
import RoleSystemManager from './RoleSystemManager';
import RoleSystemAttributeManager from './RoleSystemAttributeManager';
import SystemEntityManager from './SystemEntityManager';
import AccountManager from './AccountManager';
import IdentityAccountManager from './IdentityAccountManager';
import SchemaObjectClassManager from './SchemaObjectClassManager';
import SchemaAttributeManager from './SchemaAttributeManager';
import SystemAttributeMappingManager from './SystemAttributeMappingManager';
import SystemMappingManager from './SystemMappingManager';
import SynchronizationLogManager from './SynchronizationLogManager';
import SynchronizationConfigManager from './SynchronizationConfigManager';
import ProvisioningOperationManager from './ProvisioningOperationManager';
import ProvisioningArchiveManager from './ProvisioningArchiveManager';
import SyncActionLogManager from './SyncActionLogManager';
import SyncItemLogManager from './SyncItemLogManager';
import RoleAccountManager from './RoleAccountManager';

const ManagerRoot = {
  SystemManager,
  RoleSystemManager,
  SystemEntityManager,
  AccountManager,
  IdentityAccountManager,
  SchemaObjectClassManager,
  SchemaAttributeManager,
  SystemAttributeMappingManager,
  SystemMappingManager,
  RoleSystemAttributeManager,
  SynchronizationLogManager,
  SynchronizationConfigManager,
  ProvisioningOperationManager,
  ProvisioningArchiveManager,
  SyncActionLogManager,
  SyncItemLogManager,
  RoleAccountManager
};

ManagerRoot.version = '0.0.1';
module.exports = ManagerRoot;
