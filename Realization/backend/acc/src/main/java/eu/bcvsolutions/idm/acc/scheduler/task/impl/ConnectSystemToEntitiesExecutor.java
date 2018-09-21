package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import eu.bcvsolutions.idm.acc.domain.*;
import eu.bcvsolutions.idm.acc.dto.*;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSyncConfigFilter;
import eu.bcvsolutions.idm.acc.service.api.*;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This LRT pairs all entities to IDM from system
 *
 * @author Marek Klement
 */
@Component
@Description("Pair all entities on system with entities in IDM")
public class ConnectSystemToEntitiesExecutor extends AbstractSchedulableTaskExecutor<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectSystemToEntitiesExecutor.class);
    //
    private static final String PARAM_SCHEMA_UUID = "UUID of Schema";
    private static final String PARAM_NAME_OF_UID = "Name of UID identifier";
    private static final String PARAM_START_SYNC = "Start synchronization - type  true or false";
    //
    private static final String NAME_OF_MAPPING = "Default mapping created by LRT";
    private static final String NAME_OF_SYNC = "Default synchronization created by LRT";
    private static final String ROLE_NAME = "DefaultRoleFor";
    private static final String IDM_ATTRIBUTE_NAME = "username";
    //
    private UUID schemaId;
    private String uidName;
    private boolean startSync;
    //
    @Autowired
    private SysSystemService systemService;
    @Autowired
    private SysSchemaObjectClassService schemaObjectClassService;
    @Autowired
    private SysSystemMappingService systemMappingService;
    @Autowired
    private SysSystemAttributeMappingService systemAttributeMappingService;
    @Autowired
    private SysSchemaAttributeService schemaAttributeService;
    @Autowired
    private SysRoleSystemService roleSystemService;
    @Autowired
    private IdmRoleService roleService;
    @Autowired
    private SynchronizationService synchronizationService;
    @Autowired
    private SysSyncConfigService configService;

    /**
     * TODO description
     */
    @Override
    public Boolean process() {
        SysSchemaObjectClassDto schema = schemaObjectClassService.get(schemaId);
        if (schema == null) {
            throw new IllegalArgumentException("Schema is null! Could not get schema from mapping!");
        }
        //
        SysSystemDto system = systemService.get(schema.getSystem());
        if (system == null) {
            throw new IllegalArgumentException("System is null! LRT was not able to get system from schema UUID.");
        }
        //
        SysSystemMappingDto foundMapping = createMapping(system);
        SysSystemAttributeMappingDto attributeMapping = createAttributeMapping(foundMapping.getId());
        createRoleAncConnectToSystem(system, foundMapping.getId());
        AbstractSysSyncConfigDto synchronization = createReconciliationConfig(attributeMapping.getId(), foundMapping.getId(), system.getId());

        processReconciliation(synchronization);
        //
        return true;
    }

    private void createRoleAncConnectToSystem(SysSystemDto system, UUID foundMapping) {
        String code = ROLE_NAME + system.getName();
        IdmRoleDto newRole = roleService.getByCode(code);
        if(newRole==null){
            newRole = new IdmRoleDto();
            newRole.setCode(code);
            newRole.setName(code);
            newRole.setPriority(0);
            newRole = roleService.save(newRole);
        }
        //
        SysRoleSystemFilter systemFilter = new SysRoleSystemFilter();
        systemFilter.setRoleId(newRole.getId());
        List<SysRoleSystemDto> systemRoles = roleSystemService.find(systemFilter, null).getContent();
        if (systemRoles.size() == 0) {
            SysRoleSystemDto systemRole = new SysRoleSystemDto();
            systemRole.setRole(newRole.getId());
            systemRole.setSystem(system.getId());
            systemRole.setSystemMapping(foundMapping);
            roleSystemService.save(systemRole);
        }
    }

    private AbstractSysSyncConfigDto createReconciliationConfig(UUID correlationAttribute, UUID systemMapping, UUID systemId, UUID roleId) {
        SysSyncConfigFilter filter = new SysSyncConfigFilter();
        filter.setName(NAME_OF_SYNC);
        filter.setSystemId(systemId);
        List<AbstractSysSyncConfigDto> allSync = configService.find(filter, null).getContent();
        AbstractSysSyncConfigDto synchronization;
        if (allSync.size() > 0) {
            synchronization = allSync.get(0);
        } else {
            synchronization = new SysSyncIdentityConfigDto();
            synchronization.setEnabled(true);
            synchronization.setName(NAME_OF_SYNC);
            synchronization.setCorrelationAttribute(correlationAttribute);
            synchronization.setReconciliation(true);
            synchronization.setSystemMapping(systemMapping);
            synchronization.setUnlinkedAction(SynchronizationUnlinkedActionType.LINK);
            synchronization.setLinkedAction(SynchronizationLinkedActionType.IGNORE);
            synchronization.setMissingEntityAction(SynchronizationMissingEntityActionType.IGNORE);
            ((SysSyncIdentityConfigDto) synchronization).setDefaultRole();
            synchronization = configService.save(synchronization);
        }
        return synchronization;
    }

    private void processReconciliation(AbstractSysSyncConfigDto synchronization) {
        if (startSync) {
            synchronizationService.startSynchronization(synchronization);
        } else {
            LOG.info("Synchronization was set up - now it is needed to start it!");
        }
    }

    private SysSystemAttributeMappingDto createAttributeMapping(UUID foundMapping) {
        SysSchemaAttributeFilter filter = new SysSchemaAttributeFilter();
        filter.setObjectClassId(schemaId);
        List<SysSchemaAttributeDto> schemaAttributes = schemaAttributeService.find(filter, null).getContent();
        UUID idOfAttributeName = null;
        for (SysSchemaAttributeDto attribute : schemaAttributes) {
            if (attribute.getName().equals(uidName)) {
                idOfAttributeName = attribute.getId();
                break;
            }
        }
        //
        SysSystemAttributeMappingDto attributeMapping = systemAttributeMappingService.findBySystemMappingAndName(foundMapping, IDM_ATTRIBUTE_NAME);
        //
        if (attributeMapping == null) {
            attributeMapping = new SysSystemAttributeMappingDto();
            attributeMapping.setEntityAttribute(true);
            if (idOfAttributeName != null) {
                attributeMapping.setSchemaAttribute(idOfAttributeName);
            } else {
                throw new IllegalArgumentException("Attribute uid name not found!");
            }
            attributeMapping.setIdmPropertyName(IDM_ATTRIBUTE_NAME);
            attributeMapping.setSystemMapping(foundMapping);
            attributeMapping.setName(IDM_ATTRIBUTE_NAME);
            attributeMapping.setUid(true);
            attributeMapping = systemAttributeMappingService.save(attributeMapping);
        } else if (!attributeMapping.isUid()) {
            throw new IllegalArgumentException("Attribute mapping with name was already set and is not IDENTIFIER!");
        }

        return attributeMapping;
    }

    private SysSystemMappingDto createMapping(SysSystemDto system) {
        boolean alreadyExists = false;
        SysSystemMappingDto foundMapping = null;
        List<SysSystemMappingDto> mappings = systemMappingService.findBySystem(system, SystemOperationType.SYNCHRONIZATION, SystemEntityType.IDENTITY);
        for (SysSystemMappingDto mapping : mappings) {
            if (mapping.getName().equals(NAME_OF_MAPPING)) {
                alreadyExists = true;
                foundMapping = mapping;
                break;
            }
        }
        SysSystemMappingDto newMapping;
        if (!alreadyExists) {
            newMapping = new SysSystemMappingDto();
            newMapping.setName(NAME_OF_MAPPING);
            newMapping.setEntityType(SystemEntityType.IDENTITY);
            newMapping.setOperationType(SystemOperationType.SYNCHRONIZATION);
            newMapping.setObjectClass(schemaId);
            newMapping.setProtectionEnabled(true);
            newMapping = systemMappingService.save(newMapping);
        } else {
            newMapping = foundMapping;
            LOG.warn("Attribute mapping already exists!");
        }

        return newMapping;
    }

    /**
     * TODO description
     *
     * @return all parameters
     */
    @Override
    public List<String> getPropertyNames() {
        LOG.debug("Start getPropertyName");
        List<String> params = super.getPropertyNames();
        params.add(PARAM_SCHEMA_UUID);
        params.add(PARAM_NAME_OF_UID);
        params.add(PARAM_START_SYNC);
        return params;
    }

    /**
     * TODO description
     *
     * @param properties map of properties given
     */
    @Override
    public void init(Map<String, Object> properties) {
        LOG.debug("Start init");
        super.init(properties);
        schemaId = getParameterConverter().toUuid(properties, PARAM_SCHEMA_UUID);
        uidName = getParameterConverter().toString(properties, PARAM_NAME_OF_UID);
        startSync = getParameterConverter().toBoolean(properties, PARAM_START_SYNC);
    }
}
