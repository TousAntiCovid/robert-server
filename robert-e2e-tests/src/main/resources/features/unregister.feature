# language: fr
Fonctionnalité: Désinscription

  En tant qu'utilisateur de Robert
  Je souhaite me désinscrire
  Dans le but de ne plus utiliser l'application

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Scénario: Un utilisateur peut faire retirer complètement son compte
    Etant donné que l'on est aujourd'hui
    Lorsque Sarah se désinscrit
    Alors le compte de Sarah et ses données n'existent plus

  Scénario: Un signalement contient une personne qui s'est désinscrite
    Etant donné que Paul et Sarah étaient à proximité 60 minutes il y a 5 jours
    Et que Sarah s'est désinscrite aujourd'hui
    Lorsque Paul se déclare malade
    Et que le batch robert est exécuté
    Alors les logs du batch robert contiennent INFO "The contact could not be validated. Discarding all its hello messages"
