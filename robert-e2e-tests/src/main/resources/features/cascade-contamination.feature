Feature: Cascade contamination

  Scenario: Cascade contamination
    Given I install the application TAC
    Given Sarah install the application TAC
    Given Paul install the application TAC
    Given tomorrow at 12:00, I will be near Sarah during 60 minutes
    Given in three days at 12:00, Sarah will be near Paul during 60 minutes
    When I report myself sick
    Then Sarah has no notification
    Then Paul has no notification
    When robert batch has been triggered
    Then Sarah is notified at risk
    Then Paul has no notification
    When Sarah report herself sick
    When robert batch has been triggered
    Then Paul is notified at risk
