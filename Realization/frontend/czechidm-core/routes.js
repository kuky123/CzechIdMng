module.exports = {
  module: 'core',
  childRoutes: [
    {
      path: 'login',
      component: require('./src/content/Login'),
      access: [ { type: 'PERMIT_ALL' } ]
    },
    {
      path: 'logout',
      component: require('./src/content/Logout'),
      access: [ { type: 'PERMIT_ALL' } ]
    },
    {
      path: 'password/change',
      component: require('./src/content/PasswordChange'),
      access: [ { type: 'PERMIT_ALL' } ]
    },
    {
      path: 'identity/new',
      component: require('./src/content/identity/Create'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_CREATE' ] } ],
      priority: 0
    },
    {
      path: 'identity/:entityId/',
      component: require('./src/content/identity/Identity'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ' ] } ],
      childRoutes: [
        {
          path: 'profile',
          component: require('./src/content/identity/IdentityProfile'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ' ] } ],
          priority: 0
        },
        {
          path: 'password',
          component: require('./src/content/identity/PasswordChangeRoute'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_PASSWORDCHANGE' ] } ]
        },
        {
          path: 'roles',
          component: require('./src/content/identity/IdentityRoles'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYROLE_READ' ] } ]
        },
        {
          path: 'authorities',
          component: require('./src/content/identity/IdentityAuthorities'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTHORIZATIONPOLICY_READ' ] } ]
        },
        {
          path: 'contracts',
          component: require('./src/content/identity/IdentityContracts'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYCONTRACT_READ' ] } ]
        },
        {
          path: 'contract-slices',
          component: require('./src/content/identity/ContractSlices'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_READ' ] } ]
        },
        {
          path: 'revision',
          component: require('./src/content/identity/Audit'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
        },
        {
          path: 'subordinates',
          component: require('./src/content/identity/IdentitySubordinates'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ' ] } ]
        },
        {
          path: 'eav',
          component: require('./src/content/identity/IdentityEav'),
          access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['IDENTITY_READ', 'FORMDEFINITION_AUTOCOMPLETE'] } ]
        },
        {
          path: 'garanted-roles',
          component: require('./src/content/identity/IdentityGarantedRoles'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ]
        },
        {
          path: 'events',
          component: require('./src/content/identity/IdentityEvents'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN' ] } ]
        }
      ]
    },
    {
      path: 'identity/:entityId/revision/:revID',
      component: require('./src/content/identity/AuditDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
    },
    {
      path: 'identity/:identityId/identity-contract/:entityId/',
      component: require('./src/content/identity/contract/IdentityContract'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYCONTRACT_READ' ] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/identity/contract/IdentityContractContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYCONTRACT_READ' ] } ]
        },
        {
          path: 'eav',
          component: require('./src/content/identity/contract/IdentityContractEav'),
          access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['IDENTITYCONTRACT_READ', 'FORMDEFINITION_AUTOCOMPLETE' ] } ]
        },
        {
          path: 'guarantees',
          component: require('./src/content/identity/contract/IdentityContractGuarantees'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYCONTRACT_READ' ] } ]
        },
        {
          path: 'positions',
          component: require('./src/content/identity/contract/IdentityContractPositions'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTPOSITION_READ' ] } ]
        },
        {
          path: 'contract-slices',
          component: require('./src/content/identity/ContractSlices'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_READ' ] } ]
        },
      ]
    },
    {
      path: 'identity/:identityId/identity-contract/:entityId/new',
      component: require('./src/content/identity/contract/IdentityContractContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITYCONTRACT_CREATE' ] } ]
    },
    {
      path: 'identity/:identityId/contract-slice/:entityId/',
      component: require('./src/content/identity/contractSlice/ContractSlice'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_READ' ] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/identity/contractSlice/ContractSliceContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_READ' ] } ]
        },
        {
          path: 'eav',
          component: require('./src/content/identity/contractSlice/ContractSliceEav'),
          access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['CONTRACTSLICE_READ', 'FORMDEFINITION_AUTOCOMPLETE' ] } ]
        },
        {
          path: 'guarantees',
          component: require('./src/content/identity/contractSlice/ContractSliceGuarantees'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_READ' ] } ]
        }
      ]
    },
    {
      path: 'identity/:identityId/contract-slice/:entityId/new',
      component: require('./src/content/identity/contractSlice/ContractSliceContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONTRACTSLICE_CREATE' ] } ]
    },
    {
      path: 'identities',
      component: require('./src/content/identity/Identities'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
    },
    {
      path: 'organizations',
      component: require('./src/content/organization/Organizations'),
      access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['TREETYPE_AUTOCOMPLETE', 'TREENODE_READ'] } ]
    },
    {
      path: 'tree',
      childRoutes: [
        {
          path: 'nodes',
          component: require('./src/content/tree/node/Nodes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREENODE_READ'] } ],
        },
        {
          path: 'nodes/:entityId',
          component: require('./src/content/tree/node/Node'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREENODE_READ'] } ],
          childRoutes: [
            {
              path: 'detail',
              component: require('./src/content/tree/node/NodeContent'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREENODE_READ'] } ]
            },
            {
              path: 'eav',
              component: require('./src/content/tree/node/NodeEav'),
              access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['TREENODE_READ', 'FORMDEFINITION_AUTOCOMPLETE'] } ]
            },
            {
              path: 'roles',
              component: require('./src/content/tree/node/TreeNodeRoles'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
            },
            {
              path: 'identities',
              component: require('./src/content/tree/node/TreeNodeIdentities'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
            }
          ]
        },
        {
          path: 'nodes/:entityId/new',
          component: require('./src/content/tree/node/NodeContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREENODE_CREATE'] } ]
        },
        {
          path: 'types',
          component: require('./src/content/tree/type/Types'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREETYPE_READ'] } ],
        },
        {
          path: 'types/new',
          component: require('./src/content/tree/type/TypeContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREETYPE_CREATE' ] } ]
        },
        {
          path: 'types/:entityId',
          component: require('./src/content/tree/type/TypeContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['TREETYPE_READ'] } ]
        },
      ]
    },
    {
      path: 'role-catalogues',
      component: require('./src/content/rolecatalogue/RoleCatalogues'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUE_READ'] } ]
    },
    {
      path: 'role-catalogue/:entityId',
      component: require('./src/content/rolecatalogue/RoleCatalogue'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUE_READ'] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/rolecatalogue/RoleCatalogueContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUE_READ'] } ]
        }
      ]
    },
    {
      path: 'role-catalogue/:entityId/new',
      component: require('./src/content/rolecatalogue/RoleCatalogueContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUE_CREATE'] } ],
    },
    {
      path: 'roles',
      component: require('./src/content/role/Roles'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ]
    },
    {
      path: 'role-requests',
      component: require('./src/content/requestrole/RoleRequests'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLEREQUEST_READ'] } ]
    },
    {
      path: 'role-requests/:entityId/detail',
      component: require('./src/content/requestrole/RoleRequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLEREQUEST_READ'] } ]
    },
    {
      path: 'role-requests/:entityId/new',
      component: require('./src/content/requestrole/RoleRequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLEREQUEST_CREATE'] } ]
    },
    {
      path: 'automatic-role-requests',
      component: require('./src/content/automaticrolerequest/AutomaticRoleRequests'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEREQUEST_READ'] } ]
    },
    {
      path: 'automatic-role-requests/:entityId/detail',
      component: require('./src/content/automaticrolerequest/AutomaticRoleRequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEREQUEST_READ'] } ]
    },
    {
      path: 'automatic-role-requests/:entityId/new',
      component: require('./src/content/automaticrolerequest/AutomaticRoleRequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEREQUEST_CREATE'] } ]
    },
    {
      path: 'requests',
      component: require('./src/content/request/Requests'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['REQUEST_READ'] } ]
    },
    {
      path: 'requests/:entityId/detail',
      component: require('./src/content/request/RequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['REQUEST_READ'] } ]
    },
    {
      path: 'requests/:entityId/new',
      component: require('./src/content/request/RequestDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['REQUEST_CREATE'] } ]
    },
    {
      path: 'role/:entityId/',
      component: require('./src/content/role/Role'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/role/RoleContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ]
        },
        {
          path: 'compositions',
          component: require('./src/content/role/RoleCompositions'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECOMPOSITION_READ'] } ]
        },
        {
          path: 'guarantees',
          component: require('./src/content/role/RoleGuarantees'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLEGUARANTEE_READ', 'ROLEGUARANTEEROLE_READ'] } ]
        },
        {
          path: 'catalogues',
          component: require('./src/content/role/RoleCatalogueRoles'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUEROLE_READ'] } ]
        },
        {
          path: 'identities',
          component: require('./src/content/role/RoleIdentities'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
        },
        {
          path: 'eav',
          component: require('./src/content/role/RoleEav'),
          access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['ROLE_READ', 'FORMDEFINITION_AUTOCOMPLETE'] } ]
        },
        {
          path: 'tree-nodes',
          component: require('./src/content/role/RoleTreeNodes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
        },
        {
          path: 'automatic-roles',
          component: require('./src/content/role/RoleAutomaticRoleRoutes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ],
          childRoutes: [
            {
              path: 'attributes',
              component: require('./src/content/role/RoleAutomaticAttributes'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
            },
            {
              path: 'trees',
              component: require('./src/content/role/RoleTreeNodes'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
            }
          ]
        },
        {
          path: 'automatic-roles/attributes/:automaticRoleId',
          component: require('./src/content/role/RoleAutomaticRoleAttributeRoutes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ],
          childRoutes: [
            {
              path: 'rules',
              component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeRules'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTERULE_READ'] } ]
            },
            {
              path: 'identities',
              component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeIdentities'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
            },
            {
              path: 'detail',
              component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeContent'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
            },
          ]
        },
        {
          path: 'authorization-policies',
          component: require('./src/content/role/AuthorizationPolicies'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTHORIZATIONPOLICY_READ'] } ]
        }
      ]
    },
    {
      path: 'requests/:requestId',
      component: require('./src/content/request/RequestInfo'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ],
      childRoutes: [
        {
          path: 'role/:entityId',
          component: require('./src/content/role/Role'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ],
          childRoutes: [
            {
              path: 'detail',
              component: require('./src/content/role/RoleContent'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ]
            },
            {
              path: 'compositions',
              component: require('./src/content/role/RoleCompositions'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECOMPOSITION_READ'] } ]
            },
            {
              path: 'guarantees',
              component: require('./src/content/role/RoleGuarantees'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLEGUARANTEE_READ', 'ROLEGUARANTEEROLE_READ'] } ]
            },
            {
              path: 'catalogues',
              component: require('./src/content/role/RoleCatalogueRoles'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLECATALOGUEROLE_READ'] } ]
            },
            {
              path: 'identities',
              component: require('./src/content/role/RoleIdentities'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_READ'] } ]
            },
            {
              path: 'eav',
              component: require('./src/content/role/RoleEav'),
              access: [ { 'type': 'HAS_ALL_AUTHORITIES', 'authorities': ['ROLE_READ', 'FORMDEFINITION_AUTOCOMPLETE'] } ]
            },
            {
              path: 'tree-nodes',
              component: require('./src/content/role/RoleTreeNodes'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
            },
            {
              path: 'automatic-roles',
              component: require('./src/content/role/RoleAutomaticRoleRoutes'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ],
              childRoutes: [
                {
                  path: 'attributes',
                  component: require('./src/content/role/RoleAutomaticAttributes'),
                  access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
                },
                {
                  path: 'trees',
                  component: require('./src/content/role/RoleTreeNodes'),
                  access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
                }
              ]
            },
            {
              path: 'automatic-roles/attributes/:automaticRoleId',
              component: require('./src/content/role/RoleAutomaticRoleAttributeRoutes'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ],
              childRoutes: [
                {
                  path: 'rules',
                  component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeRules'),
                  access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTERULE_READ'] } ]
                },
                {
                  path: 'identities',
                  component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeIdentities'),
                  access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
                },
                {
                  path: 'detail',
                  component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeContent'),
                  access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
                },
              ]
            },
            {
              path: 'authorization-policies',
              component: require('./src/content/role/AuthorizationPolicies'),
              access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTHORIZATIONPOLICY_READ'] } ]
            }
          ]
        }
      ]
    },
    {
      path: 'role/:entityId/new',
      component: require('./src/content/role/RoleContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLE_CREATE'] } ],
    },
    {
      path: 'tasks/',
      component: require('./src/content/task/TaskRoutes'),
      access: [ { 'type': 'IS_AUTHENTICATED'}],
      childRoutes: [
        {
          path: 'identity/:entityId',
          component: require('./src/content/task/TaskInstances'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['WORKFLOWTASK_READ'] } ]
        },
        {
          path: 'all',
          component: require('./src/content/task/TaskInstancesView'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['WORKFLOWTASK_ADMIN'] } ]
        }
      ]
    },
    {
      path: 'task/:taskID',
      component: require('./src/content/task/Task')
    },
    {
      path: 'messages',
      component: require('./src/content/Messages')
    },
    {
      path: 'configurations',
      component: require('./src/content/Configurations'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONFIGURATION_READ'] } ]
    },
    {
      path: 'modules',
      component: require('./src/content/module/ModuleRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['MODULE_READ'] } ],
      childRoutes: [
        {
          path: 'fe-modules',
          component: require('./src/content/module/FrontendModules'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] } ]
        },
        {
          path: 'be-modules',
          component: require('./src/content/module/BackendModules'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['MODULE_READ'] } ]
        },
        {
          path: 'entity-event-processors',
          component: require('./src/content/module/EntityEventProcessors'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['MODULE_READ'] } ]
        }
      ]
    },
    {
      path: 'scheduler',
      component: require('./src/content/scheduler/SchedulerRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ],
      childRoutes: [
        {
          path: 'running-tasks',
          component: require('./src/content/scheduler/RunningTasks'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ]
        },
        {
          path: 'schedule-tasks',
          component: require('./src/content/scheduler/ScheduleTasks'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ]
        },
        {
          path: 'all-tasks',
          component: require('./src/content/scheduler/AllTasks'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ],
        }
      ]
    },
    {
      path: 'scheduler/all-tasks/:entityId',
      component: require('./src/content/scheduler/LongRunningTaskRoute'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/scheduler/LongRunningTaskContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ]
        },
        {
          path: 'items',
          component: require('./src/content/scheduler/LongRunningTaskItems'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_READ'] } ]
        },
        {
          path: 'queue',
          component: require('./src/content/scheduler/LongRunningTaskQueue'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCHEDULER_EXECUTE'] } ]
        },
      ]
    },
    {
      path: 'workflow',
      component: 'div',
      childRoutes: [
        {
          path: 'definitions',
          component: require('./src/content/workflow/Definitions'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] } ]
        },
        {
          path: 'history/processes',
          component: require('./src/content/workflow/HistoricProcessInstances')
        }
      ]
    },
    {
      path: 'workflow/definitions/:definitionId',
      component: require('./src/content/workflow/Definition'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] } ]
    },
    {
      path: 'workflow/history/processes/:historicProcessInstanceId',
      component: require('./src/content/workflow/HistoricProcessInstanceDetail')
    },
    {
      path: 'scripts',
      component: require('./src/content/script/Scripts'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_READ'] } ]
    },
    {
      path: 'scripts/:entityId/',
      component: require('./src/content/script/Script'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_READ'] } ],
      childRoutes: [
        {
          path: 'detail',
          component: require('./src/content/script/ScriptContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_READ'] } ]
        },
        {
          path: 'authorities',
          component: require('./src/content/script/ScriptAuthorities'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_READ'] } ]
        },
        {
          path: 'references',
          component: require('./src/content/script/ScriptReferences'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_READ'] } ]
        }
      ]
    },
    {
      path: 'scripts/:entityId/new',
      component: require('./src/content/script/ScriptContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['SCRIPT_CREATE'] } ],
    },
    {
      path: 'automatic-role/',
      component: require('./src/content/automaticrole/AutomaticRoleRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ', 'ROLETREENODE_READ'] } ],
      childRoutes: [
        {
          path: 'attributes',
          component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
        },
        {
          path: 'trees',
          component: require('./src/content/automaticrole/tree/AutomaticRoleTrees'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['ROLETREENODE_READ'] } ]
        }
      ]
    },
    {
      path: 'automatic-role/attributes/',
      component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ],
      childRoutes: [
        {
          path: ':automaticRoleId',
          component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTE_READ'] } ]
        },
        {
          path: ':automaticRoleId/rules',
          component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeRules'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTERULE_READ'] } ]
        },
        {
          path: ':automaticRoleId/identities',
          component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeIdentities'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['IDENTITY_READ'] } ]
        }
      ]
    },
    {
      path: 'automatic-role/attributes/:entityId/rule/:ruleId',
      component: require('./src/content/automaticrole/attribute/AutomaticRoleAttributeRuleContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUTOMATICROLEATTRIBUTERULE_READ'] } ]
    },
    {
      path: 'forms',
      component: require('./src/content/form/FormDefinitions'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMDEFINITION_READ'] } ]
    },
    {
      path: 'forms/',
      component: require('./src/content/form/FormDefinitionRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMDEFINITION_READ'] } ],
      childRoutes: [
        {
          path: ':entityId/detail',
          component: require('./src/content/form/FormDefinitionDetail'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMDEFINITION_READ'] } ]
        },
        {
          path: ':entityId/attributes',
          component: require('./src/content/form/FormAttributes'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMATTRIBUTE_READ'] } ]
        },
        {
          path: ':entityId/localization',
          component: require('./src/content/form/FormDefinitionLocalization'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMDEFINITION_READ'] } ]
        },
        {
          path: ':entityId/values',
          component: require('./src/content/form/FormDefinitionValues'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] } ]
        }
      ]
    },
    {
      path: 'forms/attribute/',
      component: require('./src/content/form/FormAttributeRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMATTRIBUTE_READ'] } ],
      childRoutes: [
        {
          path: ':entityId/detail',
          component: require('./src/content/form/FormAttributeDetail'),
          access: [{ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['FORMATTRIBUTE_READ'] }]
        },
        {
          path: ':entityId/values',
          component: require('./src/content/form/FormAttributeValues'),
          access: [{ 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] }]
        }
      ]
    },
    {
      path: 'password-policies',
      component: require('./src/content/passwordpolicy/PasswordPolicies'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ]
    },
    {
      path: 'password-policies/',
      component: require('./src/content/passwordpolicy/PasswordPolicyRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ],
      childRoutes: [
        {
          path: ':entityId',
          component: require('./src/content/passwordpolicy/PasswordPolicyBasic'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ]
        },
        {
          path: ':entityId/advanced',
          component: require('./src/content/passwordpolicy/PasswordPolicyAdvanced'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ]
        },
        {
          path: ':entityId/characters',
          component: require('./src/content/passwordpolicy/PasswordPolicyCharacters'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ]
        }
      ]
    },
    {
      path: 'confidential-storage',
      component: require('./src/content/confidentialstorage/ConfidentialStorageValues'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONFIDENTIALSTORAGEVALUE_READ'] } ]
    },
    {
      path: 'confidential-storage/:entityId',
      component: require('./src/content/confidentialstorage/ConfidentialStorageValueContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['CONFIDENTIALSTORAGEVALUE_READ'] } ]
    },
    {
      path: 'audit',
      component: require('./src/content/audit/AuditRoutes'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['PASSWORDPOLICY_READ'] } ]
    },
    {
      path: 'audit/entities/:entityId/diff/:revID',
      component: require('./src/content/audit/AuditDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
    },
    {
      path: 'audit/entities/:entityId/diff',
      component: require('./src/content/audit/AuditDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
    },
    {
      path: 'audit/logging-event/:entityId',
      component: require('./src/content/audit/loggingEvent/LoggingEventDetail'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
    },
    {
      path: 'audit/logging-events',
      component: require('./src/content/audit/loggingEvent/LoggingEventContent'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
    },
    {
      path: 'audit/',
      component: require('./src/content/audit/AuditRoutes'),
      childRoutes: [
        {
          path: 'entities',
          component: require('./src/content/audit/AuditContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
        },
        {
          path: 'identities',
          component: require('./src/content/audit/identity/AuditIdentityContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['AUDIT_READ'] } ]
        }
      ]
    },
    {
      path: 'audit/entity-events',
      component: require('./src/content/audit/event/EntityEvents'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['APP_ADMIN'] } ]
    },
    {
      path: 'generate-values',
      component: require('./src/content/generatevalues/GenerateValues'),
      access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['GENERATEVALUE_READ'] } ]
    },
    {
      path: 'notification/',
      component: 'div',
      childRoutes: [
        {
          path: 'notifications',
          component: require('./src/content/notification/Notifications'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'notification/:entityId',
          component: require('./src/content/notification/NotificationContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'emails',
          component: require('./src/content/notification/email/Emails'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'emails/:entityId',
          component: require('./src/content/notification/email/EmailContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'websockets',
          component: require('./src/content/notification/websocket/Websockets'),
          _deprecated: '@deprecated @since 9.2.0 websocket notification will be removed',
          access: [ { 'type': 'DENY_ALL' } ]
        },
        {
          path: 'websockets/:entityId',
          component: require('./src/content/notification/websocket/WebsocketContent'),
          _deprecated: '@deprecated @since 9.2.0 websocket notification will be removed',
          access: [ { 'type': 'DENY_ALL' } ]
        },
        {
          path: 'sms',
          component: require('./src/content/notification/sms/Sms'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'sms/:entityId',
          component: require('./src/content/notification/sms/SmsContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATION_READ'] } ]
        },
        {
          path: 'configurations',
          component: require('./src/content/notification/configuration/NotificationConfigurations'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATIONCONFIGURATION_READ'] } ]
        },
        {
          path: 'templates',
          component: require('./src/content/notification/template/Templates'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATIONTEMPLATE_READ'] } ]
        },
        {
          path: 'templates/:entityId',
          component: require('./src/content/notification/template/TemplateContent'),
          access: [ { 'type': 'HAS_ANY_AUTHORITY', 'authorities': ['NOTIFICATIONTEMPLATE_READ'] } ]
        }
      ]
    },
    // About site
    {
      path: 'about',
      component: require('./src/content/About'),
      priority: 5
    },
    // error pages
    {
      path: 'unavailable',
      component: require('./src/content/error/503'),
      access: [ { type: 'PERMIT_ALL' } ]
    },
    {
      path: 'error/403',
      component: require('./src/content/error/403'),
      access: [ { type: 'PERMIT_ALL' } ]
    },
    {
      path: '*',
      component: require('./src/content/error/404'),
      access: [ { type: 'PERMIT_ALL' } ],
      order: 99999 // need to be on end
    },
  ]
};
