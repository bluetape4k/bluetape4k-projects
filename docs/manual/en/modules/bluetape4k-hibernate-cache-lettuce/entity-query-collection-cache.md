---
title: Entity, collection, and query caching
description: Configure Hibernate annotations, new-Session tests, collection regions, and query-cache invalidation.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: entity-query-collection-cache
---

# Entity, collection, and query caching

## Opt entities in explicitly

Enabling the global second-level cache does not cache every entity. Add JPA `@Cacheable` and Hibernate `@Cache` to selected entities.

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

Start with read-heavy reference data. Frequently changing or strict-freshness entities can cost more in invalidation and stale windows than they save.

## Collections use separate regions

Caching an entity does not automatically cache its association collections. Annotate the collection itself.

```kotlin
@OneToMany(mappedBy = "department", cascade = [CascadeType.ALL])
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
val employees: MutableList<Employee> = mutableListOf()
```

The collection role is normally `owner FQCN.property`. Use the same role with `containsCollection` and `evictCollectionData`. A collection region stores membership while entity state may come from separate entity regions, so inspect both.

## Query caching needs two opt-ins

Enable query caching globally and mark each query cacheable.

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

Query caching stores result identifiers and relies on the update-timestamps region to invalidate results after related tables change. That timestamps region must not expire in Redis.

## Rollback and eviction

A new Session after rollback must observe the pre-transaction value. The 1.12.1 suite checks update and delete rollback behavior. Never edit Redis keys directly, because that bypasses Hibernate's transaction-completion eviction flow.

## Sources and tests

- [`HibernateEntityCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateRelationCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateRelationCacheTest.kt)
- [`HibernateElementCollectionCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateElementCollectionCacheTest.kt)
- [`HibernateQueryCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateQueryCacheTest.kt)
- [`HibernateTransactionRollbackTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateTransactionRollbackTest.kt)
