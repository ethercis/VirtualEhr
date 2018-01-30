package com.ethercis.vehr;

import com.jayway.restassured.response.Response;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompositionAPISteps {

    private static final String COMPOSITION_ENDPOINT = "/rest/v1/composition";
    private final RestAPIBackgroundSteps bground;
    private String body;
    private String compositionUid;

    public CompositionAPISteps(RestAPIBackgroundSteps pBackgroundSteps){
        bground = pBackgroundSteps;
    }

    @When("^Flat json file ([a-zA-Z \\-\\.0-9]+\\.json) with template id ([a-zA-Z \\-\\.0-9]+) is committed to service$")
    public void flatJsonFileIsCommittedToService(String pTemplateFileName, String pTemplateId) throws Exception {
        Path jsonFilePath =
            Paths
                .get(bground.resourcesRootPath + "/test_data/" + pTemplateFileName);
        byte[] fileContents = Files.readAllBytes(jsonFilePath);

        Response commitCompositionResponse =
            given()
                .header(bground.secretSessionId, bground.SESSION_ID_TEST_SESSION)
                .header(bground.CONTENT_TYPE, bground.CONTENT_TYPE_JSON)
            .content(fileContents)
            .when()
                .post(COMPOSITION_ENDPOINT + "?format=FLAT&templateId=" + pTemplateId)
            .then().statusCode(200).extract().response();

        compositionUid = commitCompositionResponse.body().jsonPath().getString("compositionUid");
    }

    @After
    public void cleanUp() throws Exception {
        bground.launcher.stop();
    }

    @Then("^A composition id should be returned by the API$")
    public void aCompositionIdShouldBeReturnedByTheAPI() throws Throwable {
        assertTrue(compositionUid.split("::").length == 3);
    }

    @And("^Composition id should allow retrieval of composition in raw format$")
    public void compositionIdShouldAllowRetrievalOfCompositionInRawFormat() throws Throwable {
        String objectId = compositionUid.substring(0, compositionUid.indexOf("::"));

        Response response = given()
            .header(bground.secretSessionId, bground.SESSION_ID_TEST_SESSION)
            .header(bground.ACCEPT, bground.CONTENT_TYPE_JSON)
            .when()
                .get(COMPOSITION_ENDPOINT + "/" + objectId + "?format=RAW")
            .then()
                .statusCode(200)
                .extract()
                .response();

        Object composition = response.body().jsonPath().getJsonObject("composition");
        assertNotNull(composition);
    }
}
