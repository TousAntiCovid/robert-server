Feature: Three people meet and the are successive contaminations

  Scenario: Three people meet and the are successive contaminations
    Given François install the application TAC
    Given Sarah install the application TAC
    Given Paul install the application TAC

    Given tomorrow at 12:00, François will be near Sarah during 60 minutes
    Given in three days at 13:05, Sarah will be near Paul during 65 minutes

    When François report himself sick
    Then Sarah has no notification
    When robert batch has been triggered

    Then Paul has no notification
    Then Sarah is notified at risk
    When Sarah report herself sick

    When robert batch has been triggered
    Then Paul is notified at risk