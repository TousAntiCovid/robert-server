Feature: One healthy visitor visits a single place simultaneously to single sick visitor
  The healthy visitor must be warned being at risk

  Background:
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Robert" registered on TAC
    Given "CinemaCGR" created a static QRCode "Sceance20h" as a "cinema" with a capacity of 100 and category "NUMBER_1"

  
  Scenario: Robert meets Stephanie
    Given "Robert" recorded a visit to "CinemaCGR" at "20:05, 3 days ago" with static QRCode "Sceance20h"
    Given "Stephanie" recorded a visit to "CinemaCGR" at "19:58, 3 days ago" with static QRCode "Sceance20h"
    Given "Stephanie" scanned covid positive QRCode
    Given "Stephanie" reported to TACWarning a valid covid19 positive QRCode
    When "Robert" asks for exposure status
    Then Exposure status should reports "Robert" as not being at risk

  Scenario: Robert meets Stephanie, Sophie and Sylvie
    Given "Robert" recorded a visit to "CinemaCGR" at "20:05, 1 days ago" with static QRCode "Sceance20h"
    Given "Stephanie" recorded a visit to "CinemaCGR" at "19:58, 1 days ago" with static QRCode "Sceance20h"
    Given "Sylvie" recorded a visit to "CinemaCGR" at "19:50, 1 days ago" with static QRCode "Sceance20h"
    Given "Sophie" recorded a visit to "CinemaCGR" at "20:03, 1 days ago" with static QRCode "Sceance20h"
    Given "Stephanie" scanned covid positive QRCode
    Given "Stephanie" reported to TACWarning a valid covid19 positive QRCode
    Given "Sylvie" scanned covid positive QRCode
    Given "Sylvie" reported to TACWarning a valid covid19 positive QRCode
    Given "Sophie" scanned covid positive QRCode
    Given "Sophie" reported to TACWarning a valid covid19 positive QRCode
    When "Robert" asks for exposure status
    Then Exposure status should reports "Robert" as being at low level risk

#
#  Scenario:
#    Given "Hugo" recorded a visit to "Chez Gusto" at 12:30, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" recorded a visit to "Chez Gusto" at 11:55, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" reported being covid19 positive # TODO with ROBERT QRCode
#    When "Hugo" asks for exposure status
#    Then Exposure status should reports "Hugo" as being at risk
