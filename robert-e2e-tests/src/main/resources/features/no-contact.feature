Feature: No contact

  Scenario: No contact
    Given François installs the application TAC
    Given Sarah installs the application TAC
    When François report himself sick
    When robert batch has been triggered
    Then Sarah has no notification
