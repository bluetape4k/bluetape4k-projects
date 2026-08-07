---
manualId: bluetape4k-lettuce
title: "Lettuce 코루틴 클라이언트"
description: "Lettuce Redis client를 Kotlin에서 운용하는 connection, coroutine, codec, map, script 도구를 설명합니다."
kind: library
group: caching
learningOrder: 540
---

# Lettuce 코루틴 클라이언트

## 제공하는 기능 {#problem}

`bluetape4k-lettuce`는 Lettuce Redis client 위에 Kotlin 친화적인 client·connection factory, sync/async/coroutine command 진입점, `RedisFuture` adapter, 객체 codec과 분산 자료구조를 제공합니다. read-through/write-through map, Lua script runner, Bloom/Cuckoo filter 같은 응용 도구도 포함합니다.

이 모듈은 Redis client lifecycle을 대신 관리하는 framework가 아닙니다. client, connection, write-behind worker를 누가 닫을지 애플리케이션이 정해야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- Lettuce의 raw API만 필요한지, Kotlin coroutine과 자료구조 wrapper가 필요한지 구분합니다.
- `RedisClient`, cached connection, 공유 `ClientResources`의 종료 주체를 정합니다.
- 동기, async future, coroutine command 중 한 호출 경로를 선택합니다.
- Redis에 저장할 wire format과 codec 변경 절차를 먼저 고정합니다.
- cache miss, write-through 실패, write-behind queue 포화를 어떻게 처리할지 정합니다.
- 단일 Redis 명령으로 충분한지, Lua script가 필요한 원자 연산인지 확인합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 library 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

coroutine adapter, cache abstraction, Kryo/Fory 압축 codec처럼 `compileOnly` 기능을 쓰면 해당 runtime dependency도 애플리케이션에 포함해야 합니다.

## 첫 연결 {#quick-start}

```kotlin
val client = LettuceClients.clientOf("redis://localhost:6379")
try {
    val commands = LettuceClients.commands(client)
    commands.set("greeting", "hello")
    check(commands.get("greeting") == "hello")
} finally {
    LettuceClients.shutdown(client)
}
```

`commands(client)`는 client별 cached connection을 재사용합니다. 직접 연 connection과 `LettuceLoadedMap`이 만든 connection은 각 소유자가 닫아야 합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| client와 cached connection | `LettuceClients` | `shutdown(client)`와 process 종료 시 `shutdown()`을 구분합니다. |
| sync/async/coroutine command | `commands`, `asyncCommands`, `coroutinesCommands` | coroutine API는 Lettuce experimental API입니다. |
| future를 suspend로 대기 | `awaitSuspending`, `awaitAll` | 실패와 cancellation을 숨기지 않습니다. |
| 객체 저장 | `LettuceBinaryCodecs`, `LettuceJsonCodecs` | codec은 Redis wire format이며 임의 변경할 수 없습니다. |
| map과 loader/writer | `LettuceMap`, `LettuceLoadedMap` | write-through와 write-behind의 실패 시점이 다릅니다. |
| 원자 script | `RedisScriptRunner` | `EVALSHA` 후 `NOSCRIPT`일 때만 `EVAL`로 재시도합니다. |
| 확률 자료구조 | `LettuceBloomFilter`, `LettuceCuckooFilter` | false positive와 초기화 parameter를 계약에 포함합니다. |

## 학습 경로 {#concepts}

아래 장은 기능을 나열하는 데서 끝나지 않습니다. 1.12.1 배포 소스와 대표 테스트를 따라 client ownership, cancellation, codec 호환성, write-behind 장애를 실제 코드 예제와 함께 설명합니다.

1. [Client와 connection](./bluetape4k-lettuce/clients-and-connections.md) — 공유 resource, cached connection, pipeline과 종료 책임을 정합니다.
2. [Command와 coroutine](./bluetape4k-lettuce/commands-and-coroutines.md) — sync/async/coroutine 선택과 future 대기·취소를 다룹니다.
3. [Codec과 직렬화](./bluetape4k-lettuce/codecs-and-serialization.md) — binary/JSON/primitive codec, 신뢰 경계와 migration을 설명합니다.
4. [Map과 cache loader](./bluetape4k-lettuce/maps-and-cache-loading.md) — read-through, write-through, write-behind와 1.12.1 Near Cache 범위를 확인합니다.
5. [Filter, script와 분산 primitive](./bluetape4k-lettuce/filters-scripts-and-primitives.md) — 확률 자료구조와 Lua 기반 원자 연산을 선택합니다.
6. [운영과 생태계 경로](./bluetape4k-lettuce/operations-and-ecosystem.md) — 관측·테스트에서 cache, Hibernate, Exposed, workshop으로 이어갑니다.

