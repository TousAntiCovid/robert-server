# language: fr
Fonctionnalité: Suppression de l'historique d'exposition

  En tant qu'utilisateur de Robert
  Je souhaite supprimer mon historique d'exposition
  Dans le but de faire respecter mon droit de modification sur mes données

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Scénario: Une personne supprime son historique d'exposition
    Etant donné que Paul et Sarah étaient à proximité 15 minutes il y a 1 heure
    Et que Paul se déclare malade maintenant
    Et que le batch robert est exécuté
    Lorsque Sarah supprime son historique d'exposition
    Alors les données d'exposition de Sarah n'existent plus

  Scénario: Une personne sans historique demande la suppression de son historique
    Etant donné que l'on est aujourd'hui
    Lorsque Sarah supprime son historique d'exposition
    Alors les données d'exposition de Sarah n'existent plus
