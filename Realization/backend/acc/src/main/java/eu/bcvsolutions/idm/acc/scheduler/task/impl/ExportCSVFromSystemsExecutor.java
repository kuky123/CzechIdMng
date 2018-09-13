package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import com.opencsv.CSVWriter;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;
import eu.bcvsolutions.idm.ic.api.IcConfigurationProperty;
import eu.bcvsolutions.idm.ic.api.IcConnectorConfiguration;
import eu.bcvsolutions.idm.ic.api.IcConnectorInstance;
import eu.bcvsolutions.idm.ic.api.IcObjectClass;
import eu.bcvsolutions.idm.ic.connid.domain.ConnIdIcConvertUtil;
import eu.bcvsolutions.idm.ic.connid.service.impl.ConnIdIcConnectorService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private SysSchemaObjectClassService sysSchemaObjectClassService;
    @Autowired
    private ConnIdIcConnectorService connIdIcConnectorService;

    /**
     * TODO text
     *
     * @return
     */
    @Override
    public Boolean process(){
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
        // konfigurace danneho systemu
        IcConnectorConfiguration config = sysSystemService.getConnectorConfiguration(system);
        // property samotneho nastaveni - TODO mozna neuzitecne
        List<IcConfigurationProperty> props = config.getConfigurationProperties().getProperties();
        // Vytahneme si a prevedeme __ACCOUNT__
        IcObjectClass icObjectClass = ConnIdIcConvertUtil.convertConnIdObjectClass(ObjectClass.ACCOUNT);
        // vytvorime writer - TODO projit zda odpovida popisu
        try {
            final CSVWriter writer = new CSVWriter(new FileWriter(pathToFile, true), ';', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, "\n");
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        // vyhledame objekty systemu s null filterem a kazdy preparsujeme - TODO preparsovat
        connIdIcConnectorService.search(icConnectorInstance, config, icObjectClass, null, connectorObject -> {
            // TODO novou metodu
            return true;
        });
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
