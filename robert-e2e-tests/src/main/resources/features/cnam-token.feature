# language: fr
Fonctionnalité: Token CNAM

  En tant qu'utilisateur de Robert
  Je veux obtenir un token CNAM
  Dans le but de me déclarer facilement en arrêt dans AMELI

  Scénario: Le token CNAM est mis à jour à chaque nouveau contact à risque
    Etant donné que l'on est il y a 7 jours
    Et que John, Sarah et Paul ont l'application TAC
    Et que Sarah et John étaient à proximité 60 minutes
    Et que John se déclare malade
    Lorsque l'on est aujourd'hui
    Et que Sarah et Paul sont à proximité 60 minutes
    Et que  Paul se déclare malade
    Et que le batch robert est exécuté
    Alors le token CNAM de Sarah est proche de maintenant
