Feature: Bluetooth contact tracing

  As a user,
  I want to be notified when I met someone having COVID19
  In order to protect my neighbors

  Background:
    Given François installs the application TAC
    Given Sarah installs the application TAC

  Scenario: They install the application TAC
    Given two days ago at 12:00, François was near Sarah during 45 minutes
    When François report himself sick