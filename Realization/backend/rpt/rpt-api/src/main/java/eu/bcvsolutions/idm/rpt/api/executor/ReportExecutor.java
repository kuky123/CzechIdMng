package eu.bcvsolutions.idm.rpt.api.executor;

import org.springframework.plugin.core.Plugin;

import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.service.Configurable;
import eu.bcvsolutions.idm.core.scheduler.api.service.LongRunningTaskExecutor;
import eu.bcvsolutions.idm.rpt.api.dto.RptReportDto;

/**
 * Generate report. Report executor implements LRT.
 * 
 * @author Radek Tomiška
 *
 */
public interface ReportExecutor extends Configurable, Plugin<String>, LongRunningTaskExecutor<RptReportDto> {
	
	static final String CONFIGURABLE_TYPE = "report-executor";

	@Override
	default String getConfigurableType() {
		return CONFIGURABLE_TYPE;
	}
	
	/**
	 * Generate report data
	 * 
	 * @param filter
	 * @return
	 */
	RptReportDto generate(RptReportDto report);
	
	/**
	 * Generate report for given event content
	 * 
	 * @param event
	 */
	void setEvent(EntityEvent<RptReportDto> event);
}
