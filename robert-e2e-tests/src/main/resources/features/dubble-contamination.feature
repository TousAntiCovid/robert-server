Feature: I declare myself positive which leads to the notification of two contact cases

  Scenario: I declare myself positive which leads to the notification of two contact cases
    Given I install the application TAC
    Given Sarah install the application TAC
    Given Paul install the application TAC
    Given tomorrow at 12:00, I will be near Sarah during 60 minutes
    Given tomorrow at 12:00, I will be near Paul during 60 minutes
    When I report myself sick
    Then Sarah has no notification
    When robert batch has been triggered
    Then Sarah is notified at risk
    Then Paul is notified at risk
