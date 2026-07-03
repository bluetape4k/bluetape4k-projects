# Hibernate Lettuce Classpath Guards

## Context

Issue #945 found that the Hibernate Lettuce Spring Boot auto-configurations used
direct class references for optional compile-only Hibernate, Actuator, and
Micrometer integrations.

## Decision

Keep optional integration metadata string-based:

- use `@ConditionalOnClass(name = [...])` for optional classpath probes
- use `@ConditionalOnBean(type = [...])` for optional bean types
- use `@AutoConfiguration(afterName = [...])` for optional ordering targets

## Outcome

`FilteredClassLoader` slice tests now prove the configuration backs off cleanly
when Hibernate customizer, Actuator endpoint annotations, or Micrometer registry
types are unavailable.

## Verification

- `./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --tests 'io.bluetape4k.spring.boot.autoconfigure.cache.lettuce.LettuceNearCacheAutoConfigurationTest'`

## Future Guidance

When an auto-configuration imports a `compileOnly` integration, avoid class
literals in annotation metadata and add a missing-class `ApplicationContextRunner`
test before marking the integration classpath-safe.
