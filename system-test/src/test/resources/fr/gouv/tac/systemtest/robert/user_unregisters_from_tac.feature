@dropIdTableCollection
Feature: One person unregisters from TAC

  Scenario: Hugo unregisters from TAC
    Given "Hugo" registered on TAC
    And one user is present in database
    When "Hugo" unregisters from TAC
    Then "Hugo" cannot call status endpoint
    And "Hugo" cannot call delete history exposure endpoint
    And no user are present in database