  Feature: Several healthy visitors and a single sick visitor visit different places
  Visits are simultaneous or not
  The healthy visitors must be warned being at risk


  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given VType of "restaurant", VCategory1 of "fastfood" and VCategory2 of 1 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,60,3.0) and for forward (1,60,2.0)
    Given "Chez Gusto" created a dynamic QRCode at "04:00am, 10 days ago" with VType as "restaurant" and with VCategory1 as "fastfood" and with VCategory2 as 1 and with a renewal time of "15 minutes" and with a periodDuration of "24 hours"


  
  Scenario: One sick and two persons at risk (same location and average RiskLevel)
    Given "Hugo" recorded a visit to "Chez Gusto" at "14:45, 4 days ago"
    Given "Henry" recorded a visit to "Chez Gusto" at "14:00, 4 days ago"
    Given "Heather" recorded a visit to "Chez Gusto" at "13:30, 4 days ago"
  
    When "Heather" declares himself sick
    When Cluster detection triggered

    Then "Heather" sends his visits 
    Then Exposure status should reports "Hugo" as being at risk of 2.0
    Then Exposure status should reports "Henry" as being at risk of 2.0