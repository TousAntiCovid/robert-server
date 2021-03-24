@dropIdTableCollection
Feature: One sick person reports positive covid test

  Scenario: Hugo meets Stephanie enough time to infect her
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Hugo" met "Stephanie"
    And "Hugo" reported positive to covid test via a doctor code
    And They spent enough time together to propagate infection
    When "Stephanie" requests exposure status
    Then "Stephanie" exposure status should report risk level superior to 0 and updated last contact date and last risk scoring date

  Scenario: Hugo meets Stephanie but not long enough to infect her
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Hugo" met "Stephanie"
    And "Hugo" reported positive to covid test via a doctor code
    And They did not spend enough time together to propagate infection
    When "Stephanie" requests exposure status
    Then "Stephanie" exposure status should report risk level equal to 0

  Scenario: Hugo does not meet Stephanie
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Hugo" reported positive to covid test via a doctor code
    And Did not meet anyone
    When "Stephanie" requests exposure status
    Then "Stephanie" exposure status should report risk level equal to 0