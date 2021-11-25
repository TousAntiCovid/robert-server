Feature: No notification if the batch has not passed

  Scenario: No notification if the batch has not passed
    Given François installs the application TAC
    Given Sarah installs the application TAC
    Given tomorrow at 12:00, François will be near Sarah during 60 minutes
    When François report himself sick
    Then Sarah has no notification
