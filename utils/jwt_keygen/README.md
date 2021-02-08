# JWT key generation tool

# Compile

```
mvn clean package
```

# Run

Generate a key pair and display them to the console:

```bash
cd target
java -jar jwt_keygen-0.1.0-SNAPSHOT.jar
```
Generate a key pair and inject them into Vault:

```bash
cd target
java -jar jwt_keygen-0.1.0-SNAPSHOT.jar http://127.0.0.1:8500/v1/kv/ $VAULT_TOKEN
```

