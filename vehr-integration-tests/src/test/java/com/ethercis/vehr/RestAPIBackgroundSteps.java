package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import org.junit.Assert;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RestAPIBackgroundSteps {
    protected final String USER_ID_GUEST = "guest";
    protected final String PASSWORD_GUEST = "guest";
    protected final String SESSION_ID_TEST_SESSION = "TEST-SESSION";
    protected final String SUBJECT_CODE_ID = UUID.randomUUID().toString();
    protected final String SUBJECT_NAMESPACE = "2.16.840.1.113883.2.1.4.3";
    protected final String CONTENT_TYPE = "Content-Type";
    protected final String ACCEPT = "Accept";
    protected final String CONTENT_TYPE_XML = "application/xml";
    protected final String CONTENT_TYPE_JSON = "application/json";
    protected final String COMPOSITION_UID_PATH_IN_XML = "compositionCreateRestResponseData.compositionUid";

    protected Launcher launcher;
    protected String etherCISSessionId;
    protected String resourcesRootPath;
    protected String secretSessionId;
    protected UUID ehrId;

    public RestAPIBackgroundSteps(){
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
            "-propertyFile", resourcesRootPath + "config/services.properties",
            "-java_util_logging_config_file", resourcesRootPath + "config/logging.properties",
            "-servicesFile", resourcesRootPath + "config/services.xml",
            "-dialect", "EHRSCAPE",
            "-server_port", "8080",
            "-server_host", "localhost",
            "-debug", "true"
        });
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

    @And("^The openEHR template ([a-zA-Z \\-\\.0-9]+\\.opt) for the composition is available to the server$")
    public void theOpenEHRTemplateForTheCompositionIsAvailableToTheServer(String optFileName) throws Throwable {

        String optPath = resourcesRootPath + "knowledge/operational_templates/" + optFileName;
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
}
