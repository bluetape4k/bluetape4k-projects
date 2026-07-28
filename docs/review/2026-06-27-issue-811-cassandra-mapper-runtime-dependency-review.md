# Review: Issue #811 Cassandra Mapper Runtime Dependency

## Scope

- `data/cassandra/build.gradle.kts`
- `data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/mapper/EntitySupport.kt`
- `data/cassandra/src/consumerRuntimeTest/kotlin/io/bluetape4k/cassandra/mapper/CassandraMapperConsumerRuntimeClasspathTest.kt`
- `data/cassandra/README.md`
- `data/cassandra/README.ko.md`

## 발견 사항

P0/P1: none.

## Review Notes

- The issue is reproduced at the consumer classpath boundary. The new
  `consumerRuntimeTest` source set excludes module `compileOnly` dependencies,
  so it catches missing published dependency metadata.
- Promoting `java-driver-mapper-runtime` to `api` matches the public extension
  signatures because `EntityHelper` and `NullSavingStrategy` are directly
  exposed.
- README and Korean README now describe the selected single-artifact dependency
  contract.
- Public KDoc touched in `EntitySupport.kt` is now English.
- CodeGraph impact lookup returned `0 nodes` for the worktree diff, so this
  review used direct source, diff, dependency, and Gradle verification evidence.

## Verification Evidence

- RED: `:bluetape4k-cassandra:compileConsumerRuntimeTestKotlin` failed before
  the dependency fix with unresolved `mapper` imports and missing
  `EntityHelper` classpath.
- GREEN: `:bluetape4k-cassandra:compileConsumerRuntimeTestKotlin` passed.
- Runtime metadata: `runtimeClasspath` includes
  `org.apache.cassandra:java-driver-mapper-runtime:4.19.2`.
- Compile: `:bluetape4k-cassandra:compileKotlin`,
  `:bluetape4k-cassandra:compileTestKotlin`, and
  `:bluetape4k-cassandra:compileConsumerRuntimeTestKotlin` passed with
  `--warning-mode all`; remaining warnings are existing repo Gradle Kotlin DSL
  deprecations outside the new source-set declaration.
- Tests: `:bluetape4k-cassandra:test` passed with 178 tests, 0 failures,
  0 errors, 0 skipped; `:bluetape4k-cassandra:consumerRuntimeTest` passed with
  1 test, 0 failures, 0 errors, 0 skipped.
- `git diff --check` passed.

## Concurrency Helper Gate

Not applicable. The change does not modify thread safety, coroutine
cancellation, structured task scope behavior, or virtual-thread execution.
