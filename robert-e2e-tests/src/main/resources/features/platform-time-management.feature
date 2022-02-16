# language: fr

Fonctionnalité: : Gestion du temps dans TAC
  En tant que développeur
  Je souhaite pouvoir changer le temps des composants TAC
  Afin d'avoir des tests fonctionnels plus riches

  Scénario: On est aujourd'hui
    Etant donné que l'on est aujourd'hui
    Alors l'horloge de ws-rest est à il y a 0 jours
    Alors l'horloge de crypto-server est à il y a 0 jours

  Plan du Scénario: On est il y a <jours> jours dans le passé
    Etant donné que l'on est il y a <jours> jours
    Alors l'horloge de ws-rest est à il y a <jours> jours
    Alors l'horloge de crypto-server est à il y a <jours> jours
    Exemples:
      | jours |
      | 1     |
      | 3     |
      | 5     |
      | 7     |
      | 9     |