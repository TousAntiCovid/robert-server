# language: fr
Fonctionnalité: Respect des conditions générales sur l'usage des données

  En tant qu'utilisateur de Robert
  Je souhaite que mes données soient supprimées

  Scénario: Les données d'exposition sont supprimées au bout de 15 jours
    Etant donné que on est il y a 15 jours
    Et que John et Sarah avaient l'application TAC
    Et que Sarah et John étaient à proximité 60 minutes
    Et que John s'est déclaré malade
    Lorsque on est aujourd'hui
    Et que le batch robert est exécuté
    Alors les données de Sarah n'existent plus
