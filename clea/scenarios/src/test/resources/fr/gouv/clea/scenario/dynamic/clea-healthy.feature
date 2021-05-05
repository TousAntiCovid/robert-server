Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given VType of "restaurant", VCategory1 of "fastfood" and VCategory2 of 1 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,60,3.0) and for forward (1,60,2.0)
    Given "Chez Gusto" created a dynamic QRCode at "04:00am, 10 days ago" with VType as "restaurant" and with VCategory1 as "fastfood" and with VCategory2 as 1 and with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
    Given "La fontaine aux perles" created a dynamic QRCode at "04:00am, 10 days ago" with VType as "restaurant" and with VCategory1 as "fastfood" and with VCategory2 as 1 and with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
  

  Scenario: One healthy visitor alone
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago"
    When Cluster detection triggered
    Then Exposure status should reports "Hugo" as not being at risk
  
  Scenario: One healthy visitor alone
    Given "Heather" recorded a visit to "Chez Gusto" at "12:00, 2 days ago"
    When Cluster detection triggered
    Then Exposure status should reports "Heather" as not being at risk    


  Scenario: two simultaneous healthy visitors (same location)
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago"
    Given "Henry" recorded a visit to "Chez Gusto" at "12:30, 2 days ago"
    When Cluster detection triggered
    Then Exposure status should reports "Hugo" as not being at risk
    Then Exposure status should reports "Henry" as not being at risk

  Scenario: two simultaneous healthy visitors (different location)
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago"
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago"
    When Cluster detection triggered
    Then Exposure status should reports "Heather" as not being at risk
    Then Exposure status should reports "Hugo" as not being at risk

  Scenario: two overlapping healthy visitors (within the same hour)
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago"
    Given "Hugo" recorded a visit to "Chez Gusto" at "11:45, 2 days ago"
    When Cluster detection triggered
    Then Exposure status should reports "Heather" as not being at risk  
    Then Exposure status should reports "Hugo" as not being at risk

  Scenario: Multiple scans of the qrcode by same visitor within the dupScanThreshold of 3 hours
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago"
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:47, 2 days ago"
    When "Heather" declares himself sick
    When Cluster detection triggered
    Then "Heather" has 1 rejected visit

  Scenario: Multiple scans of the qrcode by same visitor outside of the dupScanThreshold of 3 hours
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago"
    Given "Heather" recorded a visit to "La fontaine aux perles" at "19:47, 2 days ago"
    When "Heather" declares himself sick
    When Cluster detection triggered
    Then "Heather" has 0 rejected visit
