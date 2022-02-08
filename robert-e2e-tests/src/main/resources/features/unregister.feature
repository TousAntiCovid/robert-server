# language: fr
Fonctionnalité: Désinscription

  En tant qu'utilisateur de Robert
  Je souhaite me désinscrire
  Dans le but de ne plus utiliser l'application

  Contexte:
    Etant donné que John a l'application TAC
    Et que Sarah a l'application TAC
    Et que Paul a l'application TAC

  Scénario: Un signalement contient une personne qui s'est désinscrite
    Etant donné que Paul et Sarah sont à proximité 60 minutes
    Et que Sarah se désinscrit
    Lorsque Paul se déclare malade
    Et que le batch robert est exécuté
    Alors les logs du batch robert contiennent INFO "The contact could not be validated. Discarding all its hello messages"
