# language: fr
@Smoke
Fonctionnalité: Health check

  Scénario: Robert is up
    Etant donné que l' application robert ws rest est démarrée

  Scénario: Postgresql is ready
    Etant donné que John a l'application TAC
    Et que Paul a l'application TAC
    Et que Paul et John sont à proximité 15 minutes
    Et que Paul se déclare malade
    Et que le batch robert est exécuté
    Et que John supprime son historique d'exposition
