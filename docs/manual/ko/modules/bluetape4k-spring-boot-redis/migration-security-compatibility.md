---
title: 보안, 호환성과 마이그레이션
description: JDK 역직렬화 위험과 serializer schema 변화, rolling deployment에서 Redis 값을 안전하게 이전하는 방법을 설명합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: migration-security-compatibility
---

# 보안, 호환성과 마이그레이션

## Redis 값도 입력입니다

Redis가 사설 네트워크에 있어도 여러 애플리케이션과 배치, 운영 도구가 값을 쓸 수 있습니다. 탈취된 자격 증명이나 잘못된 producer가 만든 payload도 읽는 쪽에서는 외부 입력입니다. 역직렬화 전에 key namespace와 쓰기 권한을 제한하고 허용할 type을 좁힙니다.

## JDK 역직렬화 경계

JDK serialization은 classpath의 gadget chain을 악용할 수 있습니다. 1.12.1은 아래 상수를 모두 deprecated로 표시합니다.

- `Jdk`
- `GzipJdk`
- `LZ4Jdk`
- `SnappyJdk`
- `ZstdJdk`

deprecated replacement는 같은 압축의 Kryo 조합 또는 기본 `Kryo`입니다. 이 표시는 기존 데이터를 즉시 못 읽게 하지는 않지만 새 저장 형식으로 선택하지 말라는 계약입니다.

## serializer 변경은 schema migration

Kryo에서 Fory로, LZ4에서 Zstd로 바꾸면 byte format이 바뀝니다. 같은 data class라도 field나 polymorphic subtype 변화가 호환성을 깨뜨릴 수 있습니다.

안전한 전략은 새 key prefix를 쓰는 것입니다.

```text
orders:v1:{id}  -> old serializer
orders:v2:{id}  -> new serializer
```

reader는 migration 동안 v2를 먼저 읽고 없으면 v1을 읽어 v2로 다시 저장할 수 있습니다. 이 방식은 단순하지만 fallback read와 write amplification이 생기므로 종료 조건을 정합니다.

## rolling deployment 확인

배포 전 최소 네 조합을 검증합니다.

| Writer | Reader | 확인할 것 |
| --- | --- | --- |
| old | old | 현재 baseline |
| old | new | 기존 값 읽기 |
| new | new | 새 형식 round trip |
| new | old | rolling 기간 역호환 또는 key 분리 |

마지막 조합이 실패한다면 새 writer가 구 reader와 같은 key 공간에 값을 쓰지 않도록 배포 순서나 prefix를 나눕니다.

## 실패를 숨기지 않기

deserialize 실패 시 무조건 cache miss로 처리하면 corruption과 공격 payload가 원본 저장소 부하로 바뀔 수 있습니다. 예외 수, key prefix와 schema version을 기록하고 특정 key를 격리하거나 삭제하는 정책을 둡니다.

payload 자체와 개인정보는 log에 남기지 않습니다. 실패 byte size, codec 조합, exception type과 배포 version이면 대부분의 운영 진단에 충분합니다.

## null과 삭제 의미

serializer의 null은 빈 byte array가 되지만 Redis key 삭제와 같지 않습니다. “값 없음”은 key 삭제 또는 명시적인 envelope로 표현합니다. null을 저장해 negative cache로 사용하려면 다른 정상 빈 payload와 구분 가능한 marker를 정의합니다.

## Source와 tests

- [`RedisBinarySerializers.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisBinarySerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializerTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
