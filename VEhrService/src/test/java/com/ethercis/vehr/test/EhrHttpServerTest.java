//Copyright
package com.ethercis.vehr.test;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.partyservice.I_PartyIdentifiedService;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.vehr.Launcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.javafx.binding.StringFormatter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/2/2015.
 */
public class EhrHttpServerTest extends TestServerSimulator {
    List<Long> timings = new ArrayList<>();
    long start;
    UUID localPatient;
    //    String subjectCodeId = "9999999099";
    String subjectCodeId = UUID.randomUUID().toString();
    String subjectCodePrefix = "99999-";

    String subjectNameSpace = "uk.nhs.nhs_number";
    Gson json = new GsonBuilder().create();

    protected HttpClient client;

//    private static final String hostname = "188.166.246.78"

    @Before
    public void setUp() throws Exception {
        super.setUp();

        //setup a dummy subject for Ehr
        I_PartyIdentifiedService partyIdentifiedService = ClusterInfo.getRegisteredService(global, "PartyIdentifiedService", "1.0", new Object[]{null});

        if (partyIdentifiedService == null) {
            fail("Could not retrieve partyIdentifiedService, check configuration and CLASSPATH");
        }
        //create a dummy identified party
        localPatient = partyIdentifiedService.getOrCreateParty("testParty", subjectCodeId, subjectNameSpace, "testAssigner", "testType");

        if (localPatient == null)
            fail("Could not create test patient...");

        client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(30000); // 30 seconds timeout; if no server reply, the request expires
        client.start();

    }

    @After
    public void tearDown() throws Exception {
        //stop the client
        if (client != null)
            client.stop();

        //stop the server
        if (launcher != null)
            launcher.stop();
    }

    private ContentResponse stopWatchRequestSend(Request request) throws Exception {
        ContentResponse response;
        try {
            start = System.nanoTime();
            response = request.send();
            timings.add(System.nanoTime() - start);
            return response;
        } catch (Exception exception) {
//            System.out.println("Exception:"+exception);
            fail(exception.getMessage());
            return null;
        }
    }

    private void dumpTimings() {
        int i = 0;
        for (Long time : timings) {
            System.out.println("Step#" + (i++) + ":" + time / 1000000 + "[ms]");
        }
    }

