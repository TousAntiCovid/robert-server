# language: fr
Fonctionnalité: Gestion des statistiques

  En tant qu'administrateur de ROBERT.
  Je souhaite pouvoir récupérer et suivre l'évolution des kpis.
  Afin de mesurer l'efficacité de TAC et transmettre les données.

  Scénario: 2 utilisateurs se rencontrent, l'un des deux dépasse le seuil du score et la dernière exposition est entre 8 et 14 jours
    Etant donné que l'on est il y a 8 jours
    Et que Sarah et Paul ont l'application TAC
    Et que Sarah et Paul sont à proximité 60 minutes
    Et que le batch robert est exécuté
    Et que le kpi usersAboveRiskThresholdButRetentionPeriodExpired est 0
    Lorsque l'on est aujourd'hui
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Alors le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de 1

  Scénario: 2 utilisateurs se rencontrent, l'un des deux dépasse le seuil du score et la dernière exposition est de moins de 7 jours
    Etant donné que l'on est il y a 6 jours
    Et que Sarah et Paul ont l'application TAC
    Et que Sarah et Paul sont à proximité 60 minutes
    Et que le batch robert est exécuté
    Et que le kpi usersAboveRiskThresholdButRetentionPeriodExpired est 0
    Lorsque l'on est aujourd'hui
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Alors le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé

  Scénario: 4 utilisateurs se rencontrent, 3 d'entre eux dépassent le seuil du score et la dernière exposition est entre 8 et 14 jours
    Etant donné que l'on est il y a 8 jours
    Et que Inès, Hugo, Sarah et Paul ont l'application TAC
    Et que Inès, Hugo, Sarah et Paul sont à proximité 60 minutes
    Et que le batch robert est exécuté
    Et que le kpi usersAboveRiskThresholdButRetentionPeriodExpired est 0
    Lorsque l'on est aujourd'hui
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Alors le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de 3

  Scénario: 4 utilisateurs se rencontrent, 3 d'entre eux dépassent le seuil du score et la dernière exposition est de moins de 7 jours
    Etant donné que l'on est il y a 6 jours
    Et que Inès, Hugo, Sarah et Paul ont l'application TAC
    Et que Inès, Hugo, Sarah et Paul sont à proximité 60 minutes
    Et que le batch robert est exécuté
    Et que le kpi usersAboveRiskThresholdButRetentionPeriodExpired est 0
    Lorsque l'on est aujourd'hui
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Alors le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé

  Scénario: Hugo est notifié une première fois
    Etant donné que l'on est aujourd'hui
    Et que Paul et Hugo ont l'application TAC
    Et que Paul et Hugo sont à proximité 60 minutes
    Lorsque Paul se déclare malade
    Et que le batch robert est exécuté
    Et que Hugo est à risque
    Alors le kpi notifiedUsers est incrémenté de 1

  Scénario: Sarah est notifié une première fois
    Etant donné que l'on est aujourd'hui
    Et que Paul, Hugo et Sarah ont l'application TAC
    Et que Paul et Hugo sont à proximité 60 minutes
    Lorsque Paul se déclare malade
    Et que le batch robert est exécuté
    Et que Hugo est à risque
    Alors le kpi notifiedUsers est incrémenté de 1
    Lorsque Sarah et Hugo sont à proximité 60 minutes
    Et que Hugo se déclare malade
    Et que le batch robert est exécuté
    Et que Hugo est à risque
    Alors le kpi notifiedUsers n'a pas changé