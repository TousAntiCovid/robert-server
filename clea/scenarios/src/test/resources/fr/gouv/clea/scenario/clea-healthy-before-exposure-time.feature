  Feature: One healthy and one sick visitors visit the same location
  the healthy visitor visit the location after the exposure time
  The healthy visitor must not be warned being at risk


  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Chez Gusto" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"

  
  Scenario: One sick and two persons at risk (same location and average RiskLevel)
    Given "Heather" recorded a visit to "Chez Gusto" at "17:00, 4 days ago" withQRCode "LunchService1"
    Given "Hugo" recorded a visit to "Chez Gusto" at "13:00, 4 days ago" withQRCode "LunchService1"
  
    When "Heather" declares himself sick
    When "Heather" trigger batch processing
    When "Hugo" asks for exposure status

    Then "Heather" sends his visits 
    Then Exposure status should reports "Hugo" as not being at risk