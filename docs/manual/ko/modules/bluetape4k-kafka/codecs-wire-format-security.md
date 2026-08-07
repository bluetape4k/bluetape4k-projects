---
title: Codec, wire format과 보안
description: KafkaCodec의 type header, Jackson allowlist, poison pill 정책과 선택형 binary·compression backend를 설명합니다.
manualId: bluetape4k-kafka
chapterId: codecs-wire-format-security
---

# Codec, wire format과 보안

## serializer와 deserializer를 한 타입으로

`KafkaCodec<T>`는 Kafka `Serializer<T>`, `Deserializer<T>`, `Closeable`을 함께 구현합니다. `StringKafkaCodec`, `ByteArrayKafkaCodec`, `JacksonKafkaCodec`과 binary codec을 producer와 consumer 양쪽에 같은 wire-format 계약으로 배치할 수 있습니다.

```kotlin
val codec = StringKafkaCodec()
val bytes = codec.serialize("events", "안녕하세요")
val restored = codec.deserialize("events", bytes)
```

`StringKafkaCodec`은 key/value별 serializer·deserializer encoding 설정을 읽고, 잘못된 charset 이름은 UTF-8로 되돌립니다. 빈 byte array는 `null`로 읽으므로 빈 문자열과 null을 구분해야 하는 protocol에는 그대로 쓰기 어렵습니다.

## type header의 의미

`AbstractKafkaCodec`은 기본적으로 `bluetape4k.kafka.codec.value.type` header에 Java FQN을 씁니다. 역직렬화할 때 Jackson codec은 이 header로 class를 고릅니다. wire format은 payload bytes만이 아니라 이 header까지 포함합니다.

header를 신뢰할 수 없는 producer가 바꿀 수 있다면 class loading 입력도 공격자가 결정합니다. 그래서 1.12.1의 `allowedTypePackages` 기본값은 빈 집합, 즉 모두 거부입니다.

```kotlin
val codec = JacksonKafkaCodec(
    allowedTypePackages = setOf("com.example.events")
)
```

entry는 정확한 FQN 또는 package prefix로 비교합니다. `ALLOW_ALL_TYPES_UNSAFE`는 검사를 완전히 끄므로 모든 producer와 broker가 통제되는 폐쇄 환경 외에는 사용하지 않습니다.

## poison pill 정책

역직렬화 중 일반 `Exception`이 발생하면 `AbstractKafkaCodec`은 WARN을 남기고 `null`을 반환합니다. consumer loop는 계속 돌 수 있지만 실패한 record를 조용히 잃을 수 있습니다. `CancellationException`과 `Error`는 전파합니다.

`null` 반환을 metric으로 세고 Spring Kafka의 `ErrorHandlingDeserializer`, `DeadLetterPublishingRecoverer` 같은 복구 경로를 구성합니다. 정상 payload로 null을 허용하면 성공과 실패를 구별할 envelope가 필요합니다.

## binary codec과 선택 dependency

`KafkaCodecs`에는 Kryo/Fory와 LZ4·Snappy·Zstd 조합이 있습니다. Kryo, Fory, Snappy와 Zstd는 1.12.1 build에서 선택형 dependency입니다. 해당 singleton을 처음 사용할 때 runtime class가 없으면 실패할 수 있습니다.

Fory 계열은 등록하지 않은 class를 허용하는 기본 serializer를 사용하므로 trusted topic에만 적용합니다. registry를 강제해야 한다면 application이 구성한 `BinarySerializer` 기반 codec을 별도로 만듭니다.

## 호환성 변경 절차

codec, compression 또는 class 이름을 바꾸는 것은 배포 옵션 변경이 아니라 topic schema 변경입니다.

1. 기존 consumer가 새 producer record를 읽는지 검사합니다.
2. 새 consumer가 저장된 과거 record를 읽는지 검사합니다.
3. package rename과 allowlist를 함께 갱신합니다.
4. header와 payload 크기, compression CPU를 측정합니다.
5. 실패 record의 DLQ와 replay 절차를 검증합니다.

## Source와 tests

- [`KafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt)
- [`JacksonKafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodec.kt)
- [`BinaryKafkaCodecs.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt)
- [`StringKafkaCodec.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/StringKafkaCodec.kt)
- [`JacksonKafkaCodecSecurityTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/JacksonKafkaCodecSecurityTest.kt)
- [`AbstractKafkaCodecPoisonPillTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt)
