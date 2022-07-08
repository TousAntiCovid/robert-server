# language: fr
Fonctionnalité: Covid-19 positive declaration

  En tant qu'utilisateur de Robert
  Je souhaite être notifié si je croise une personne malade
  Dans le but de limiter la propagation du Covid19

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

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

  Scénario: La personne qui se déclare malade n'est pas à risque
    Etant donné que John et Sarah sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors John n'est pas à risque

  Scénario: Pas d'alerte si des personnes croisent furtivement une personne qui se déclare malade
    Etant donné que John, Sarah et Paul sont à proximité 5 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Et Paul n'est pas à risque

  Plan du Scénario: On passe à risque lorsque notre dernier contact date d'il y a 7 jours ou moins (exemple avec il y a <jours>)
    Etant donné que John et Sarah étaient à proximité 60 minutes il y a <jours> jours et que Sarah s'est déclarée malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors John est à risque
    Exemples:
      | jours |
      | 1     |
      | 2     |
      | 3     |
      | 4     |
      | 5     |
      | 6     |
      | 7     |

  Plan du Scénario: Pas d'alerte si le dernier contact date d'il y a plus de 7 jours (exemple avec il y a <jours>)
    Etant donné que John et Sarah étaient à proximité 60 minutes il y a <jours> jours et que Sarah s'est déclarée malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors John n'est pas à risque
    Exemples:
      | jours |
      | 8     |
      | 9     |
      | 10    |
      | 11    |
      | 12    |
      | 13    |
      | 14    |
      | 15    |
      | 16    |
