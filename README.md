# Application TousAntiCovid

Sous la supervision du Ministère de la Santé et des solidarités et du
Secrétariat d’État au numérique, en lien avec le Ministère de l’enseignement
supérieur, de la recherche et de l’innovation, **Inria pilote depuis le 7
avril 2020** le développement de l’application « TousAntiCovid » auquel
contribue à titre gracieux un ensemble d’acteurs publics et privés, au sein
de l’équipe-projet TousAntiCovid, qui rassemble ANSSI, Capgemini, Dassault
Systèmes, INSERM, Lunabee, Orange, Santé Publique France et Withings , et que
complète un écosystème de contributeurs. Ce projet contribue à la gestion de
la crise sanitaire Covid-19 et au suivi épidémiologique par les autorités de
santé.

L’objectif du projet est de pouvoir rendre possible la mise à disposition
d’une application permettant d’informer les usagers s’ils ont été en contact
avec une personne ayant été testée positive au Covid-19, et de leur proposer
des conduites à tenir, conformément aux préconisations du Ministère de la
Santé et des solidarités.

Le projet repose sur l’implémentation d’un protocole, ROBERT, qui a donné lieu à un avis du Conseil national du numérique (rendu public le 24 avril 2020) et à une délibération de la CNIL (rendue publique le 26 avril 2020). Cinq fondements ont guidé les développements : 
* L’inscription de l’application TousAntiCovid dans la **stratégie globale** de gestion de la crise sanitaire et de suivi épidémiologique. 
* **Le strict respect du cadre de protection des données et de la vie privée** au niveau national et européen, tel que défini notamment par la loi française et le RGPD ainsi que la boîte à outils récemment définie par la commission européenne sur les applications de suivi de proximité. 
* **La transparence**, qui passe notamment par la diffusion, sous une licence open source, des travaux spécifiques menés dans le cadre du projet. L’objectif est d’apporter toutes les garanties : transparence des algorithmes, code ouvert à terme, interopérabilité, auditabilité, sécurité et réversibilité des solutions. 
* **Le respect des principes de souveraineté numérique du système de santé publique** : maîtrise des choix de santé par la société française et européenne, protection et structuration du patrimoine des données de santé pour guider la réponse à l’épidémie et accélérer la recherche médicale. 
* **Le caractère temporaire du projet**, dont la durée de vie correspondra, s’il est déployé, à la durée de gestion de l’épidémie de Covid-19.

# Processus de création de l'application TousAntiCovid
* Le 18 avril, le protocole de communication ROBERT a été publié par Inria et Fraunhofer/AISEC, dans le cadre d’un projet franco-allemand, permettant de donner un cadre pour le fonctionnement global, d’exposer les aspects sécurité et respect de la vie privée, et de garantir une interopérabilité au niveau européen pour le déploiement d’une application.
* Sur la base de ce protocole, les développeurs membres de l’équipe-projet TousAntiCovid ont travaillé à l’implémentation des premières briques fonctionnelles de l’application et de son infrastructure, dans l’optique de proposer une application déployable opérationnellement en tant que de besoin, dans le cadre d’un calendrier fixé par le gouvernement.
* La publication des codes sources et de la documentation de Stop Covid  démarre le 12 mai et se poursuivre pendant la durée du projet. L’évolution du code prévoit l’analyse et l’intégration éventuelle des améliorations qui seront soumises par la communauté des développeurs.
* Les mises à jour de l’application seront disponibles au fur et à mesure.

# Principe général de publication 
Pour permettre aux différentes communautés de développeurs et de spécialistes d’expertiser les algorithmes implémentés et la façon dont cette application est programmée, en particulier si elle met en œuvre correctement le protocole ROBERT, le code source est publié sur [https://gitlab.inria.fr/stopcovid19/](https://gitlab.inria.fr/stopcovid19/). Le code source présenté est le résultat d’un processus de développement collaboratif impliquant de nombreuses personnes et organisations au sein de l’équipe-projet TousAntiCovid.

Ce processus de développement collaboratif, qui a été contraint par l’agenda du projet, va s’ouvrir progressivement pour permettre de proposer des évolutions à l’application, de signaler des bugs, de proposer des changements pour la documentation et de suivre la prise en compte ou non de ces propositions. Pour ce faire, le choix de la plateforme Gitlab d’Inria a été retenu.

Les contributions attendues par la communauté des développeurs permettront de faire évoluer des briques logicielles pour, au final, améliorer la qualité de l’application. Pour contribuer, merci de prendre connaissance du fichier [CONTRIBUTING.md](CONTRIBUTING.md). La plateforme Gitlab n’a pas vocation à héberger les débats d’ordre plus général, politique ou sociétal.
La politique de publication du code source développé dans le cadre du projet repose sur trois catégories :
* Une partie (restreinte) qui n’est pas publiée car correspondant à des tests ou à des parties critiques pour la sécurité de l’infrastructure ; en revanche une documentation, publiée sur le Gitlab présentera les grands principes de sécurité mis en œuvre sur TousAntiCovid (afin de respecter les demandes ou avis de la CNIL et les recommandations de l’ANSSI) ;  
* Une partie qui est rendue publique sans qu’un appel à contribution ne soit attendu (les propositions seront bien entendu étudiées) : cela correspond par exemple à des parties qui implémentent directement des spécifications très précises ;
* Une partie qui relève à strictement parler de l’open source, avec des appels à contribution qui sont attendus : cela concerne le cœur de l’application, notamment l’implémentation du protocole ROBERT.

# Description des sous-projets et de la façon dont ils interagissent

Le projet principal est découpé en plusieurs composants dont
l’articulation globale est détaillée dans le document [comment
contribuer](CONTRIBUTING.md).

# Contribution au projet

Pour contribuer au projet, merci de prendre connaissance du fichier [comment contribuer](CONTRIBUTING.md).

# Licence

Sauf mention contraire, les composants du backend TousAntiCovid sont publiés sous licence MPL 2.0 : [LICENSE.md](LICENSE.md).

# Liens
* La présentation globale du projet TousAntiCovid sur inria.fr : [https://www.inria.fr/fr/le-projet-stopcovid](https://www.inria.fr/fr/le-projet-stopcovid)
* Les membres de l’équipe-projet TousAntiCovid : [https://www.inria.fr/fr/stopcovid](https://www.inria.fr/fr/stopcovid)
* Le protocole ROBERT v1 : [https://github.com/ROBERT-proximity-tracing/](https://github.com/ROBERT-proximity-tracing/)
* Le document [comment contribuer](CONTRIBUTING.md)
* Le document [de ressources scientifiques du projet](SCIENTIFIC_RESOURCES.md)
* La [liste des sous-projets déjà publiés](https://gitlab.inria.fr/stopcovid19)

