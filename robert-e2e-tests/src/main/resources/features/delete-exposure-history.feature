# language: fr
Fonctionnalité: Suppression de l'historique d'exposition

  En tant qu'utilisateur de Robert
  Je souhaite supprimer mon historique d'exposition
  Dans le but de faire respecter mon droit de modification sur mes données

  Contexte:
    Etant donné que l'on est aujourd'hui
    Et que Sarah et Paul ont l'application TAC

  Scénario: Une personne supprime son historique d'exposition
    Etant donné que Paul et Sarah sont à proximité 15 minutes
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Lorsque Sarah supprime son historique d'exposition
    Alors les données de Sarah n'existent plus

  Scénario: Une personne sans historique demande la suppression de son historique
    Lorsque Sarah supprime son historique d'exposition
    Alors les données de Sarah n'existent plus
