@beforeEmptyBase
Feature: One person deletes her exposure history from TAC

  Scenario: Hugo deletes his exposure history from TAC
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Hugo" met "Stephanie"
    When "Hugo" deletes his exposure history from TAC
    Then "Hugo"'s application acknowledges a successful operation message for exposure history deletion