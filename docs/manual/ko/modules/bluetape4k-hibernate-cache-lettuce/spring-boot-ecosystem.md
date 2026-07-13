---
title: Spring Boot와 생태계 경로
description: Spring Boot 자동 설정과 demo를 사용하고 Hibernate, Lettuce, Exposed cache 학습 경로를 선택하는 방법을 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: spring-boot-ecosystem
---

# Spring Boot와 생태계 경로

## Core provider와 자동 설정을 나눈다

현재 모듈은 Hibernate RegionFactory 자체입니다. Spring Boot properties binding, `HibernatePropertiesCustomizer`, Micrometer와 Actuator가 필요하면 별도 artifact를 추가합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce")
}
```

Spring Boot module은 core provider를 API dependency로 포함하므로 둘의 버전을 따로 맞추지 않습니다.

```yaml
bluetape4k:
  cache:
    lettuce-near:
      redis-uri: redis://localhost:6379
      codec: lz4fory
      use-resp3: true
      local:
        max-size: 10000
        expire-after-write: 30m
      redis-ttl:
        default: 120s
        regions:
          io.example.Product: 300s
      metrics:
        enabled: true
        enable-caffeine-stats: true
```

자동 설정은 이 값을 `hibernate.cache.lettuce.*`로 변환하고 2차 캐시를 켭니다. `enabled=false`이면 자동 설정이 물러납니다. Metrics를 켜면 Hibernate statistics도 활성화되므로 운영 비용을 측정합니다.

## 실행 가능한 demo

[bluetape4k-spring-boot-hibernate-lettuce-demo](../bluetape4k-spring-boot-hibernate-lettuce-demo.md)는 `Product` entity, Spring Data repository, cache controller와 실제 설정을 담았습니다. core 모듈 테스트로 provider 계약을 확인한 뒤 demo에서 HTTP 요청과 cache statistics를 연결합니다.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce-demo:bootRun
```

demo 설정을 그대로 운영에 복사하지 않습니다. Redis 인증·TLS, DB migration, TTL, pool과 observability 정책은 배포 환경에 맞게 정합니다.

## 어디로 이어갈까

| 필요한 학습 | 다음 경로 | 이유 |
| --- | --- | --- |
| entity와 transaction 기본 | [bluetape4k-hibernate](../bluetape4k-hibernate.md) | cache 전에 ORM lifecycle을 이해합니다. |
| Near Cache 직접 사용 | [bluetape4k-cache-lettuce](../bluetape4k-cache-lettuce.md) | Hibernate Region 밖의 cache API를 다룹니다. |
| Redis command와 codec | [bluetape4k-lettuce](../bluetape4k-lettuce.md) | Lettuce client·serialization 계층을 확인합니다. |
| Spring Boot Metrics·Actuator | [Spring Boot Hibernate Lettuce](../bluetape4k-spring-boot-hibernate-lettuce.md) | 자동 설정과 운영 endpoint를 붙입니다. |
| repository cache 전략 | [exposed-workshop](https://github.com/bluetape4k/exposed-workshop) | cache-aside와 read/write-through를 DB repository 관점에서 비교합니다. |

Hibernate 2차 캐시는 entity state와 query 결과를 Hibernate lifecycle 안에서 캐시합니다. Exposed workshop의 `JdbcCacheRepository`, `EntityMapLoader`, `EntityMapWriter`는 애플리케이션 repository가 read-through·write-through·write-behind를 소유하는 다른 구조입니다. 어느 쪽이 더 낫다기보다 persistence owner가 다릅니다.

## Source와 links

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheSpringProperties.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`application.yml` in demo](../../../../../spring-boot/hibernate-lettuce-demo/src/main/resources/application.yml)
- [bluetape4k-exposed 저장소](https://github.com/bluetape4k/bluetape4k-exposed)
- [exposed-workshop](https://github.com/bluetape4k/exposed-workshop)
