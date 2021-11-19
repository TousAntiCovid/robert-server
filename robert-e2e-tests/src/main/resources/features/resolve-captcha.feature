Feature: Two people meet and one declare himself at risk

  Scenario: They install the application TAC
    Given François install the application TAC
    Given Sarah install the application TAC
    Given today at 12:00, François was near Sarah during 60 minutes
    When François report himself sick
    Given the robert batch pass