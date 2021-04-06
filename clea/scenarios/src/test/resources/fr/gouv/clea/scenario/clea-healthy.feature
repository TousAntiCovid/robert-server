Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk
  The venues use a single QRCode for the whole day

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given "Henry" registered on TAC
    Given "Chez Gusto" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "Chez Gusto" created a QRCode "LunchService2" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "La fontaine aux perles" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"
    Given "La fontaine aux perles" created a QRCode "LunchService1" as a "restaurant" at "04:00am, 10 days ago" with a capacity of 20 and category "NUMBER_1" and with a renewal time of "15 minutes"

  Scenario: One healthy visitor alone
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" withQRCode "LunchService1"
    When "Hugo" asks for exposure status
    Then Exposure status should reports "Hugo" as not being at risk
  
  Scenario: One healthy visitor alone
    Given "Heather" recorded a visit to "Chez Gusto" at "12:00, 2 days ago" withQRCode "LunchService1"
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk    


  Scenario: two simultaneous healthy visitors (same location)
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" withQRCode "LunchService1"
    Given "Henry" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" withQRCode "LunchService1"
    When "Hugo" asks for exposure status
    When "Henry" asks for exposure status
    Then Exposure status should reports "Hugo" as not being at risk
    Then Exposure status should reports "Henry" as not being at risk

  Scenario: two simultaneous healthy visitors (different location)
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" withQRCode "LunchService1"
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" withQRCode "LunchService1"
    When "Hugo" asks for exposure status
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk
    Then Exposure status should reports "Hugo" as not being at risk

  Scenario: two overlapping healthy visitors (within the same hour)
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" withQRCode "LunchService1"
    Given "Hugo" recorded a visit to "Chez Gusto" at "11:45, 2 days ago" withQRCode "LunchService1"
    When "Hugo" asks for exposure status
    When "Heather" asks for exposure status
    Then Exposure status should reports "Heather" as not being at risk  
    Then Exposure status should reports "Hugo" as not being at risk

  Scenario: Multiple scans of the qrcode by same visitor within the dupScanThreshold of 3 hours
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" withQRCode "LunchService1"
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:47, 2 days ago" withQRCode "LunchService1"
    When "Heather" asks for exposure status
    Then Exposure status request for "Heather" should include only 1 visit(s) to "La fontaine aux perles" at "2 days ago"

  Scenario: Multiple scans of the qrcode by same visitor outside of the dupScanThreshold of 3 hours
    Given "Heather" recorded a visit to "La fontaine aux perles" at "12:30, 2 days ago" withQRCode "LunchService1"
    Given "Heather" recorded a visit to "La fontaine aux perles" at "19:47, 2 days ago" withQRCode "LunchService1"
    When "Heather" asks for exposure status
    Then Exposure status request for "Heather" should include only 2 visit(s) to "La fontaine aux perles" at "2 days ago"

  Scenario: One sick and two persons at risk (same location and average RiskLevel)
    Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 4 days ago" withQRCode "LunchService1"
    Given "Henry" recorded a visit to "Chez Gusto" at "11:30, 4 days ago" withQRCode "LunchService1"
    Given "Heather" recorded a visit to "Chez Gusto" at "13:30, 4 days ago" withQRCode "LunchService1"
  
    When "Heather" declares himself sick
    When "Heather" trigger batch processing
    When "Hugo" asks for exposure status
    When "Henry" asks for exposure status

    Then "Heather" sends his visits 
    Then Exposure status should reports "Hugo" as being at risk
    Then Exposure status should reports "Henry" as being at risk
