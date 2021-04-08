# Introduction

Ce projet gitlab.inria.fr est un des composants de la solution plus
globale [StopCovid](https://gitlab.inria.fr/stopcovid19/accueil/-/blob/master/README.md).

Ce composant gère la fonctionnalité appelée "analytics", c'est une fonctionnalité qui permet de recevoir de la part des
applications mobiles leurs statistiques d'usage (quel model de téléphone, quelle version d'OS, à quelle date a été
effectué le dernier status, la liste des erreurs rencontrées...)

# Architecture générale

![](analytics.png)

Dans une première itération de cette application, la base de données mongo n'est pas utilisée.

L'application repose sur une application springboot en frontal. Celle-ci traite la requête provenant du terminal de
l'utilisateur final. Les principaux traitements effectués par cette application sont :

- validation du token JWT fourni par l'application robert, à noter qu'un token ne peut être utilisé qu'une unique fois
  pour soumettre des analytics. Une base de donnée mongodb est utilisée pour conserver la liste des tokens déjà
  utilisée.
- validation du contenu de la requête, on vérifie la structure de la requête vis à vis du contrat d'interface
- envoi des analytics dans un topic kafka.

Par la suite, le topic kafka est consommé par logstash qui repousse les analytics dans un elasticsearch. Les analytics
sont ensuite analysés grâce à un kibana.

note : le fichier source du diagramme est fourni [ici](analytics.drawio)

## contrat d'interface

le contrat d'interface du service rest exposé est disponible au format
openapi [openapi-analytics.yaml](src/main/doc/openapi-analytics.yaml)

## Configuration Kafka SSL
Ce chapitre explique comment configurer une connexion sécurisée SSL entre le producteur de message (notre application) et le server kafka.

Il faut générer un certificat pour le server Kafka (avec CN = hostname). Ce certificat est stocké dans un truststore (au format jks) et protégé par un mot de passe.

Afin de générer un magasin au fomat `jks`, vous pouvez suivre les instructions suivantes:

```sh
openssl pkcs12 -inkey node-1.pem -in node-1.pem -name node-1 -export -out node-1.p12
keytool -importkeystore -deststorepass changeme \
    -destkeystore node-1-keystore.jks -srckeystore node-1.p12 -srcstoretype PKCS12
```
Le trustore doit être installé sur le system de fichier des serveurs hébergeant l'application analytics.

La configuration SSL est sauvegardée dans Vault et injectée à l'application via spring-cloud-vault-config-consul in apps.
Voici les propriétés devant être utilisées :
- `spring.kafka.bootstrap-servers` doit être initialisé avec la liste des hostname:port des serveurs kafka (il faut indiquer le même nom que le `CN` du certificat)
- `spring.kafka.properties.security.protocol` égal à `ssl` pour activer le SSL
- `spring.kafka.ssl.trust-store-location` suivant le nommage suivant `file:///path/to/kafka.client.truststore.jks` (ne pas oublier le préfixe `file://` pour une URL valide)
- `spring.kafka.ssl.trust-store-password`

# Environment de développement

Si ce n'est déjà fait, se logguer, sur la registry docker de l'inria.

    docker login registry.gitlab.inria.fr

Pour fonctionner, cette application à besoin à minima :

- De mongodb
- De kafka

La stack complète d'outil tel que décrit dans le schéma d'architecture générale afin de manipuler les analytics qui
auront été stockés dans l'elasticsearch

Ces différents services sont disponibles dans un environnement docker lançable par :

    docker-compose -f ../environment-setup/dev/compose/docker-compose-analytics-server.yaml up -d

Les services dockerisés ne sont utilisés que pour pouvoir lancer l'application par elle-même. Les tests untaires
utilisent des services embarqués (embedded mongodb et kafka).

## Appel rest

Des appels rest peuvent être effectué sur l'application en utilisant la collection postman
fournie [postmail](src/main/doc/TAC-analytics.postman_collection.json)

## Spécificité de la configuration sous Windows 10

Dans le fichier etc/hosts (c:\windows\system32\drivers\etc\hosts) ajouter l'entrée suivante :

    127.0.0.1 docker-desktop

Dans l'application docker desktop settings, placer la quantité de mémoire à 10G.

