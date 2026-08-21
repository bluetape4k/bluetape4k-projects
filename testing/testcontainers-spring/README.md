# Module bluetape4k-testcontainers-spring

English | [한국어](./README.ko.md)

An optional Spring Test bridge for `PropertyExportingServer` from
`bluetape4k-testcontainers`. It registers the server's existing
`testcontainers.{namespace}.{key}` properties with Spring's
`DynamicPropertyRegistry` without adding Spring to the core Testcontainers module.

<!-- issue-1321-spring-bridge:start -->
## Dependency

```kotlin
testImplementation("io.bluetape4k:bluetape4k-testcontainers-spring:<version>")
```

The module exposes the core `PropertyExportingServer` API and Spring Test's
`DynamicPropertyRegistry`. The version is managed by the Bluetape4k release
catalog; do not pin a separate Spring Test version.

## Usage

```kotlin
import io.bluetape4k.testcontainers.spring.registerDynamicProperties
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

companion object {
    @DynamicPropertySource
    @JvmStatic
    fun registerProperties(registry: DynamicPropertyRegistry) {
        RedisServer.Launcher.redis.registerDynamicProperties(registry)
    }
}
```

For a server with namespace `redis` and key `host`, the registered Spring key is
`testcontainers.redis.host`. The bridge registers every key returned by
`propertyKeys()` and evaluates `properties()` lazily when Spring resolves a
value. It does not start or stop the container and does not mutate JVM system
properties; the test owns the server lifecycle.

If a key is declared by `propertyKeys()` but missing from `properties()`, the
supplier fails with `IllegalStateException` when evaluated. Duplicate keys are
not preflighted or overwritten by this bridge; choose one registration path and
let Spring registry ordering semantics apply.

The existing `registerSystemProperties()` API remains independent. Use it only
when a test explicitly needs JVM system properties instead of Spring's dynamic
property source.

See Spring's [DynamicPropertyRegistry Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/DynamicPropertyRegistry.html)
and [DynamicPropertySource reference](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dynamicpropertysource.html)
for the supplier lifecycle and registration model.
<!-- issue-1321-spring-bridge:end -->

## Scope

This module is a small adapter. It does not provide Spring Boot auto-configuration,
container startup, property caching, collision resolution, or migration of the
existing Workshop helpers.
