Feature: No contamination

  Scenario: No contamination
    Given François install the application TAC
    Given Sarah install the application TAC
    Given tomorrow at 12:00, François will be near Sarah during 5 minutes
    When François report himself sick
    When robert batch has been triggered
    Then Sarah has no notification
