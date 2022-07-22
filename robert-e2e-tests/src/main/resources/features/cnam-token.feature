# language: fr
Fonctionnalité: Token CNAM

  En tant qu'utilisateur de Robert
  Je veux obtenir un token CNAM
  Dans le but de me déclarer facilement en arrêt dans AMELI

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Scénario: Obtenir un token CNAM lorsque je suis contact à risque
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a 4 jours
    Et que John se déclare malade aujourd'hui
    Lorsque le batch robert est exécuté
    Alors le token CNAM de Sarah est proche de il y a 4 jours

  Scénario: Le token CNAM est mis à jour à chaque nouveau risque
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a 7 jours et que John s'est déclaré malade
    Et que Sarah et Paul étaient à proximité 60 minutes il y a 2 heures
    Lorsque Paul se déclare malade aujourd'hui
    Et que le batch robert est exécuté
    Alors le token CNAM de Sarah est proche de aujourd'hui
