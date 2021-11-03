Feature: Two people meet and one declare himself at risk

  Background:
    Given François has the application TAC
    Given Sarah has the application TAC

  Scenario: They register themself on TAC
    Given François resolve the captcha challenge
    Given Sarah resolve the captcha challenge
    Then François is registered on TAC
    Then Sarah is registered on TAC
    When Sarah is near François during 1 hour