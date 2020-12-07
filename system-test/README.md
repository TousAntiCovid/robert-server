
End 2 End System tests

Objective being to create End to End scenarios for testing the backend through the web api.

The test can run either on a locally deployed backend or on the test/prod servers.

Test scenario are in `/src/test/resources` in cucumber format.

## Run

Copy the `config-template.properties` to `config.properties`
Optionnaly, adapt it in order to point to the target endpoints. (default is localhost)

Compile and launch tests:

```
mvn verify
```

