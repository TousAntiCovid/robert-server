Feature: Covid-19 risk evolution
  As a user
  I want to know when I'm no longer at risk
  in order to get out personal confinement.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Yoko installs the application TAC

  Scenario: Fourteen days after contact user is still at risk
    Given fourteen days ago, Sarah and John met and Sarah was at risk following John report
    Then Sarah is notified at risk

  Scenario: Fifteen days after contact nobody is at risk
    Given fifteen days ago, Sarah and John met and Sarah was at risk following John report
    Then Sarah has no notification

  Scenario: User data is not deleted before 15 days
    Given fourteen days ago, Sarah and John met and Sarah was at risk following John report
    Then Sarah data was not deleted

  Scenario: User data is deleted after 15 days
    Given fifteen days ago, Sarah and John met and Sarah was at risk following John report
    Then Sarah data was deleted

  Scenario: If a user meet an other ill person, CNAME will be updated
    Given fourteen days ago, Sarah and John met and Sarah was at risk following John report
    And Sarah is notified at risk
    And Sarah last contact is now near fourteen days ago
    When just now, the users Yoko and Sarah will be near during 60 minutes
    And Yoko report herself sick
    And robert batch has been triggered
    Then Sarah last contact is now near now