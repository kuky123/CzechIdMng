package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor;
import eu.bcvsolutions.idm.ic.api.*;
import eu.bcvsolutions.idm.ic.connid.domain.ConnIdIcConvertUtil;
import eu.bcvsolutions.idm.ic.czechidm.domain.IcConnectorConfigurationCzechIdMImpl;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.impl.IcConfigurationPropertiesImpl;
import eu.bcvsolutions.idm.ic.impl.IcUidAttributeImpl;
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
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This LRT imports all items from csv to IDM
 *
 * @author Marek Klement
 */
@Component
@Description("Get all items on mapping - system and import from CSV to IDM")
public class ImportFromCSVToSystemExecutor extends AbstractSchedulableTaskExecutor<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ImportFromCSVToSystemExecutor.class);
    //
    private static final String PARAM_CSV_FILE_PATH = "Path to file";
    private static final String PARAM_SCHEMA_UUID = "UUID of Schema";
    private static final String PARAM_ATTRIBUTE_SEPARATOR = "Attribute separator";
    private static final String PARAM_NAME_ATTRIBUTE = "Name attribute";
    private static final String PARAM_UID_ATTRIBUTE = "Uid attribute";
    private static final String PARAM_MULTIVALUED_SEPARATOR = "Separator of multivalued attributes";
    //
    private String DEFAULT_NOTIFY_PROPERTY = "requiredConfirmation";
    //
    private UUID schemaId;
    private String pathToFile;
    private Character separator;
    private String nameHeaderAttribute;
    private String uidHeaderAttribute;
    private String multivaluedSeparator;

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
        SysSchemaObjectClassDto schema = sysSchemaObjectClassService.get(schemaId);
        if (schema == null) {
            throw new IllegalArgumentException("Schema is null! Could not get schema from mapping!");
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
        IcConnectorConfigurationCzechIdMImpl config = getUnnoticedConfiguration((IcConnectorConfigurationCzechIdMImpl) sysSystemService.getConnectorConfiguration(system));
        if (config == null) {
            throw new IllegalArgumentException("configuration is null! LRT was not able to get configuration of system.");
        }
        //__ACCOUNT__
        IcObjectClass icObjectClass = ConnIdIcConvertUtil.convertConnIdObjectClass(ObjectClass.ACCOUNT);
        if (icObjectClass == null) {
            throw new IllegalArgumentException("ConnIdIcConvertUtil failed to convert ObjectClass.ACCOUNT.");
        }
        return convertToSystem(icConnectorInstance, config, icObjectClass);
    }

    /**
     * Takes configuration of system and makes requiredConfirmation as false
     *
     * @param config configuration of system
     * @return new configuration with requiredConfirmation to false
     */
    private IcConnectorConfigurationCzechIdMImpl getUnnoticedConfiguration(IcConnectorConfigurationCzechIdMImpl config) {
        IcConnectorConfigurationCzechIdMImpl newConfig = new IcConnectorConfigurationCzechIdMImpl();
        IcConfigurationPropertiesImpl newProperties = new IcConfigurationPropertiesImpl();
        boolean wasThereProperty = false;
        for (IcConfigurationProperty property : config.getConfigurationProperties().getProperties()) {
            if (property.getName().equals(DEFAULT_NOTIFY_PROPERTY)) {
                newProperties.addProperty(DEFAULT_NOTIFY_PROPERTY, false, property.getType(), property.getDisplayName(), property.getHelpMessage(), property.isRequired());
                wasThereProperty = true;
            } else {
                newProperties.addProperty(property.getName(), property.getValue(), property.getType(), property.getDisplayName(), property.getHelpMessage(), property.isRequired());
            }
        }
        if (!wasThereProperty) {
            newProperties.addProperty(DEFAULT_NOTIFY_PROPERTY, false, null, null, null, false);
        }
        newConfig.setConfigurationProperties(newProperties);
        newConfig.setSystemId(config.getSystemId());
        newConfig.setConnectorPoolConfiguration(config.getConnectorPoolConfiguration());
        newConfig.setConnectorPoolingSupported(config.isConnectorPoolingSupported());
        newConfig.setProducerBufferSize(config.getProducerBufferSize());
        return newConfig;
    }

    /**
     * Coverts all lines from CSV into the system.
     *
     * @param icConnectorInstance instance of connector
     * @param config              configuration of connector
     * @param icObjectClass       connecting object class
     * @return true if everything was OK
     */
    private boolean convertToSystem(IcConnectorInstance icConnectorInstance, IcConnectorConfiguration config, IcObjectClass icObjectClass) {
        //
        CSVParser parser = new CSVParserBuilder()
                .withEscapeChar(CSVParser.DEFAULT_ESCAPE_CHARACTER)
                .withQuoteChar('"')
                .withSeparator(separator)
                .build();
        CSVReader reader = null;
        try {
            reader = new CSVReaderBuilder(new FileReader(pathToFile))
                    .withCSVParser(parser)
                    .build();
            String[] header = reader.readNext();
            for (String[] line : reader) {
                List<IcAttribute> list = new LinkedList<>();
                IcUidAttribute uidAttribute = null;
                for (int column = 0; column < line.length; ++column) {
                    String[] values = line[column].split(multivaluedSeparator);
                    String name = header[column];
                    //
                    list.add(createAttribute(name, values));
                    //
                    if (name.equals(nameHeaderAttribute)) {
                        list.add(createAttribute(Name.NAME, values));
                    }
                    //
                    if (name.equals(uidHeaderAttribute)) {
                        list.add(createAttribute(Uid.NAME, values));
                        uidAttribute = new IcUidAttributeImpl(name, values[0], null);
                    }
                }
                IcConnectorObject object = defaultIcConnectorFacade.readObject(icConnectorInstance, config, icObjectClass, uidAttribute);
                if (object == null) {
                    defaultIcConnectorFacade.createObject(icConnectorInstance, config, icObjectClass, list);
                } else {
                    defaultIcConnectorFacade.updateObject(icConnectorInstance, config, icObjectClass, uidAttribute, list);
                }

            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Creates new attribute for system
     *
     * @param name   of attribute from header
     * @param values which we got from CSV
     * @return new Attribute
     */
    private IcAttributeImpl createAttribute(String name, String[] values) {
        IcAttributeImpl attribute = new IcAttributeImpl();
        attribute.setName(name);
        if (values.length > 1) {
            attribute.setMultiValue(true);
        }
        attribute.setValues(Arrays.asList(values));
        return attribute;
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
        params.add(PARAM_NAME_ATTRIBUTE);
        params.add(PARAM_UID_ATTRIBUTE);
        params.add(PARAM_ATTRIBUTE_SEPARATOR);
        params.add(PARAM_MULTIVALUED_SEPARATOR);
        return params;
    }

    /**
     * Schema id and path to file are retrieved for following usage.
     *
     * @param properties map of properties given
     */
    @Override
    public void init(Map<String, Object> properties) {
        LOG.debug("Start init");
        super.init(properties);
        schemaId = getParameterConverter().toUuid(properties, PARAM_SCHEMA_UUID);
        pathToFile = getParameterConverter().toString(properties, PARAM_CSV_FILE_PATH);
        nameHeaderAttribute = getParameterConverter().toString(properties, PARAM_NAME_ATTRIBUTE);
        uidHeaderAttribute = getParameterConverter().toString(properties, PARAM_UID_ATTRIBUTE);
        separator = getParameterConverter().toString(properties, PARAM_ATTRIBUTE_SEPARATOR).charAt(0);
        multivaluedSeparator = getParameterConverter().toString(properties, PARAM_MULTIVALUED_SEPARATOR);
    }
}