    //other_details_xml
    String other_details_xml =
            "<items xmlns=\"http://schemas.openehr.org/v1\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "archetype_node_id=\"openEHR-EHR-ITEM_TREE.person_anonymised_parents.v0\">\n" +
                    "    <name>\n" +
                    "        <value>Person anonymised parents</value>\n" +
                    "    </name>\n" +
                    "    <items xmlns:v1=\"http://schemas.openehr.org/v1\" archetype_node_id=\"at0001\" xsi:type=\"v1:CLUSTER\">\n" +
                    "        <name>\n" +
                    "            <value>person</value>\n" +
                    "        </name>\n" +
                    "        <items archetype_node_id=\"openEHR-EHR-CLUSTER.person_anoymised_parent.v0\" xsi:type=\"v1:CLUSTER\">\n" +
                    "            <name>\n" +
                    "                <value>Person anonymised parent</value>\n" +
                    "            </name>\n" +
                    "            <archetype_details>\n" +
                    "                <archetype_id>\n" +
                    "                    <value>openEHR-EHR-CLUSTER.person_anoymised_parent.v0</value>\n" +
                    "                </archetype_id>\n" +
                    "                <rm_version>1.0.1</rm_version>\n" +
                    "            </archetype_details>\n" +
                    "            <items archetype_node_id=\"at0002\" xsi:type=\"v1:ELEMENT\">\n" +
                    "                <name>\n" +
                    "                    <value>Administrative Gender</value>\n" +
                    "                </name>\n" +
                    "                <value xsi:type=\"v1:DV_CODED_TEXT\">\n" +
                    "                    <value>Male</value>\n" +
                    "                    <defining_code>\n" +
                    "                        <terminology_id>\n" +
                    "                            <value>local</value>\n" +
                    "                        </terminology_id>\n" +
                    "                        <code_string>at0003</code_string>\n" +
                    "                    </defining_code>\n" +
                    "                </value>\n" +
                    "            </items>\n" +
                    "            <items archetype_node_id=\"at0006\" xsi:type=\"v1:ELEMENT\">\n" +
                    "                <name>\n" +
                    "                    <value>Birth Sex</value>\n" +
                    "                </name>\n" +
                    "                <value xsi:type=\"v1:DV_CODED_TEXT\">\n" +
                    "                    <value>Male</value>\n" +
                    "                    <defining_code>\n" +
                    "                        <terminology_id>\n" +
                    "                            <value>local</value>\n" +
                    "                        </terminology_id>\n" +
                    "                        <code_string>at0007</code_string>\n" +
                    "                    </defining_code>\n" +
                    "                </value>\n" +
                    "            </items>\n" +
                    "            <items archetype_node_id=\"at0009\" xsi:type=\"v1:ELEMENT\">\n" +
                    "                <name>\n" +
                    "                    <value>Vital Status</value>\n" +
                    "                </name>\n" +
                    "                <value xsi:type=\"v1:DV_CODED_TEXT\">\n" +
                    "                    <value>OK</value>\n" +
                    "                    <defining_code>\n" +
                    "                        <terminology_id>\n" +
                    "                            <value>local</value>\n" +
                    "                        </terminology_id>\n" +
                    "                        <code_string>at0010</code_string>\n" +
                    "                    </defining_code>\n" +
                    "                </value>\n" +
                    "            </items>\n" +
                    "            <items archetype_node_id=\"at0012\" xsi:type=\"v1:ELEMENT\">\n" +
                    "                <name>\n" +
                    "                    <value>Birth Year</value>\n" +
                    "                </name>\n" +
                    "                <value xsi:type=\"v1:DV_DATE\">\n" +
                    "                    <value>2013-01-01</value>\n" +
                    "                </value>\n" +
                    "            </items>\n" +
                    "        </items>\n" +
                    "    </items>\n" +
                    "</items>";

