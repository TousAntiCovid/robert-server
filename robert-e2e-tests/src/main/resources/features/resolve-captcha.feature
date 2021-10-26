Feature: Resolve captcha

  Background:
    Given François has the application TAC
    Given Sarah has the application TAC

  Scenario: Request and resolve captcha
    Given François application request a captcha challenge id
    Given Sarah application request a captcha challenge id
    Given François application request an image captcha challenge with the previously received id
    Given Sarah application request an image captcha challenge with the previously received id
    Then François resolve the Captcha image into the application
    Then Sarah resolve the Captcha image into the application