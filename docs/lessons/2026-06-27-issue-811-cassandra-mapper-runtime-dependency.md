# Issue #811: Cassandra Mapper Runtime Dependency

## Context

`bluetape4k-cassandra` exposes DataStax mapper runtime types in public mapper
extension signatures:

- `EntityHelper<T>` receiver and parameters
- `NullSavingStrategy` parameter default

The module declared `java-driver-mapper-runtime` as `compileOnly`, while the
regular test classpath extended `compileOnly`. That made module tests pass even
though consumers using only the documented `bluetape4k-cassandra` dependency
could not compile mapper helper APIs.

## Decision

Treat mapper helpers as part of the `bluetape4k-cassandra` public API contract:

- Promote `libs.cassandra.java.driver.mapper.runtime` from `compileOnly` to
  `api`.
- Add a `consumerRuntimeTest` source set whose compile/runtime classpath uses
  `main.output + runtimeClasspath`, not the regular test classpath.
- Document that the single `bluetape4k-cassandra` artifact includes the mapper
  runtime required by `io.bluetape4k.cassandra.mapper`.

## Verification

- RED: `./gradlew :bluetape4k-cassandra:compileConsumerRuntimeTestKotlin --no-build-cache --no-daemon --no-configuration-cache`
  failed with unresolved `com.datastax.oss.driver.api.mapper.*` and
  `Cannot access class 'EntityHelper'`.
- GREEN: the same compile task passed after promoting mapper runtime to `api`.
- `./gradlew :bluetape4k-cassandra:dependencies --configuration runtimeClasspath --no-daemon --no-configuration-cache`
  showed `org.apache.cassandra:java-driver-mapper-runtime:4.19.2`.
- `./gradlew :bluetape4k-cassandra:compileKotlin :bluetape4k-cassandra:compileTestKotlin :bluetape4k-cassandra:compileConsumerRuntimeTestKotlin --warning-mode all --no-daemon --no-configuration-cache`
  passed.
- `./gradlew :bluetape4k-cassandra:test :bluetape4k-cassandra:consumerRuntimeTest --no-build-cache --no-daemon --no-configuration-cache`
  passed with 178 module tests and 1 consumer runtime test.
- `git diff --check` passed.

## Future Guidance

When a bluetape4k module exposes third-party types in public signatures, the
dependency must be exported (`api`) or the API must move behind an optional
artifact. Regular tests that inherit `compileOnly` are not sufficient evidence;
add a consumer-style source set for compile/runtime contracts.

## Concurrency Helper Gate

No shared mutable state, coroutine lifecycle, structured concurrency, or virtual
thread behavior changed. `MultithreadingTester`, `SuspendedJobTester`, and
`StructuredTaskScopeTester` are not applicable to this dependency metadata and
consumer compile-contract fix.