    String other_details_json = "{" +
            " \"otherDetails\": {\n" +
            "  \"@class\": \"ITEM_TREE\",\n" +
            "  \"archetype_node_id\": \"at0001\",\n" +
            "  \"items\": [\n" +
            "   {\n" +
            "    \"@class\": \"CLUSTER\",\n" +
            "    \"archetype_details\": {\n" +
            "     \"@class\": \"ARCHETYPED\",\n" +
            "     \"archetype_id\": {\n" +
            "      \"@class\": \"ARCHETYPE_ID\",\n" +
            "      \"value\": \"openEHR-EHR-CLUSTER.person_anonymised_parent.v1\"\n" +
            "     },\n" +
            "     \"rm_version\": \"1.0.1\"\n" +
            "    },\n" +
            "    \"archetype_node_id\": \"openEHR-EHR-CLUSTER.person_anonymised_parent.v1\",\n" +
            "    \"items\": [\n" +
            "     {\n" +
            "      \"@class\": \"ELEMENT\",\n" +
            "      \"archetype_node_id\": \"at0001\",\n" +
            "      \"name\": {\n" +
            "       \"@class\": \"DV_TEXT\",\n" +
            "       \"value\": \"Administrative Gender\"\n" +
            "      },\n" +
            "      \"value\": {\n" +
            "       \"@class\": \"DV_CODED_TEXT\",\n" +
            "       \"defining_code\": {\n" +
            "        \"@class\": \"CODE_PHRASE\",\n" +
            "        \"code_string\": \"at0009\",\n" +
            "        \"terminology_id\": {\n" +
            "         \"@class\": \"TERMINOLOGY_ID\",\n" +
            "         \"value\": \"local\"\n" +
            "        }\n" +
            "       },\n" +
            "       \"value\": \"Male\"\n" +
            "      }\n" +
            "     },\n" +
            "     {\n" +
            "      \"@class\": \"ELEMENT\",\n" +
            "      \"archetype_node_id\": \"at0001\",\n" +
            "      \"name\": {\n" +
            "       \"@class\": \"DV_TEXT\",\n" +
            "       \"value\": \"Birth Sex\"\n" +
            "      },\n" +
            "      \"value\": {\n" +
            "       \"@class\": \"DV_CODED_TEXT\",\n" +
            "       \"defining_code\": {\n" +
            "        \"@class\": \"CODE_PHRASE\",\n" +
            "        \"code_string\": \"at0009\",\n" +
            "        \"terminology_id\": {\n" +
            "         \"@class\": \"TERMINOLOGY_ID\",\n" +
            "         \"value\": \"local\"\n" +
            "        }\n" +
            "       },\n" +
            "       \"value\": \"Male\"\n" +
            "      }\n" +
            "     },\n" +
            "     {\n" +
            "      \"@class\": \"ELEMENT\",\n" +
            "      \"archetype_node_id\": \"at0001\",\n" +
            "      \"name\": {\n" +
            "       \"@class\": \"DV_TEXT\",\n" +
            "       \"value\": \"Vital Status\"\n" +
            "      },\n" +
            "      \"value\": {\n" +
            "       \"@class\": \"DV_CODED_TEXT\",\n" +
            "       \"defining_code\": {\n" +
            "        \"@class\": \"CODE_PHRASE\",\n" +
            "        \"code_string\": \"at0004\",\n" +
            "        \"terminology_id\": {\n" +
            "         \"@class\": \"TERMINOLOGY_ID\",\n" +
            "         \"value\": \"local\"\n" +
            "        }\n" +
            "       },\n" +
            "       \"value\": \"Alive\"\n" +
            "      }\n" +
            "     },\n" +
            "     {\n" +
            "      \"@class\": \"ELEMENT\",\n" +
            "      \"archetype_node_id\": \"at0014\",\n" +
            "      \"name\": {\n" +
            "       \"@class\": \"DV_TEXT\",\n" +
            "       \"value\": \"Birth Year\"\n" +
            "      },\n" +
            "      \"value\": {\n" +
            "       \"@class\": \"DV_DATE\",\n" +
            "       \"value\": \"1944\"\n" +
            "      }\n" +
            "     }\n" +
            "    ]\n" +
            "   }\n" +
            "  ]\n" +
            " }";

    @Test
    public void testCreateEhr() throws Exception {
        String userId = "guest";
        String password = "guest";
        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        //get the session id from the header
        assertNotNull(response);

        //create a new EHR
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));
        //generate a random subjectCodeId
        Integer integer = new Random().nextInt();
        String id = StringFormatter.format("%d", Math.abs(integer % 9999)).getValue();
        String subjectId = subjectCodePrefix + id;
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectId + "&subjectNamespace=" + subjectNameSpace);

        //pass other_details_xml in body
//        Map<String, String> otherDetailsMap = new HashMap(){{
//            put("otherDetails", other_details_xml);
//            put("otherDetailsTemplateId", "person anonymised parent");
//        }};
//        String otherDetailsMapAsString = json.toJson(otherDetailsMap, Map.class);
//        request.content(new BytesContentProvider(otherDetailsMapAsString.getBytes()));

        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);

        response = stopWatchRequestSend(request);

        //get the session id from the header
        assertNotNull(response);

        //decode the content
        String body = response.getContentAsString();

        Map<String, String> map = json.fromJson(body, Map.class);

        UUID ehrId = UUID.fromString(map.get("ehrId"));

        //test retrieve ehr
        //NB. use implicit method!
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        map = json.fromJson(body, Map.class);
        UUID retrievedEhrId = UUID.fromString(map.get("ehrId"));
//
//        assertEquals(ehrId, retrievedEhrId);
//
//        request = client.newRequest("http://"+hostname+":"+httpPort+"/rest/v1/ehr/status?ehrId=" + retrievedEhrId);
//        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
//        request.method(HttpMethod.GET);
//        response = stopWatchRequestSend(request);
//        assertNotNull(response);

        //#===================== update EHR STATUS =================================================
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/status/" + retrievedEhrId + "/other_details_xml?format=XML");
        //pass other_details_xml in body
        Map<String, String> otherDetailsMap = new HashMap() {{
            put("otherDetails", other_details_json);
//            put("otherDetailsTemplateId", "person anonymised parent");
        }};
        String otherDetailsMapAsString = json.toJson(otherDetailsMap, Map.class);
        request.content(new BytesContentProvider(otherDetailsMapAsString.getBytes()));
