---
title: Codec과 직렬화
description: Redis wire format, serializer 신뢰 경계와 안전한 codec 변경 절차를 설명합니다.
manualId: bluetape4k-lettuce
chapterId: codecs-and-serialization
---

# Codec과 직렬화

## codec은 저장 형식이다

`LettuceBinaryCodec<V>`는 String key와 `BinarySerializer`가 만든 value bytes를 조합합니다. `LettuceJsonCodec<V>`는 지정한 `valueType`으로 JSON을 복원합니다. `LettuceIntCodec`과 `LettuceLongCodec`은 fixed-width big-endian 형식이라 Redisson primitive codec과 호환됩니다.

```kotlin
val codec = LettuceBinaryCodecs.lz4Fory<User>()
val connection = LettuceClients.connect(client, codec)
connection.sync().set("user:1", User(1, "Alice"))
```

## migration 없이 바꾸지 않는다

기본 binary codec은 LZ4+Fory입니다. FastFory는 Fory와 상호 호환되지 않고 fallback도 없으며 순환 참조 객체를 지원하지 않습니다. 이미 저장된 key를 다른 codec으로 읽으면 역직렬화 예외가 납니다. 새 prefix에 이중 기록하거나 기존 cache를 비운 뒤 전환합니다.

## 신뢰하지 않는 payload

JDK/Kryo/Fory 같은 객체 역직렬화기는 애플리케이션이 쓴 신뢰 가능한 cache 값에만 사용합니다. 외부 사용자가 임의 bytes를 Redis에 넣을 수 있다면 JSON schema·허용 type·접근 권한을 좁히거나 primitive/string codec을 선택합니다. 압축은 작은 값에서 CPU와 header 비용이 더 클 수 있으므로 실제 payload로 측정합니다.

## Source와 tests

- [`LettuceBinaryCodecs.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
- [`LettuceJsonCodec.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt)
- [`FastForyCompatibilityTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt)

다음은 [Map과 cache loader](./maps-and-cache-loading.md)입니다.
