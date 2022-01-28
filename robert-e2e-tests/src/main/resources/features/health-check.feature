@Smoke
Feature: Health check

  Scenario: Robert is up
    Given application robert ws rest is ready

  Scenario: Postgresql is ready
    Given John installs the application TAC
    Given Paul installs the application TAC
    Given we are 5 days ago in the past
    Given tomorrow at 23:45, the users Paul and John will be near during 15 minutes
    When Paul report himself sick
    And robert batch has been triggered
    And John delete her risk exposure history