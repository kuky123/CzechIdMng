package eu.bcvsolutions.idm.acc.scheduler.task.impl;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaObjectClassFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemFilter;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private static final String PARAM_SYSTEM_NAME = "Name of system";
    private static final String PARAM_ATTRIBUTE_SEPARATOR = "Attribute separator";
    private static final String PARAM_NAME_ATTRIBUTE = "Name attribute";
    private static final String PARAM_UID_ATTRIBUTE = "Uid attribute";
    private static final String PARAM_MULTIVALUED_SEPARATOR = "Separator of multivalued attributes";
    //
    private String DEFAULT_NOTIFY_PROPERTY = "requiredConfirmation";
    //
    private String systemName;
    private String pathToFile;
    private Character separator;
    private String nameHeaderAttribute;
    private String uidHeaderAttribute;
    private String multivaluedSeparator;

    @Autowired
    private SysSystemService sysSystemService;
    @Autowired
    private DefaultIcConnectorFacade defaultIcConnectorFacade;
    @Autowired
    private SysSchemaObjectClassService schemaObjectClassService;
    @Autowired
    private SysSchemaAttributeService schemaAttributeService;

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
        SysSystemFilter systemFilter = new SysSystemFilter();
        systemFilter.setCodeableIdentifier(systemName);
        List<SysSystemDto> systems = sysSystemService.find(systemFilter, null).getContent();
        //
        if (systems.isEmpty()) {
            throw new IllegalArgumentException("No system with name " + systemName + " found, check if name is right!");
        }
        SysSystemDto system = systems.get(0);
        //
        if (system == null) {
            throw new IllegalArgumentException("System is null!");
        }
        validateCSV(system);
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
        if (config instanceof IcConnectorConfigurationCzechIdMImpl) {
            config = getUnnoticedConfiguration((IcConnectorConfigurationCzechIdMImpl) sysSystemService.getConnectorConfiguration(system));
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
    private IcConnectorConfiguration getUnnoticedConfiguration(IcConnectorConfigurationCzechIdMImpl config) {
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
                    if (name.equals(IcAttributeInfo.ENABLE)) {
                        IcAttributeImpl attribute = new IcAttributeImpl();
                        attribute.setName(name);
                        attribute.setValues(Collections.singletonList(Boolean.valueOf(values[0])));
                        list.add(attribute);
                    } else {
                        list.add(createAttribute(name, values));
                    }
                    //
                    if (name.equals(nameHeaderAttribute)) {
                        list.add(createAttribute(Name.NAME, values));
                    }
                    //
                    if (name.equals(uidHeaderAttribute)) {
                        //list.add(createAttribute(Uid.NAME, values));
                        uidAttribute = new IcUidAttributeImpl(name, values[0], null);
                    }
                }
                IcConnectorObject object = defaultIcConnectorFacade.readObject(icConnectorInstance, config, icObjectClass, uidAttribute);
                if (object == null) {
                    defaultIcConnectorFacade.createObject(icConnectorInstance, config, icObjectClass, list);
                    increaseCounter();
                } else {
                    String[] temp = new String[1];
                    temp[0] = uidAttribute.getUidValue();
                    list.add(createAttribute(Uid.NAME, temp));
                    defaultIcConnectorFacade.updateObject(icConnectorInstance, config, icObjectClass, uidAttribute, list);
                    increaseCounter();
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
     * Validate CSV header and file - header have to be same as schema and cannot have attribute __PASSWORD__
     *
     * @param system need for schema
     * @return true if valid - no exception popped up
     */
    private boolean validateCSV(SysSystemDto system) {
        SysSchemaObjectClassFilter objectClassFilter = new SysSchemaObjectClassFilter();
        objectClassFilter.setSystemId(system.getId());
        objectClassFilter.setObjectClassName(IcObjectClassInfo.ACCOUNT);
        List<SysSchemaObjectClassDto> schemas = schemaObjectClassService.find(objectClassFilter, null).getContent();
        //
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("No schema with name __ACCOUNT__ were found for this system - " + systemName);
        }
        //
        SysSchemaObjectClassDto schema = schemas.get(0);
        //
        if (schema == null) {
            throw new IllegalArgumentException("Schema is null! Could not get schema from mapping!");
        }
        SysSchemaAttributeFilter attributeFilter = new SysSchemaAttributeFilter();
        attributeFilter.setObjectClassId(schema.getId());
        List<SysSchemaAttributeDto> attributes = schemaAttributeService.find(attributeFilter, null).getContent();
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException("No schema schema attributes were found for this system: " + systemName);
        }
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
            //
            boolean usernamePresent = false;
            // check header
            for (String head : header) {
                if (head.equals(nameHeaderAttribute)) {
                    usernamePresent = true;
                    continue;
                }
                // we don't want to import passwords
                if (head.equals(IcAttributeInfo.PASSWORD)) {
                    continue;
                }
                boolean toReturn = false;
                for (SysSchemaAttributeDto schemaAttribute : attributes) {
                    if (head.equals(schemaAttribute.getName())) {
                        toReturn = true;
                        break;
                    }
                }
                if (!toReturn) {
                    throw new IllegalArgumentException("Attribute " + head + " was not found in schema!");
                }
            }
            //
            if (!usernamePresent) {
                throw new IllegalArgumentException("Attribute " + nameHeaderAttribute + " was not found in schema!");
            }
            //
            int size = header.length;
            long lineNumber = 1;
            for (String[] line : reader) {
                if (line.length != size) {
                    throw new IllegalArgumentException("On line " + lineNumber + " was found some error!");
                }
                ++lineNumber;
            }
            //
            setCount(lineNumber - 1);
            setCounter(0L);
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
        attribute.setValues(convertEmptyStrings(values));
        return attribute;
    }

    private List<Object> convertEmptyStrings(String[] values) {
        List<Object> toReturn = new LinkedList<>();
        for (String value : values) {
            if (!value.isEmpty()) {
                toReturn.add(value);
            }
        }
        return toReturn;
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
        params.add(PARAM_SYSTEM_NAME);
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
        systemName = getParameterConverter().toString(properties, PARAM_SYSTEM_NAME);
        pathToFile = getParameterConverter().toString(properties, PARAM_CSV_FILE_PATH);
        nameHeaderAttribute = getParameterConverter().toString(properties, PARAM_NAME_ATTRIBUTE);
        uidHeaderAttribute = getParameterConverter().toString(properties, PARAM_UID_ATTRIBUTE);
        separator = getParameterConverter().toString(properties, PARAM_ATTRIBUTE_SEPARATOR).charAt(0);
        multivaluedSeparator = getParameterConverter().toString(properties, PARAM_MULTIVALUED_SEPARATOR);
    }
}
