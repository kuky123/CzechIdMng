package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;
import eu.bcvsolutions.idm.ic.api.IcConnectorInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This LRT exports all items from Identity synchronization to CSV file
 */
@Component
@Description("Get all items on synchronization - system and parse it into CSV")
public class ExportCSVFromSystemsExecutor extends AbstractSchedulableTaskExecutor<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ExportCSVFromSystemsExecutor.class);
    //
    private static final String PARAM_CSV_FILE_PATH = "Path to file";
    //

    private UUID synchronizationId;
    private String pathToFile;

    @Autowired
    private SysSystemService sysSystemService;
    @Autowired
    private SynchronizationService synchronizationService;
    @Autowired
    private SysSyncConfigService service;
    @Autowired
    private IdmIdentityService identityService;
    @Autowired
    private SysSchemaObjectClassService sysSchemaObjectClassService;

    /**
     * TODO text
     *
     * @return
     */
    @Override
    public Boolean process() {
        LOG.debug("Start process");
        //
        File fl = new File(pathToFile);
        if (!fl.canRead() || !fl.canWrite()) LOG.error("Path {[]} is WRONG!", pathToFile);
        // vytahneme si schema na zaklade id schematu
        SysSchemaObjectClassDto schema = sysSchemaObjectClassService.get(synchronizationId);
        // kontrola
        if (schema == null) throw new NullPointerException("System is null! Probably wrong ID.");
        SysSystemDto system = sysSystemService.get(schema.getSystem());
        // instance konektoru
        IcConnectorInstance icConnectorInstance = system.getConnectorInstance();
        //
        return true;
    }

    /**
     * We need to add synchronization id - needed as identifier of synchronization which we will export
     * and path to file where we will export data.
     *
     * @return all parameters
     */
    @Override
    public List<String> getPropertyNames() {
        LOG.debug("Start getPropertyName");
        List<String> params = super.getPropertyNames();
        params.add(SynchronizationService.PARAMETER_SYNCHRONIZATION_ID);
        params.add(PARAM_CSV_FILE_PATH);
        return params;
    }

    /**
     * Synchronization id and path to file are retrieved for following usage.
     *
     * @param properties map of properties given
     */
    @Override
    public void init(Map<String, Object> properties) {
        LOG.debug("Start init");
        super.init(properties);
        synchronizationId = getParameterConverter().toUuid(properties, SynchronizationService.PARAMETER_SYNCHRONIZATION_ID);
        pathToFile = getParameterConverter().toString(properties, PARAM_CSV_FILE_PATH);
    }
}
