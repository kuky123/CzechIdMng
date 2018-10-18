module.exports = {
  'id': 'core',
  'name': 'Core',
  'description': 'Components for Core module',
  'components': [
    {
      'id': 'dynamicRoleTaskDetail',
      'component': require('./src/content/task/identityRole/DynamicTaskRoleDetail')
    },
    {
      'id': 'dynamicAutomaticRoleTaskDetail',
      'component': require('./src/content/task/identityRole/DynamicTaskAutomaticRoleDetail')
    },
    {
      'id': 'dynamicRequestTaskDetail',
      'component': require('./src/content/task/request/DynamicRequestTaskDetail')
    },
    {
      'id': 'role-tree-node-task-detail',
      'component': require('./src/content/task/roleTreeNode/AutomaticRoleTaskDetail')
    },
    {
      'id': 'assigned-task-dashboard',
      'type': 'dashboard',
      'span': '8',
      'order': '3',
      'component': require('./src/content/dashboards/AssignedTaskDashboard')
    },
    {
      'id': 'profile-dashboard',
      'type': 'dashboard',
      'span': '4',
      'order': '1',
      'component': require('./src/content/dashboards/ProfileDashboard')
    },
    {
      'id': 'long-running-task-dashboard',
      'type': 'dashboard',
      'span': '12',
      'order': '50',
      'component': require('./src/content/dashboards/LongRunningTaskDashboard')
    },
    {
      'id': 'password-change-content',
      'priority': 0,
      'component': require('./src/content/identity/PasswordChangeContent')
    },
    {
      'id': 'identity-info',
      'type': 'entity-info',
      'entityType': ['identity', 'IdmIdentity', 'IdmIdentityDto'],
      'component': require('./src/components/advanced/IdentityInfo/IdentityInfo').default,
      'manager': require('./src/redux').IdentityManager
    },
    {
      'id': 'role-info',
      'type': 'entity-info',
      'entityType': ['role', 'IdmRole', 'IdmRoleDto'],
      'component': require('./src/components/advanced/RoleInfo/RoleInfo').default,
      'manager': require('./src/redux').RoleManager
    },
    {
      'id': 'role-catalogue-info',
      'type': 'entity-info',
      'entityType': ['roleCatalogue', 'IdmRoleCatalogue', 'IdmRoleCatalogueDto', 'ROLE_CATALOGUE'],
      'component': require('./src/components/advanced/RoleCatalogueInfo/RoleCatalogueInfo').default,
      'manager': require('./src/redux').RoleManager
    },
    {
      'id': 'tree-node-info',
      'type': 'entity-info',
      'entityType': ['tree', 'treeNode', 'IdmTreeNode', 'IdmTreeNodeDto'],
      'component': require('./src/components/advanced/TreeNodeInfo/TreeNodeInfo').default,
      'manager': require('./src/redux').TreeNodeManager
    },
    {
      'id': 'notification-template-info',
      'type': 'entity-info',
      'entityType': ['notificationTemplate', 'IdmNotificationtemplate', 'IdmNotificationtemplateDto'],
      'component': require('./src/components/advanced/NotificationTemplateInfo/NotificationTemplateInfo').default,
      'manager': require('./src/redux').NotificationTemplateManager
    },
    {
      'id': 'tree-type-info',
      'type': 'entity-info',
      'entityType': ['treeType', 'IdmTreeType', 'IdmTreeTypeDto'],
      'component': require('./src/components/advanced/TreeTypeInfo/TreeTypeInfo').default,
      'manager': require('./src/redux').TreeTypeManager
    },
    {
      'id': 'identity-contract-info',
      'type': 'entity-info',
      'entityType': ['contract', 'identityContract', 'IdmIdentityContract', 'IdmIdentityContractDto'],
      'component': require('./src/components/advanced/IdentityContractInfo/IdentityContractInfo').default,
      'manager': require('./src/redux').IdentityContractManager
    },
    {
      'id': 'contract-position-info',
      'type': 'entity-info',
      'entityType': ['contractPosition', 'IdmContractPosition', 'IdmContractPositionDto'],
      'component': require('./src/components/advanced/ContractPositionInfo/ContractPositionInfo').default,
      'manager': require('./src/redux').ContractPositionManager
    },
    {
      'id': 'identity-role-info',
      'type': 'entity-info',
      'entityType': ['identityRole', 'IdmIdentityRole', 'IdmIdentityRoleDto'],
      'component': require('./src/components/advanced/IdentityRoleInfo/IdentityRoleInfo').default,
      'manager': require('./src/redux').IdentityRoleManager
    },
    {
      'id': 'contract-slice-info',
      'type': 'entity-info',
      'entityType': ['contractSlice', 'contractSlice', 'IdmContractSlice', 'IdmContractSliceDto'],
      'component': require('./src/components/advanced/ContractSliceInfo/ContractSliceInfo').default,
      'manager': require('./src/redux').ContractSliceManager
    },
    {
      'id': 'text-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'TEXT',
      'faceType': 'TEXT',
      'component': require('./src/components/advanced/Form/TextFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.TEXT'
    },
    {
      'id': 'short-text-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'SHORTTEXT',
      'faceType': 'SHORTTEXT',
      'component': require('./src/components/advanced/Form/ShortTextFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.SHORTTEXT'
    },
    {
      'id': 'char-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'CHAR',
      'faceType': 'CHAR',
      'component': require('./src/components/advanced/Form/CharFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.CHAR'
    },
    {
      'id': 'int-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'INT',
      'faceType': 'INT',
      'component': require('./src/components/advanced/Form/IntFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.INT'
    },
    {
      'id': 'long-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'LONG',
      'faceType': 'LONG',
      'component': require('./src/components/advanced/Form/LongFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.LONG'
    },
    {
      'id': 'double-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'DOUBLE',
      'faceType': 'DOUBLE',
      'component': require('./src/components/advanced/Form/DoubleFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.DOUBLE'
    },
    {
      'id': 'currency-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'DOUBLE',
      'faceType': 'CURRENCY',
      'component': require('./src/components/advanced/Form/CurrencyFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.CURRENCY'
    },
    {
      'id': 'boolean-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'BOOLEAN',
      'faceType': 'BOOLEAN',
      'component': require('./src/components/advanced/Form/BooleanFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.BOOLEAN'
    },
    {
      'id': 'date-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'DATE',
      'faceType': 'DATE',
      'component': require('./src/components/advanced/Form/DateFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.DATE'
    },
    {
      'id': 'datetime-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'DATETIME',
      'faceType': 'DATETIME',
      'component': require('./src/components/advanced/Form/DateTimeFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.DATETIME'
    },
    {
      'id': 'textarea-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'TEXT',
      'faceType': 'TEXTAREA',
      'component': require('./src/components/advanced/Form/TextAreaFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.TEXTAREA'
    },
    {
      'id': 'richtextarea-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'TEXT',
      'faceType': 'RICHTEXTAREA',
      'component': require('./src/components/advanced/Form/RichTextAreaFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.RICHTEXTAREA'
    },
    {
      'id': 'bytearray-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'BYTEARRAY',
      'faceType': 'BYTEARRAY',
      'component': require('./src/components/advanced/Form/ByteArrayFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.BYTEARRAY'
    },
    {
      'id': 'uuid-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'UUID',
      'faceType': 'UUID',
      'component': require('./src/components/advanced/Form/UuidFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.UUID'
    },
    {
      'id': 'identity-select-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'UUID',
      'faceType': 'IDENTITY-SELECT',
      'component': require('./src/components/advanced/Form/SelectBoxFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.IDENTITY-SELECT',
      'manager': require('./src/redux').IdentityManager
    },
    {
      'id': 'role-select-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'UUID',
      'faceType': 'ROLE-SELECT',
      'component': require('./src/components/advanced/Form/SelectBoxFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.ROLE-SELECT',
      'manager': require('./src/redux').RoleManager
    },
    {
      'id': 'form-definition-select-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'UUID',
      'faceType': 'FORM-DEFINITION-SELECT',
      'component': require('./src/components/advanced/Form/SelectBoxFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.FORM-DEFINITION-SELECT',
      'manager': require('./src/redux').FormDefinitionManager
    },
    {
      'id': 'boolean-select-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'BOOLEAN',
      'faceType': 'BOOLEAN-SELECT',
      'component': require('./src/components/advanced/Form/BooleanSelectFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.BOOLEAN-SELECT'
    },
    {
      'id': 'attachment-form-value',
      'type': 'form-attribute-renderer',
      'persistentType': 'ATTACHMENT',
      'faceType': 'ATTACHMENT',
      'component': require('./src/components/advanced/Form/AttachmentFormAttributeRenderer'),
      'labelKey': 'core:component.advanced.EavForm.faceType.ATTACHMENT'
    },
    {
      'id': 'identity-select-box',
      'type': 'entity-select-box',
      'priority': 0,
      'localizationKey': 'entity.Identity',
      'entityType': ['identity'],
      'searchInFields': ['username', 'firstName', 'lastName', 'email', 'description'],
      'manager': require('./src/redux').IdentityManager
    },
    {
      'id': 'role-select-box',
      'type': 'entity-select-box',
      'priority': 0,
      'localizationKey': 'entity.Role',
      'entityType': ['role'],
      'searchInFields': ['name', 'description'],
      'manager': require('./src/redux').RoleManager
    },
    {
      'id': 'tree-node-select-box',
      'type': 'entity-select-box',
      'priority': 0,
      'localizationKey': 'entity.TreeNode',
      'entityType': ['treeNode'],
      'searchInFields': ['code', 'name'],
      'manager': require('./src/redux').TreeNodeManager
    },
    {
      'id': 'basic-password-change',
      'type': 'password-change-component',
      'component': require('./src/components/advanced/PasswordChangeComponent/PasswordChangeComponent'),
      'disabled': false,
      'order': 0,
      'col': 6
    }
  ]
};
