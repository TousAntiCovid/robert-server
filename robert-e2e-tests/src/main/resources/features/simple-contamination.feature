Feature: Two people meet and one declare himself at risk

  Scenario: They install the application TAC
    Given François installs the application TAC
    Given Sarah installs the application TAC
    Given tomorrow at 12:00, François will be near Sarah during 60 minutes
    When François report himself sick
    When robert batch has been triggered
    Then Sarah is notified at risk
