  Feature: Several healthy visitors and a single sick visitor visit different places
  Visits are simultaneous or not
  The healthy visitors must be warned being at risk


  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given "Chez Gusto" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "Chez Gusto" created a QRCode "LunchService2" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "La fontaine aux perles" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "La fontaine aux perles" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"


  
  Scenario: One sick and two persons at risk (same location and average RiskLevel)
    Given "Hugo" recorded a visit to "Chez Gusto" at "15:30, 4 days ago" withQRCode "LunchService1"
    Given "Henry" recorded a visit to "Chez Gusto" at "14:30, 4 days ago" withQRCode "LunchService1"
    Given "Heather" recorded a visit to "Chez Gusto" at "13:30, 4 days ago" withQRCode "LunchService1"
  
    When "Heather" declares himself sick
    When "Heather" trigger batch processing
    When Cluster detection triggered

    Then "Heather" sends his visits 
    Then Exposure status should reports "Hugo" as being at risk
    Then Exposure status should reports "Henry" as being at risk