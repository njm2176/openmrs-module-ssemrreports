package org.openmrs.module.ssemrreports.reporting.library.reports;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.openmrs.module.reporting.ReportingException;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.ssemrreports.manager.SsemrDataExportManager;
import org.openmrs.module.ssemrreports.reporting.library.datasets.AftInitiationsDatasetDefinition;
import org.openmrs.module.ssemrreports.reporting.utils.constants.reports.shared.SharedReportConstants;
import org.openmrs.module.ssemrreports.reporting.utils.constants.templates.shared.SharedTemplatesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SetupAftInitiationsRegister extends SsemrDataExportManager {
	
	private final AftInitiationsDatasetDefinition aftInitiationsDatasetDefinition;
	
	@Autowired
	public SetupAftInitiationsRegister(AftInitiationsDatasetDefinition aftInitiationsDatasetDefinition) {
		this.aftInitiationsDatasetDefinition = aftInitiationsDatasetDefinition;
	}
	
	@Override
	public String getExcelDesignUuid() {
		return SharedTemplatesConstants.AFT_INITIATIONS_TEMPLATE_UUID;
	}
	
	@Override
	public String getUuid() {
		return SharedReportConstants.AFT_INITIATIONS_REPORT_UUID;
	}
	
	@Override
	public String getName() {
		return "AFT Initiations Register";
	}
	
	@Override
	public String getDescription() {
		return "Listing of AFT Initiations from ETL";
	}
	
	@Override
	public ReportDefinition constructReportDefinition() {
		ReportDefinition rd = new ReportDefinition();
		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		rd.addDataSetDefinition("AFT_INITIATIONS",
		    aftInitiationsDatasetDefinition.constructAftInitiationsDatasetDefinition(), null);
		return rd;
	}
	
	@Override
	public String getVersion() {
		return "1.0-SNAPSHOT";
	}
	
	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		ReportDesign reportDesign = null;
		try {
			reportDesign = createXlsReportDesign(reportDefinition, "aft_initiations.xls",
			    "Listing of AFT Initiations from ETL", getExcelDesignUuid(), null);
			Properties props = new Properties();
			props.put("repeatingSections", "sheet:1,row:2,dataset:AFT_INITIATIONS");
			reportDesign.setProperties(props);
		}
		catch (IOException e) {
			throw new ReportingException(e.toString());
		}
		
		return Arrays.asList(reportDesign);
	}
}
