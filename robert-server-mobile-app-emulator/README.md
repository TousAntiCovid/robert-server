# robert-server-mobile-app-emulator

This module emulates mobile applications

## Utilisation

Cette procédure doit être réalisée pour chaque application mobile qu'on désire émuler.

1. Générer un captcha

Pour la plateforme d'intégration :

    $ curl --request POST https://api-int.tousanticovid.gouv.fr/api/v5/captcha
    {"id":"4e67f4189b154e919483865553b33a03"}

2.  Utiliser le catpcha ID retourné précédemment afin de visualiser le captcha dans un navigateur internet,  
    dans notre exemple, il faut aller à l'URL : `https://api-int.tousanticovid.gouv.fr/api/v5/captcha/4e67f4189b154e919483865553b33a03/image`.  
    Il faut récupérer la valeur du captcha.

3.  Enregistrer cette application mobile en faisant appel `POST /api/v1/emulator/register` exposé par l'emulateur en passant les paramètres précédemment générés.

        $ cat - <<EOR | curl --fail --silent --header "content-type: application/json" -x post --data @- http://localhost:8180/api/v1/emulator/register
        {
          "captchaid": "4e67f4189b154e919483865553b33a03",
          "captcha": "sgic"      # sgic étant la solution de l'image captcha
        }
        EOR

4.  Il faut répéter les étapes 1 à 3 autant de fois qu'on souhaite d'utilisateurs.

5.  Démarrer des échanges de HelloMessages avec `POST /api/v1/emulator/helloMessageExchanges`

        $ cat - <<EOR | curl --silent --header "Content-Type: application/json" -X POST --data @- http://localhost:8180/api/v1/emulator/helloMessageExchanges
        {
          "captchaId": "4e67f4189b154e919483865553b33a03",
          "frequencyInSeconds": 1,
          "captchaIdOfOtherApps": [ "<autre captcha id>", "<autre captcha id>", "<...>"  ]
        }
        EOR

6.  Arrêter des échanges de HelloMessages avec `DELETE /api/v1/emulator/helloMessageExchanges?captchaId=<id captcha>`

        $ curl --fail --silent -X DELETE http://localhost:8180/api/v1/emulator/helloMessageExchanges?captchaId=4e67f4189b154e919483865553b33a03

7.  Obtenir le status d'une application avec `POST /api/v1/emulator/status?captchaId=<id captcha>`

        $ curl --fail --silent -X POST http://localhost:8180/api/v1/emulator/status?captchaId=4e67f4189b154e919483865553b33a03
        {"riskLevel":0}

8.  Obtenir un code pour se déclarer malade aupres du submission-code-server. Cette opération doit se faire sur l'une des machines où il est déployé.

        # sur l'un des instances submission code server
        $ curl localhost:8080/api/v1/generate/short
        {"code":"TMN2ZU", ...}

9.  Déclarer un utilisateur malade avec le code récupéré précédemment :

        cat - <<EOR | curl --fail --silent --header "Content-Type: application/json" -X POST --data @- http://localhost:8180/api/v1/emulator/report
        {
          "captchaId": "4e67f4189b154e919483865553b33a03",
          "qrCode": "TMN2ZU"
        }
        EOR

10. Déclencher l'exécution du batch robert `systemctl start robert-server.batch`

11. Obtenir de nouveau le status d'une application avec `POST /api/v1/emulator/status?captchaId=<id captcha>` (cf. étape 6).
