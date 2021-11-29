Feature: Covid-19 risk exposure deletion
  As a user
  I want to be able to delete my exposure history

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Paul installs the application TAC

  Scenario: One People delete its exposure history
    Given tomorrow at 23:45, Paul will be near Sarah during 15 minutes
    When Paul report himself sick
    And robert batch has been triggered
    And Sarah delete her risk exposure history
    Given in two days at 00:00, John will be near Sarah during 15 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah has no notification

  Scenario: One People does not delete its exposure history
    Given tomorrow at 23:45, Paul will be near Sarah during 15 minutes
    When Paul report himself sick
    And robert batch has been triggered
    And Sarah does not delete her risk exposure history
    Given in two days at 00:00, John will be near Sarah during 15 minutes
    When John report himself sick
    And robert batch has been triggered
    Then Sarah is notified at risk