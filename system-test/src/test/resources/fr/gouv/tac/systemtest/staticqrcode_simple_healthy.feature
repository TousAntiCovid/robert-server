Feature: Several healthy visitors visit differents places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk
  The venues use a single Static QRCode for the whole day

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Chez Gusto" created a static QRCode "LunchService" as a "restaurant" with a capacity of 20 and category "NUMBER_1"
    Given "Chez Gusto" created a static QRCode "DinerService" as a "restaurant" with a capacity of 20 and category "NUMBER_1"
    Given "La fontaine aux perles" created a static QRCode "LunchService" as a "restaurant" with a capacity of 20 and category "NUMBER_1"
    Given "La fontaine aux perles" created a static QRCode "DinerService" as a "restaurant" with a capacity of 20 and category "NUMBER_1"
    
  Scenario: One healthy visitor alone
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" with static QRCode "LunchService"
    When "Hugo" asks for exposure status
    Then Exposure status should reports "Hugo" as not being at risk
  
  Scenario: One healthy visitor alone
    Given "Heather" recorded a visit to "La fontaine aux perles" at "10:00, 2 days ago" with static QRCode "LunchService"
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk    
    
  Scenario: two simultaneous healthy visitors
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" with static QRCode "LunchService"
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" with static QRCode "LunchService"
    When "Hugo" asks for exposure status
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk  
    Then Exposure status should reports "Hugo" as not being at risk

  Scenario: two overlapping healthy visitors
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" with static QRCode "LunchService"
    Given "Hugo" recorded a visit to "Chez Gusto" at "11:55, 2 days ago" with static QRCode "LunchService"
    When "Hugo" asks for exposure status
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk  
    Then Exposure status should reports "Hugo" as not being at risk
    
  Scenario: two overlapping healthy visitors
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" with static QRCode "LunchService"
    Given "Hugo" recorded a visit to "Chez Gusto" at "13:00, 2 days ago" with static QRCode "LunchService"
    When "Hugo" asks for exposure status
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk  
    Then Exposure status should reports "Hugo" as not being at risk    