Feature: Covid-19 risk evolution
  As a user
  I want to know when I'm no longer at risk
  in order to get out personal confinement.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC

  Scenario: Fourteen days after contact user is still at risk
    Given Sarah was at risk fourteen days ago
    Given just now, the users John and Sarah will be near during 60 minutes
    And John report himself sick
    And robert batch has been triggered
    When changes last contact date to fourteen days ago for user Sarah
    And robert batch has been triggered
    Then Sarah is notified at risk

  Scenario: Fifteen days after contact nobody is at risk
    Given just now, the users John and Sarah will be near during 60 minutes
    And John report himself sick
    And robert batch has been triggered
    When changes last contact date to fifteen days ago for user Sarah
    And robert batch has been triggered
    Then Sarah has no notification

  Scenario: User data was deleted after 15 days
    Given just now, the users John and Sarah will be near during 60 minutes
    And John report himself sick
    And robert batch has been triggered
    When changes last contact date to fifteen days ago for user Sarah
    And robert batch has been triggered
    Then Sarah data was deleted
