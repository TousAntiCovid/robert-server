Feature: Fanch declare himself at risk

  Background:
    Given "Fanch" has the application TAC
    Given "Sarah" has the application TAC

  Scenario: They register themself on TAC
    Given "Fanch" registred on TAC with the Captcha sevice
    Given "Sarah" registred on TAC with the Captcha sevice
    Then "Fanch" is registed on TAC
    Then "Sarah" is registed on TAC

    When "Fanch" is near "Sarah" during 1 hour
    When "François" declare himself sick with a long code
#    When parameters scoring are valid
#--    Then batch scoring should reports that "Sarah"  is being at risk of 4

#    When "Sarah" asks for exposure status
#    Then Est qu'on liste les champs envoyés à l'App?
#    Then "Sarah" is notified at risk

#    When "Sarah" asks to delete her history
#    When "François" asks to delete her history
#    Then "Sarah" delete her history
#    Then "François" delete her history
#
#    When "Sarah" asks for unregisted to TAC
#    When "François" asks for unregisted to TAC
#    Then "Sarah" is unregisted to TAC
#    Then "François" is unregisted to TAC