---
title: 압축과 Redis wire format
description: 압축 전용 serializer와 객체 직렬화·압축 조합을 구분하고 저장 형식, 성능과 호환성 경계를 설명합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: compression-wire-format
---

# 압축과 Redis wire format

## 압축 전용 serializer

`RedisCompressSerializer`는 `ByteArray`를 받아 압축하고 복원합니다. 객체를 binary format으로 바꾸지 않습니다.

```kotlin
val serializer = RedisCompressSerializer(Compressors.LZ4)
val compressed = serializer.serialize(payload)
val restored = serializer.deserialize(compressed)
```

이미 Protobuf, image, document처럼 byte array를 만드는 계층이 있을 때 사용할 수 있습니다. 객체를 전달해야 한다면 `RedisBinarySerializer` 조합을 사용합니다.

## 제공되는 압축 선택지

압축 전용 singleton은 `Gzip`, `LZ4`, `Snappy`, `Zstd`입니다. binary 조합은 serializer 결과에 같은 네 compressor 중 하나를 적용합니다.

압축 이름은 Redis value에 자동 기록되지 않습니다. `LZ4Fory`로 쓴 값을 `ZstdFory`로 읽으면 이 모듈이 이전 형식을 감지해 주지 않습니다. key prefix, cache 이름 또는 envelope header로 형식을 구분해야 합니다.

## 작은 payload는 측정하기

압축은 network byte와 Redis memory를 줄일 수 있지만 CPU와 allocation을 추가합니다. header와 block overhead 때문에 작은 payload가 오히려 커질 수도 있습니다.

대표 데이터로 다음을 함께 측정합니다.

- 압축 전후 byte size와 compression ratio
- serialize·compress와 decompress·deserialize latency
- command 전체 latency와 application CPU
- Redis memory usage와 network throughput
- 실제 traffic의 payload size 분포

이 모듈의 unit test는 round trip 정확성을 검증할 뿐 특정 조합이 더 빠르다는 benchmark 주장을 하지 않습니다.

## null 계약과 corrupted payload

`serialize(null)`은 빈 byte array, `deserialize(null)`은 `null`입니다. 빈 byte array를 compressor에 넣는 동작과 null을 저장하는 정책은 호출부에서 분리합니다.

잘렸거나 다른 compressor로 만든 payload는 decompress 단계에서 실패합니다. fallback으로 여러 compressor를 순서대로 시도하면 corruption과 schema drift를 숨길 수 있으므로 형식 version을 명시하고 실패를 관찰하는 편이 낫습니다.

## 배포 형식 관리

serializer 조합 변경은 다음 순서로 다룹니다.

1. 새 key prefix 또는 schema version을 정합니다.
2. 새 writer와 reader의 round trip을 검증합니다.
3. rolling deployment에서 구 reader가 새 값을 볼 수 있는지 확인합니다.
4. 필요하면 dual-read 또는 명시적인 migration을 둡니다.
5. 기존 key TTL과 삭제 시점을 정합니다.

압축만 바뀌어도 wire format은 달라집니다. “serializer는 Fory로 같으니 호환된다”고 가정하지 않습니다.

## Source와 tests

- [`RedisCompressSerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisCompressSerializerTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializerTest.kt)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
