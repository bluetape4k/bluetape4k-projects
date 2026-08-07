---
title: Entity, collection과 query cache
description: Hibernate annotation, 새 Session 검증, collection Region과 query cache invalidation을 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: entity-query-collection-cache
---

# Entity, collection과 query cache

## Entity cache는 명시적으로 켠다

전역 2차 캐시를 켜도 모든 entity가 자동으로 캐시되는 것은 아닙니다. 캐시할 entity에 JPA `@Cacheable`과 Hibernate `@Cache`를 지정합니다.

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Product(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String = "",
)
```

변경 빈도가 높거나 항상 최신 값이 필요한 entity까지 습관적으로 캐시하면 invalidation 비용과 stale window만 늘어납니다. read-heavy reference data부터 시작합니다.

## Collection은 별도 Region이다

entity를 캐시했다고 association collection이 자동으로 캐시되지는 않습니다. collection field에도 `@Cache`가 필요합니다.

```kotlin
@OneToMany(mappedBy = "department", cascade = [CascadeType.ALL])
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
val employees: MutableList<Employee> = mutableListOf()
```

collection Region 이름은 보통 `소유Entity의 FQCN.property`입니다. `containsCollection`과 `evictCollectionData`에도 같은 role 이름을 사용합니다. collection cache에는 member id 목록이 들어가고 각 entity state는 entity Region에서 따로 읽힐 수 있으므로 두 Region의 hit를 함께 봅니다.

## Query cache는 한 단계 더 필요하다

query cache는 전역 설정과 query 단위 opt-in이 모두 필요합니다.

```properties
hibernate.cache.use_query_cache=true
hibernate.generate_statistics=true
```

```kotlin
val people = session
    .createSelectionQuery(
        "select p from Person p where p.age > :age order by p.id",
        Person::class.java,
    )
    .setParameter("age", 20)
    .setCacheable(true)
    .list()
```

query cache는 entity 전체보다 결과 식별자 집합을 저장합니다. 같은 query string과 parameter라도 관련 table이 바뀌면 update timestamps Region을 이용해 결과를 무효화합니다. 그래서 timestamps Region에는 Redis TTL을 적용하지 않습니다.

## Rollback과 eviction

실패한 transaction의 변경을 새 Session에서 읽었을 때 rollback 전 값이 보여야 합니다. 1.12.1 테스트는 entity update와 delete rollback 뒤 DB와 cache가 기존 값을 유지하는지 검증합니다. 운영 코드가 Redis key를 직접 고치면 Hibernate transaction completion에 맞춘 eviction 흐름을 우회하므로 금지합니다.

## Source와 tests

- [`HibernateEntityCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateRelationCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateRelationCacheTest.kt)
- [`HibernateElementCollectionCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateElementCollectionCacheTest.kt)
- [`HibernateQueryCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateQueryCacheTest.kt)
- [`HibernateTransactionRollbackTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateTransactionRollbackTest.kt)
