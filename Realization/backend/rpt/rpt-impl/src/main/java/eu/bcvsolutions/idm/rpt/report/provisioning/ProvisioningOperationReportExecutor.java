package eu.bcvsolutions.idm.rpt.report.provisioning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.acc.dto.SysProvisioningOperationDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningOperationFilter;
import eu.bcvsolutions.idm.acc.eav.domain.AccFaceType;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningOperation_;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningOperationService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.security.api.domain.Enabled;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.rpt.RptModuleDescriptor;
import eu.bcvsolutions.idm.rpt.api.dto.RptReportDto;
import eu.bcvsolutions.idm.rpt.api.exception.ReportGenerateException;
import eu.bcvsolutions.idm.rpt.api.executor.AbstractReportExecutor;
import eu.bcvsolutions.idm.rpt.dto.RptProvisioningOperationDto;

/**
 * List of active provisioning operations in queue
 * 
 * @author Radek Tomiška
 *
 */
@Component
@Enabled(RptModuleDescriptor.MODULE_ID)
@Description("Provisioning operations")
public class ProvisioningOperationReportExecutor extends AbstractReportExecutor {
	
	public static final String REPORT_NAME = "provisioning-operation-report"; // report ~ executor name
	public static final String PARAMETER_SYSTEM = "system"; // parameter name in url
	//
	@Autowired private SysProvisioningOperationService provisioningOperationService;
 
	/**
	 * Report ~ executor name
	 */
	@Override
	public String getName() {
		return REPORT_NAME;
	}
	
	/**IdmProvisioningOperationDto
	 * Filter form attributes:
	 * - enabled / disabled identities
	 */
	@Override
	public List<IdmFormAttributeDto> getFormAttributes() {
		IdmFormAttributeDto system = new IdmFormAttributeDto(
				PARAMETER_SYSTEM,
				"System", 
				PersistentType.UUID);
		system.setFaceType(AccFaceType.SYSTEM_SELECT);
		//
		return Lists.newArrayList(system);
	}
 
	@Override
	protected IdmAttachmentDto generateData(RptReportDto report) {
		File temp = null;
		FileOutputStream outputStream = null;
		try {
			// prepare temp file for json stream
			temp = getAttachmentManager().createTempFile();
	        outputStream = new FileOutputStream(temp);
	        // write into json stream
			JsonGenerator jGenerator = getMapper().getFactory().createGenerator(outputStream, JsonEncoding.UTF8);
			try {
				// json will be array of identities
				jGenerator.writeStartArray();		
				// form instance has useful methods to transform form values
				IdmFormInstanceDto formInstance = new IdmFormInstanceDto(report, getFormDefinition(), report.getFilter());
				Pageable pageable = new PageRequest(0, 100, new Sort(Direction.ASC, SysProvisioningOperation_.created.getName()));
				SysProvisioningOperationFilter filterOperations = new SysProvisioningOperationFilter();
				Serializable systemId = formInstance.toSinglePersistentValue(PARAMETER_SYSTEM);
				if (systemId != null) {
					filterOperations.setSystemId(getParameterConverter().toEntityUuid(systemId.toString(), SysSystemDto.class));
				}
				//
				counter = 0L;
				do {
					Page<SysProvisioningOperationDto> operations = provisioningOperationService.find(filterOperations, pageable, IdmBasePermission.READ);
					if (count == null) {
						count = operations.getTotalElements();
					}
					boolean canContinue = true;
					for (Iterator<SysProvisioningOperationDto> i = operations.iterator(); i.hasNext() && canContinue;) {
						// transfer SysProvisioningOperationDto to IdmProvisioningOperationDto and  write IdmProvisioningOperationDto into json
						
						RptProvisioningOperationDto idmProvisioningOperationDto = transferSysProvisioningOperationDto(i.next());
						getMapper().writeValue(jGenerator,idmProvisioningOperationDto );		
						// supports cancel report generating (report extends long running task)
						++counter;
						canContinue = updateState();
					}		
					// iterate while next page of identities is available
					pageable = operations.hasNext() && canContinue ? operations.nextPageable() : null;
				} while (pageable != null);
				//
				// close array of identities
				jGenerator.writeEndArray();
			} finally {
				// close json stream
				jGenerator.close();
			}
			// save create temp file with array of identities in json as attachment
			return createAttachment(report, new FileInputStream(temp));
		} catch (IOException ex) {
			throw new ReportGenerateException(report.getName(), ex);
		} finally {
			IOUtils.closeQuietly(outputStream); // just for sure - jGenerator should close stream itself
			FileUtils.deleteQuietly(temp);
		}
	}
	
	private RptProvisioningOperationDto transferSysProvisioningOperationDto(SysProvisioningOperationDto operation) {
		RptProvisioningOperationDto rptOperationDto = new RptProvisioningOperationDto(operation);
		SysSystemDto system = DtoUtils.getEmbedded(operation, SysProvisioningOperation_.system);
		rptOperationDto.setSystemEntityType(operation.getEntityType().name());
		rptOperationDto.setSystem(system.getCode());
		rptOperationDto.setSystemEntityUid(operation.getSystemEntityUid());
		rptOperationDto.setOperationType(operation.getOperationType());
		rptOperationDto.setEntityIdentifier(operation.getEntityIdentifier());
		if (operation.getProvisioningContext() != null && operation.getProvisioningContext().getConnectorObject() != null) {
			// transform values to string representations
			for (int i = 0; i < operation.getProvisioningContext().getConnectorObject().getAttributes().size(); i++) {
				IcAttribute attr = operation.getProvisioningContext().getConnectorObject().getAttributes().get(i);
				//
				// Value can be multivalued or single valued
				String valueAsString = "";
				if (attr.isMultiValue()) {
					valueAsString = String.valueOf(StringUtils.join(attr.getValues(), ","));
				} else {
					valueAsString = String.valueOf(attr.getValue());
				}
				rptOperationDto.getProvisioningValues().put(
						attr.getName(),
						valueAsString);
			}
		}
		return rptOperationDto;
	}

}