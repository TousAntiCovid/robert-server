# language: fr
Fonctionnalité: Covid-19 positive declaration

  En tant qu'utilisateur de Robert
  Je souhaite être notifié si je croise une personne malade
  Dans le but de limiter la propagation du Covid19

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC

  @RealTimeEnvironment
  Scénario: Une personne est prévenue si un de ses contacts se déclare malade
    Etant donné que John et Sarah sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah est à risque

  @RealTimeEnvironment
  Scénario: Deux personnes sont prévenues si elles croisent une personne qui se déclare malade
    Etant donné que John, Sarah et Paul sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah est à risque
    Et Paul est à risque

  @RealTimeEnvironment
  Scénario: Personne ne croise la personne qui se déclare malade
    Etant donné que Sarah et Paul sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Et Paul n'est pas à risque

  @RealTimeEnvironment
  Scénario: La personne qui se déclare malade n'est pas à risque
    Etant donné que John et Sarah sont à proximité 60 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors John n'est pas à risque

  @RealTimeEnvironment
  Scénario: Pas d'alerte si des personnes croisent furtivement une personne qui se déclare malade
    Etant donné que John, Sarah et Paul sont à proximité 5 minutes
    Lorsque John se déclare malade
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Et Paul n'est pas à risque

  Plan du Scénario: On passe à risque lorsque notre dernier contact date d'il y a 7 jours ou moins (exemple avec il y a <date de visite>)
    Etant donné que Marc et Serge ont l'application TAC depuis 18 jours
    Et que Marc et Serge étaient à proximité 60 minutes il y a <date de visite>
    Lorsque Serge se déclare malade aujourd'hui
    Et que le batch robert est exécuté
    Alors Marc est à risque
    Exemples:
      | date de visite     |
      | 1 jour             |
      | 2 jours            |
      | 3 jours            |
      | 4 jours            |
      | 5 jours            |
      | 6 jours            |
      | 7 jours            |
      | 7 jours 30 minutes |

  Plan du Scénario: Pas d'alerte si le dernier contact date d'il y a plus de 7 jours (exemple avec il y a <date de visite>)
    Etant donné que Marc et Serge ont l'application TAC depuis 18 jours
    Et que Marc et Serge étaient à proximité 60 minutes il y a <date de visite>
    Lorsque Serge se déclare malade aujourd'hui
    Et que le batch robert est exécuté
    Alors Marc n'est pas à risque
    Exemples:
      | date de visite              |
      | 7 jours 1 heures 15 minutes |
      | 8 jours                     |
      | 9 jours                     |
      | 10 jours                    |
      | 11 jours                    |
      | 12 jours                    |
      | 13 jours                    |
      | 14 jours                    |
      | 15 jours                    |
      | 16 jours                    |

  @RealTimeEnvironment
  Scénario: Une personne infectée par la Covid19 a un contact prolongé avec d'autres personnes
    Etant donné que John, Sarah et Paul sont à proximité 48 heures
    Lorsque Sarah se déclare malade
    Et que le batch robert est exécuté
    Alors John est à risque
    Et Paul est à risque
