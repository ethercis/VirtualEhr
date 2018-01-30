Feature: Provide Composition API access over REST
  As a clinical informatics actor
  In order to create, access and modify data based on openEHR compositions
  I want to use a REST API to perform operations on compositions.

  Background:
  The server is ready, an EHR and a template is in place and the user is logged in.

    Given The server is running
    And The client system is logged into a server session
    And The openEHR template IDCR - Immunisation summary.v0.opt for the composition is available to the server
    And An EHR is created

  Scenario: Commit composition in flat json format
    When Flat json file IDCR - Immunisation summary.v0.flat.json with template id IDCR - Immunisation summary.v0 is committed to service
    Then A composition id should be returned by the API

  Scenario: Commit composition in flat json and retrieve raw format
    When Flat json file IDCR - Immunisation summary.v0.flat.json with template id IDCR - Immunisation summary.v0 is committed to service
    Then A composition id should be returned by the API
    And Composition id should allow retrieval of composition in raw format