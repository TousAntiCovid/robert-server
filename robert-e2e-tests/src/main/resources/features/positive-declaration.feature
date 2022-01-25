Feature: Covid-19 positive declaration
  As a user
  I want to declare myself at risk
  in order to notify people I met.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Paul installs the application TAC

  Scenario: No notification if the batch has not been executed
    Given just now, the users John and Sarah will be near during 60 minutes
    When John report himself sick
    And robert batch as not been executed yet
    Then Sarah is not notified at risk

  Scenario: One people is notified when someone declare himself positive
    Given just now, the users John and Sarah will be near during 60 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah is notified at risk

  Scenario: Two people are notified when someone declare himself positive
    Given just now, the users John, Sarah and Paul will be near during 60 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah is notified at risk
    And Paul is notified at risk

  Scenario: Nobody meets in person
    When Paul report himself sick
    And robert batch has been triggered
    Then Sarah is not notified at risk

  Scenario: People meet not long enough
    Given just now, the users Paul and Sarah will be near during 5 minutes
    When Paul report himself sick
    And robert batch has been triggered
    Then Sarah is not notified at risk