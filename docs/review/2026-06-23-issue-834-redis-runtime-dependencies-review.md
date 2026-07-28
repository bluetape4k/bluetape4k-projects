# Issue #834 Redis Runtime Dependencies 검토

## Scope

- `spring-boot/redis/build.gradle.kts`
- `spring-boot/redis/README.md`
- `spring-boot/redis/README.ko.md`
- `spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Runtime contract | Documented `LZ4Fory` and `LZ4Kryo` usage failed on a consumer runtime classpath because Fory/Kryo/LZ4 were compile-only. | P1 | Moved the documented codec and compressor libraries to `runtimeOnly`. |
| Test adequacy | Default module tests inherited `compileOnly`, hiding missing runtime dependencies. | P1 | Added `consumerRuntimeTest`, which uses the module runtime classpath instead of default test inheritance. |
| Documentation | README examples implied hidden dependencies were available. | P1 | Updated English and Korean README installation notes to state the runtime dependency contract. |
| Stack safety | #834 touches the same Redis README area as open #835. | P2 | Kept this work as a stacked branch based on #835. |
| Concurrency | No shared state, coroutine, or concurrent lifecycle behavior changed. | N/A | Concurrency tester gate is not applicable. |

## Verification

- RED: `./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache` failed with missing Fory and LZ4 classes.
- GREEN: `./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache` passed with 2 tests.
- Runtime dependency proof: `./gradlew :bluetape4k-spring-boot-redis:dependencies --configuration runtimeClasspath` lists Fory, Kryo, LZ4, Zstd, and Snappy.
- Targeted module verification: `./gradlew :bluetape4k-spring-boot-redis:compileKotlin :bluetape4k-spring-boot-redis:compileTestKotlin :bluetape4k-spring-boot-redis:test :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache` passed.
- Test XML totals: default Redis tests 83 tests and consumer runtime tests 2 tests; all failures/errors 0.
- `git diff --check` passed.
