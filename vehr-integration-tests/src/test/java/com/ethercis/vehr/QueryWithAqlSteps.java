package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Response;
import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QueryWithAqlSteps {

    private RestAPIBackgroundSteps bacgroundSteps;

    public QueryWithAqlSteps(RestAPIBackgroundSteps pBackgroundSteps){
        bacgroundSteps = pBackgroundSteps;
    }

    public QueryWithAqlSteps() {
    }

    @After
    public void cleanUp() throws Exception {
        bacgroundSteps.launcher.stop();
    }

    @And("^A composition is persisted under the EHR$")
    public void aCompositionIsPersistedUnderTheEHR() throws Throwable {
        MakeCallToPersistComposition(true);
    }

    private void MakeCallToPersistComposition(boolean pPassEhrId) throws IOException {
        byte[] xmlContent =
            Files
            .readAllBytes(
                Paths.get(bacgroundSteps.resourcesRootPath + "/test_data/Prescription.xml"));

        Response response =
            given()
                .header(bacgroundSteps.secretSessionId, bacgroundSteps.SESSION_ID_TEST_SESSION)
                .header(bacgroundSteps.CONTENT_TYPE, bacgroundSteps.CONTENT_TYPE_XML)
                .content(xmlContent)
            .when()
            .post("/rest/v1/composition?format=XML"
                + (pPassEhrId
                    ? "&ehrId=" + bacgroundSteps.ehrId
                    : ""));
        assertNotNull(response);

        String xml = response.getBody().asString();
        String uid =
            XmlPath
                .from(xml)
                .getString(bacgroundSteps.COMPOSITION_UID_PATH_IN_XML);
        assertNotNull(uid);
        Pattern pattern = Pattern.compile("[a-z0-9-]*::[a-z0-9.]*::[0-9]*");
        Matcher matcher = pattern.matcher(uid);
        assertTrue(matcher.matches());
    }

    @Then("^An AQL query should return data from the composition in the EHR$")
    public void anAQLQueryShouldReturnDataPersistedIntoTheComposition() throws Throwable {
        Response response = given()
            .header(bacgroundSteps.secretSessionId, bacgroundSteps.SESSION_ID_TEST_SESSION)
            .param("aql", buildAqlQuery())
            .get("/rest/v1/query");

        List<Map<String,String>> queryResults = response.getBody().jsonPath().getList("resultSet");
        assertNotNull(queryResults);
        assertTrue(queryResults.size() == 1);
        for(Map<String,String> row:queryResults){
            assertTrue(row.keySet().size() == 1 && row.keySet().contains("uid"));
        }
    }

    private String buildAqlQuery(){
        return "select a/uid/value " +
            "from EHR e [ehr_id/value='" + bacgroundSteps.ehrId.toString() + "']" +
            "contains COMPOSITION a[openEHR-EHR-COMPOSITION.prescription.v1] ";
    }

    @And("^A composition is persisted under the EHR without an EHR identifier$")
    public void aCompositionIsPersistedUnderTheEHRWithoutAnEHRIdentifier() throws Throwable {
        MakeCallToPersistComposition(false);
    }
}
