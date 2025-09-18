# OpenMRS SSEMR Reports Module - AI Coding Agent Guide

## Project Overview
This is an **OpenMRS module** for South Sudan EMR (SSEMR) reporting. It extends OpenMRS's reporting framework to generate HIV/AIDS care reports with Excel templates, ETL processing via Mamba, and web UI integration.

## Architecture & Patterns

### OpenMRS Module Structure
- **API module** (`api/`): Core business logic, datasets, cohorts, report managers
- **OMOD module** (`omod/`): Web controllers, UI pages, configuration
- **Parent POM**: Uses `maven-parent-openmrs-module` (v1.1.1), targets OpenMRS 2.6.1
- **Spring Integration**: Auto-scans `org.openmrs.module.ssemrreports` for `@Component` beans

### Report Architecture Pattern
Every report follows this 3-layer pattern, but differs based on report type:

## Report Types

### 1. Line List Reports (Patient Listings)
Display individual patient records with detailed columns. Use `PatientDataSetDefinition`:

```java
@Component
public class IITDatasetDefinition extends SsemrBaseDataSet {
    public DataSetDefinition constructIITDatasetDefinition() {
        PatientDataSetDefinition dsd = new PatientDataSetDefinition();
        dsd.setName("IIT");
        // Add individual patient columns
        dsd.addColumn("Name", nameDef, "");
        dsd.addColumn("Age", new CustomAgeDataDefinition(), "", null);
        return dsd;
    }
}
```

**Examples**: IIT Register, All Clients, Appointments Due, RTT Register, TPT Register
**Pattern**: Names contain "Register", "List", or show individual patient data
**Templates**: Map to Excel with `repeatingSections` starting from a data row

### 2. Indicator/Aggregate Reports (Computed Totals)
Display aggregated counts by dimensions (age, gender). Use `CohortIndicatorDataSetDefinition`:

```java
@Component
public class MerIndicatorsDatasetDefinition extends SsemrBaseDataSet {
    public DataSetDefinition getTxCurrDataset() {
        CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
        dsd.addDimension("gender", map(dimension.gender(), ""));
        dsd.addDimension("age", map(dimension.age(), "effectiveDate=${endDate}"));
        // Add indicator rows with age/gender disaggregation
        addRow(dsd, "ALL", "Total on ART", indicatorMapped, ageGenderColumns);
        return dsd;
    }
}
```

**Examples**: MER TX_CURR, TX_NEW, ART Monthly, HTS Report
**Pattern**: Names contain "Indicators", "Dataset", show aggregate counts
**Templates**: Map to specific cells in pre-formatted Excel dashboards

## Common 3-Layer Structure

1. **Report Setup** (`reporting/library/reports/Setup*.java`):
   ```java
   @Component
   public class SetupIITRegister extends SsemrDataExportManager {
       @Override
       public ReportDefinition constructReportDefinition() {
           // Maps dataset to cohort, adds parameters
       }
   }
   ```

2. **Dataset Definition** (`reporting/library/datasets/*DatasetDefinition.java`):
   - Line lists extend `SsemrBaseDataSet`, return `PatientDataSetDefinition`
   - Indicators extend `SsemrBaseDataSet`, return `CohortIndicatorDataSetDefinition`

3. **Cohort Queries** (`reporting/library/cohorts/*CohortQueries.java`):
   ```java
   @Component
   public class BaseCohortQueries {
       // SQL-based patient cohort definitions
   }
   ```

### Excel Template Integration
- **Templates**: Store `.xls` files in `api/src/main/resources/`
- **Template Mapping**: Reports map to Excel via `repeatingSections` property:
  ```java
  Properties props = new Properties();
  props.put("repeatingSections", "sheet:1,row:4,dataset:IIT");
  ```
- **UUID Constants**: Excel design UUIDs defined in `SharedTemplatesConstants`

### ETL with Mamba Framework
- **Stored Procedures**: Located in `api/src/main/resources/mamba/`
- **ETL Pattern**: Creates `mamba_dim_*` tables from OpenMRS data
- **Execution**: Run via `sp_mamba_etl_execute()` stored procedure
- **Report Metadata**: JSON-driven report definitions in `sp_mamba_dim_concept_metadata_insert()`

