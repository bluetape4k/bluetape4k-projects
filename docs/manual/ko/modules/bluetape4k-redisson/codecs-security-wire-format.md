---
title: Codec과 보안
description: Redisson Codec 선택, wire compatibility, package allow-list, fallback decode와 압축 해제 상한을 설명합니다.
manualId: bluetape4k-redisson
chapterId: codecs-security-wire-format
---

# Codec과 보안

## Codec은 배포 계약이다

Codec을 바꾸면 같은 Redis key의 byte format이 바뀝니다. 성능 설정 하나를 바꾸는 일이 아니라 rolling deployment와 rollback에 영향을 주는 schema migration입니다. 같은 cache name을 공유하는 모든 producer와 consumer가 key·value Codec을 맞춰야 합니다.

| 목적 | 1.12.1 선택 | 주의할 점 |
| --- | --- | --- |
| 일반 내부 객체 | `RedissonCodecs.Fory` | 지원하지 않는 값은 Kryo5 fallback을 사용할 수 있습니다. |
| 휘발성 고처리량 cache | `FastForyCodec`, `LZ4FastFory` | FastFory data를 기존 ForyCodec이 역방향으로 읽지 못합니다. |
| 사람이 확인할 JSON | `Jackson3Codec` | type envelope와 package allow-list를 관리합니다. |
| JSONB | `Fastjson2Codec` | class name을 load하기 전에 allow-list로 검증합니다. |
| 큰 값 압축 | LZ4·Zstd·Snappy·GZip wrapper | CPU 비용과 압축 해제 크기 제한을 함께 봅니다. |

## Allow-list를 신뢰 경계에 맞춘다

`Jackson3Codec`과 `Fastjson2Codec`은 `allowedPackagePrefixes`가 없으면 모든 class name을 허용하고 fallback decode도 엽니다. 여러 tenant나 서비스가 Redis를 공유하면 접두사를 좁게 지정합니다.

```kotlin
val codec = Jackson3Codec(
    allowedPackagePrefixes = setOf("com.acme.billing."),
)
```

allow-list가 있으면 JSON envelope가 아닌 binary payload의 fallback decode는 기본적으로 `SecurityException`으로 거부됩니다. trusted migration에서만 `allowFallbackDecode = true`를 일시적으로 사용하고 제거 날짜를 정합니다.

## FastFory의 비대칭 호환성

`FastForyCodec`은 FastFory decode가 실패하면 기존 Fory Codec으로 재시도할 수 있습니다. 따라서 기존 Fory data를 새 reader가 읽는 migration은 가능합니다. 반대로 기존 `ForyCodec`은 FastFory byte를 해석하지 못하므로 old/new application이 동시에 write하는 rolling deployment에는 맞지 않습니다.

안전한 전환은 새 reader가 old format을 읽는 기간을 먼저 배포하고, write format을 바꾼 뒤 old reader를 제거하는 순서입니다. rollback 가능성을 유지해야 한다면 새 format write를 늦춥니다.

## 압축 해제 크기를 제한한다

`GzipCodec`은 `maxDecompressedSize`를 받아 압축 폭탄과 비정상적으로 큰 Redis value를 막습니다.

```kotlin
val codec = GzipCodec(
    innerCodec = RedissonCodecs.Fory,
    maxDecompressedSize = 16 * 1024 * 1024,
)
```

정상 payload의 p99 크기를 기준으로 상한을 정하고 초과는 실패로 관찰합니다. 깨진 gzip payload도 빈 값으로 바꾸지 않고 decode exception을 전파합니다.

## Benchmark를 선택 규칙으로 오해하지 않는다

모듈에 Codec benchmark가 있지만 결과는 payload, JVM, CPU와 library version에 따라 달라집니다. 숫자를 고정된 우열로 옮기지 말고 실제 domain object와 크기로 다시 측정합니다. wire compatibility와 보안 경계가 throughput보다 먼저입니다.

## Source와 tests

- [`RedissonCodecs.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt)
- [`FastForyCodec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt)
- [`Jackson3Codec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt)
- [`Fastjson2Codec.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt)
- [`GzipCodecTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/GzipCodecTest.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt)
