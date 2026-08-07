---
title: Binary serializer 선택
description: RedisBinarySerializer의 null 계약과 Kryo·Fory·JDK 조합, lazy singleton의 사용 기준을 설명합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: binary-serializers
---

# Binary serializer 선택

## BinarySerializer를 Spring 계약에 연결하기

`RedisBinarySerializer`는 bluetape4k `BinarySerializer`를 Spring Data Redis의 `RedisSerializer<Any>`로 감쌉니다. factory operator로 필요한 구현을 직접 연결할 수 있습니다.

```kotlin
val serializer = RedisBinarySerializer(BinarySerializers.LZ4Fory)

val encoded = serializer.serialize(order)
val decoded = serializer.deserialize(encoded) as Order
```

wrapper는 type metadata 정책을 추가하지 않습니다. class registration, schema 호환성과 역직렬화 가능한 type은 내부 binary serializer의 계약을 그대로 따릅니다.

## 제공되는 singleton 조합

`RedisBinarySerializers`는 모든 instance를 `lazy`로 만듭니다. 사용하지 않는 codec은 object 초기화 시점에 만들지 않습니다.

| 직렬화 | 압축 없음 | GZip | LZ4 | Snappy | Zstd |
| --- | --- | --- | --- | --- | --- |
| Kryo | `Kryo` | `GzipKryo` | `LZ4Kryo` | `SnappyKryo` | `ZstdKryo` |
| Fory | `Fory` | `GzipFory` | `LZ4Fory` | `SnappyFory` | `ZstdFory` |
| JDK | `Jdk` | `GzipJdk` | `LZ4Jdk` | `SnappyJdk` | `ZstdJdk` |

JDK 행은 모두 deprecated입니다. 일반 객체에는 Kryo 또는 Fory를 고릅니다.

## null과 빈 byte array

`serialize(null)`은 `emptyByteArray`를 반환하고 `deserialize(null)`은 `null`을 반환합니다. 이 계약은 모든 binary 조합 테스트에서 반복 검증됩니다.

문제는 빈 byte array가 실제 값일 수도 있다는 점입니다. 업무상 null, key 부재, 빈 payload를 구분해야 한다면 template 호출부에서 null 저장을 금지하거나 별도 envelope를 사용합니다. Redis key 부재는 template API의 반환값으로 처리하고 serializer null을 domain 상태로 사용하지 않는 편이 단순합니다.

## Kryo와 Fory 선택

두 serializer 모두 객체를 compact binary format으로 저장합니다. 선택은 일반적인 속도 인상보다 실제 model 호환성으로 결정합니다.

- representative DTO, collection과 nullable field를 왕복 테스트합니다.
- writer와 reader가 다른 애플리케이션 version일 때도 검증합니다.
- class 이름·field 변화와 polymorphic type 정책을 확인합니다.
- 저장 데이터를 다른 언어나 외부 도구가 읽어야 하면 JSON 또는 명시적인 schema format도 비교합니다.

1.12.1 테스트는 String, data class, list와 여러 압축 조합의 같은-version round trip을 검증합니다. 장기 schema evolution까지 보장하지는 않습니다.

## JDK serializer를 피해야 하는 이유

JDK 역직렬화는 신뢰하지 않는 Redis 값에 gadget-chain 공격 경로를 만들 수 있습니다. 그래서 `Jdk`와 네 compressed JDK 상수는 대체 Kryo 상수를 안내하는 `@Deprecated`가 붙어 있습니다.

기존 JDK 값이 있다면 deprecated 경고를 숨기고 계속 쓰기보다 key 공간을 분리하고 migration 기간을 정합니다. Redis write 권한이 다른 서비스나 운영 도구에도 열려 있다면 “내부 Redis라서 안전하다”는 가정을 두지 않습니다.

## Source와 tests

- [`RedisBinarySerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisBinarySerializerTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
