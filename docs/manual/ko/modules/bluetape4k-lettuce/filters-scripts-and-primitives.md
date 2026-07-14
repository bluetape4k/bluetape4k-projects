---
title: Filter, script와 분산 primitive
description: 확률 자료구조, Lua fallback과 Redis 원자 연산 wrapper를 선택합니다.
manualId: bluetape4k-lettuce
chapterId: filters-scripts-and-primitives
---

# Filter, script와 분산 primitive

## 확률 자료구조를 정확히 읽는다

Bloom filter의 `contains=false`는 확실한 부재지만 `true`는 false positive일 수 있습니다. Cuckoo filter는 삭제를 지원하지만 제한된 bucket에서 insert가 실패할 수 있습니다. 서로 다른 option으로 이미 만든 filter를 다시 초기화하면 예외가 납니다.

```kotlin
val filter = LettuceBloomFilter(
    connection,
    "blocked-email",
    BloomFilterOptions(expectedInsertions = 100_000, falseProbability = 0.01),
)
filter.tryInit()
filter.add("spam@example.com")
```

HyperLogLog는 정확한 set 크기가 아니라 근사 cardinality를 제공합니다. 과금이나 quota처럼 정확성이 필요한 판단에는 쓰지 않습니다.

## Lua script의 fallback

`RedisScript`는 source의 SHA1을 미리 계산합니다. `RedisScriptRunner`는 sync, async, suspend 모두 `EVALSHA`를 먼저 실행하고 Redis가 `NOSCRIPT`를 반환할 때 원문 `EVAL`로 재시도합니다. 다른 script 오류는 fallback으로 숨기지 않습니다.

AtomicLong, semaphore, lock은 Redis 명령과 Lua script를 조합합니다. lock과 permit은 `try/finally`에서 반환하고, network 분할이 있는 업무 원자성을 단순 mutex와 동일하게 보지 않습니다.

## Source와 tests

- [`RedisScript.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt)
- [`LettuceBloomFilter.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/filter/LettuceBloomFilter.kt)
- [`LettuceCuckooFilter.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/filter/LettuceCuckooFilter.kt)
- [`RedisScriptTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt)

다음은 [운영과 생태계 경로](./operations-and-ecosystem.md)입니다.
