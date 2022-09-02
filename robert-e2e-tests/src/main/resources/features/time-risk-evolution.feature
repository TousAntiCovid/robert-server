# language: fr
Fonctionnalité: Evolution du risque au fil du temps

  En tant qu'utilisateur de Robert
  Je veux être prévenu lorsque ma période à risque est terminée
  Dans le but de sortir d'isolement

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Plan du Scénario: On n'est plus à risque <jours> jours après le dernier contact
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a <date de visite> et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Exemples:
      | date de visite  |
      | 8 jours à 23:59 |
      | 8 jours         |
      | 10 jours        |
      | 15 jours        |

  Plan du Scénario: On est toujours à risque <jours> jours après le dernier contact
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a <date de visite> et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah est à risque
    Exemples:
      | date de visite |
      | 1 jour         |
      | 2 jours        |
      | 6 jours        |
      | 6 jours à 0:01 |

