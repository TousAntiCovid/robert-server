Feature: Fanch declare himself at risk

  Background:
    Given "Fanch" has the application TAC
    Given "Sarah" has the application TAC

  Scenario: They register themself on TAC
    Given "Fanch" registered on TAC with the Captcha service
    Given "Sarah" registered on TAC with the Captcha service
    Then "Fanch" is registed on TAC
    Then "Sarah" is registed on TAC

    When "Fanch" is near "Sarah" during 1 hour
    When "Fanch" declare himself sick with a long code
#    When parameters scoring are valid
#--    Then batch scoring should reports that "Sarah"  is being at risk of 4

    When "Sarah" asks for exposure status
#    Then Est qu'on liste les champs envoyés à l'App?
    Then "Sarah" is notified at risk

    When "Sarah" asks to delete her history
    When "Fanch" asks to delete her history
#    Then "Sarah" delete her history --> refaire un status ?
#    Then "Fanch" delete her history
#
    When "Sarah" ask to be unsubscribed from TAC
    When "Fanch" ask to be unsubscribed from TAC
#    Then "Sarah" is unregisted to TAC
#    Then "Fanch" is unregisted to TAC