//        Map<String, String> statusMap = new HashMap(){{
//            put("queryable", false);
//            put("modifiable", false);
//        }};
//        String statusMapString = json.toJson(statusMap, Map.class);
//        request.content(new BytesContentProvider(statusMapString.getBytes()));
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.PUT);
        response = stopWatchRequestSend(request);

        //#===================== retrieve EHR STATUS =================================================
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/status?ehrId=" + retrievedEhrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        //dummy composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?templateId=testTemplate");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.content(new BytesContentProvider("test".getBytes()));
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
        assertNotNull(response);

        //delete the poor bugger...

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        assertEquals(200, response.getStatus()); //success

        //disconnect cleanly from server...
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
//        request.content(new BytesContentProvider(statusMapString.getBytes()));
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus()); //success

        dumpTimings();
    }

    private byte[] setQueryBody() {
        Map<String, String> kvPairs = new HashMap<>();

        kvPairs.put("/context/health_care_facility|name", "Northumbria Community NHS");
        kvPairs.put("/context/health_care_facility|identifier", "999999-345");
        kvPairs.put("/context/start_time", "2015-09-28T10:18:17.352+07:00");
        kvPairs.put("/context/end_time", "2015-09-28T11:18:17.352+07:00");
        kvPairs.put("/context/participation|function", "Oncologist");
        kvPairs.put("/context/participation|identifier", "1345678");
        kvPairs.put("/context/participation|name", "Dr. Marcus Johnson");
        kvPairs.put("/context/participation|mode", "openehr::216|face to face communication|");
        kvPairs.put("/context/location", "local");
        kvPairs.put("/context/setting", "openehr::227|emergency care|");
        kvPairs.put("/composer|identifier", "1345678");
        kvPairs.put("/composer|name", "Dr. Marcus Johnson");
        kvPairs.put("/category", "openehr::433|event|");
        kvPairs.put("/territory", "FR");
        kvPairs.put("/language", "fr");

//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:0", "Nurse|1345678::Jessica|openehr::216|face-to-face communication|");
//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:1", "Assistant|1345678::2.16.840.1.113883.2.1.4.3::NHS-UK::ANY::D. Mabuse|openehr::216|face-to-face communication|");

//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:0|function", "Nurse");
//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:0|identifier", "1345678");
//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:0|name", "Jessica");
//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:0|mode", "face-to-face communication|openehr::216|");
//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/participation:1", "Assistant|1345678::2.16.840.1.113883.2.1.4.3::NHS-UK::ANY::D. Mabuse|openehr::216|face-to-face communication|");

        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]/timing", "before sleep");

        //should be rejected...
        //kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]/action_archetype_id", "ZZZZZZZ\\.medication\\.v1");

        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]/timing", "lunch");
        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]" +
                "/description[openEHR-EHR-ITEM_TREE.medication_mod.v1]/items[at0001]", "aspirin");

        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]" +
                "/description[openEHR-EHR-ITEM_TREE.medication_mod.v1]/items[at0003]", "@1|3,pg");

//        this modification passes the validation that requires 2..* cardinality for items[at0001] but
//        fails during retrieval. so I will modify the opt for the moment to work around the issue

