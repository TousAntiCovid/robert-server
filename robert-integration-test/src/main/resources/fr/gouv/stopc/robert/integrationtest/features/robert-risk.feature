Feature: François declare himself at risk

  Background:
    Given "François" has the application TAC
    Given "Sarah" has the application TAC

  Scenario: They register themself on TAC
    Given "François" registered on TAC with the Captcha service
    Given "Sarah" registered on TAC with the Captcha service
    Then "François" is registed on TAC
    Then "Sarah" is registed on TAC

  Scenario: Temporary to test triggering of robert batch raising at risk app
    Given they spent enough time together to propagate infection

  Scenario: Temporary to test triggering of robert batch that doesn't raise at risk app
    Given they did not spend enough time together to propagate infection
