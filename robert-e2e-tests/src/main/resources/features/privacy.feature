# language: fr
Fonctionnalité: Respect des conditions générales sur l'usage des données

  En tant qu'utilisateur de Robert
  Je souhaite que mes données soient supprimées
  Dans le but de faire respecter mon droit

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Scénario: Les données d'exposition sont supprimées au bout de 15 jours
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a 15 jours et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors les données d'exposition de Sarah n'existent plus
