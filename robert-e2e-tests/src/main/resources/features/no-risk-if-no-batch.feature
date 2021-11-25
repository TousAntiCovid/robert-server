Feature: No notification if the batch has not passed

  Scenario: No notification if the batch has not passed
    Given François install the application TAC
    Given Sarah install the application TAC
    Given tomorrow at 12:00, François will be near Sarah during 60 minutes
    When François report himself sick
    Then Sarah has no notification
