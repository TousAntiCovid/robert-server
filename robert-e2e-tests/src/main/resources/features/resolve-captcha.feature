Feature: Two people meet and one declare himself at risk

  Scenario: They install the application TAC
    Given François install the application TAC
    Given Sarah install the application TAC
    Given tomorrow at 12:00, François will be near Sarah during 60 minutes
    When François report himself sick
    When robert batch has been triggered