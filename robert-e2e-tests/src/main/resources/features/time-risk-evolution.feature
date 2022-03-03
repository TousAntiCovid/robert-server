# language: fr
Fonctionnalité: Evolution du risque au fil du temps

  En tant qu'utilisateur de Robert
  Je veux être prévenu lorsque ma période à risque est terminée
  Dans le but de sortir d'isolement

  Plan du Scénario: On n'est plus à risque <jours> après le dernier contact
    Etant donné que on est il y a <jours> jours
    Et que John, Sarah et Paul avaient l'application TAC
    Et que Sarah et John étaient à proximité 60 minutes
    Et que John s'est déclaré malade
    Lorsque on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah n'est pas à risque
    Exemples:
      | jours |
      | 8     |
      | 10    |
      | 15    |

  Plan du Scénario: On est toujours à risque <jours> après le dernier contact
    Etant donné que on est il y a <jours> jours
    Et que John, Sarah et Paul avaient l'application TAC
    Et que Sarah et John étaient à proximité 60 minutes
    Et que John s'est déclaré malade
    Lorsque on est aujourd'hui
    Et que le batch robert est exécuté
    Alors Sarah est à risque
    Exemples:
      | jours |
      | 1     |
      | 2     |
      | 6     |
      | 7     |

