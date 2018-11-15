# Changelog
All notable changes to this project will be documented in this file.

## [9.3.0]
- [#1290](https://redmine.czechidm.com/issues/1290) - ``SchedulableStatefulExecutor`` supports separating processing of items, read more in [documentation](https://wiki.czechidm.com/devel/documentation/application_configuration/dev/scheduled_tasks/task-scheduler#stateful_task_executors). Use new properties ``continueOnException``, ``requireNewTransaction`` in your custom LRT, when the same behavior is needed.
- [#1285](https://redmine.czechidm.com/issues/1285) - Password is now transformed via transformation script to resource. Beware all password including \_\_PASSWORD\_\_ must be marked as password attribute (attribute mapping detail). All password attributes **can't be overridden in role mapping**. Transformation script obtain password as GuardedString. **GuardedString or null must be returned by the script**. Password generation during create new account works same as before.
- [#1358](https://redmine.czechidm.com/issues/1358) - Frontend ``Tree`` component was redesigned from the scratch. I hope no one used original ``Tree`` component in custom modules (original was ugly and buged, I'm apologize :(), but if yes, check please your contents on frontend and use new properties respecting the component properties or the component's ``readme.txt`` - the main change is about removed header decorator and added ``onSelect``, ``onDetail`` callbacks instead. New tree component is used in forms (e.g. for creating new contract or position) - tree structure can be selected with tree usage now.
- [#1323](https://redmine.czechidm.com/issues/1323) - Provisioning ``Merge`` was redesigned. Controlled values in IdM are computed from transformations on role-system-attribute now. Provisioning archive is no longer used for merge. All system attributes was marked with 'evictControlledValuesCache = true'. We need ensure recalculation for all merge attribute (resolves state when somewho could set parent attribute strategy on different value than Merge). All merge attribute caches will be recalculated on first provisioning.

## [9.2.2]
- [#1322](https://redmine.czechidm.com/issues/1322) - Preferred language and collapsed navigation is persisted to identity profile now and is loaded after login. Selecting locale doesn't refresh whole page now - add listening redux property ``i18nReady: state.config.get('i18nReady')`` if you content is not refreshed automatically.
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [9.2.0]
- [#1261](https://redmine.czechidm.com/issues/1261) - Internal processing of events in ``AbstractEntityEventProcessor`` was improved. It's possible to create processor for super classes, e.g. one processor can handle all BaseDto generalizations. ``AbstractEntityEventProcessor#onApplicationEvent`` method intercepts ``ApplicationEvent`` now - this method is not in ``EntityEventProcessor`` interface, but if you overrided her in custom processor, refactor your processors to new input parameter.
- [#1267](https://redmine.czechidm.com/issues/1267) - Eav attribute values can be saved together with owner - e.g. save identity with filled eavs. ``FormableDto`` super class was added and has to be defined for all dtos with eav support - change generalization tor your custom dtos with eav attribute support from ``AbstractDto`` to new ``FormableDto``.
- [#1272](https://redmine.czechidm.com/issues/1272) - Default value is not used for new extended attributes on entity details. Make sure your custom component implementing ``AbstractFormAttributeRenderer`` behaves the same way, if ``#toInputValue`` method was overrided. Sending read only (~disabled) extended attribute values from FE to BE was removed - read only attributes can be secured and this check worked only if value is not changed in the meantime (update and security check was skipped). Now it will be more transparent.
- [#1259](https://redmine.czechidm.com/issues/1259) - New agenda for generated values was created. Now is possible generate for example username, email and eavs. Agenda now supports only ``IdmIdentity`` and their eavs.
- [#1281](https://redmine.czechidm.com/issues/1281) - Websocket notifications are no longer supported. Configured websocket notifications are sent to configured global default notification sender (console by default). Reconfigure your custom topics, which uses websocket, to other notification type (e.g. email, console). Websocket sender and service (FE+BE) is marked as deprecated and they are not initialized by default - move senders to your project if needed (don't forget to copy and rename (or create) the new table for saving websocket notification logs in your database - table ``idm_notification_websocket`` will be removed in next major release). ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.
- [#1287](https://redmine.czechidm.com/issues/1287 - New generate type for passwords - prefixes and suffixes) Is possible generated passwords with prefixes and suffixes. Both strings are optional and can be setup only for generated password policy. Prefix and suffix are not counted into maximum and minimum length.
- [#1158](https://redmine.czechidm.com/issues/1158) - Long running task implement Configurable interface now. Form definition can be used for LRT configuration.


## [9.1.0]
- Authentication is not needed for using Spring Data queries in repositories (fixed mainly for test purposes).
- Role code is used in role select boxes (code is shown, only if name is different than code).
- [#1145](https://redmine.czechidm.com/issues/1145) - Other contract positions can be configured. Other positions are used for assign automatic roles by tree nodes.
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [9.0.0]
- [#1200](https://redmine.czechidm.com/issues/1200) - Business roles:
  - ``code`` unique attribute added to ``IdmRole`` entity - filled by ``name`` values by default . ``name`` attribute is not unique anymore - **use code attribute in your modules** for lookup roles. ``name`` is used as user friendly role name on frontend. Both fields ``name`` and ``code`` are required on controller layer.
  - Deprecated ``IdmRoleService#getByName`` method was removed - use ``IdmRoleService#getByCode`` method.
  - Deprecated lazy lists were removed from ``IdmRole``, ``IdmRoleDto`` - ``subRoles``, ``superiorRoles``, ``guarantees``, ``roleCatalogues``. Check usage of this lists in your modules and replace them with appropriate services.
  - Make sure all your workflow processes are correctly ended - ``IdmRole``, ``IdmRoleDto``, ``IdmIdentityRole`, ``IdmIdentityRoleDto``, ``IdmConceptRoleRequestDto`` structure changed.
   - All Lazy lists from  `IdmRole``, ``IdmRoleDto`` were removed - see above.
   - ``roleTreeNode`` attribute was renamed to ``automaticRole`` (previous ``automaticRole`` boolean flag was removed) in dtos ``IdmIdentityRoleDto``, ``IdmConceptRoleRequestDto``.Check attribute ``automaticRole`` (for automatic role) or ``directRole`` (for sub roles of business role) - this assigned roles cannot be removed directly. ``IdmAutomaticRoleAttributeService#ROLE_TREE_NODE_ATTRIBUTE_NAME`` was removed (used just for compatibility issues, which is not needed anymore) - use standard ``IdmIdentityRole_.automaticRole`` metamodel.
  - Obsolete, deprecated and unused methods ``IdmRoleRepository#findOneByName``,``IdmRoleRepository#getPersistedRole`` and ``IdmRoleRepository#getSubroles`` removed. Use ``IdmRoleService`` instead (with filter usage).
  - ``RoleGuaranteeEvaluator`` - evaluator supports guarantees configured by identity and by role now.
  - **Role guarantees, business roles (composition), role catalogues, tree nodes supports authorization policies now - configure [authorization policies](https://wiki.czechidm.com/devel/documentation/security/dev/authorization#default_settings_of_permissions_for_a_role_detail).**
- Event mechanism was improved:
  - ``EntityEventManager`` constants for event properties were moved directly into ``EntityEvent``. Use event getters and setters to use property values.
  - Parent event can be propagated, when sub event is created by the parent event. Executing account management was improved thanks to this mechanism - is called after change role request is executed (not for the all single assigned roles). When role is assigned outside the request (directly in some backend business logic), then account management s executed the same way as before - for each assigned role.
  - child events are deleted automatically - remove events from event queue will be easier.
- Method ``IdmAuthorizationPolicyService#getEnabledPersistedRoleAuthorities`` was removed - use previously loaded dto in event processing.
- [#1204](https://redmine.czechidm.com/issues/1204) - Reschedule HR long running task  ``HrEnableContractProcess`` - processed item queue should be cleared

## [8.2.0]

- [#1125](https://redmine.czechidm.com/issues/1125) - Identity extended attributes supports authorization policies, read [more](https://wiki.czechidm.com/devel/documentation/security/dev/authorization#secure_identity_form_extended_attribute_values).
- [#1121](https://redmine.czechidm.com/issues/1121) - Execute synchronization asynchronously
  - SynchronizationService is no longer LRT (Long Running Task).
  - Event types and processors to start and cancel sync has been removed.
- [#636](https://redmine.czechidm.com/issues/636) - JWT token are persisted now. **JWT token doesn't contain authorities now - use ''LoginDto.authorities'' instead**. JWT contains only authentication data. Logout feature is supported now. ``AuthenticationTestUtils`` was removed - this utility used SecurityContext directly and this was dangerous (identity id was not filled and authorization policies was skipped). ``IdmAuthorityChange`` entity and repository was removed at all - ``IdmTokenDto`` and ``DefaultTokenManager`` can be used now.
- [#1163](https://redmine.czechidm.com/issues/1163) - ``ConfidentialStorage`` service supports ``AbstractDto`` as value owner now. Check your module, where ``AbstractDto`` as owner for ``ConfidentialStorage`` was used before this update and provide appropriate change script if needed (see ``V8_02_006__confidential-storage-support-identifiable-owner.sql``).

## [8.1.7]
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [8.1.3]

- ``Configurable`` interface contains form definition now. ``Configurable`` instances can be configured by eav form on frontend (the richer ui). **Methods ``getFormDefinition`` and ``getFormAttributes`` were moved to ``Configurable`` interface from ``AbstractReportExecutor`` super class. Make sure you are using ``public`` modifier on this methods in your reports**.
- [#1153](https://redmine.czechidm.com/issues/1153) - Add event support for ``IdmPasswordDto`` (create, update, delete)

## [8.1.2]

- [#1137](https://redmine.czechidm.com/issues/1137) - Sync of slices does not use contract EAV definition

## [8.1.1]

- [#1128](https://redmine.czechidm.com/issues/1128) - Event processing - check parent event type for duplicates.
- [#1129](https://redmine.czechidm.com/issues/1129) - Boolean eav attribute with default value.
- [#1135](https://redmine.czechidm.com/issues/1135) - Bulk actions evaluates permission on frontend in select box. Problem with filtering and selecting identities was fixed (permission are now evaluated correctly). AbstractBulkAction is now part of api, AbstractIdentityBulkAction was removed from impl package and functionality was moved to api. For all bulk action is now used Enabled annotation for check if action is enabled/disabled.


## [[8.1.0](https://github.com/bcvsolutions/CzechIdMng/releases/tag/8.1.0)]

- Bulk action for identities - More about bulk backend actions you can find [here](https://wiki.czechidm.com/devel/documentation/bulk_actions).
- Time slices of contracts - More about time slices you can find [here](https://wiki.czechidm.com/devel/documentation/identities#time_slices_of_the_contractual_relationship).
- Password history - More about password history you can find [here](https://wiki.czechidm.com/8.0/documentation/security/dev/password-policies#standard_policy_for_validation).
- [#1076](https://redmine.czechidm.com/issues/1076) -  Extended form value - added support for ``org.joda.time.LocalDate`` and string in ISO 8601 format as value for DATE or DATETIME persistent type. Persistent type INT, LONG, BOOLEAN - supports setting string value now (default conversions from string representation added ~ the same meaning as``#valueOf(String)``).
- [#853](https://redmine.czechidm.com/issues/853) - Extended form values without filled values (``null``) are not saved.

## [8.0.0]

- [#938](https://redmine.czechidm.com/issues/938) - Requests for automatic assign roles (by tree node or attributes) was implemented.
 - Creating, updating, deleting of the any automatic role **must use** this requests. Requests are approval by "same" workflows as request for roles uses.
 - Create, update, delete methods from `IdmAutomaticRoleAttributeController` was **removed**. Method `/delete-via-request/` was **added** to this controller. This method creates request for delete given automatic role.
- Detail of user - frontend validation that required at least 3 characters for the user name was removed.
- [#468](https://redmine.czechidm.com/issues/468) - Added asynchronous event processing. Added asynchronous event type ``NOTIFY`` on identities, identity roles, roles, contracts, contract guarantees, role catalogues, tree nodes - notify about entity was changed. Processors could listen and process this new event asynchronously. Read more in [documentation](https://redmine.czechidm.com/issues/468). Account management and process automatic roles was switched to asynchronous processing by default (instead ``DELETE`` events - removing identity, their role or contact will be still synchronous). Check please your custom processors for automatic roles and account management - make sure they will work with new ``NOTIFY`` event. Make sure source file **``czechidm-app/src/Index.js``** on frontend is updated - flash message with localized processors was added.
- [#979](https://redmine.czechidm.com/issues/979) - Attribute modified from ``AbstractEntity`` isn't audited anymore. Modified date can be found in ``IdmAudit`` as revision ``timestamp``.
- **``Marked``** library was removed from frontend. Use standard localizations for writing inline help and documentation.

## [7.8.5]

- [#1037](https://redmine.czechidm.com/issues/1037) - Fix pageable in automatic role recalculation.

## [7.8.3]
- [#1005](https://redmine.czechidm.com/issues/1005) - Dry run is available for long running task which supports it.
- [#1000](https://redmine.czechidm.com/issues/1000) - Persistent type ``SHORTTEXT`` was added to eav forms. Change script for all CzechIdM tables for persist eav values is provided  - provide the same change script in your module, if extended attributes was added to custom entity. ``SHORTTEXT`` and ``UUID`` values are indexed now.
- [#980](https://redmine.czechidm.com/issues/980) - Improved performance with recalculation automatic roles and skip recalculation during synchronization.

## [7.8.0]

- [#857](https://redmine.czechidm.com/issues/857) - Filters - added support for multiple filter parameters, `MultiValueMap` is used.
- [#274](https://redmine.czechidm.com/issues/274) - Filters for entity event processors was added.


## [7.7.0]

### Added

  - [#920](https://redmine.czechidm.com/issues/920) - Event processor - support conditional processing. New method ``EntityEventProcessor#conditional``.
  - [#922](https://redmine.czechidm.com/issues/922) - ``IdmIdentityService#findValidByRole`` method was added - return valid identities with assigned valid role (valid contract and identity role relation).
  - [#842](https://redmine.czechidm.com/issues/842) - Add new automatic roles by attribute and refactor current automatic role by tree structure. Backward compatibility of current automatic role and assigned roles is guaranteed.
  - Workflow task instances has permissions now. You should add ``WORKFLOWTASK_READ`` permission for default user role.

### Changed

  - All workflow services and controllers now using DTO layer, for more information read [#529](https://redmine.czechidm.com/issues/529).

## [7.6.1]

### Added

- [#870](https://redmine.czechidm.com/issues/870) - Cannot save 0x00 to Postgres column - Provisioning operation.
- [#621](https://redmine.czechidm.com/issues/621) - add dry run mode and long running task detail with processed items.
- [#784](https://redmine.czechidm.com/issues/784) - notification template info card improvements.


## [7.6.0]

### Removed

##### Role request
- Method ``startRequestNewTransactional(UUID requestId, boolean checkRight)`` was removed from interface IdmRoleRequestService (now is using only in implementation).

##### Evaluator 'IdentityAccountByIdentityEvaluator'
- Evaluator 'IdentityAccountByIdentityEvaluator' was removed and replaced by 'IdentityAccountByAccountEvaluator'.

##### AccAccountService
- Deprecated method 'delete(AccAccountDto account, boolean deleteTargetAccount, UUID entityId)' was removed. AccAccountService supported events now, parameters 'deleteTargetAccount' and 'entityId' were transformed to properties of delete event.

##### ProvisioningOperation
- In the task #798, we solved problem with not updated UID value in Provisioning operation. This occurred when was two and more provisioning operations in the queue and when first provisioning (traged system) changed UID of the account. Next provisionings in the queue was wrong (old) value of UID. This was fixed (provisioning operation has relation on the system entity now).
- Beware - before update we recommand solve/cancel all active provisioning operations. We need to create new column "system_entity_id" and remove old "system_entity_uid". In the change script we will try transform data in the provisioning operation table (from string uid to the ID), but it may not be fully sucessfuly.

### Changed

- Method ``saveAndFlush`` was added into ``BaseEntityRepository`` and this method is used now for saving all dtos - see ``AbstractReadWriteDtoService#saveInternal``. Auditable dto's metadata (e.g. ``modifier``, ``modified``) are available now after dto is saved by ``IdentitySevaProcessor``, for more information read [#834](https://redmine.czechidm.com/issues/834).

##### Sync
- Since version **7.6 (in identity synchronization)**, the default contractual relationship (when creating a new identity) is not created, for more information read [#867](https://redmine.czechidm.com/issues/867)!

##### Identity
- Identity's last name attribute is optional, change script was provided. Make sure you check identity's last name for ``null`` values, in your project.

##### Script

- Methods ``IdmScriptService#getScriptByName``, ``IdmScriptService#getScriptByCode`` are deprecated and will be removed - use ``IdmScriptService#getByCode`` method instead.

##### Long running task
- Abstract tasks in package `eu.bcvsolutions.idm.core.scheduler.service.impl` are deprecated and will be removed, use new tasks in api:
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableStatefulExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.StatelessAsynchronousTask`

### Added

- [#780](https://redmine.czechidm.com/issues/780) - Path with resources - support multiple locations for scripts, notification templates and workflow definitions.

##### Identity

- [#815](https://redmine.czechidm.com/issues/815) - Identity state was added. When identity starts to be valid, then new password is generated for all their accounts - one password is set on all identity's accounts and in CzechIdM. Identity can be disabled (and enabled) manually through rest endpoint. Other states are controlled by system and by identity contract states.

##### Report module
- Reports are available now, read more in documentation.

##### Attachment manager
- Saving binary files to filesystem is supported now, read more in documentation.

##### Common forms
- `CommonFormService` is used for saving report filter and configuration for long running taskc
- `IdmFormDefinitionService#updateDefinition` is used for updating form definitions for common forms automatically (see compatible vs incompatible changes).


## [7.5.2]

#### Acc module

- [#797](https://redmine.czechidm.com/issues/797) - target system security was hidden by provisioning break (fixed). ``SysSystemRepository#find`` method is deprecated and will be removed - use ``SysSystemService#find`` method instead.

## [7.5.1]

##### Contractual relation

- Long running tasks (LRT) for HR processes were restored - they are useful for pre-production checks (disable processors + show what LRT wants to process).

## [7.5.0]

### Added

#### Core module

##### Scheduler

- Added ``DependentTaskTrigger`` - execute task, when other task successfully ended. Dependent tasks are executed by ``LongRunningTaskExecuteDependentProcessor``.

#### Acc module

##### System
- Add three new attributes for block system provisioning operation (createOperation, updateOperation, deleteOperation). All these attributes has default value false.


##### Provisioning

- After add, remove or update ``IdmContractGuaranteeDto`` is execute provisioning for identity that own this contract.
- It's possible to send additional attributes, when password is changed (e.g. password expiration in extended attribute). New flag ``sendOnPasswordChange`` was added to system attribute mapping - attribute with this flag checked will be send together with changed password to provisioning. Two ways for provisioning additional attributes are implemented:
  - send additional attributes together with new password in one provisioning operation
  - send additional attributes after password is changed in another provisioning operation
    - two ways are be configurable by application configuration ``idm.sec.acc.provisioning.sendPasswordAttributesTogether`` (is effective for all target systems):
    - ``true``: additional password attributes will be send in one provisioning operation together with password
    - ``false``: additional password attributes will be send in new provisioning operation, after password change operation
- Add provisioning break for basic operations (create, update, delete), provisioning break can be set per system or globally for all systems.

### Changed

##### Identity

- Warning notifications about expired password and before password expiration are not send to disabled identities.
- Base permission ``CHANGEPERMISSION`` was added for identities. This permission is used for evaluating rights to identity's role requests - changing identity permissions. Add this new permission to your configured authorization policies (e.g for SelfIdentityEvaluator, SubordinatesEvaluator).

##### Contractual relation

- Service ``IdmContractGuaranteeService`` now implements EventableDtoService for method save and delete.
- Attribute ``state`` was added. Attribute disabled was removed from ``IdmIdentityContractDto`` as redundant to new state. Previously disabled contracts is enabled with state ``ECXLUDED`` - change script is provided. Contract with state ``DISABLED`` is invalid now - all processed work with this state (automatic roles, end of contract process).
- Contract processors were moved into ``eu.bcvsolutions.idm.core.model.event.processor.contract`` package:
  - ``IdentityContractCreateByAutomaticRoleProcessor``
  - ``IdentityContractDeleteProcessor``
  - ``IdentityContractSaveProcessor``
  - ``IdentityContractUpdateByAutomaticRoleProcessor``
- Business logic from HR long running tasks (LRT) for HR processes was moved into processors. Enable / disable identity with no valid or excluded contracts is executed immediately after contract is changed. HR processors are using almost the same workflow as LRT (LRT variables were removed only), but its configurable now. Long running task were removed and will be unscheduled automatically after new version will be installed:
  - ``AbstractWorkflowEventProcessor``
  - ``HrContractExclusionProcess``
  - ``HrEnableContractProcess``
  - ``HrEndContractProcess``

### Removed

#### Core module

##### Identity

- Password reset functionality was removed from core module. Password reset will be standalone module.

##### Contractual relation

- ``IdmIdentityContractRepository#deleteByIdentity`` was removed. It was skipping audit (bug).

## [7.4.0]

### Requirements

#### All workflow instances (task) have to be closed (resolved / canceled) before update. Some services were refactored and new workflow definitions (core) will be installed automatically.

### Added

#### Core module

##### Long running task

- New long running task ExecuteScriptTaskExecutor, this task can execute any of scripts in DEFAULT category.
- new long running task RemoveOldLogsTaskExecutor for remove logging event, logging event exception and logging event property.
- ``@DisallowConcurrentExecution`` can be used for prevent to execute task concurrently.

### Changed

- CGLIB for creating proxies are enforced by default. Now is possible to use annotations on methods, which is not defined in service interface. Prevent to use some logic in service constructors and always define annotations in implementation class, [read more](https://www.credera.com/blog/technology-insights/open-source-technology-insights/aspect-oriented-programming-in-spring-boot-part-2-spring-jdk-proxies-vs-cglib-vs-aspectj/). Make sure you have ``spring.aop.proxy-target-class=true`` property configured in your project.

#### Core module
- Application property **idm.pub.core.notification.template.folder** was changed to **idm.sec.core.notification.template.folder**.
- Application property **idm.pub.core.notification.template.fileSuffix** was changed to **idm.sec.core.notification.template.fileSuffix**.
- Application property **idm.pub.core.script.folder** was changed to **idm.sec.core.script.folder**.
- Application property **idm.pub.core.script.fileSuffix** was changed to **idm.sec.core.script.fileSuffix**.
- **All service interface were moved into core api module**. Check service imports in your project:
  - ``IdmAuthorizationPolicyService``
  - ``IdmConceptRoleRequestService``
  - ``IdmContractGuaranteeService``
  - ``IdmConfigurationService``
  - ``IdmIdentityContractService``
  - ``IdmIdentityRoleValidRequestService``
  - ``IdmIdentityRoleService``
  - ``IdmIdentityService``
  - ``IdmPasswordPolicyService``
  - ``IdmPasswordService``
  - ``IdmRoleCatalogueRoleService``
  - ``IdmRoleCatalogueService``
  - ``IdmRoleGuaranteeService``
  - ``IdmRoleRequestService``
  - ``IdmRoleService``
  - ``IdmRoleTreeNodeService``
  - ``IdmScriptAuthorityService``
  - ``IdmScriptService``
  - ``IdmTreeNodeService``
  - ``IdmTreeTypeService``
  - ``ConsoleNotificationSender``
  - ``Emailer``
  - ``EmailNotificationSender``
  - ``IdmConsoleLogService``
  - ``IdmEmailLogService``
  - ``IdmNotificationConfigurationService``
  - ``IdmNotificationLogService``
  - ``IdmNotificationRecipientService``
  - ``IdmNotificationTemplateService``
  - ``IdmSmsLogService``
  - ``IdmWebsocketLogService``
  - ``NotificationManager``
  - ``NotificationSender``
  - ``WebsocketNotificationSender``
  - ``IdmLongRunningTaskService``
  - ``IdmProcessedTaskItemService``
  - ``IdmScheduledTaskService``
  - ``IdmAuditService``
  - ``IdmLoggingEventExceptionService``
  - ``IdmLoggingEventPropertyService``
  - ``IdmLoggingEventService``
  - ``GrantedAuthoritiesFactory``
  - ``JwtAuthenticationService``
  - ``LoginService``
  - ``AuthenticationFilter``

- **Idm** prefix was added to all core service filters and all filters were moved to copre api module. Check filters usage in you project:
  - ``RoleFilter`` was renamed to ``IdmRoleFilter`` and moved to core api module
  - ``TreeNodeFilter`` was renamed to ``IdmTreeNodeFilter`` and moved to core api module
  - ``TreeNodeFilter`` was renamed to ``IdmTreeNodeFilter`` and moved to core api module
  - ``FormDefinitionFilter`` was renamed to ``IdmFormDefinitionFilter`` and moved to core api module
  - ``FormAttributeFilter`` was renamed to ``IdmFormAttributeFilter`` and moved to core api module
  - ``FormValueFilter`` was renamed to ``IdmFormValueFilter`` and moved to core api module
  - ``AuditFilter`` was renamed to ``IdmAuditFilter`` and moved to core api module
  - ``AuthorizationPolicyFilter`` was renamed to ``IdmAuthorizationPolicyFilter``
  - ``ConceptRoleRequestFilter`` was renamed to ``IdmConceptRoleRequestFilter``
  - ``ContractGuaranteeFilter`` was renamed to ``IdmContractGuaranteeFilter``
  - ``IdentityContractFilter`` was renamed to ``IdmIdentityContractFilter``
  - ``IdentityFilter`` was renamed to ``IdmIdentityFilter``
  - ``IdentityRoleFilter`` was renamed to ``IdmIdentityRoleFilter``
  - ``LoggingEventExceptionFilter`` was renamed to ``IdmLoggingEventExceptionFilter`` and moved to core api module
  - ``LoggingEventFilter`` was renamed to ``IdmLoggingEventFilter`` and moved to core api module
  - ``LoggingEventPropertyFilter`` was renamed to ``IdmLoggingEventPropertyFilter`` and moved to core api module
  - ``PasswordFilter`` was renamed to ``IdmPasswordFilter``
  - ``PasswordPolicyFilter`` was renamed to ``IdmPasswordPolicyFilter``
  - ``RoleCatalogueFilter`` was renamed to ``IdmRoleCatalogueFilter``
  - ``RoleCatalogueRoleFilter`` was renamed to ``IdmRoleCatalogueRoleFilter``
  - ``RoleGuaranteeFilter`` was renamed to ``IdmRoleGuaranteeFilter``
  - ``RoleRequestFilter`` was renamed to ``IdmRoleRequestFilter``
  - ``RoleTreeNodeFilter`` was renamed to ``IdmRoleTreeNodeFilter``
  - ``ScriptAuthorityFilter`` was renamed to ``IdmScriptAuthorityFilter``
  - ``ScriptFilter`` was renamed to ``IdmScriptFilter``
  - ``LongRunningTaskFilter`` was renamed to ``IdmLongRunningTaskFilter``
  - ``IdmProcessedTaskItemFilter` was moved to core api module.
  - ``NotificationFilter`` was renamed to ``IdmNotificationFilter`` and moved to core api module
  - ``NotificationRecipientFilter`` was renamed to ``IdmNotificationRecipientFilter`` and moved to core api module
  - ``NotificationTemplateFilter`` was renamed to ``IdmNotificationTemplateFilter`` and moved to core api module
  - ```` was renamed to ````

- ``EnabledAcpect`` class was renamed to ``EnabledAspect``
- FE: improved external frentend configuration. All config properties can be changed without application build. Check external configuration in your project and move ``serverUrl`` property into config object.


##### Identity

- Deprecated method ``IdmIdentityService#saveIdentity`` was removed. Use ``save``or ``publish`` method instead.
- Method ``IdmIdentityService#passwordChange`` don't modify given password change dto - results was added.  

##### Event processiong

- Added ``AbstractEventableDtoService`` for simplify services working with events. ``CoreEvent`` with ``CoreEventType`` is published by default now for save and delete event for all domain types (e.g. ``IdmIdentityDto``, ``IdmIdentityContractDto``, ``IdmRoleCatalogueDto``, ``IdmIdentityRoleDto`` ...). **Check [appropriate event type comparison](https://wiki.czechidm.com/devel/dev/architecture/events#event_types)** in your project.

##### Password policy

- Entity IdmPasswordPolicy was transformed to DTO. Update all places where call from your project service IdmPasswordPolicyService. The service now uses DTO only. Also check your current scripts and their authorities. If you're overloaded any of frontend component which is related IdmPasswordPolicy, check her functionality.
- The attributes of IdmPasswordPolicy are now Integer (in past int). Please Check your password policy settings and remove redundant zeros in settings. The zero in the setting is already taken as a valid setting value.

##### Script

- Script with category DEFAULT can be executed from executor with another category.

##### Confidential storage

- The encryption of the confidential agenda can now be done using the key from application properties, also file with key may be defined in application properties. Backward compatibility with key defined in resource **isn't maintained**. If your project using key directly saved in **.war** on path  **<war>/lib/idm-core-impl-<version>.jar/eu/bcvsolutions/idm/confidential/key.key**, move your key outside the **.war** and configure application property ``cipher.crypt.secret.keyPath``.

##### Notifications

- Api ``NotificationSender`` was changed. Now are returned all notifications that is sent. Also mechanism of sending with topic was fixed. Check your project notification configuration.


##### Role

- Role agenda was refactored to dto usage. Search **``IdmRoleService``** usage in your project - **all methods works with dto**, entity service methods were removed (e.q. ``findSecured``). **In workflow ``roleSevice.get(roleId, null)`` method has to be used**.
- **``GrantedAuthoritiesFactory#getActiveRoleAuthorities(UUID identityId, IdmRoleDto role)``** - method using role dto as parameter.
- **``IdmRoleDto`` is used as event content - change all entity event processors from template ``IdmRole`` to ``IdmRoleDto`` in your project.** ``RoleEvent`` uses ``IdmRoleDto`` content too.
- Rest endpoint - changed role lists structure (``IdmRoleGuaranteeDto, IdmRoleCatalogueDto`` are used) - uuid only is needed, when relation is created and updated.
- ``IdmRoleGuaranteeRepository`` - removed methods ``deleteByGuarantee_Id``, ``deleteByRole`` - service layer has to be used for create audit records.
- **``RoleFilter``** was **moved to core api** module and renamed to ``IdmRoleFilter`` - we will add ``Idm`` prefix to all core filters.
- **``IdmRoleFilter``** - fields are ``UUID`` now - **roleCatalogue => roleCatalogueId**, **guarantee => guaranteeId**

##### Role request

- Added authorization policies support. New [authorization policy evaluators](https://wiki.czechidm.com/devel/dev/security/change-user-permissions#security) has to be [configured](https://wiki.czechidm.com/devel/dev/security/authorization#default_settings_of_permissions_for_an_identity_profile) to add permission for role requests. Added new permission group ``ROLEREQUEST``.
- Don't use **``IdmRoleRequestRepository#find()``** and **``IdmConceptRoleRequestRepository#find()``** methods directly => use service layer (methods are using criteria api now).
- Role request navigation item was moved to audit and enabled for all logged users - can read their own role requests history.

##### Tree structure

- Tree type and node agendas were refactored to dto usage. Search **``IdmTreeTypeService``** usage in your project - **all methods works with dto**. Search **``IdmTreeNodeService``** usage in your project - **all methods works with dto**.
- **``IdmTreeTypeService``** supports events (``CREATE``, ``UPDATE``, ``DELETE``).
- **``IdmTreeTypeService#getConfigurations(UUID)`` uses uuid as parameter**.
- **``IdmTreeTypeRepository#clearDefaultTreeType()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeRepository#clearDefaultTreeNode()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeRepository#findOneByDefaultTreeTypeIsTrue()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeService#clearDefaultTreeNode()``** was removed - configuration service is used for persist default tree type and node.
- ``TreeConfiguration`` was added - provide default tree node and type. This configuration is used in ``DefaultIdmIdentityContractService``, ``DeafultIdmTreeNodeService`` constructors.
- **``IdmTreeTypeFilter``** is used as filter in ``IdmTreeTypeService`` - its possible to find types by code from rest api.
- **``TreeNodeFilter``** was renamed to **``IdmTreeNodeFilter``**. Its possible to find node by type and code from rest api.
- Don't use **``IdmTreeTypeRepository#find()``** method directly => use service layer (methods are using criteria api now).
- **``IdmTreeNodeService#rebuildIndexes(UUID)``** uses uuid as parameter**.
- **Tree processors were moved to package ``eu.bcvsolutions.idm.core.model.event.processor.tree``**.
- **``IdmTreeNodeDto`` is used as event content - change all entity event processors from template ``IdmTreeNode`` to ``IdmTreeNodeDto`` in your project.** ``TreeNodeEvent`` uses ``IdmTreeNodeDto`` content too.
- **``IdmTreeTypeDto`` is used as event content - change all entity event processors from template ``IdmTreeType`` to ``IdmTreeTypeDto`` in your project.** ``TreeTypeEvent`` uses ``IdmTreeTypeDto`` content too.
- **Added authorization policies support.** Authorization policies has to be [configured](https://wiki.czechidm.com/devel/dev/security/authorization#default_settings_of_permissions_for_an_identity_profile) to add permission for tree types and nodes. ``BasePermissionEvaluator`` can be used.

##### Dynamic forms (eav)

- Form definitions was refactored to dto usage. Search **``IdmFormDefinitionService``** usage in your project - **all methods works with dto** and interface was moved to core api module.
- Form attributes was refactored to dto usage. Search **``IdmFormAttributeService``** usage in your project - **all methods works with dto** and interface was moved to core api module.
- Don't use **``IdmFormDefinitionRepository#find()``**, **``IdmFormAttributeRepository#find()``** and **``AbstractFormValueRepository#find()``** methods directly => use service layer (methods are using criteria api now).
- **``FormAttributeFilter``** -  **formDefinition**, **formDefinitionId** fields were removed. Use **definitionId** field. **``FormAttributeFilter``** was renamed to **``IdmFormAttributeFilter``** and moved into core api package
- **``FormValueFilter``** - - fields are ``UUID`` now - **formDefinition => definitionId**, **formAttribute => attributeId**.  **``FormValueFilter``** was renamed to **``IdmFormValueFilter``** and moved into core api package
- **``IdmFormDefinitonFilter``** was added - form definitions can be found by ``type``, ``code``, ``name`` and ``main``.
- Form values was refactored to dto usage. **``IdmFormValueDto``** is used for all form values => its not needed to know owner type. Search **``FormValueService``** usage in your project - **all methods works with dto**. Owner can be entity and dto now.
- **``IdmFormInstanceDto``** was added and its used as **value holder for form service api** - contains form definition + form values by owner. Rest endpoints for save and get form values use **``IdmFormInstanceDto``** insted raw values
- **``FormService``** was refactored to dto usage and moved to core api module. Search **``FormService``** usage in your project - **all methods works with dto** (definitions, attributes, instances). Helper methods ``toValueMap``, ``toPersistentValueMap``, ``toPersistentValues``, ``toSinglePersistentValue`` was moved to ``IdmFormDefinitionDto`` class.
- **``AbstractFormableService``** as refactored to dto usage. Generalize ``AbstractEventableDtoService`` now and automatically clear form values, when owner (dto) is deleted. Service interface was moved to core api module.
- **``FormValueService``** uses ``IdmFormDefinitionDto`` and ``IdmFormAttributeDto`` as parameters now.  Search **``FormValueService``** usage in your project. **``FormValueService``** was moved to core api module.
- **``AbstractFormValueRepository``** finds owner by attribute methods using attribute uuid as parameter. Use new methods for find values by owner id - methods for find values by owner are deprecated - needs persisted owner and this could be confusing.
- **``IdmFormDefinitionController#getOwnerTypes()``** rest endpoint method returns simple string owner types without resource wrapper.
- **Added authorization policies support.** Authorization policies has to be configured to add permission for form definition and attribute administration. ``BasePermissionEvaluator`` can be used. Permission groups were renamed to ``FORMDEFINITION`` and ``FORMATTRIBUTE``.
- **Confidential values are stored under form attribute identifier now.** Form attribute's code can be changed and stored confidential values were identified by code - can be lost after code is changed. Change script ``V1_00_028__eav-change-confidential-storage-key.sql`` is provided - provide the same change script in your module, if extended attributes was added to custom entity.
- Form attribute has new field ``faceType`` with contains information about attribute rendering on FE. Persistent types ``TEXTAREA``, ``RICHTEXTAREA``, ``CURRENCY`` were removed and appropriate face types were added. ``Change script V1_00_029__eav-add-face-type.sql`` is provided - provide the same change script in your module, if extended attributes was added to custom entity. Face type can be added in custom module for change (add) attribute rendered - its used mainly for select boxes.
- Persistent type ``UUID`` was added - entity identifiers can be saved into form value.

##### Audit

- ``IdmLoggingEventDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmLoggingEventExceptionDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmLoggingEventPropertyDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditDiffDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventExceptionFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventPropertyFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.

##### TestHelper

- **``TestHelper``** interface and **``DefaultTestHelper``** implementation was moved to core test api module. TestHelper is reused in acc module now.
- Role entity usage was removed - dto or role id (uuid) is used now.
- Tree type and node entities usage was removed - dto or id (uuid) is used now.

#### Acc module

- **Provisioning and synchronization was refactored to dto usage.** Check all ``ProvisioningEntityExecutor``, ``SynchronizationEntityExecutor`` usage in your project.

- **Acc** prefix was added to all acc service filters. Check filters usage in you project:
- ``AccountFilter`` was renamed to ``AccAccountFilter``
- ``IdentityAccountFilter`` was renamed to ``AccIdentityAccountFilter``
- ``ProvisioningOperationFilter`` was renamed to ``SysProvisioningOperationFilter``
- ``ProvisioningRequestFilter`` was renamed to ``SysProvisioningRequestFilter``
- ``RoleAccountFilter`` was renamed to ``AccRoleAccountFilter``
- ``RoleCatalogueAccountFilter`` was renamed to ``AccRoleCatalogueAccountFilter``
- ``RoleSystemAttributeFilter`` was renamed to ``SysRoleSystemAttributeFilter``
- ``RoleSystemFilter`` was renamed to ``SysRoleSystemFilter``
- ``SchemaAttributeFilter`` was renamed to ``SysSchemaAttributeFilter``
- ``SchemaObjectClassFilter`` was renamed to ``SysSchemaObjectClassFilter``
- ``SyncActionLogFilter`` was renamed to ``SysSyncActionLogFilter``
- ``SynchronizationConfigFilter`` was renamed to ``SysSyncConfigFilter``
- ``SynchronizationLogFilter`` was renamed to ``SysSyncLogFilter``
- ``SyncItemLogFilter`` was renamed to ``SysSyncItemLogFilter``
- ``SystemAttributeMappingFilter`` was renamed to ``SysSystemAttributeMappingFilter``
- ``SystemEntityFilter`` was renamed to ``SysSystemEntityFilter``
- ``SystemMappingFilter`` was renamed to ``SysSystemMappingFilter``
- ``TreeAccountFilter`` was renamed to ``AccTreeAccountFilter``

##### System service
- Method **readObject** was replaced with **readConnectorObject**
  - **Old**: IcConnectorObject readObject(SysSystem system, SysSystemMappingDto systemMapping, IcUidAttribute uidAttribute);
  - **New**: IcConnectorObject readConnectorObject(UUID systemId, String uid, IcObjectClass objectClass);


##### Synchronization

- Entity **SysSyncLog was transformed to SysSyncLogDto**. Update all places where call from your project SysSyncLogService. The service now uses only DTO. Backward compatibility of all synchronization logs is guaranteed.
- Entity **SysSyncItemLog was transformed to SysSyncItemLogDto**. Update all places where call from your project SysSyncItemLogService. The service now uses only DTO. Backward compatibility of all synchronization item logs is guaranteed.
- Entity **SysSyncActionLog was transformed to SysSyncActionLogDto**. Update all places where call from your project SysSyncActionLogService. The service now uses only DTO. Backward compatibility of all synchronization action logs is guaranteed.
- Entity **SysSyncConfig was transformed to SysSyncConfigDto**. Update all places where call from your project SysSyncConfigService. The service now uses only DTO. Backward compatibility of synchronization config is guaranteed.

##### Attribute mapping
- Entity **SysRoleSystemAttribute was transformed to SysRoleSystemAttributeDto**. Update all places where call from your project [SysRoleSystemAttributeService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysRoleSystemAttributeService.java). The service now uses only DTO.
- Entity **SysSchemaAttribute was transformed to SysSchemaAttributeDto**. Update all places where call from your project [SysSchemaAttributeService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSchemaAttributeService.java). The service now uses only DTO.
- Entity **SysSchemaObjectClass was transformed to SysSchemaObjectClassDto**. Update all places where call from your project [SysSchemaObjectClassService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSchemaObjectClassService.java). The service now uses only DTO.
- Entity **SysSystemAttributeMapping was transformed to SysSystemAttributeMappingDto**. Update all places where call from your project [SysSystemAttributeMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemAttributeMappingService.java). The service now uses only DTO.
- Entity **SysSystemMapping was transformed to SysSystemMappingDto**. Update all places where call from your project [SysSystemMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemMappingService.java). The service now uses only DTO.
- Into method getAuthenticationAttribute in [SysSystemAttributeMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemAttributeMappingService.java) was added new parameter SystemEntityType, please check usage this method.

##### Role system
- Entity **SysRoleSystem was transformed to SysRoleSystemDto**. Update all places where call from your project [SysRoleSystemService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysRoleSystemService.java). The service now uses only DTO.


##### System
- Entity **SysSystem was transformed to SysSystemDto**. Update all places where call from your project [SysSystemService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemService.java). The service now uses only DTO.

##### Account management

- Added authorization policies support for identity accounts. Added new permission group ``IDENTITYACCOUNT`` with new evaluator [IdentityAccountByIdentityEvaluator](https://wiki.czechidm.com/devel/dev/security/authorization#identityaccountbyidentityevaluator).
- Don't use **AccIdentityAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- Added authorization policies support for role accounts. Added new tab to role detail with assigned role accounts. Added new permission group ``ROLEACCOUNT``. with new evaluator [RoleAccountByRoleEvaluator](https://wiki.czechidm.com/devel/dev/security/authorization#roleaccountbyroleevaluator).
- Don't use **AccRoleAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- Added authorization policies support for tree accounts. Added new tab to tree node detail with assigned tree accounts. Added new permission group ``TREEACCOUNT``.
- Don't use **AccTreeAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- ``AccAccountManagementService`` uses dto - check service usage in your project.

##### Provisioning

- Entity **SysProvisioningBatch was transformed to SysProvisioningBatchDto**. Update all places where call from your project [SysProvisioningBatchService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningBatchService.java). The service now uses only DTO.
- Entity **SysProvisioningArchive was transformed to SysProvisioningArchiveDto**. Update all places where call from your project [SysProvisioningArchiveService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningArchiveService.java). The service now uses only DTO.
- Entity **SysProvisioningOperation was transformed to SysProvisioningOperationDto**. Update all places where call from your project [SysProvisioningOperationService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningOperationService.java). The service now uses only DTO.
- Entity **SysProvisioningRequest was removed** - use ``SysProvisioningOperation`` and ``SysProvisioningOperationDto``.
- Method **SysProvisioningBatch findBatch(SysProvisioningOperation operation)** from service ``SysProvisioningBatchService`` was moved into service ``SysProvisioningOperationService``, update all places where you call method **findBatch** to new service.
- Password provisioning was improved - operation results are returned now, without input password dto is changed.
- Provisioning operation table is sorted by created date descended.

### Removed

#### Core module

- Removed support for entity services - use dto services in your project. Removed classes:
  - ``DefaultReadWriteEntityController`` - use ``DefaultReadWriteDtoController``
  - ``AbstractReadWriteEntityController`` - use ``DefaultReadWriteDtoController``
  - ``AbstractReadEntityController`` - use ``AbstractReadWriteDtoController``
  - ``AbstractReadWriteEntityService`` - use ``AbstractReadWriteDtoService``
  - ``AbstractReadEntityService`` - use ``AbstractReadDtoService``
  - ``ReadWriteEntityService`` - use ``ReadWriteDtoService``
  - ``ReadEntityService`` - use ``ReadDtoService``
  - ``AuthorizableEntityService`` was removed - ``AuthorizableService`` is used for adding authorization policies support for dto services.
- Use ``@RestController`` for your controllers. ``@RepositoryRestController`` support from Spring Data project was removed.
- ``BaseFilter`` template and ``find(F filter, Pageable pageable)`` method was removed from ``BaseEntityRepository``. Use service layer with criteria api for find dtos.

##### Long running tasks
- From ``AbstractLongRunningTaskExecutor`` was removed deprecated method ``getParameterNames``, replace this method from your project with method ``getPropertyNames``.
- From ``AbstractScheduledTaskInitializer`` was removed method ``getLOG()``.
- From ``LongRunningTaskManager``   was removed internal method ``scheduleProcessCreated()``.
