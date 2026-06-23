# Issue 839 - Hibernate Lettuce demo README dependencies

## Context

`spring-boot/hibernate-lettuce-demo` README dependency examples used
bluetape4k-internal Gradle helpers such as `Libs.springBootStarter(...)`.
Those helpers are not available in consumer builds, so the copied example would
fail before users could run the demo.

## Decision

Document the demo dependencies from a consumer perspective:

- import `org.springframework.boot:spring-boot-dependencies` with
  `platform(...)`;
- use the published
  `io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce`
  coordinate;
- use standard Spring Boot starter and H2 coordinates;
- remove the stale explicit `compileOnly` Hibernate helper note because
  `spring-boot-starter-data-jpa` supplies the demo runtime.

## Follow-up Guard

`ReadmeDependencyContractTest` reads both README locales and fails if internal
dependency helpers return to the consumer-facing snippets.
