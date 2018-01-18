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
    private final String USER_ID_GUEST = "guest";
    private final String PASSWORD_GUEST = "guest";
    private final String SESSION_ID_TEST_SESSION = "TEST-SESSION";
    private final String SUBJECT_CODE_ID = UUID.randomUUID().toString();
    private final String SUBJECT_NAMESPACE = "2.16.840.1.113883.2.1.4.3";
    private final String CONTENT_TYPE = "Content-Type";
    private final String CONTENT_TYPE_XML = "application/xml";
    private final String COMPOSITION_UID_PATH_IN_XML = "compositionCreateRestResponseData.compositionUid";

    private Launcher launcher;
    private String etherCISSessionId;
    private String resourcesRootPath;
    private String secretSessionId;
    private UUID ehrId;

    public QueryWithAqlSteps() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        //session id for ethercis sessions
        secretSessionId =
            I_SessionManager
                .SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE);
    }

    @Given("^The server is running$")
    public void theServerIsRunning() throws Throwable {
        startLauncher();
        //TODO: no way to assert server is running?
    }

    private void startLauncher() throws Exception {
        resourcesRootPath =
            getClass()
                .getClassLoader()
                .getResource(".")
                .getFile();

        launcher = new Launcher();
        launcher.start(new String[]{
            "-propertyFile", resourcesRootPath + "/config/services.properties",
            "-java_util_logging_config_file", resourcesRootPath + "/config/logging.properties",
            "-servicesFile", resourcesRootPath + "/config/services.xml",
            "-dialect", "EHRSCAPE",
            "-server_port", "8080",
            "-server_host", "localhost",
            "-debug", "true"
        });
    }

    @After
    public void cleanUp() throws Exception {
        launcher.stop();
    }

    @And("^The client system is logged into a server session$")
    public void theClientSystemIsLoggedIntoAServerSession() throws Throwable {

        Response response =
            given()
                .header(secretSessionId, SESSION_ID_TEST_SESSION)
            .when()
            .post("/rest/v1/session?username={username}&password={password}"
            ,USER_ID_GUEST, PASSWORD_GUEST);

        etherCISSessionId = response.getHeader(secretSessionId);

        Assert.assertNotNull(etherCISSessionId);
    }

    @And("^The openEHR template for the composition is available to the server$")
    public void theOpenEHRTemplateForTheCompositionIsAvailableToTheServer() throws Throwable {

        String optPath = resourcesRootPath + "/knowledge/operational_templates/prescription.opt";
        byte[] content = Files.readAllBytes(Paths.get(optPath));

        Response response =
            given()
                .header(secretSessionId, SESSION_ID_TEST_SESSION)
                .content(content)
            .when()
            .post("/rest/v1/template");

        assertEquals(response.statusCode(), 200);
    }

    @And("^An EHR is created$")
    public void anEHRIsCreated() throws Throwable {
        Response response =
            given()
                .header(secretSessionId, SESSION_ID_TEST_SESSION)
            .when()
            .post(
                "/rest/v1/ehr?subjectId={subjectId}&subjectNamespace={subjectNs}"
                , SUBJECT_CODE_ID, SUBJECT_NAMESPACE);
        assertNotNull(response);

        Map<String,String> responseContents = response.getBody().jsonPath().get("$");
        ehrId = UUID.fromString(responseContents.get("ehrId"));
        assertNotNull(ehrId);
    }

    @And("^A composition is persisted under the EHR$")
    public void aCompositionIsPersistedUnderTheEHR() throws Throwable {
        MakeCallToPersistComposition(true);
    }

    private void MakeCallToPersistComposition(boolean pPassEhrId) throws IOException {
        byte[] xmlContent =
            Files
            .readAllBytes(
                Paths.get(resourcesRootPath + "/test_data/Prescription.xml"));

        Response response =
            given()
                .header(secretSessionId, SESSION_ID_TEST_SESSION)
                .header(CONTENT_TYPE, CONTENT_TYPE_XML)
                .content(xmlContent)
            .when()
            .post("/rest/v1/composition?format=XML"
                + (pPassEhrId
                    ? "&ehrId=" + ehrId
                    : ""));
        assertNotNull(response);

        String xml = response.getBody().asString();
        String uid =
            XmlPath
                .from(xml)
                .getString(COMPOSITION_UID_PATH_IN_XML);
        assertNotNull(uid);
        Pattern pattern = Pattern.compile("[a-z0-9-]*::[a-z0-9.]*::[0-9]*");
        Matcher matcher = pattern.matcher(uid);
        assertTrue(matcher.matches());
    }

    @Then("^An AQL query should return data from the composition in the EHR$")
    public void anAQLQueryShouldReturnDataPersistedIntoTheComposition() throws Throwable {
        Response response = given()
            .header(secretSessionId, SESSION_ID_TEST_SESSION)
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
            "from EHR e [ehr_id/value='" + ehrId.toString() + "']" +
            "contains COMPOSITION a[openEHR-EHR-COMPOSITION.prescription.v1] ";
    }

    @And("^A composition is persisted under the EHR without an EHR identifier$")
    public void aCompositionIsPersistedUnderTheEHRWithoutAnEHRIdentifier() throws Throwable {
        MakeCallToPersistComposition(false);
    }
}
