# robert-e2e-tests

Cette application exécute des scénarios de test cucumber pour tester une plateforme Robert.

## principe

C'est une application spring boot autonome classique, à un détail près : le `main(args)` de la classe `@SpringBootApplication` n'exécute pas `SpringApplication.run(args)` pour démarrer le contexte spring.

À la place, il exécute `JUnit`, qui lance le test `CucumberTest` avec l'extension Cucumber. C'est Cucumber qui va démarrer le contexte spring.

## contraintes

### ESR_LIMIT

Le paramètre _exposure status request limit_ active/désactive le rate-limiting sur les requêtes `/status`.

Dans les tests de bout en bout, on fait un usage intensif de requêtes `/status` pour vérifier l'état _à risque_ ou non des utilisateurs. Il faut donc positionner ce paramètre à `0`.
