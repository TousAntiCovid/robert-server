Feature: Covid-19 risk evolution
  As a user
  I want to know when I'm no longer at risk
  in order to get out personal confinement.

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Yoko installs the application TAC

  Scenario Outline: <last contact> days after contact, user is no longer at risk
    Given <last contact> days ago, Sarah and John met and Sarah was at risk following John report
    When robert batch has been triggered
    Then Sarah is not notified at risk
    Examples:
      | last contact |
      | 8            |
      | 10           |
      | 15           |

  Scenario Outline: <last contact> days after contact, user is no longer at risk
    Given <last contact> days ago, Sarah and John met and Sarah was at risk following John report
    When robert batch has been triggered
    Then Sarah is notified at risk
    Examples:
      | last contact |
      | 1            |
      | 2            |
      | 6            |
      | 7            |

  Scenario: If a user meet an other ill person, declaration token (CNAM) will be updated (last contact date changed)
    Given 7 days ago, Sarah and John met and Sarah was at risk following John report
    And Sarah is notified at risk
    And Sarah last contact is near 7 days ago
    When just now, the users Yoko and Sarah will be near during 60 minutes
    And Yoko report herself sick
    And robert batch has been triggered
    Then Sarah last contact is near now
