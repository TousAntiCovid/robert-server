Feature: Covid-19 risk evolution
  As a user
  I want to know when I'm no longer at risk
  in order to get out personal confinement.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Yoko installs the application TAC

  Scenario: 10 days after contact, user is no longer at risk
    Given 10 days ago, Sarah and John met and Sarah was at risk following John report
    When robert batch has been triggered
    Then Sarah is not notified at risk

  Scenario: 6 days after contact, user is still at risk
    Given 6 days ago, Sarah and John met and Sarah was at risk following John report
    When robert batch has been triggered
    Then Sarah is notified at risk

  Scenario: 7 days after contact, user is no longer at risk
    Given 7 days 1 second ago, Sarah and John met and Sarah was at risk following John report
    When robert batch has been triggered
    Then Sarah is not notified at risk

  Scenario: If a user meet an other ill person, declaration token (CNAM) will be updated (last contact date changed)
    Given 7 days ago, Sarah and John met and Sarah was at risk following John report
    And Sarah is notified at risk
    And Sarah last contact is now near 7 days ago
    When just now, the users Yoko and Sarah will be near during 60 minutes
    And Yoko report herself sick
    And robert batch has been triggered
    Then Sarah last contact is now near now

  Scenario: If a user meet an other ill person, declaration token (CNAM) will not be updated
    Given 5 days ago, Sarah and John met and Sarah was at risk following John report
    And Sarah is notified at risk
    And Sarah last contact is now near 5 days ago
    And 12 days ago, Sarah and Yoko met and Sarah was at risk following Yoko report
    When robert batch has been triggered
    Then Sarah last contact is now near 5 days ago
