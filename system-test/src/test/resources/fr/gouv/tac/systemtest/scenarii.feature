Feature: Status checking
    As a client I want to check my status

Background: TAC Warning testing
    Given I have the following visits in the tac_warning
        | who           | where      | when               | covidStatus | TAC-W Status
        | hugo          | restaurant | yesterday at 12:30 | negative    | warning
        | stacy         | restaurant | yesterday at 12:15 | positive    | NA
        | steeven       | restaurant | yesterday at 12:03 | positive    | NA
        | heather       | restaurant | yesterday at 12:30 | negative    | warning
        | serena        | restaurant | yesterday at 11:15 | positive    | NA
        | stephen       | restaurant | yesterday at 13:30 | positive    | NA
        | henry         | restaurant | yesterday at 12:30 | negative    | warning
        | stephanie     | restaurant | yesterday at 12:15 | positive    | NA


Scenario: Find status by client
    When Covid+ person have not reported covid test to TAC
    Then Covid- person status from TAC-W is false

Scenario: Positive tests are reported
    When Covid+ person report to TAC and TAC-W
    Then Covid- person status from TAC-W is true
