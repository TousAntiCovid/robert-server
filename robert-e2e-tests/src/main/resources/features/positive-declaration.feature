# language: fr
Fonctionnalité: Covid-19 positive declaration

  En tant qu'utilisateur de Robert
  Je souhaite être notifié si je croise une personne malade
  Dans le but de limiter la propagation du Covid19

  Contexte:
    Etant donné que John a l'application TAC
    Et que Sarah a l'application TAC
    Et que Paul a l'application TAC

  Scénario: Une personne est prévenue si un de ses contacts se déclare malade
    Etant donné que John et Sarah sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah est à risque

  Scénario: Deux personnes sont prévenues si elles croisent une personne qui se déclare malade
    Etant donné que John, Sarah et Paul sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah est à risque
    Et Paul est à risque

  Scénario: Personne ne croise la personne qui se déclare malade
    Etant donné que Sarah et Paul sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Et Paul n'est pas à risque

  Scénario: Pas d'alerte si des personnes croisent furtivement une personne qui se déclare malade
    Etant donné que John, Sarah et Paul sont à proximité 5 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Et Paul n'est pas à risque

  Scénario: La personne qui se déclare malade n'est pas à risque
    Etant donné que John et Sarah sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors John n'est pas à risque
