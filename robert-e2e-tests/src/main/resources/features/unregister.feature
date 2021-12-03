Feature: Covid-19 positive declaration
  As a user
  I want to unregister my application
  In order to assert my right to be forgotten

  Background:
    Given John installs the application TAC
    Given Sarah installs the application TAC
    Given Paul installs the application TAC

  Scenario: One people unregisters his application
    Given just now, the users Paul and Sarah will be near during 60 minutes
    When Paul report himself sick
    When Sarah unregisters her application
    And robert batch has been triggered
    Then robert batch logs contains: "Could not find keys for id, discarding the hello message"

  Scenario: One people unregisters his application without having any contact
    When Sarah unregisters her application
    And robert batch has been triggered
    Then robert batch logs does not contains: "Could not find keys for id, discarding the hello message"
