package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import com.opencsv.CSVWriter;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;
import eu.bcvsolutions.idm.ic.api.*;
import eu.bcvsolutions.idm.ic.connid.domain.ConnIdIcConvertUtil;
import eu.bcvsolutions.idm.ic.service.impl.DefaultIcConnectorFacade;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This LRT exports all items from Identity synchronization to CSV file
 *
 * @author Marek Klement
 */
@Component
@Description("Get all items on schema - system and parse it into CSV")
public class ExportCSVFromSystemsExecutor extends AbstractSchedulableTaskExecutor<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ExportCSVFromSystemsExecutor.class);
    //
    private static final String PARAM_CSV_FILE_PATH = "Path to file";
    private static final String PARAM_SCHEMA_UUID = "UUID of Schema";
    //
    private String DEFAULT_LINE_END = "\n";

    private Character DEFAULT_LINE_SEPARATOR = ';';
    //

    private UUID schemaId;
    private String pathToFile;
    private boolean checkedHeader = false;
    private int size = 0;

    @Autowired
    private SysSystemService sysSystemService;
    @Autowired
    private SysSchemaObjectClassService sysSchemaObjectClassService;
    @Autowired
    private DefaultIcConnectorFacade defaultIcConnectorFacade;

    /**
     * Checks for existing properties and than process system
     *
     * @return boolean if it was successful
     */
    @Override
    public Boolean process() {
        LOG.debug("Start process");
        //
        File fl = new File(pathToFile);
        if (!fl.canRead()) {
            throw new IllegalArgumentException("Can read the file! Path to file: " + pathToFile);
        }
        //
        if (!fl.canWrite()) {
            throw new IllegalArgumentException("Can write into the file! Path to file: " + pathToFile);
        }
        //
        SysSchemaObjectClassDto schema = sysSchemaObjectClassService.get(schemaId);
        if (schema == null) {
            throw new IllegalArgumentException("Schema is null! Probably wrong UUID.");
        }
        //
        SysSystemDto system = sysSystemService.get(schema.getSystem());
        if (system == null) {
            throw new IllegalArgumentException("System is null! LRT was not able to get system from schema UUID.");
        }
        //
        IcConnectorInstance icConnectorInstance = system.getConnectorInstance();
        if (icConnectorInstance == null) {
            throw new IllegalArgumentException("icConnectorInstance is null! LRT was not able to get icConnectorInstance from system.");
        }
        //
        IcConnectorConfiguration config = sysSystemService.getConnectorConfiguration(system);
        if (config == null) {
            throw new IllegalArgumentException("configuration is null! LRT was not able to get configuration of system.");
        }
        //__ACCOUNT__
        IcObjectClass icObjectClass = ConnIdIcConvertUtil.convertConnIdObjectClass(ObjectClass.ACCOUNT);
        if (icObjectClass == null) {
            throw new IllegalArgumentException("ConnIdIcConvertUtil failed to convert ObjectClass.ACCOUNT.");
        }
        return writeIntoFile(icConnectorInstance, config, icObjectClass);
    }

    /**
     * Check for framework and use its service. Than it writes all data into file.
     *
     * @param icConnectorInstance instance of connector
     * @param config              configuration of system
     * @param icObjectClass       object class to be used
     * @return true if everything went right
     */
    private boolean writeIntoFile(IcConnectorInstance icConnectorInstance, IcConnectorConfiguration config, IcObjectClass icObjectClass) {
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(pathToFile, true), DEFAULT_LINE_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, System.lineSeparator());
            //
            CSVWriter finalWriter = writer;
            defaultIcConnectorFacade.search(icConnectorInstance, config, icObjectClass, null, connectorObject -> handleConnectorObject(connectorObject, finalWriter));
            //
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Handle function for connector object.
     *
     * @param connectorObject object to be parsed
     * @param writer          buffer to be written
     * @return returns true while it was handled right
     */
    private boolean handleConnectorObject(IcConnectorObject connectorObject, CSVWriter writer) {
        if (!checkedHeader) {
            String[] header = createHeader(connectorObject);
            writer.writeNext(header);
            checkedHeader = true;
            size = header.length;
        }
        writer.writeNext(getLine(connectorObject, size));
        return true;
    }

    /**
     * Creates line from connector object. Parse all its attributes.
     *
     * @param connectorObject object to be parsed
     * @param size            size of the line
     * @return new line into csv
     */
    private String[] getLine(IcConnectorObject connectorObject, int size) {
        String[] line = new String[size];
        int i = 0;
        for (IcAttribute value : connectorObject.getAttributes()) {
            if (checkNameAndUid(value.getName())) {
                int valuesSize = value.getValues().size();
                StringBuilder toBeWriten = new StringBuilder();
                for (int j = 0; j < valuesSize; ++j) {
                    toBeWriten.append(value.getValues().get(j).toString());
                    if (j != (valuesSize - 1)) {
                        toBeWriten.append(DEFAULT_LINE_END);
                    }
                }
                line[i] = toBeWriten.toString();
                ++i;
            }

        }
        return line;
    }

    /**
     * returns if name of the property is one of following __NAME__ or __UID__
     *
     * @param name name of attribute
     * @return boolean
     */
    private boolean checkNameAndUid(String name) {
        return !name.equals(Name.NAME) && !name.equals(Uid.NAME);
    }

    /**
     * This is called right when we found first connectorObject. I takes all names of attributes and parse it in header.
     *
     * @param connectorObject object to be parsed
     * @return Header of the file.
     */
    private String[] createHeader(IcConnectorObject connectorObject) {
        LinkedList<String> line = new LinkedList<>();
        for (IcAttribute value : connectorObject.getAttributes()) {
            if (checkNameAndUid(value.getName())) {
                line.add(value.getName());
            }
        }
        int size = line.size();
        String[] header = new String[size];
        int i = 0;
        for (String name : line) {
            header[i] = name;
            ++i;
        }
        return header;
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
        params.add(PARAM_SCHEMA_UUID);
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
        schemaId = getParameterConverter().toUuid(properties, PARAM_SCHEMA_UUID);
        pathToFile = getParameterConverter().toString(properties, PARAM_CSV_FILE_PATH);
    }
}
