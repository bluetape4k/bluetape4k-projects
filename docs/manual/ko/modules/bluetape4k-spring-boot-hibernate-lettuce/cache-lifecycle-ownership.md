---
title: 캐시 수명주기와 소유권
description: Spring customizer, Hibernate SessionFactory, RegionFactory와 Redis client가 각각 소유하는 수명주기를 구분합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: cache-lifecycle-ownership
---

# 캐시 수명주기와 소유권

## 자동 구성은 자원을 만들지 않는다

`LettuceNearCacheHibernateAutoConfiguration`이 만드는 bean은 `HibernatePropertiesCustomizer` 하나입니다. 이 customizer는 mutable Hibernate property map에 RegionFactory class 이름과 cache 설정을 넣습니다. Redis client나 Caffeine cache를 직접 만들지 않습니다.

실제 자원 생성은 Hibernate가 `SessionFactory`를 시작하면서 `LettuceNearCacheRegionFactory`를 구성할 때 일어납니다. 따라서 자원의 최상위 소유자는 Spring singleton 하나가 아니라 Hibernate `SessionFactory` 수명주기입니다.

## 계층별 책임

| 계층 | 책임 |
| --- | --- |
| Spring Boot property binding | 외부 설정을 typed properties로 읽음 |
| `HibernatePropertiesCustomizer` | Spring 값을 Hibernate property 이름으로 변환 |
| Hibernate `SessionFactory` | RegionFactory 시작과 종료를 호출 |
| `LettuceNearCacheRegionFactory` | region, cache, Lettuce/Redis 자원 관리 |
| Hibernate access strategy | entity·collection의 put, read, invalidation 순서 결정 |
| 애플리케이션 | transaction, entity cache annotation, 운영 설정 소유 |

애플리케이션 코드에서 같은 Redis client를 별도로 만들거나 RegionFactory가 소유한 client를 임의로 닫으면 종료 순서가 깨질 수 있습니다. 반대로 애플리케이션이 별도 목적으로 만든 Lettuce client는 이 모듈이 닫아주지 않습니다.

## Entity와 region

2차 캐시는 모든 entity에 자동 적용되지 않습니다.

```kotlin
@Entity
@Cacheable
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "catalog.product",
)
class Product(/* ... */)
```

concurrency strategy는 update 빈도와 허용 가능한 stale read에 맞춰 고릅니다. region 이름은 TTL, 관측과 eviction의 공통 key가 되므로 팀 규칙으로 안정적으로 유지합니다. 이름을 바꾸면 새 region이 생기고 Redis의 이전 entry가 TTL까지 남을 수 있습니다.

## L1과 L2의 시간 경계

Caffeine L1은 process-local이고 Redis L2는 여러 instance가 공유합니다. RESP3 client tracking을 사용하더라도 network 단절과 재연결 구간을 고려해야 합니다. L1 expiry를 Redis TTL보다 길게 두면 remote entry가 만료된 뒤에도 local entry가 남을 수 있으므로, 특별한 이유가 없다면 L1을 더 짧게 둡니다.

캐시는 transaction의 미완료 변경을 먼저 공개해서는 안 됩니다. cache concurrency strategy와 Hibernate event 순서가 commit·rollback에서 어떻게 동작하는지 실제 database와 Redis를 붙인 테스트로 확인합니다.

## 종료와 재시작

정상 종료에서는 Spring이 JPA `EntityManagerFactory`를 닫고, Hibernate가 `SessionFactory`와 RegionFactory를 차례로 종료합니다. 강제 종료에서는 local cache 정리 callback이 실행되지 않을 수 있지만 Redis TTL이 원격 entry의 최종 수명을 제한합니다.

운영 배포에서 codec이나 key 형식을 바꾸면 rolling update 동안 서로 다른 버전이 같은 Redis region을 읽을 수 있습니다. 호환성이 보장되지 않으면 namespace나 region을 분리하고 이전 cache를 TTL 뒤 제거합니다.

## 실행 근거

- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheRegionFactory.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
