Feature: One Healthy visitor meets two sick visitor
  Visits are simultaneous
  The healthy visitor must be warned being at risk
  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given "Laure" registered on TAC
    Given "Chez Gusto" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"


  Scenario: One sick and two persons at risk (same location and high RiskLevel)
    Given "Heather" recorded a visit to "Chez Gusto" at "13:30, 4 days ago" withQRCode "LunchService1"
    Given "Laure" recorded a visit to "Chez Gusto" at "14:00, 4 days ago" withQRCode "LunchService1"
    Given "Henry" recorded a visit to "Chez Gusto" at "14:30, 4 days ago" withQRCode "LunchService1"
    Given "Hugo" recorded a visit to "Chez Gusto" at "15:00, 4 days ago" withQRCode "LunchService1"
     
    When "Heather" declares himself sick with pivot date : "2 days ago"
    When "Henry" declares himself sick with pivot date : "2 days ago"
    When "Laure" declares herself sick with pivot date : "2 days ago"
    When Cluster detection triggered

    Then "Heather" sends his visits
    Then "Henry" sends his visits 
    Then "Laure" sends her visits 
    Then Exposure status should reports "Hugo" as being at risk