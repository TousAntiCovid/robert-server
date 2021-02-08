Feature: Status checking
    As a client I want to check my status

Background: TAC Warning testing
    Given I have the following visits in the tac_warning
        | who           | where      | when               | covidStatus | outcome
        | hugo          | restaurant | yesterday at 12:30 | negative    | high level risk
        | stacy         | restaurant | yesterday at 12:15 | positive    | high level risk
        | steeven       | restaurant | yesterday at 12:03 | positive    | high level risk
        | heather       | restaurant | yesterday at 12:30 | negative    | high level risk
        | serena        | restaurant | yesterday at 11:15 | positive    | high level risk
        | stephen       | restaurant | yesterday at 13:30 | positive    | high level risk
        | henry         | restaurant | yesterday at 12:30 | negative    | high level risk
        | stephanie     | restaurant | yesterday at 12:15 | positive    | high level risk


Scenario: Find status by client
    When Covid+ person have not reported covid test to TAC
    Then Covid- person status from TAC-W is not at risk

Scenario: Positive tests are reported
    When Covid+ person report to TAC and TAC-W
    Then Covid- person status from TAC-W is at high level risk
