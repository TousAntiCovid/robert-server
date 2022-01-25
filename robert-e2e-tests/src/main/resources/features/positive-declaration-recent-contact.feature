Feature: Covid-19 positive declaration
  As a user
  I want to be notified
  when the people I met are at risk
  and I met them in the last 7 days

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Paul installs the application TAC

  Scenario: No notification if the last contact is after 7 days ago and the threshold has not been exceeded
    Given 5 days ago, the users John and Sarah has met during 5 minutes then Sarah report herself sick
    Then John has no notification

  Scenario: Notification sent if the last contact is after 7 days ago and the threshold has been exceeded
    Given 5 days ago, the users John and Sarah has met during 60 minutes then Sarah report herself sick
    Then John is notified at risk

  Scenario: No notification if the threshold has been exceeded but the last contact is after 7 days ago
    Given 8 days ago, the users John and Sarah has met during 60 minutes then Sarah report herself sick
    Then John has no notification

  Scenario: No notification if the threshold has been exceeded but the last contacts are after 7 days ago
    Given that 13 days ago, the users John and Sarah has met during 60 minutes
    Given that 12 days ago, the users John and Paul has met during 60 minutes
    Given that 10 days ago, the users John and Paul has met during 60 minutes
    Given that 8 days ago, the users John and Sarah has met during 60 minutes
    When Sarah report herself sick
    And robert batch has been triggered
    Then John has no notification

  Scenario: Notification sent if the threshold has been exceeded and the last contacts is before 7 days ago
    Given 13 days ago, the users John and Sarah has met during 60 minutes
    Given 12 days ago, the users John and Paul has met during 60 minutes
    Given 10 days ago, the users John and Paul has met during 60 minutes
    Given 8 days ago, the users John and Sarah has met during 60 minutes
    Given 5 days ago, the users John and Sarah has met during 60 minutes
    When Sarah report herself sick
    And robert batch has been triggered
    Then John is notified at risk

  Scenario: No notification if the threshold has not been exceeded even if the last contacts is before 7 days ago
    Given 13 days ago, the users John and Sarah has met during 2 minutes
    Given 8 days ago, the users John and Sarah has met during 2 minutes
    Given 5 days ago, the users John and Sarah has met during 2 minutes
    When Sarah report herself sick
    And robert batch has been triggered
    Then John has no notification

  Scenario: Notification sent for two people if the threshold has been exceeded and the last contacts is before 7 days ago
    Given 13 days ago, the users John, Sarah and Paul has met during 60 minutes
    Given 8 days ago, the users John, Sarah and Paul has met during 60 minutes
    Given 5 days ago, the users John, Sarah and Paul has met during 60 minutes
    When Sarah report herself sick
    And robert batch has been triggered
    Then John is notified at risk
    And Paul is notified at risk