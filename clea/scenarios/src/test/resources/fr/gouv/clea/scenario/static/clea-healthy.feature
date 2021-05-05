Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given "Hugo" registered on TAC
    Given "Hugo" has no duplicate verification
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given "Laure" registered on TAC
    Given "Anne" registered on TAC
    Given "Julie" registered on TAC
    Given "Mahe" registered on TAC
    Given "Yäel" registered on TAC
    Given VType of "restauration", VCategory1 of "restaurant rapide" and VCategory2 of 1 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,30,3.0) and for forward (1,30,2.0)
    Given VType of "etablissements sportifs", VCategory1 of "sport indoor" and VCategory2 of 2 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,120,3.0) and for forward (1,120,2.0)
    Given VType of "etablissements sportifs", VCategory1 of "salle de sport" and VCategory2 of 2 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,60,3.0) and for forward (1,60,2.0)
    Given "Chez McDonald's" created a static QRCode at "11:00, 13 days ago" with VType as "restauration" and with VCategory1 as "restaurant rapide" and with VCategory2 as 1 and with a periodDuration of "24 hours" 
    Given "NRFight Club Olympiades" created a static QRCode at "11:00, 13 days ago" with VType as "etablissements sportifs" and with VCategory1 as "sport indoor" and with VCategory2 as 2 and with a periodDuration of "24 hours" 
    Given "OrangeBleue" created a static QRCode at "11:00, 8 days ago" with VType as "etablissements sportifs" and with VCategory1 as "salle de sport" and with VCategory2 as 2 and with a periodDuration of "24 hours" 

  Scenario: One location with duplicated visits - Exposure Time 30 min
   Given "Hugo" recorded a visit to "Chez McDonald's" at "12:30, 6 days ago"
   Given "Hugo" recorded a visit to "Chez McDonald's" at "12:35, 6 days ago"
   Given "Hugo" recorded a visit to "Chez McDonald's" at "15:35, 6 days ago"
   Given "Heather" recorded a visit to "Chez McDonald's" at "13:30, 6 days ago"
   Given "Henry" recorded a visit to "Chez McDonald's" at "11:45, 6 days ago"
   Given "Laure" recorded a visit to "Chez McDonald's" at "12:59, 6 days ago"
   Given "Anne" recorded a visit to "Chez McDonald's" at "20:30, 6 days ago"
   Given "Julie" recorded a visit to "Chez McDonald's" at "16:00, 6 days ago"
   Given "Mahe" recorded a visit to "Chez McDonald's" at "12:13, 6 days ago" 

   When "Hugo" declares himself sick with a "14 days ago" pivot date
   When "Heather" declares himself sick with a "12 days ago" pivot date
   When "Henry" declares himself sick with a "8 days ago" pivot date
   When "Laure" declares herself sick with a "7 days ago" pivot date
   When Cluster detection triggered

   When "Anne" asks for exposure status
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status

   Then "Hugo" sends his visits
   Then "Heather" sends her visits
   Then "Henry" sends her visits
   Then "Laure" sends her visits
   And "Hugo" has 1 rejected visit

   Then Exposure status should reports "Anne" as not being at risk
   Then Exposure status should reports "Julie" as being at risk of 2.0
   Then Exposure status should reports "Mahe" as being at risk of 2.0

  Scenario: One location with duplicated visits - Exposure Time 120 min
   Given "Hugo" recorded a visit to "NRFight Club Olympiades" at "12:30, 13 days ago"
   Given "Hugo" recorded a visit to "NRFight Club Olympiades" at "12:32, 13 days ago"
   Given "Hugo" recorded a visit to "NRFight Club Olympiades" at "12:56, 13 days ago"
   Given "Hugo" recorded a visit to "NRFight Club Olympiades" at "12:30, 2 days ago"
   Given "Heather" recorded a visit to "NRFight Club Olympiades" at "13:30, 13 days ago"
   Given "Henry" recorded a visit to "NRFight Club Olympiades" at "11:46, 13 days ago"
   Given "Anne" recorded a visit to "NRFight Club Olympiades" at "12:31, 13 days ago"
   Given "Julie" recorded a visit to "NRFight Club Olympiades" at "13:00, 2 days ago"
   Given "Mahe" recorded a visit to "NRFight Club Olympiades" at "20:13, 13 days ago"

   When "Hugo" declares himself sick with a "8 days ago" pivot date
   When "Heather" declares himself sick with a "6 days ago" pivot date 
   When "Henry" declares himself sick with a "7 days ago" pivot date
   When Cluster detection triggered

   When "Anne" asks for exposure status
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status

   Then "Hugo" sends his visits
   Then "Heather" sends her visits
   Then "Henry" sends her visits
   And "Hugo" has 2 rejected visits
   
   Then Exposure status should reports "Anne" as being at risk of 3.0
   Then Exposure status should reports "Julie" as being at risk of 2.0
   Then Exposure status should reports "Mahe" as not being at risk

  Scenario: One location with duplicated visits - Exposure Time 60 min
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:30, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:46, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:56, 8 days ago" 
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:57, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:58, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:59, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "16:00, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "16:01, 8 days ago"
   Given "Heather" recorded a visit to "OrangeBleue" at "13:30, 8 days ago"
   Given "Henry" recorded a visit to "OrangeBleue" at "13:46, 8 days ago"
   Given "Anne" recorded a visit to "OrangeBleue" at "14:31, 8 days ago"
   Given "Julie" recorded a visit to "OrangeBleue" at "21:00, 8 days ago"
   Given "Mahe" recorded a visit to "OrangeBleue" at "19:13, 7 days ago"

   When "Hugo" declares himself sick with a "5 days ago" pivot date
   When "Heather" declares himself sick with a "4 days ago" pivot date 
   When "Henry" declares himself sick with a "5 days ago" pivot date
   When Cluster detection triggered
  
   When "Anne" asks for exposure status
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status

   Then "Hugo" sends his visits
   Then "Heather" sends her visits
   Then "Henry" sends her visits
   And "Hugo" has 6 rejected visits

   Then Exposure status should reports "Anne" as being at risk of 3.0
   Then Exposure status should reports "Julie" as not being at risk
   Then Exposure status should reports "Mahe" as not being at risk


  Scenario: Overlaps - 3 days with 3 different location and a malformed pivot date for 1 person
   
   Given "Anne" recorded a visit to "Chez McDonald's" at "11:50, 6 days ago"
   Given "Hugo" recorded a visit to "Chez McDonald's" at "12:30, 6 days ago"
   Given "Laure" recorded a visit to "Chez McDonald's" at "12:45, 6 days ago"
   Given "Heather" recorded a visit to "Chez McDonald's" at "12:58, 6 days ago"
   Given "Julie" recorded a visit to "Chez McDonald's" at "12:50, 6 days ago"

   Given "Anne" recorded a visit to "NRFight Club Olympiades" at "11:58, 4 days ago" 
   Given "Mahe" recorded a visit to "NRFight Club Olympiades" at "11:50, 4 days ago"
   Given "Henry" recorded a visit to "NRFight Club Olympiades" at "11:46, 4 days ago" 
   Given "Hugo" recorded a visit to "NRFight Club Olympiades" at "12:30, 4 days ago" 
   Given "Julie" recorded a visit to "NRFight Club Olympiades" at "13:45, 4 days ago" 
   Given "Yäel" recorded a visit to "NRFight Club Olympiades" at "13:45, 10 days ago"  
   Given "Anne" recorded a visit to "NRFight Club Olympiades" at "14:06, 4 days ago"
    
   Given "Anne" recorded a visit to "OrangeBleue" at "14:03, 8 days ago"
   Given "Mahe" recorded a visit to "OrangeBleue" at "14:12, 8 days ago"
   Given "Hugo" recorded a visit to "OrangeBleue" at "15:30, 8 days ago"
   Given "Heather" recorded a visit to "OrangeBleue" at "16:30, 8 days ago"

   When "Henry" declares himself sick with a "3 days ago" pivot date
   When "Laure" declares herself sick with a "5 days ago" pivot date   
   When Cluster detection triggered
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status
   Then "Henry" sends her visits
   Then "Laure" sends her visits
   Then Exposure status should reports "Julie" as not being at risk
   Then Exposure status should reports "Mahe" as not being at risk
      
   When "Hugo" declares himself sick with a "7 days ago" pivot date
   When "Heather" declares himself sick with a "5 days ago" pivot date
   When Cluster detection triggered
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status
   Then "Hugo" sends his visits
   Then "Heather" sends her visits
   Then Exposure status should reports "Julie" as being at risk of 2.0
   Then Exposure status should reports "Mahe" as being at risk of 2.0
   
   When "Anne" declares herself sick with a "5 days ago" pivot date
   When "Yäel" declares himself sick with a malformed pivot date
   When Cluster detection triggered
   When "Julie" asks for exposure status
   When "Mahe" asks for exposure status
   Then "Anne" sends her visits
   Then "Yäel" cannot send his visits
   Then Exposure status should reports "Julie" as being at risk of 2.0
   Then Exposure status should reports "Mahe" as being at risk of 2.0
   
   Scenario: Visits staff trigger a cluster of no STAFF visits
   Given "Hugo" recorded a visit to "Chez McDonald's" at "20:32, 6 days ago" as a STAFF
   Given "Henry" recorded a visit to "Chez McDonald's" at "20:40, 6 days ago" as a STAFF
   Given "Laure" recorded a visit to "Chez McDonald's" at "20:30, 6 days ago" as a STAFF
   Given "Julie" recorded a visit to "Chez McDonald's" at "20:55, 6 days ago"

   When "Hugo" declares himself sick with a "5 days ago" pivot date
   When "Henry" declares himself sick with a "3 days ago" pivot date
   When "Laure" declares herself sick with a "5 days ago" pivot date
   When Cluster detection triggered

   When "Julie" asks for exposure status

   Then "Hugo" sends his visits
   Then "Henry" sends his visits
   Then "Laure" sends her visits

   Then Exposure status should reports "Julie" as being at risk of 3.0


  Scenario: Nominal case
    Given "Hugo" recorded a visit to "Chez McDonald's" at "12:30, 4 days ago" 
    Given "Henry" recorded a visit to "Chez McDonald's" at "11:30, 4 days ago" 
    Given "Heather" recorded a visit to "Chez McDonald's" at "13:35, 4 days ago" 
  
    When "Heather" declares himself sick with a "5 days ago" pivot date
    When Cluster detection triggered
    When "Hugo" asks for exposure status
    When "Henry" asks for exposure status

    Then "Heather" sends his visits
    Then Exposure status should reports "Hugo" as not being at risk
    Then Exposure status should reports "Henry" as not being at risk 

    Scenario: Duplicated QR code 
    Given "Hugo" recorded a visit to "Chez McDonald's" at "12:30, 6 days ago"
    Given "Hugo" recorded a visit to "Chez McDonald's" at "12:35, 6 days ago"
    Given "Laure" recorded a visit to "Chez McDonald's" at "12:59, 6 days ago"
    When "Hugo" declares himself sick with a "14 days ago" pivot date
    When Cluster detection triggered
    Then "Hugo" sends his visits
    And "Hugo" has 1 rejected visit
    Then Exposure status should reports "Laure" as being at risk of 2.0 

     Scenario: Malformed pivot date (not in timestamp)
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago"  
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with a malformed pivot date
     When Cluster detection triggered
     Then "Yäel" cannot send his visits
     Then Exposure status should reports "Julie" as not being at risk

     Scenario: Malformed QrCode 
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago"  
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with a malformed QrCode 
     When Cluster detection triggered
     Then "Yäel" cannot send his visits
     Then Exposure status should reports "Julie" as not being at risk

     Scenario: No QrCODE 
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago" 
     When "Yäel" declares himself sick with a "5 days ago" pivot date 
     When Cluster detection triggered
     Then "Yäel" cannot send his visits
     Then Exposure status should reports "Julie" as not being at risk

     Scenario: Malformed scan time 
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago"  
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with a malformed scan time
     When Cluster detection triggered
     Then "Yäel" cannot send his visits
     Then Exposure status should reports "Julie" as not being at risk

     Scenario: No scan time
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago" 
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with no scan time
     When Cluster detection triggered
     Then "Yäel" cannot send his visits
     Then Exposure status should reports "Julie" as not being at risk

     
     Scenario: ERROR - A DEVELOPPER Pivot date in the past
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago" 
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with a "18 days ago" pivot date
     When Cluster detection triggered
     Then "Yäel" sends his visits
     Then Exposure status should reports "Julie" as being at risk of 2

     Scenario: ERROR - A DEVELOPPER Pivot date in the future
     Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago" 
     Given "Julie" recorded a visit to "Chez McDonald's" at "13:40, 4 days ago" 
     When "Yäel" declares himself sick with a "in 3 days" pivot date
     When Cluster detection triggered
     Then "Yäel" sends his visits
     Then Exposure status should reports "Julie" as being at risk of 2

     Scenario: ERROR - A DEVELOPPER Duplicated QR code STAFF and NO STAFF
     Given "Hugo" recorded a visit to "Chez McDonald's" at "12:30, 6 days ago" as a STAFF
     Given "Hugo" recorded a visit to "Chez McDonald's" at "12:35, 6 days ago"
     Given "Laure" recorded a visit to "Chez McDonald's" at "12:56, 6 days ago"
     When "Hugo" declares himself sick with a "14 days ago" pivot date
     When Cluster detection triggered
     Then "Hugo" sends his visits
     And "Hugo" has 1 rejected visit
     Then Exposure status should reports "Laure" as being at risk of 2.0 
  

    Scenario: ERROR - A DEVELOPPER malformed Token (validity period passed)
    Given "Yäel" recorded a visit to "Chez McDonald's" at "13:45, 10 days ago"  
    Given "Julie" recorded a visit to "Chez McDonald's" at "13:45, 4 days ago" 
    When "Yäel" declares himself sick with a malformed token 
    When Cluster detection triggered
    Then "Yäel" cannot send his visits
    Then Exposure status should reports "Julie" as not being at risk