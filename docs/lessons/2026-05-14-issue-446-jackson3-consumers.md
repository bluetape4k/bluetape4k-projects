# Issue 446 Jackson3 Consumers

## Context

Spring Boot 4 uses Jackson 3, so supported modules that only consumed
`bluetape4k-jackson2` were migrated to `bluetape4k-jackson3`.

## Decision

Convert modules whose local code or dependencies can compile against
`tools.jackson.*` and `io.bluetape4k.jackson3.*`. Keep upstream integrations
that still require Jackson 2, such as Retrofit's Jackson converter, JJWT
Jackson, and Spring Kafka 3.x converters.

## Outcome

Supported data, IO, Feign, HTTP, Vert.x, NATS, Micrometer, gRPC, Cassandra,
Hibernate, MongoDB, R2DBC, Geo, and Redisson demo dependencies now use
Jackson3. Jackson2-only upstream integrations remain unchanged.

## Verification

- `./gradlew :bluetape4k-cassandra:testClasses :bluetape4k-hibernate:testClasses :bluetape4k-hibernate-reactive:testClasses :bluetape4k-mongodb:testClasses :bluetape4k-r2dbc:testClasses :bluetape4k-examples-redisson-demo:testClasses :bluetape4k-micrometer:testClasses :bluetape4k-nats:testClasses :bluetape4k-feign:testClasses :bluetape4k-grpc:testClasses :bluetape4k-http:testClasses :bluetape4k-vertx:testClasses :bluetape4k-geo:testClasses`
- `./gradlew :bluetape4k-geo:testClasses`

## Future Notes

Do not migrate a module just because it references Jackson2 by name. First
check whether upstream adapters expose a Jackson3 API on the actual resolved
classpath.
