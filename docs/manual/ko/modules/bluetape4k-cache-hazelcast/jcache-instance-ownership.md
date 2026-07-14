---
title: JCache와 HazelcastInstance 소유권
description: Hazelcast provider를 명시적으로 선택하고 기존 instance로 CacheManager와 cache를 구성하는 방법을 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: jcache-instance-ownership
---

# JCache와 HazelcastInstance 소유권

## SPI 자동 탐색이 아니라 명시적 provider다

이 모듈의 `META-INF/services/javax.cache.spi.CachingProvider` 파일에는 provider class가 없습니다. `HazelcastJCaching.cacheManagerOf`가 `HazelcastCachingProvider`를 직접 만들고 `propertiesByInstanceItself(hazelcastInstance)`로 이미 연결된 instance를 manager에 넘깁니다.

```kotlin
val manager = HazelcastJCaching.cacheManagerOf(hazelcast)
val users = HazelcastJCaching.getOrCreate<String, User>(
    hazelcastInstance = hazelcast,
    name = "users-v1",
    configuration = MutableConfiguration<String, User>().apply {
        setTypes(String::class.java, User::class.java)
    },
)
```

따라서 classpath의 기본 JCache provider가 Hazelcast라고 가정하지 않습니다. 여러 JCache provider를 함께 쓰는 애플리케이션에서도 이 factory 경로는 전달한 Hazelcast instance를 명시적으로 선택합니다.

## cache identity는 cluster와 이름으로 결정된다

`getOrCreate`는 manager에서 같은 이름의 cache를 먼저 조회하고 없으면 설정으로 생성합니다. 같은 cluster에서 이름을 재사용하면 다른 애플리케이션이나 기능도 같은 분산 데이터를 볼 수 있습니다. 이름에 도메인과 schema 세대를 담는 편이 안전합니다.

```kotlin
val cacheName = "billing-users-v2"
```

`MutableConfiguration.setTypes`는 JCache typed lookup 계약을 분명하게 하지만 Hazelcast wire serialization 자체를 정의하지는 않습니다. value serializer와 schema 호환성은 cluster 설정에서 관리합니다.

## 누가 무엇을 닫는가

`HazelcastJCaching`은 외부 `HazelcastInstance`를 받아 사용합니다. module source에는 `shutdown()` 호출이 없습니다. cache나 manager를 정리하는 것과 cluster client/member를 종료하는 것은 별개의 일입니다.

```kotlin
try {
    users.put("42", user)
} finally {
    users.close()
    // application lifecycle owner closes hazelcast later
}
```

Near Cache의 `close`도 listener와 L1만 정리하며 `IMap`을 destroy하지 않습니다. 배포 종료, cache proxy 종료, 분산 데이터 폐기를 하나의 동작으로 묶지 않습니다.

## Source와 tests

- [`HazelcastJCaching.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastJCaching.kt)
- [`HazelcastCaches.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`CachingProvider` service resource](../../../../../cache/cache-hazelcast/src/main/resources/META-INF/services/javax.cache.spi.CachingProvider)
- [`HazelcastCachesTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
