# Issue 814: Hibernate Converter Runtime Dependencies

## Context

`bluetape4k-hibernate` exposes converter classes for Tink encryption, Jackson3 JSON, Kryo/Fory serialization,
and LZ4/Snappy/Zstd/Commons Compress compression. These converter engines were declared as `compileOnly` or
test-only dependencies, while the module's tests extended `compileOnly` into `testImplementation`. That made
module tests pass even though downstream consumers could miss runtime classes.

## Decision

Treat documented built-in converter engines as part of the `bluetape4k-hibernate` artifact contract:

- Use `api` when the dependency type appears in public converter API (`bluetape4k-tink`, `bluetape4k-jackson3`).
- Use `runtimeOnly` when the dependency is needed by built-in converter implementations but not exposed in method
  signatures (Kryo, Fory, LZ4, Snappy, Zstd, Commons Compress).
- Add a `consumerRuntimeTest` source set that compiles and runs smoke tests against `sourceSets.main.output` plus
  `runtimeClasspath`, not the regular test classpath.

## Outcome

The new consumer smoke test verifies Tink encryption, Kryo/Fory byte-array converters, and compression converters
from the published runtime classpath. README dependency snippets no longer tell consumers to add those converter
engines as `compileOnly`.

## Future Guidance

When a module offers optional-looking concrete implementations in the main artifact, verify the consumer runtime
classpath separately from unit tests. Do not rely on tests that extend `compileOnly`; they can hide missing
transitive runtime dependencies.
