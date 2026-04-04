# Module bluetape4k-testcontainers

English | [한국어](./README.ko.md)

A server wrapper and utility library for building integration tests quickly on top of Testcontainers `2.0.3`.

## Key Features

- wrappers for database, graph DB, storage, messaging, infrastructure, and distributed SQL services
- HTTP mocking through WireMock
- AWS LocalStack support
- shared `GenericServer` / `GenericContainer` utilities
- automatic PostgreSQL extension activation for PostGIS and pgvector
- declarative activation of extra PostgreSQL extensions through `withExtensions()`

## Recent Stability Improvements

- `GenericContainer.exposeCustomPorts(...)` now creates port bindings even when `hostConfig` starts empty.
- `GenericServer.writeToSystemProperties(...)` registers default and additional properties in a stable, consistent order.
- `KafkaServer.Launcher` creates fresh serializer/deserializer instances per use to avoid reuse after `close()`.
- `TiDBServer` is deprecated because Testcontainers 2.x does not support it reliably.

## Extra Features Compared with Raw Testcontainers Usage

- optional fixed-port mapping with `useDefaultPort=true`
- automatic export of system properties such as `testcontainers.<name>.host|port|url`
- simplified Spring Boot wiring through `${testcontainers...}` placeholders
- helper methods such as `getDataSource()`

## System Property Export (`PropertyExportingServer`)

Every server implements `PropertyExportingServer`, which automatically registers connection details as system properties at `start()` time.

- property keys use lowercase kebab-case
- format: `testcontainers.{namespace}.{kebab-case-key}`
- examples: `testcontainers.postgresql.jdbc-url`, `testcontainers.kafka.bootstrap-servers`

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## Detailed Features

Main groups include:

- database containers
- storage and search containers
- graph database containers
- messaging, infrastructure, and AWS containers
- distributed SQL and time-series containers

The Korean README contains the full server list, exported-key tables, extension examples, and setup snippets.

## Usage Examples

Common usage patterns include:

- starting a DB container and reading system properties
- creating a datasource through helper methods
- using graph DB, WireMock, Keycloak, and InfluxDB wrappers

## Spring Boot Configuration

`application-test.yml` can use `${testcontainers...}` placeholders directly, for example for datasource URLs, driver names, usernames, and passwords exported by the wrapper servers.

## Container Lifecycle Diagram

The Korean README documents the lifecycle sequence for startup, property export, use, and cleanup in diagram form.

## Supported Container Class Diagram

The Korean README also includes the class-level grouping diagram for database, storage, infra, and messaging wrappers.

## Supported Container Structure

Container wrappers are organized by domain-specific package structure, such as `database/`, `storage/`, `graphdb/`, `mq/`, `infra/`, `aws/`, and `http/`.

## References

- Testcontainers official documentation
- Local service wrappers in this repository

## Colima + LocalStack Troubleshooting

When running under Colima or other container backends, see the Korean README for the detailed troubleshooting notes around LocalStack networking and startup behavior.
