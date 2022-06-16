# language: fr
Fonctionnalité: Respect des conditions générales sur l'usage des données

  En tant qu'utilisateur de Robert
  Je souhaite que mes données soient supprimées

  Contexte:
    Etant donné que l'on est il y a 15 jours
    Et que John et Sarah ont l'application TAC

  Scénario: Les données d'exposition sont supprimées au bout de 15 jours
    Etant donné que Sarah et John étaient à proximité 60 minutes
    Et que John se déclare malade
    Lorsque le batch robert est exécuté
    Alors les données de Sarah n'existent plus
