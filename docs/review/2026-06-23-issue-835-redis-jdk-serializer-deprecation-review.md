# Code Review - Issue #835 Redis JDK serializer deprecation

Date: 2026-06-23
Issue: #835
Module: `:bluetape4k-spring-boot-redis`

## Summary

Redis-facing JDK serializer constants now preserve the lower-level JDK
deserialization deprecation warning and guide callers toward Kryo or Fory.
The README serializer matrices now mark JDK variants as deprecated,
trusted-data-only choices.

## Findings

- P0: 0
- P1: 0
- P2: 0
- P3: 0

## Review Follow-up

- Fixed the review finding that a source-text regex guard could pass when only
  the first JDK constant carried the warning.
- Replaced that guard with Kotlin reflection over `RedisBinarySerializers`
  member properties, checking each expected JDK property for a `Deprecated`
  annotation and exact `ReplaceWith` expression.
- Added an unexpected-`*Jdk` property guard so future Redis JDK constants must
  be explicitly added to the deprecation contract.
- Suppressed deprecation only at intentional legacy-serializer test call sites.

## Verification

RED:

```text
./gradlew :bluetape4k-spring-boot-redis:test --tests 'io.bluetape4k.spring.redis.serializer.RedisBinarySerializerTest' --no-build-cache
GRADLE_STATUS=1
1 failing
Redis JDK serializer constants preserve deprecation warning
```

The first reflection-based RED attempt was discarded because it passed before
the production annotation existed. The failing source-contract RED established
the missing-warning behavior before implementation.

GREEN:

```text
./gradlew :bluetape4k-spring-boot-redis:cleanTest :bluetape4k-spring-boot-redis:compileKotlin :bluetape4k-spring-boot-redis:compileTestKotlin :bluetape4k-spring-boot-redis:test --no-build-cache
GRADLE_STATUS=0
83 passing
BUILD SUCCESSFUL in 8s
```

Static checks:

```text
git diff --check
clean
```

## Scope Notes

- Full repository build was not run.
- Merge is intentionally out of scope for this PR per user instruction.
