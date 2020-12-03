Feature: One healthy visitor visits a single place simultaneously to single sick visitor
  The healthy visitor must be warned being at risk

  Background:
    Given "Hugo" registered on TAC
    Given "Stephanie" registered on TAC
    Given "Chez Gusto" created a static QRCode "LunchService" as a "restaurant" with a capacity of 20 and category "CAT1"
    Given "Chez Gusto" created a static QRCode "DinerService" as a "restaurant" with a capacity of 20 and category "CAT1"
    
  Scenario:
    Given "Hugo" recorded a visit to "Chez Gusto" at 12:30, 2 days ago with static QRCode "LunchService"
    Given "Stephanie" recorded a visit to "Chez Gusto" at 12:40, 2 days ago with static QRCode "LunchService"
    Given "Stephanie" scanned covid positive QRCode
    Given "Stephanie" reported to TACWarning a valid covid19 positive QRCode
    When "Hugo" asks for exposure status
    Then Exposure status should reports "Hugo" as being at risk
  
#  Scenario:
#    Given "Hugo" recorded a visit to "Chez Gusto" at 12:30, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" recorded a visit to "Chez Gusto" at 13:00, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" reported being covid19 positive # TODO with ROBERT QRCode
#    When "Hugo" asks for exposure status
#    Then Exposure status should reports "Hugo" as being at risk 
#   
#  Scenario:
#    Given "Hugo" recorded a visit to "Chez Gusto" at 12:30, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" recorded a visit to "Chez Gusto" at 11:55, 2 days ago with static QRCode "LunchService"
#    Given "Stephanie" reported being covid19 positive # TODO with ROBERT QRCode 
#    When "Hugo" asks for exposure status
#    Then Exposure status should reports "Hugo" as being at risk 
    