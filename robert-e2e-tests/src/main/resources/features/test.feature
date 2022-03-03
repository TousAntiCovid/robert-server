# language: fr
Fonctionnalité: Test

  Scénario: Test
    Etant donné que on est il y a 15 jours
    Et que John, Sarah et Paul ont l'application TAC
    Et que John et Sarah sont à proximité 60 minutes
    Etant donné que on est il y a 6 jours
    Et que Sarah et Paul sont à proximité 60 minutes
    Lorsque on est aujourd'hui
    Et que Sarah se déclare malade
    Et que le batch robert est exécuté
    Alors Paul est à risque
    Mais John n'est pas à risque