## Development Workflows

### Building
```bash
mvn clean package                    # Builds .omod file
mvn package -P deploy-web           # Deploy web resources without reinstall
```

### Testing
- **Unit Tests**: Use Mockito, extend standard JUnit patterns
- **Integration Tests**: Extend `BaseModuleContextSensitiveTest` for OpenMRS context
- **Example**:
  ```java
  @Mock SsemrReportsDao dao;
  @InjectMocks SsemrReportsServiceImpl service;
  ```

### Adding New Reports

#### For Line List Reports:
1. Create Excel template in `api/src/main/resources/` with column headers
2. Add UUID constants in `SharedTemplatesConstants` and `SharedReportConstants`
3. Implement `*DatasetDefinition` extending `SsemrBaseDataSet`:
   - Return `PatientDataSetDefinition`
   - Use `dsd.addColumn()` for each patient data field
   - Include custom `*DataDefinition` classes for complex data
4. Create `Setup*Register` class extending `SsemrDataExportManager`
5. Set Excel `repeatingSections` property: `"sheet:1,row:4,dataset:DATASETNAME"`

#### For Indicator/Aggregate Reports:
1. Create Excel template with pre-defined cells for indicators
2. Add UUID constants in `SharedTemplatesConstants` and `SharedReportConstants`
3. Implement `*DatasetDefinition` extending `SsemrBaseDataSet`:
   - Return `CohortIndicatorDataSetDefinition`
   - Add dimensions (age, gender) with `addDimension()`
   - Use `addRow()` for each indicator with disaggregation columns
4. Create `Setup*Report` class extending `SsemrDataExportManager`
5. Map indicators to specific Excel cells (no `repeatingSections`)

## Key Conventions

### Naming Patterns
- **Line List Reports**: `Setup[ReportName]Register` (e.g., `SetupIITRegister`)
- **Indicator Reports**: `Setup[ReportName]Report` (e.g., `SetupMerTxCurrTxNewIndicatorsReport`)
- **Datasets**: `[ReportName]DatasetDefinition`
- **Templates**: `[report_name].xls` (lowercase with underscores)
- **UUIDs**: Follow format `[REPORT]_[TYPE]_UUID` in constants

### Data Definitions
- **Line Lists**: Use `PersonDataDefinition`, `PatientDataDefinition` for individual columns
- **Indicators**: Use `CohortIndicator` with dimension disaggregation
- **Custom evaluators**: Implement `PersonDataEvaluator` with `@Handler` annotation
- **Caching**: Use `@Caching(strategy = ConfigurationPropertyCachingStrategy.class)`
- **SQL patterns**: Use `SqlQueryBuilder` for parameterized queries

### Spring Configuration
- **Component Scanning**: All classes in package auto-detected with `@Component`
- **Service Layer**: Wrapped in transaction proxies via Spring AOP
- **Context Access**: Use `Context.getService(SsemrReportsService.class)`

## Critical Dependencies
- **OpenMRS Reporting**: `org.openmrs.module:reporting-api` (v1.25.0)
- **Mamba ETL**: Custom stored procedures for data warehouse functionality
- **UI Framework**: `uiframework-api`, `appframework-api` for web integration
- **Excel Rendering**: Uses OpenMRS `ExcelTemplateRenderer`

## File Locations to Remember
- **Module Config**: `omod/src/main/resources/config.xml`
- **Spring Beans**: `api/src/main/resources/moduleApplicationContext.xml`
- **Web Pages**: `omod/src/main/webapp/pages/`
- **SQL Queries**: `api/src/main/java/org/openmrs/module/ssemrreports/reporting/library/queries/`

## Common Gotchas
- Always extend appropriate base classes (`SsemrDataExportManager`, `SsemrBaseDataSet`)
- Report UUIDs must be unique across the entire OpenMRS installation
- Excel template column mapping is case-sensitive
- Mamba ETL requires proper JSON format in metadata procedures
- All dates should use OpenMRS parameter pattern: `${endDate+23h}` for end-of-day

When implementing new features, follow existing patterns in the codebase and maintain consistency with OpenMRS reporting framework conventions.