//        kvPairs.put("/content[openEHR-EHR-SECTION.medications.v1]/items[openEHR-EHR-INSTRUCTION.medication.v1]/activities[at0001]" +
//            "/description[openEHR-EHR-ITEM_TREE.medication_mod.v1]/items[at0002]", "aspirin1");


        return json.toJson(kvPairs).getBytes();
    }

    private Map<String, String> decodeBodyResponse(ContentResponse response) {
//        String body = response.getContentAsString();
        if (response.getStatus() == 200)
            return json.fromJson(response.getContentAsString(), Map.class);
        else {
            Map<String, String> map = new HashMap<>();
            map.put("HTTP Error:" + response.getStatus(), response.getReason());
            return map;
        }
    }

    @XStreamAlias("compositionCreateRestResponseData")
    public static class compositionCreateRestResponseData implements Serializable {
        @XStreamAlias("action")
        String action;
        @XStreamAlias("compositionUid")
        String compositionUid;
        @XStreamAlias("meta")
        String meta;
    }

    private String decodeXMLResponse(ContentResponse response, String element) {
        XStream xStream = new XStream();
        xStream.alias("compositionCreateRestResponseData", compositionCreateRestResponseData.class);
        xStream.aliasField("action", compositionCreateRestResponseData.class, "action");
        xStream.aliasField("compositionUid", compositionCreateRestResponseData.class, "compositionUid");
        xStream.aliasField("meta", compositionCreateRestResponseData.class, "meta");
        String content = response.getContentAsString();

        compositionCreateRestResponseData compositionData = (compositionCreateRestResponseData) xStream.fromXML(content);
        return compositionData.compositionUid;
    }


    @Test
    public void testCompositionHandling() throws Exception {
        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        //get the list of templates
        Request requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.GET);
        response = stopWatchRequestSend(requestTemplateService);

        assertNotNull(response);

        //add a template anc check response
        String prescriptionFilePath = "prescription.opt";

        byte[] content = readXMLNoBOM("src/test/resources/knowledge/operational_templates/" + prescriptionFilePath);

        requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.POST);
        requestTemplateService.content(new BytesContentProvider(content), "text/xml;charset=UTF-8");
        response = stopWatchRequestSend(requestTemplateService);
        assertNotNull(response);

        //get an example
        requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template/prescription/example");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.GET);
        requestTemplateService.content(new BytesContentProvider(content), "text/xml;charset=UTF-8");
        response = stopWatchRequestSend(requestTemplateService);
        assertNotNull(response);

        //create ehr

        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        //retrieve EHR
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        //create a composition
//        request = client.newRequest("http://"+hostname+":"+httpPort+"/rest/v1/composition?templateId=prescription.opt&format=ECISFLAT");
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?templateId=prescription&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        request.content(new BytesContentProvider(setQueryBody()), "text/plain");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
//        UUID compositionId = I_CompositionService.decodeUuid(decodeBodyResponse(response).get("compositionUid"));
        String strCompositionId = decodeBodyResponse(response).get("compositionUid");

        //retrieve the composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId + "&format=XML");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //update context participation...
        Map<String, String> kvPairs = new HashMap<>();
        kvPairs.put("/context/participation|function", "Pediatric");
        kvPairs.put("/context/participation|identifier", "99999-123");
        kvPairs.put("/context/participation|name", "Dr. Mabuse");
        kvPairs.put("/context/participation|mode", "openehr::216|face to face communication|");

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId + "&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.PUT);
        request.content(new BytesContentProvider(json.toJson(kvPairs).getBytes()), "text/plain");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        System.out.println(response.getContentAsString());

        //retrieve the composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId + "&format=XML");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //retrieve the composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId + "&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //house keeping
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

    @Test
    public void testCompositionHandlingCanonical() throws Exception {
        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        //create ehr
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Origin", "http://localhost:1234");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);
        ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));


        //create required template
        String prescriptionFilePath = "prescription.opt";
        //read in a template into a string
        byte[] content = readXMLNoBOM("src/test/resources/knowledge/operational_templates/" + prescriptionFilePath);

        Request requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.POST);
        requestTemplateService.content(new BytesContentProvider(content), "text/xml;charset=UTF-8");
        response = stopWatchRequestSend(requestTemplateService);
        assertNotNull(response);

        File xmlFile = new File("src/test/resources/Prescription.xml");
        InputStream is = new FileInputStream(xmlFile);
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?format=RAW");
        request.header("Content-Type", "application/xml");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] xmlContent = new byte[(int) xmlFile.length()];
        int i = is.read(xmlContent);
        request.content(new BytesContentProvider(xmlContent), "application/xml");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        String compositionId = decodeXMLResponse(response, "compositionUid");

        //retrieve this composition under two formats...
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=RAW");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/xml");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());

        //delete composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=RAW");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/xml");
        request.method(HttpMethod.DELETE);