## 권장 패턴 {#patterns}

client는 애플리케이션 단위로 재사용하고 shutdown hook에서 닫습니다. 대량 async 명령은 `withPipeline` 안에서 발행만 하고, block 밖에서 `awaitAll()`합니다. codec 변경은 새 key prefix나 전체 cache 비우기 같은 migration 경계를 둡니다. write-behind를 선택했다면 queue 용량, flush 실패, dead-letter와 shutdown drain을 운영 지표로 취급합니다.

## 연동 {#integrations}

Lettuce core는 API dependency입니다. coroutine, cache-core, Kryo, Fory와 압축 serializer는 선택 기능입니다. 함수 결과 memoization은 [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md), Hibernate second-level cache는 [`bluetape4k-hibernate-cache-lettuce`](./bluetape4k-hibernate-cache-lettuce.md)에서 다룹니다.

## 설정 {#configuration}

기본 client는 NCPU 크기의 공유 `ClientResources`, keep-alive, TCP_NODELAY를 사용합니다. connect timeout은 `-Dbluetape4k.lettuce.connectTimeoutMs`로 바꿀 수 있습니다. `LettuceCacheConfig`는 TTL, key prefix, write mode, queue와 shutdown timeout을 검증합니다.

## 실패 동작 {#failures}

`awaitAll()`은 하나의 future라도 실패하면 결과 list 대신 예외를 전파합니다. write-through는 writer가 실패하면 Redis를 갱신하지 않습니다. write-behind queue가 가득 차면 `IllegalStateException`이 발생하며, 반복 flush 실패는 dead-letter key/value에 기록을 시도합니다. coroutine map은 `CancellationException`을 fallback으로 삼키지 않습니다.

## 운영 {#operations}

connection 수와 reconnect, command latency, pipeline batch 크기, write-behind queue와 dead-letter, cache hit/miss를 함께 관찰합니다. `LettuceClients.shutdown(client)`는 해당 client의 cached connection을 닫고, 인자 없는 `shutdown()`은 공유 resource를 닫으므로 process 종료 전에는 호출하지 않습니다.

## 테스트 {#testing}

```bash
./gradlew :bluetape4k-lettuce:test --no-build-cache --no-configuration-cache
```

대표 테스트는 cached connection 재사용, future 결과 순서와 실패 전파, codec 호환성 실패, loader/writer 모드, cancellation과 script fallback을 검증합니다. Redis Testcontainers를 사용하므로 다른 heavy database suite와 병렬 실행하지 않습니다.

## 워크숍 {#workshops}

가장 작은 실행 예제는 `LettuceClientsTest`, `RedisFutureSupportTest`, `LettuceLoadedMapTest`, `RedisScriptTest`입니다. 실제 cache-aside와 database 경계를 연결하려면 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)과 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)의 cache 예제로 이어갑니다.

## 1.12.1 범위 {#limitations}

`LettuceCacheConfig`에는 Near Cache 설정이 있지만 1.12.1의 loaded map은 이를 읽지 않으며 Caffeine 저장소나 RESP3 client tracking invalidation도 구현하지 않습니다. 따라서 `READ_ONLY_WITH_NEAR_CACHE` 같은 preset을 실제 local cache 보장으로 해석하면 안 됩니다. RESP3 설정은 benchmark의 비권장 조합으로만 기록돼 있습니다.

## Source와 tests {#sources}

- [`LettuceClients.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`RedisFutureSupport.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`LettuceBinaryCodecs.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceLoadedMap.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt)
- [`RedisScript.kt`](../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt)
- [`LettuceClientsTest.kt`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)
- [`LettuceSuspendedLoadedMapTest.kt`](../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMapTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 분산 Primitive API 패밀리 다이어그램

[![분산 Primitive API 패밀리 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-01.svg)

_배포본 README: [`infra/lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.ko.md)_

### Lettuce Codec API 구조 다이어그램

[![Lettuce Codec API 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-diagram-02.svg)

_배포본 README: [`infra/lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.ko.md)_

### LettuceLoadedMap Read-Through / Write-Through 흐름 다이어그램

[![LettuceLoadedMap Read-Through / Write-Through 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-lettuce-sequence-01.svg)

_배포본 README: [`infra/lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/lettuce/README.ko.md)_

<!-- release-readme-diagrams:end -->
