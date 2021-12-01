Feature: Covid-19 positive declaration
  As a user
  I want to declare myself at risk
  in order to notify people I met.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Paul installs the application TAC

  Scenario: No notification if the batch has not been executed
    Given tomorrow at 12:00, the users John and Sarah will be near during 60 minutes
    When John report himself sick
    And robert batch as not been executed yet
    Then Sarah has no notification

  Scenario: One people is notified when someone declare himself positive
    Given tomorrow at 12:00, the users John and Sarah will be near during 60 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah is notified at risk

  Scenario: Two people are notified when someone declare himself positive
    Given tomorrow at 12:00, the users John, Sarah and Paul will be near during 60 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah is notified at risk
    And Paul is notified at risk

  Scenario: Nobody meets in person
    When Paul report himself sick
    And robert batch has been triggered
    Then Sarah has no notification

  Scenario: People meet not long enough
    Given tomorrow at 12:00, the users Paul and Sarah will be near during 5 minutes
    When Paul report himself sick
    And robert batch has been triggered
    Then Sarah has no notification