//        response = stopWatchRequestSend(request);
//        assertNotNull(response);
//        //output the content
//        System.out.println(response.getContentAsString());


        //house keeping
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

    @Test
    public void testCompositionHandlingOtherContext() throws Exception {
        String userId = "guest";
        String password = "guest";

        String subjectCodeId = "99999-1234";

        String templateId = "UK AoMRC Outpatient Letter.opt";

        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        //load the template in the embedded test server
        byte[] content = readXMLNoBOM("src/test/resources/knowledge/operational_templates/" + templateId);
        Request requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.POST);
        requestTemplateService.content(new BytesContentProvider(content), "text/xml;charset=UTF-8");
        response = stopWatchRequestSend(requestTemplateService);
        assertEquals(Response.SC_OK, response.getStatus());

        //create ehr
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);

        assertEquals(Response.SC_OK, response.getStatus());
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));


        File jsonSample = new File("src/test/resources/AOMRC GENERIC OUTPATIENT LETTER.json");
        InputStream is = new FileInputStream(jsonSample);
//        URL url = new URL("http://"+hostname+":"+httpPort+"/rest/v1/composition?templateId="+templateId+"&format=ECISFLAT");
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?templateId=" + "UK AoMRC Outpatient Letter".replaceAll(" ", "%20") + "&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] jsonContent = new byte[(int) jsonSample.length()];
        int i = is.read(jsonContent);
        request.content(new BytesContentProvider(jsonContent), "application/json");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        String openEHRUUID = decodeBodyResponse(response).get("compositionUid");
        UUID compositionId = UUID.fromString(openEHRUUID.substring(0, openEHRUUID.indexOf("::")));

        //retrieve this composition under two formats...
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + compositionId + "&format=ECISFLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //house keeping
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

    private BytesContentProvider jsonContentFromTestFile(String file) throws Exception {
        File flatjsonFile = new File(file);
        InputStream is = new FileInputStream(flatjsonFile);
        byte[] jsonContent = new byte[(int) flatjsonFile.length()];
        int i = is.read(jsonContent);
        return new BytesContentProvider(jsonContent);
    }

    String[] testFlatJsonFile = {
            "RIPPLE_conformanceTesting_RAW_FLATJSON",
            "IDCR-LabReportRAW1_FLATJSON",
            "Vital_signs_TEST_FLATJSON",
            "IDCR Problem List.v1_FLATJSON",
            "IDCR Procedures List_1 RAW_FLATJSON",
            "IDCR Lab Order RAW1_FLATJSON"
    };

    String[] testTemplateId = {
            "RIPPLE - Conformance Test template",
            "IDCR - Laboratory Test Report.v0",
            "Vital Signs Encounter (Composition)",
            "IDCR Problem List.v1",
            "IDCR Procedures List.v0",
            "IDCR - Laboratory Test Report.v0"
    };

    @Test
    public void testCompositionHandlingFlatJson() throws Exception {
        Logger log = LogManager.getLogManager().getLogger("testCompositionHandlingFlatJson");
        int t = 0;
        String testFile = testFlatJsonFile[t];
        String testTemplate = testTemplateId[t];

        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        //create ehr
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        String requestString = "http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace;

        Request request = client.newRequest(requestString);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);

//
//        Request request = client.newRequest("http://"+hostname+":"+httpPort+"/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
//        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
//        request.method(HttpMethod.GET);
//        response = stopWatchRequestSend(request);

//        log.severe(requestString);
//        log.severe(response.getContentAsString());
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Origin", "http://localhost:1234");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);
        ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        File flatjsonFile = new File("src/test/resources/IDCR - Immunisation summary.v0.flat.json");
