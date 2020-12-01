Feature: TAC Warning basic scenarii
  If I visited a venue along with a number of covid19-negative people,
  I would like my TAC app to not warn me that I am at risk of contracting covid19

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    
  Scenario:
    Given "Hugo" recorded a visit to the "restaurant" "Chez Gusto" at 12:30, 2 days ago
    When "Hugo" asks for exposure status
    Then Exposure status should reports "Hugo" as not being at risk
  
  Scenario:
    Given "Heather" recorded a visit to the "restaurant" "La fontaine aux perles" at 12:30, 2 days ago
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk    