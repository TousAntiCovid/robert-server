Feature: TAC Warning basic scenarii
  If I visited a venue along with a number of covid19-negative people,
  I would like my TAC app to not warn me that I am at risk of contracting covid19

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Chez Gusto" created a static QRCode "LunchService" as a "restaurant" with a capacity of 20 and category "CAT1"
    Given "Chez Gusto" created a static QRCode "DinerService" as a "restaurant" with a capacity of 20 and category "CAT1"
    Given "La fontaine aux perles" created a static QRCode "LunchService" as a "restaurant" with a capacity of 20 and category "CAT1"
    Given "La fontaine aux perles" created a static QRCode "DinerService" as a "restaurant" with a capacity of 20 and category "CAT1"
    
  Scenario:
    Given "Hugo" recorded a visit to "Chez Gusto" at 12:30, 2 days ago with static QRCode "LunchService"
#    Given "Hugo" recorded a visit to the "restaurant" "Chez Gusto" at 12:30, 2 days ago
    When "Hugo" asks for exposure status
    Then Exposure status should reports "Hugo" as not being at risk
  
  Scenario:
    Given "Heather" recorded a visit to "La fontaine aux perles" at 12:30, 2 days ago with static QRCode "LunchService"
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk    