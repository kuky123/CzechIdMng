package eu.bcvsolutions.idm.acc.bulk.action.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opencsv.CSVWriter;
import eu.bcvsolutions.idm.acc.domain.AccGroupPermission;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSchemaObjectClassFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemFilter;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.bulk.action.AbstractBulkAction;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.ic.api.*;
import eu.bcvsolutions.idm.ic.connid.domain.ConnIdIcConvertUtil;
import eu.bcvsolutions.idm.ic.service.impl.DefaultIcConnectorFacade;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * This BulkAction exports all items from system to CSV file
 *
 * @author Marek Klement
 */
@Component("exportSystemBulkAction")
@Description("Export all from system.")
public class ExportSystemBulkAction extends AbstractBulkAction<SysSystemDto, SysSystemFilter> {

	public static final String DIRECTORY = "Directory";
	//
	public static final String NAME = "system-export";
	private static final String CSV_EXTENSION = ".csv";
	private Character DEFAULT_LINE_SEPARATOR = ';';
	private Character DEFAULT_MULTIVALUED_SEPARATOR = ',';

	@Autowired
	private SysSystemService systemService;
	@Autowired
	private DefaultIcConnectorFacade defaultIcConnectorFacade;
	@Autowired
	private SysSchemaObjectClassService schemaObjectClassService;
	@Autowired
	private SysSchemaAttributeService schemaAttributeService;

	/**
	 * Process one of DTO in queue
	 *
	 * @param dto
	 * @return return operation result for current processed DTO
	 */
	@Override
	protected OperationResult processDto(SysSystemDto dto) {
		String file = processFile(dto);
		IcConnectorInstance icConnectorInstance = dto.getConnectorInstance();
		if (icConnectorInstance == null) {
			//todo change to ResultCode
			throw new IllegalArgumentException("icConnectorInstance is null! LRT was not able to get icConnectorInstance from system.");
		}
		//
		IcConnectorConfiguration config = systemService.getConnectorConfiguration(dto);
		if (config == null) {
			//todo change to ResultCode
			throw new IllegalArgumentException("configuration is null! LRT was not able to get configuration of system.");
		}
		//
		//__ACCOUNT__
		IcObjectClass icObjectClass = ConnIdIcConvertUtil.convertConnIdObjectClass(ObjectClass.ACCOUNT);
		if (icObjectClass == null) {
			throw new IllegalArgumentException("ConnIdIcConvertUtil failed to convert ObjectClass.ACCOUNT.");
		}
		List<SysSchemaAttributeDto> schemaAttributes = getSchemaAttributes(dto, icObjectClass);
		LinkedList header = new LinkedList();
		schemaAttributes.forEach(attribute -> header.add(attribute.getName()));
		OperationResult result;
		if (writeIntoFile(icConnectorInstance, config, icObjectClass, file, header)) {
			result = new OperationResult.Builder(OperationState.EXECUTED).build();
		} else {
			result = new OperationResult.Builder(OperationState.EXCEPTION).build();
		}
		return result;
	}

	private List<SysSchemaAttributeDto> getSchemaAttributes(SysSystemDto system, IcObjectClass icObjectClass) {
		SysSchemaObjectClassFilter objectClassFilter = new SysSchemaObjectClassFilter();
		objectClassFilter.setSystemId(system.getId());
		objectClassFilter.setObjectClassName(icObjectClass.getType());
		List<SysSchemaObjectClassDto> schemas = schemaObjectClassService
				.find(objectClassFilter, null)
				.getContent();
		//
		if (schemas.isEmpty()) {
			throw new ResultCodeException(AccResultCode.CONNECTOR_SCHEMA_FOR_SYSTEM_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		//
		SysSchemaObjectClassDto schema = schemas.get(0);
		SysSchemaAttributeFilter attributeFilter = new SysSchemaAttributeFilter();
		attributeFilter.setObjectClassId(schema.getId());
		List<SysSchemaAttributeDto> attributes = schemaAttributeService
				.find(attributeFilter, null)
				.getContent();
		if (attributes.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_SCHEMA_ATTRIBUTES_NOT_FOUND,
					ImmutableMap.of("schema", schema.getObjectClassName(),
							"system", system.getName()));
		}
		return attributes;
	}

	/**
	 * Find all connector object from system. Than it writes all data into file.
	 *
	 * @param icConnectorInstance instance of connector
	 * @param config              configuration of system
	 * @param icObjectClass       object class to be used
	 * @return true if everything went right
	 */
	private boolean writeIntoFile(IcConnectorInstance icConnectorInstance, IcConnectorConfiguration config, IcObjectClass icObjectClass, String pathToFile, LinkedList<String> header) {
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter(pathToFile, true), DEFAULT_LINE_SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, System.lineSeparator());
			//
			CSVWriter finalWriter = writer;
			finalWriter.writeNext(header.toArray(new String[header.size()]));
			defaultIcConnectorFacade.search(icConnectorInstance, config, icObjectClass, null, connectorObject -> handleConnectorObject(connectorObject, finalWriter, header));
			//
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (writer != null) {
				try {
					writer.flush();
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
	private boolean handleConnectorObject(IcConnectorObject connectorObject, CSVWriter writer, LinkedList<String> header) {
		LinkedList<String> newLine = new LinkedList<>();
		for (String name : header) {
			IcAttribute attribute = connectorObject.getAttributeByName(name);
			if (attribute == null) {
				newLine.add(""); //todo null??
			} else {
				newLine.add(createLine(attribute.getValues()));
			}
		}
		writer.writeNext(newLine.toArray(new String[newLine.size()]));
		return true;
	}

	private String createLine(List<Object> values) {
		StringBuilder stringBuilder = new StringBuilder();
		boolean begin = false;
		if (values == null) return ""; //todo null??
		for (Object value : values) {
			if (begin) {
				stringBuilder.append(DEFAULT_MULTIVALUED_SEPARATOR).append(value.toString());
			} else {
				stringBuilder.append(value.toString());
				begin = true;
			}
		}
		return stringBuilder.toString();
	}

	private String processFile(SysSystemDto dto) {
		String directory = getDirectory();
		if (directory == null) {
			throw new ResultCodeException(AccResultCode.EXPORT_NULL_DIRECTORY_PATH,
					ImmutableMap.of("directory", directory));
		}
		File file = new File(directory);
		if (file.exists() && !file.isDirectory()) {
			throw new ResultCodeException(AccResultCode.EXPORT_NAME_IS_NOT_DIRECTORY,
					ImmutableMap.of("directory", directory));
		}
		return createNewFile(directory, dto);
	}

	private String createNewFile(String directory, SysSystemDto dto) {
		int milis = DateTime.now().getMillisOfDay();
		String now = DateTime.now().toLocalDate().toString();

		String path = directory + File.separator + dto.getName() + File.separator + dto.getName() + now + "-" + milis + CSV_EXTENSION;
		// actual create file in system
		File file = new File(path);

		file.getParentFile().mkdirs();
		try {
			if (!file.createNewFile()) {
				throw new ResultCodeException(AccResultCode.EXPORT_CANT_CREATE_FILE_IN_DIRECTORY,
						ImmutableMap.of("filename", path,
								"directory", directory));
			}
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return path;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(AccGroupPermission.SYSTEM_READ, AccGroupPermission.SYSTEM_UPDATE);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder() + 200;
	}

	@Override
	public ReadWriteDtoService<SysSystemDto, SysSystemFilter> getService() {
		return systemService;
	}

	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		List<IdmFormAttributeDto> attributes = super.getFormAttributes();
		attributes.add(createAttributeDirectory());
		return attributes;
	}

	private String getDirectory() {
		return this.getParameterConverter().toString(getProperties(), DIRECTORY);
	}

	private IdmFormAttributeDto createAttributeDirectory() {
		return new IdmFormAttributeDto(
				DIRECTORY,
				DIRECTORY,
				PersistentType.TEXT);
	}
}
