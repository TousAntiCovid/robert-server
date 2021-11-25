Feature: No contact

  Scenario: No contact
    Given François install the application TAC
    Given Sarah install the application TAC
    When François report himself sick
    When robert batch has been triggered
    Then Sarah has no notification