//        File flatjsonFile = new File("/Development/Dropbox/eCIS_Development/samples/"+testFile+".json");
        InputStream is = new FileInputStream(flatjsonFile);
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?format=FLAT&templateId=IDCR - Immunisation summary.v0".replaceAll(" ", "%20"));
//        request = client.newRequest("http://" + hostname + ":"+httpPort+"/rest/v1/composition?format=FLAT&templateId=" + testTemplate.replaceAll(" ", "%20"));
        request.header("Content-Type", "application/json");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] jsonContent = new byte[(int) flatjsonFile.length()];
        int i = is.read(jsonContent);
        request.content(new BytesContentProvider(jsonContent), "application/json");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        String compositionId = decodeBodyResponse(response).get("compositionUid");

        //retrieve this composition under two formats...
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=FLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/json");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());

        //get an example
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template/IDCR%20-%20Laboratory%20Order.v0/example?format=FLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);

        //update composition
        BytesContentProvider bytesContentProvider = jsonContentFromTestFile("src/test/resources/IDCR Lab Order RAW1_FLATJSON_UPDATE.json");
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=FLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Content-Type", "application/json");
        request.content(bytesContentProvider, "application/json");
        request.method(HttpMethod.PUT);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //retrieve this composition under two formats...
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=FLAT");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/json");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());

        //delete composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=RAW");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/xml");
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());


        //house keeping
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

//    String query = "SELECT c/uid/value,"+
//            "c/name/value,"+
//            "eval/data[at0001]/items[at0002]/value/value AS problem, " +
//            "eval/data[at0001]/items[at0002]/value/defining_code/code_string AS code, " +
//            "eval/data[at0001]/items[at0002]/value/defining_code/terminology_id/name AS code, " +
//            "eval/data[at0001]/items[at0009]/value/value AS description, "+
//            "eval/data[at0001]/items[at0077]/value/value AS onset "+
//            "FROM EHR e  " +
//            "CONTAINS COMPOSITION c " +
//            "CONTAINS EVALUATION eval [openEHR-EHR-EVALUATION.problem-diagnosis.v1] " +
//            "WHERE c/uid/value = '08fd487b-765a-41b4-9501-334d48dc2b00::test::1'";

    String query = "select a/uid/value as uid, " +
            "a/composer/name as author, " +
            "e/ehr_status/subject/external_ref/id/value," +
            "a/context/start_time/value as date_submitted, " +
            "b_a/description[at0001]/items[at0002]/value/value as procedure_name, " +
            "b_a/description[at0001]/items[at0049, 'Procedure notes']/value/value as procedure_notes, " +
            "b_a/other_participations/performer/name as performer, " +
            "b_a/time/value as procedure_date, " +
            "b_a/ism_transition/careflow_step/value as status, " +
            "b_a/ism_transition/careflow_step/defining_code/code_string as status_code, " +
            "b_a/ism_transition/careflow_step/defining_code/terminology_id/value as terminology " +
            "from EHR e " +
            "contains COMPOSITION a[openEHR-EHR-COMPOSITION.care_summary.v0] " +
            "contains ACTION b_a[openEHR-EHR-ACTION.procedure.v1] " +
            "where a/name/value='Procedures list' " +
//                "and a/uid/value='" + procedureId + "' " +
            "and e/ehr_status/subject/external_ref/id/value = '9999999000'";

    String prescriptionQuery = "select a/uid/value " +
            "from EHR e " +
            "contains COMPOSITION a[openEHR-EHR-COMPOSITION.prescription.v1] ";
