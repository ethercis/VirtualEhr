package com.ethercis.vehr;

import com.jayway.restassured.response.Response;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.ethercis.vehr.RestAPIBackgroundSteps.COMPOSITION_ENDPOINT;
import static com.ethercis.vehr.RestAPIBackgroundSteps.STATUS_CODE_OK;
import static com.ethercis.vehr.RestAPIBackgroundSteps.TEST_DATA_DIR;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompositionAPISteps {

    private static final String FORMAT_RAW = "RAW";
    private static final String FORMAT_XML = "XML";
    private final RestAPIBackgroundSteps bg;
    private String _compositionUid;

    public CompositionAPISteps(RestAPIBackgroundSteps pBackgroundSteps){
        bg = pBackgroundSteps;
    }

    public void setCompositionUid(String value) {
        _compositionUid = value;
    }

    public String getCompositionUid() {
        return _compositionUid;
    }

    @When("^Flat json file ([a-zA-Z \\-\\.0-9]+\\.json) with template id ([a-zA-Z \\-\\.0-9]+) is committed to service$")
    public void flatJsonFileIsCommittedToService(String pCompositionFileName, String pTemplateId) throws Exception {
        _compositionUid =
            bg.postFlatJsonComposition(
                bg.resourcesRootPath + TEST_DATA_DIR + "/" + pCompositionFileName,
                pTemplateId);
        assertNotNull(_compositionUid);
        bg.assertUidFormat(_compositionUid);
    }

    @After
    public void cleanUp() throws Exception {
        bg.launcher.stop();
    }

    @Then("^A composition id should be returned by the API$")
    public void aCompositionIdShouldBeReturnedByTheAPI() throws Throwable {
        assertTrue(_compositionUid.split("::").length == 3);
    }

    @And("^Composition id should allow retrieval of composition in raw format$")
    public void compositionIdShouldAllowRetrievalOfCompositionInRawFormat() throws Throwable {
        String objectId = _compositionUid.substring(0, _compositionUid.indexOf("::"));

        Response response = getComposition(objectId, bg.CONTENT_TYPE_JSON, FORMAT_RAW);

        Object composition = response.body().jsonPath().getJsonObject("composition");
        assertNotNull(composition);
    }

    private Response getComposition(String objectId, String pContentType, String pFormat) {
        Response response =
            given()
                .header(bg.secretSessionId, bg.SESSION_ID_TEST_SESSION)
                .header(bg.ACCEPT, pContentType)
            .when()
                .get(COMPOSITION_ENDPOINT + "/" + objectId + "?format=" + pFormat)
            .then()
            .extract()
            .response();

        assertTrue(response.statusCode() == STATUS_CODE_OK);
        return response;
    }

    @And("^Composition id should allow retrieval of composition in xml format$")
    public void compositionIdShouldAllowRetrievalOfCompositionInXmlFormat() throws Exception {
        Document xmlComposition = getXmlCompositionFromRestApi();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        Node rootNode = (Node) xPath.evaluate("/composition", xmlComposition.getDocumentElement(), XPathConstants.NODE);
        assertNotNull(rootNode);
        assertTrue(rootNode.getNodeName().equals("composition"));

    }

    public Document getXmlCompositionFromRestApi() throws ParserConfigurationException, SAXException, IOException {
        String composition = getXmlStringFromRestAPI();
        assertNotNull(composition);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder
            .parse(new ByteArrayInputStream(composition.getBytes()));
    }

    public String getXmlStringFromRestAPI() {
        String objectId = _compositionUid.substring(0, _compositionUid.indexOf("::"));

        Response response = getComposition(objectId, bg.CONTENT_TYPE_XML, FORMAT_XML);

        return response.body().asString();
    }
}
