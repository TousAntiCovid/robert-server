# Contribution

## Relecture de code

### Objectifs

Réactivité : ne pas laisser les MR trop longtemps en suspens sous peine d'engranger un travail de maintenance
supplémentaire.

Amélioration : La MR doit améliorer la base de code.  
D'un point de vue général : la maintenabilité, la lisibilité et la compréhension du code doit être meilleure.

Si le critère précédent n'est pas respecté, aucune situation mis à part une urgence ne peut permettre l'intégration de
la MR.

Le relecteur peut apporter des suggestions sur tout ce qu'il pense qui est améliorable. Mais si ce n'est pas
important/bloquant et que l'auteur peut l'ignorer il préfixe son commentaire par `:bulb:` :bulb: .

La MR est d'une taille raisonnable pour être relue rapidement.

[ ] le code est formatté conformément aux standard de formattage du projet (TODO)
[ ] les tests sont conformes au besoin initial
[ ] les tests sont efficaces : ils remontent des messages clairs, ils sont en echec si les inputs sont incorrects, ils
survivent à un certain degré de refactoring
[ ] l'analyse statique du code est conforme aux critères de qualité (TODO)
[ ] les messages de commit sont conformes et suivent la convention [angular](https://github.com/angular/angular/blob/master/CONTRIBUTING.md#commit-message-header)

### Qui fait la review ?

On souhaite à minima un relecteur tech lead sur le composant impact.  
Idéalement, on souhaite d'autres relecteurs. Ce sont des personnes :

- qui veulent apprendre
- qui veulent apporter un avis extérieur
- qui se sentent pertinentes sur le sujet
- qui ont des connaissances fonctionnelles à apporter
- qui sont appelées par l'auteur

### Qui merge ?

C'est l'auteur de la Merge Request qui merge.

### Quand merge-t-on ?

L'auteur de la Merge Request peur merger à partir du moment où une mention "approved" est donnée par un tech lead et
qu'un délai raisonnable s'est écoulé pour laisser les contributeurs intervenir.

Tous les commentaires sont résolus.

### Qui ferme les commentaires ?

Celui qui a ouvert un commentaire doit approuver la réponse qui y a été apportée.  
L'auteur d'un commentaire est responsable de le marquer comme résolu.

Si le commentaire est un :bulb: il peut etre fermé par l'auteur de la MR pour l'ignorer.

### Comment communiquer les nouvelles MR ?

L'auteur partage le lien vers la merge request dans le canal dédié dans Mattermost.

Les relecteurs se manifestent en régissant au message (✋ ✔ ❔ ...) pour signaler au reste de l'équipe qu'ils
interviennent :

- :eyes: je regarde
- :timer: intéressé mais j'ai pas le temps pour le moment

Cela permet :

- de rassurer l'auteur en l'avertissant que quelqu'un prend en charge la relecture
- de communiquer de manière passive au reste de l'équipe qui se charge de la relecture
- de marquer son intérêt pour la relecture d'une merge request et ainsi définir le _délai raisonnable_ à laisser avant
  de merger

### Leviers d'efficacité

💡 Utiliser les suggestions Gitlab pour les changements mineurs. Cela permet d'éviter de faire retomber tout l'effort de
correction sur l'auteur.

💡 Utiliser la fonction "Start review" pour envoyer les commentaires en une seul bloc.  
Cela permet d'aller au bout de la relecture et d'obtenir soi-même des réponses à ses propres questions.

❔❔❔ Lorsque trop de questions sont soulevées : visio / partage d'écran

## Git

Les messages de commit sont clairs :
Cf. [Git commit guidelines](https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project)

- 1 titre succint
- une description étendue

On utilise le workflow de branches Gitflow allégé : on évite la branche de release car nous n'avons pas de longue phase
de stabilisation.

- la branche `master` contient les releases
- la branche `develop` contient la version en cours de développement
- les branches préfixées par `feature/` contiennent les branches en cours de développement
- les branches de correctifs urgents sont préfixés par `hotfix/`

Develop n'est jamais mergé dans les Feature branch.
Les features branch sont _rebasées_ avant merge et les commits de "test" supprimés à l'aide d'un _rebase interactif_.

Les branches sont supprimées lorsqu'elles sont mergées.
