/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ethercis.query;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by christian on 10/14/2016.
 */
public class QueryServiceTest extends TestCase {

    String failed = "{ \"sql\":\"select\n" +
            "  \"ehr\".\"comp_expand\".\"entry\"->(select json_object_keys(\"ehr\".\"comp_expand\".\"entry\"::json)) #>> '{/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1],0,/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1],0,/data[at0001],/items[at0002],0,/value,value}' as \"cause\",\n" +
            "  \"ehr\".\"comp_expand\".\"entry\"->(select json_object_keys(\"ehr\".\"comp_expand\".\"entry\"::json)) #>> '{/content[openEHR-EHR-SECTION.allergies_adverse_reactions_rcp.v1],0,/items[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1],0,/data[at0001],/items[at0009],0,/items[at0011],0,/value,value}' as \"reaction\",\n" +
            "  \"ehr\".\"comp_expand\".\"composition_id\"||'::'||'test-server'||'::'||(\n" +
            "    select (count(*) + 1)\n" +
            "    from \"ehr\".\"composition_history\"\n" +
            "    where \"ehr\".\"composition_history\".\"id\" = '052541fd-8c32-4ef6-a2f1-69252b47b789'\n" +
            "  ) as \"uid\"\n" +
            "from \"ehr\".\"comp_expand\"\n" +
            "where (\n" +
            "  (\"ehr\".\"comp_expand\".\"composition_name\"='Adverse reaction list')\n" +
            "  and (\"ehr\".\"comp_expand\".\"subject_externalref_id_value\"='9999999000')\n" +
            ");\"}";

    public void testExtractQuery() throws Exception {

//        String json = "{\"sql\":\"SELECT comp_expand.composition_id as uid,  jsonb_array_elements((comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1]}')::jsonb),  comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1],0, /ism_transition/careflow_step,/value, value }' as step_id,  comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1],0, /ism_transition/careflow_step,/value, definingCode,codeString }' as step_code,  comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1],0, /ism_transition/current_state,/value, value }' as state_id,  (comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1],0, /ism_transition/current_state,/value, definingCode,codeString }')::INT as state_code,  comp_expand.entry #>> '{/composition[openEHR-EHR-COMPOSITION.medication_action.v0 and name/value=''Medication action''], /content[openEHR-EHR-ACTION.medication.v1],0, /description[openEHR-EHR-ITEM_TREE.medication.v1],/items[at0001],0,/value,value }' as medication_name FROM ehr.comp_expand WHERE comp_expand.composition_id = '1abd5476-3ab8-481f-b33c-2fcf9ddb2ddd';\"   }      ";

        Map<String, String> decoded = QueryService.extractQuery(failed);

        assertNotNull(decoded.get("sql"));

    }

    String aqlStuff = "{\"aql\" : \"select a/uid/value as uid, \n" +
            "a/composer/name as author, \n" +
            "a/context/start_time/value as date_created, \n" +
            "b_a/data[at0001]/items[at0002]/value/value as cause, \n" +
            "b_a/data[at0001]/items[at0002]/value/defining_code/code_string as cause_code, \n" +
            "b_a/data[at0001]/items[at0002]/value/defining_code/terminology_id/value as cause_terminology, \n" +
            "b_a/data[at0001]/items[at0009]/items[at0011]/value/value as reaction, \n" +
            "b_a/data[at0001]/items[at0009]/items[at0011]/value/defining_code/codeString as reaction_code, \n" +
            "b_a/data[at0001]/items[at0009]/items[at0011]/value/terminology_id/value as reaction_terminology \n" +
            "from EHR e [ehr_id/value = 'bb872277-40c4-44fb-8691-530be31e1ee9'] \n" +
            "contains COMPOSITION a[openEHR-EHR-COMPOSITION.adverse_reaction_list.v1]\n" +
            " contains EVALUATION b_a[openEHR-EHR-EVALUATION.adverse_reaction_risk.v1]\n" +
            " where a/name/value='Adverse reaction list'"+
            "\"}";

    public void testExtractAql() throws Exception {
        Map<String, String> decoded = QueryService.extractQuery(aqlStuff);

        assertNotNull(decoded.get("aql"));
    }
}