# language: fr
Fonctionnalité: Evolution du risque au fil du temps

  En tant qu'utilisateur de Robert
  Je veux être prévenu lorsque ma période à risque est terminée
  Dans le but de sortir d'isolement

  Contexte:
    Etant donné que John, Sarah et Paul ont l'application TAC depuis 15 jours

  Plan du Scénario: On n'est plus à risque <jours> après le dernier contact
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a <jours> jours et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Exemples:
      | jours |
      | 7     |
      | 8     |
      | 10    |
      | 15    |

  Scénario: On est toujours à risque 6 jours 23 heures et 45 minutes après le dernier contact
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a 6 jours 23 heures 45 minutes et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah est à risque

  Plan du Scénario: On est toujours à risque <jours> jours après le dernier contact
    Etant donné que Sarah et John étaient à proximité 60 minutes il y a <jours> jours et que John s'est déclaré malade
    Lorsque l'on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah est à risque
    Exemples:
      | jours |
      | 1     |
      | 2     |
      | 6     |

