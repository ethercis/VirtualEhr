package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Response;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import gherkin.formatter.model.DataTableRow;
import org.apache.xmlbeans.XmlException;
import org.junit.Assert;
import org.openehr.schemas.v1.TemplateDocument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.jayway.restassured.RestAssured.given;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RestAPIBackgroundSteps {
    public static final int STATUS_CODE_OK = 200;
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
    private String DEFAULT_OPT_DIR = "knowledge/operational_templates";

    public static final String TEST_DATA_DIR = "test_data";
    public static final String COMPOSITION_ENDPOINT = "/rest/v1/composition";
    private Pattern uidPattern;
    private Map<String, List<String>> persistedCompositions = new HashMap<>();
    private List<AbstractMap.SimpleEntry<String, String>> _persistedCompositionsAsXml;
    private DataTable _testCompositions;

    public RestAPIBackgroundSteps(){
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        //session id for ethercis sessions
        secretSessionId =
            I_SessionManager
                .SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE);

        uidPattern = Pattern.compile("[a-z0-9-]*::[a-z0-9.]*::[0-9]*");
    }



    @Given("^The server is running$")
    public void theServerIsRunning() throws Throwable {
        startLauncher();
        //TODO: no way to assert server is running?
    }

    private void startLauncher() throws Exception {
        resourcesRootPath =
                Paths.get(getClass()
                        .getClassLoader()
                        .getResource(".")
                        .toURI()).toString() + "/";

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
        postTemplateToServer(DEFAULT_OPT_DIR, optFileName);
    }

    public void postTemplateToServer(String operationalTemplatesDir, String optFileName) {
        try {
            String optPath = resourcesRootPath + operationalTemplatesDir + "/" + optFileName;
            byte[] content = Files.readAllBytes(Paths.get(optPath));
            byte[] utfBytes = new String(content, "UTF-8").getBytes("UTF-8");
            try {
                TemplateDocument.Factory.parse(new ByteArrayInputStream(content));
            } catch (XmlException e) {
                e.printStackTrace();
            }

            Response response =
                given()
                    .header(secretSessionId, SESSION_ID_TEST_SESSION)
                    .content(utfBytes)
                    .when()
                    .post("/rest/v1/template");

            assertEquals(response.statusCode(), 200);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
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



    private  String getCompositionAsXML(String uid) {
        try {
            CompositionAPISteps compositionAPISteps = new CompositionAPISteps(this);
            compositionAPISteps.setCompositionUid(uid);
            return compositionAPISteps.getXmlStringFromRestAPI();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void postXMLComposition(boolean pPassEhrId, String pCompositionPath, CompositionFormat pFormat) {
        try{
            byte[] xmlContent =
                Files
                    .readAllBytes(
                        Paths.get(pCompositionPath));

            Response response =
                given()
                    .header(secretSessionId, SESSION_ID_TEST_SESSION)
                    .header(CONTENT_TYPE, CONTENT_TYPE_XML)
                    .content(xmlContent)
                    .when()
                    .post("/rest/v1/composition?format="+pFormat
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

            assertUidFormat(uid);
        }catch(IOException e){
            throw new RuntimeException("could not commit composition to server", e);
        }
    }

    public void assertUidFormat(String uid) {
        Matcher uidMatcher = uidPattern.matcher(uid);
        assertTrue(uidMatcher.matches());
    }

    public String postFlatJsonComposition(String pCompositionFilePath, String pTemplateId) throws IOException {
        Path jsonFilePath =
            Paths
                .get(pCompositionFilePath);
        byte[] fileContents = Files.readAllBytes(jsonFilePath);

        Response commitCompositionResponse =
            given()
                .header(secretSessionId, SESSION_ID_TEST_SESSION)
                .header(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .content(fileContents)
                .when()
                .post(COMPOSITION_ENDPOINT + "?format=FLAT&templateId=" + pTemplateId)
                .then()
                .extract()
                .response();
        int statusCode = commitCompositionResponse.statusCode();
        assertEquals(statusCode,200);

        return commitCompositionResponse.body().jsonPath().getString("compositionUid");
    }

    public List<Map<String, String>> extractAqlResults(Response response) {
        return response
            .getBody()
            .jsonPath().getList("resultSet");
    }

    public Response getAqlResponse(String query) {
        return given()
            .header(secretSessionId, SESSION_ID_TEST_SESSION)
            .param("aql", query)
            .get("/rest/v1/query");
    }
}