//            "contains SECTION b_a[openEHR-EHR-SECTION.medications.v1] ";


    @Test
    public void testPrescriptionQuery() throws Exception {
        int t = 0;

        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), "TEST-SESSION");
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
//        response = client.POST("http://" + hostname + ":"+httpPort+"/rest/v1/session?username=" + userId + "&password=" + password).send();
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        //create ehr
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?subjectId=" + subjectCodeId + "&subjectNamespace=" + subjectNameSpace);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
        UUID ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr/" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Origin", "http://localhost:1234");
        request.method(HttpMethod.GET);
        response = stopWatchRequestSend(request);

        assertNotNull(response);
        ehrId = UUID.fromString(decodeBodyResponse(response).get("ehrId"));

        //create required template
        String prescriptionFilePath = "prescription.opt";
        //read in a template into a string
        byte[] content = readXMLNoBOM("src/test/resources/knowledge/operational_templates/" + prescriptionFilePath);

        Request requestTemplateService = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/template");
        requestTemplateService.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        requestTemplateService.method(HttpMethod.POST);
        requestTemplateService.content(new BytesContentProvider(content), "text/xml;charset=UTF-8");
        response = stopWatchRequestSend(requestTemplateService);
        assertEquals(Response.SC_OK, response.getStatus());

        //create composition
        File xmlFile = new File(resourcesRootPath + "/Prescription.xml");
        InputStream is = new FileInputStream(xmlFile);
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?format=RAW");
        request.header("Content-Type", "application/xml");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
        byte[] xmlContent = new byte[(int) xmlFile.length()];
        int i = is.read(xmlContent);
        request.content(new BytesContentProvider(xmlContent), "application/xml");
        response = stopWatchRequestSend(request);
        assertNotNull(response);
        String compositionId = decodeXMLResponse(response, "compositionUid");

        String encodedQuery = prescriptionQuery.replaceAll(" ", "%20");

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/query?aql=" + encodedQuery);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
//        //set query string in body
//        request.content(new BytesContentProvider(query.getBytes()), "application/text");
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        System.out.println(response.getContentAsString());

        //delete composition
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition/" + compositionId + "?format=RAW");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.header("Accept", "application/xml");
        request.method(HttpMethod.DELETE);
//        response = stopWatchRequestSend(request);
//        assertNotNull(response);
//        //output the content
//        System.out.println(response.getContentAsString());


        //house keeping
        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/ehr?ehrId=" + ehrId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

    @Test
    public void testQuery() throws Exception {
        int t = 0;

        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), "TEST-SESSION");
        request.method(HttpMethod.POST);
        response = stopWatchRequestSend(request);
//        response = client.POST("http://" + hostname + ":"+httpPort+"/rest/v1/session?username=" + userId + "&password=" + password).send();
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        String encodedQuery = query.replaceAll(" ", "%20");

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/query?aql=" + encodedQuery);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
//        //set query string in body
//        request.content(new BytesContentProvider(query.getBytes()), "application/text");
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        System.out.println(response.getContentAsString());

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }

    String sqlQuery1 = "select\n" +
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
            ");";


    String aqlQuery = "select a/uid/value as uid, \n" +
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
            " where a/name/value='Adverse reaction list'";

    @Test
    public void testQueryPost() throws Exception {
        int t = 0;
//
        hostname = "localhost";

        String userId = "guest";
        String password = "guest";

        timings.clear();

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));

        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/query");
        request.header("Content-Type", "application/json");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.POST);
//        byte[] jsonContent = new byte[queryPost.length()];

//        String encoded = URLEncoder.encode(sqlQuery1, "UTF-8");
        String json = "{ \"aql\":\"" + aqlQuery + "\"}";
        request.content(new BytesContentProvider(json.getBytes()), "application/json");
//        //set query string in body
//        request.content(new BytesContentProvider(query.getBytes()), "application/text");
        response = stopWatchRequestSend(request);

        assertNotNull(response);

        System.out.println(response.getContentAsString());

        request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.DELETE);
        response = stopWatchRequestSend(request);
        assertEquals(200, response.getStatus());

        dumpTimings();
    }


    @Test
    public void testLoginWithHeader() throws Exception {
        String userId = "guest";
        String password = "guest";

        //login first!
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password);
        request.header("x-bypass-credential", "true");
        request.header("x-session-timeout", "0");
        request.header("x-session-name", "TEST-INTERNAL");
        request.header("Ehr-Session", "TEST-INTERNAL-SECRET");
        request.header("x-max-session", "200");
        request.method(HttpMethod.POST);
        ContentResponse response = stopWatchRequestSend(request);
        assertNotNull(response);
    }
}

