# Issue #834 Redis Serializer Runtime Dependencies

Issue #834 exposed that `bluetape4k-spring-boot-redis` documented
`RedisBinarySerializers.LZ4Fory` and `LZ4Kryo` as ready-to-use serializer
choices while the codec and compressor libraries behind those choices were
only `compileOnly` in the module.

## Decision

- Publish Fory, Kryo, LZ4, Zstd, and Snappy as runtime dependencies of
  `bluetape4k-spring-boot-redis`.
- Keep the default module tests unchanged, but add a separate
  `consumerRuntimeTest` source set that does not inherit the module's
  `compileOnly` test classpath.
- Document that consumers do not need separate codec/compressor dependencies
  for the serializer matrix shown in the README locale set.

## Lessons

- A module test classpath that extends `compileOnly` can hide consumer runtime
  gaps. For published runtime contracts, add a source set whose runtime
  classpath is based on `runtimeClasspath`, not `testImplementation`.
- README examples that use named serializer constants are runtime contracts.
  Either publish the required runtime dependencies or document each optional
  dependency explicitly.
- Stacked PRs are appropriate when a follow-up issue touches the same module
  files as an already-open PR.

## Verification

- RED: `./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache` failed with missing `org.apache.fory.ThreadSafeFory` and `net.jpountz.lz4.LZ4Factory`.
- GREEN: the same task passed with 2 consumer-runtime tests after moving the documented codec/compressor libraries to `runtimeOnly`.
- `./gradlew :bluetape4k-spring-boot-redis:dependencies --configuration runtimeClasspath` showed Fory, Kryo, LZ4, Zstd, and Snappy.
- `./gradlew :bluetape4k-spring-boot-redis:compileKotlin :bluetape4k-spring-boot-redis:compileTestKotlin :bluetape4k-spring-boot-redis:test :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache` passed.
- Test XML totals: default Redis tests 83 tests and consumer runtime tests 2 tests; all failures/errors 0.
- `git diff --check` passed.
