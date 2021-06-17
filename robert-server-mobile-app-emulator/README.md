This module emulates the mobile application


# How to

## Enregistrer une nouvelle application

Cette procédeure doit être réalisée pour chaque application mobile qu'on désire émuler !

1. Générer un captcha 
   
Dans l'exemple de la plateforme d'intégration :

        $ curl -k --fail --header "Content-Type: application/json" --request POST https://api-int.tousanticovid.gouv.fr/api/v5/captcha
        % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
        Dload  Upload   Total   Spent    Left  Speed
        100    41    0    41    0     0    259      0 --:--:-- --:--:-- --:--:--   261{"id":"4e67f4189b154e919483865553b33a03"}
   
2. Utiliser le catpcha ID retourné précédemment afin de visualiser le captcha dans un navigateur internet, 
dans notre exemple, il faut aller à l'URL : `https://api-int.tousanticovid.gouv.fr/api/v5/captcha/4e67f4189b154e919483865553b33a03/image`
   
   Il faut récupérer la valeur du captcha

3. Enregistrer cette application mobile en faisant appel au /register exposé par l'emulateur en passant les paramètres précédemment générés
* captcha ID ==> il sera utilisé comme identifiant d'application mobile
* valeur du captcha





# TODO
* voir s'il est possible d'attaquer le submission code server depuis nos postes de dev ?
pour le moment nous avons l'erreur suivante : 

      $ curl -k https://api-int.tousanticovid.gouv.fr/generate/short
      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
      Dload  Upload   Total   Spent    Left  Speed
      100    26  100    26    0     0    288      0 --:--:-- --:--:-- --:--:--   295{"message":"Unauthorized"}

**contournement** : fourniture du qrCode au moment de la soumission d'un report, ce qui est
**problématique** pour les tests en preprod sur laquelle
nous avons besoin d'une personne de l'infogérence pour les générer !

