# Ktor

https://start.ktor.io
or ktor intellij plugin
configuration in code, yaml, hocon file

vault no puc afegir
postgres puc afegir

https://github.com/karlazzampersonal/ktor-vault

https://ktor.io/docs/testing.html

https://ktor.io/docs/configuration-file.html#read-configuration-in-code

ktor + flyway es third party o sigui que res


## Run

```shell
# Start Vault and Database
docker compose up -d vault vault-cli db

# Start Application
./gradlew run

# Make requests
curl http://localhost:8080/hello

# Stop Application with control-c

# Stop all containers
docker compose down
```


pas a junit5