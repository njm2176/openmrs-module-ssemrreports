/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ssemrreports.reporting.library.data.evaluator;

import java.util.Date;
import java.util.Map;

import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.evaluator.PersonDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.openmrs.module.ssemrreports.reporting.library.data.definition.RepeatVLResultDataDefinition;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Evaluates Repeat VLResult Data Definition
 */
@Handler(supports = RepeatVLResultDataDefinition.class, order = 50)
public class RepeatVLResultDataEvaluator implements PersonDataEvaluator {
	
	@Autowired
	private EvaluationService evaluationService;
	
	public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context)
	        throws EvaluationException {
		EvaluatedPersonData c = new EvaluatedPersonData(definition, context);
		
		String qry = "WITH LatestHVL AS ( "
		        + "    SELECT *, ROW_NUMBER() OVER(PARTITION BY client_id ORDER BY encounter_datetime DESC) as rn "
		        + "    FROM ssemr_etl.ssemr_flat_encounter_high_viral_load " + "    WHERE encounter_datetime <= :endDate "
		        + ") " + "SELECT " + "    hvl.client_id, " + "    CASE "
		        + "        WHEN hvl.date_of_collection_of_repeat_vl IS NOT NULL AND hvl.repeat_vl_result_date IS NULL "
		        + "        THEN 'Pending Result' " + "        ELSE " + "            CASE "
		        + "                WHEN hvl.repeat_vl_results = 'Viral Load Value' THEN CAST(hvl.repeat_vl_value AS CHAR) "
		        + "                WHEN hvl.repeat_vl_results = 'Below Detectable (BDL)' THEN 'BDL' "
		        + "                ELSE hvl.repeat_vl_results " + "            END " + "    END AS final_status "
		        + "FROM LatestHVL hvl " + "WHERE hvl.rn = 1";
		
		SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
		queryBuilder.append(qry);
		Date startDate = (Date) context.getParameterValue("startDate");
		Date endDate = (Date) context.getParameterValue("endDate");
		queryBuilder.addParameter("endDate", endDate);
		queryBuilder.addParameter("startDate", startDate);
		
		Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
		c.setData(data);
		return c;
	}
}
