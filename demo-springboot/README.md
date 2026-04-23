# demo-springboot

A standalone Spring Boot example showing how to expose `killport` as an HTTP API with:

- token-based access control
- allowlisted ports and process names
- optional PID blocking
- unified JSON responses

## Prerequisites

This demo depends on the main `killport` library:

```bash
cd ..
mvn -pl . clean install
```

If you only want to consume the published artifact, use `io.github.super1windcloud:killport:1.0.0` from Maven Central instead.

## Run

```bash
mvn spring-boot:run
```

## Default configuration

See [src/main/resources/application.yml](src/main/resources/application.yml).

Default values:

- token header: `X-Admin-Token`
- token value: `change-me`
- allowed ports: `8080`, `8081`
- allowed process names: `java`, `node`
- PID killing: disabled

## Request example

```http
POST /api/processes/kill
X-Admin-Token: change-me
Content-Type: application/json

{
  "targets": [":8080", "java"],
  "ignoreCase": true,
  "tree": true,
  "silent": false,
  "forceAfterTimeoutMs": 1000,
  "waitForExitMs": 3000
}
```

## Response example

```json
{
  "success": true,
  "message": "Processes terminated.",
  "data": {
    "targets": [":8080", "java"],
    "tree": true
  }
}
```
