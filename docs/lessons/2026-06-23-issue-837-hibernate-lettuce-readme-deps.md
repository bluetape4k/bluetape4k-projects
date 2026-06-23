# Issue 837 - Hibernate Lettuce README dependencies

## Context

`spring-boot/hibernate-lettuce` README dependency snippets used repository-local
Gradle helpers such as `Libs.springBootStarter(...)` and `Libs.micrometer_core`.
Those examples are not copyable in consumer projects.

## Decision

Document the module dependencies with public Gradle coordinates:

- `org.springframework.boot:spring-boot-dependencies` through `platform(...)`;
- `io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce`;
- Spring Boot starter coordinates for Data JPA and Actuator;
- `io.micrometer:micrometer-core`;
- `org.springframework.boot:spring-boot-hibernate` for the Spring Boot 4
  Hibernate integration API used by `HibernatePropertiesCustomizer`.

## Follow-up Guard

`ReadmeDependencyContractTest` reads both README locales and fails if internal
Gradle helper snippets return to the consumer-facing dependency examples.
