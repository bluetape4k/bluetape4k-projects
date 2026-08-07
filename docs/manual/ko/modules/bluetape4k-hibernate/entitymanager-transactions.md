---
title: EntityManager와 transaction
description: EntityManager 소유권, persist·merge·delete, flush와 bulk query의 경계를 설명합니다.
manualId: bluetape4k-hibernate
chapterId: entitymanager-transactions
---

# EntityManager와 transaction

## transaction 소유자는 하나여야 한다

독립 실행 코드에서 `withNewEntityManager`는 새 `EntityManager`와 transaction을 만들고 block 성공 시 commit, 실패 시 rollback한 뒤 manager를 닫습니다.

```kotlin
val result = emf.withNewEntityManager { em ->
    val account = Account("user@example.com")
    em.persist(account)
    account.identifier
}
```

Spring의 `@Transactional` 안에서는 이미 framework가 `EntityManager`와 commit을 소유합니다. 그 안에서 `withNewEntityManager`를 호출하면 같은 service 작업에 별도 persistence context와 transaction이 생길 수 있습니다. framework-managed 경계에서는 주입된 `EntityManager`를 사용합니다.

block이나 commit이 실패하면 rollback을 시도합니다. 1.12.1에서 rollback 실패는 warn log로만 남고 원래 예외에 suppressed로 붙지 않습니다. 원래 예외는 유지되지만 rollback 원인을 사후 분석하려면 해당 logger를 수집해야 합니다.

## save는 persist와 merge를 고른다

`save`는 `entity.isPersisted && !contains(entity)`이면 `merge`, 그 밖에는 `persist`를 호출합니다.

```kotlin
val detached: Account = loadOutsideCurrentContext()
detached.name = "new name"

val managed = entityManager.save(detached)
// 이후 변경은 managed에 적용한다.
```

`merge`는 detached instance를 다시 managed로 바꾸는 함수가 아닙니다. 상태를 managed instance로 복사하고 그 instance를 반환합니다. `save` 반환값을 버리고 원본을 계속 수정하면 변경이 저장되지 않을 수 있습니다.

`delete`는 id가 없는 entity를 무시합니다. detached entity는 merge한 뒤 삭제합니다. `deleteById`는 reference로 삭제를 시도하지만 SQL, foreign-key와 optimistic-lock 오류는 `remove` 호출보다 flush나 commit에서 늦게 나타날 수 있습니다.

## flush 시점을 이해한다

Hibernate는 SQL을 API 호출과 동시에 실행하지 않을 수 있습니다. constraint 위반이나 database 오류가 `persist`가 아니라 query 전 자동 flush, 명시적 `flush`, transaction commit에서 나타날 수 있습니다. 테스트에서 실패 위치를 고정하려면 검증하려는 작업 뒤에 `flush()`를 명시합니다.

```kotlin
entityManager.persist(account)
entityManager.flush() // database constraint까지 이 지점에서 검증
```

## 전체 조회와 bulk delete

`findAll`은 조건 없는 Criteria query에 paging을 설정합니다. 기본 `maxResults`가 `Int.MAX_VALUE`이므로 운영에서는 반드시 상한을 지정합니다.

```kotlin
val page = entityManager.findAll(
    Account::class.java,
    firstResult = 0,
    maxResults = 100,
)
```

`deleteAll<T>()`은 JPQL bulk delete입니다. managed entity를 하나씩 지우지 않으므로 entity callback, cascade와 persistence-context 상태를 맞춰 주지 않습니다. 먼저 필요한 변경을 flush하고, 실행 뒤에는 `clear()`하거나 transaction을 끝냅니다.

```kotlin
entityManager.flush()
val deleted = entityManager.deleteAll<ExpiredSession>()
entityManager.clear()
```

## Hibernate 내부 경계

`currentSession`은 표준 `unwrap(Session::class.java)`를 사용합니다. 반면 `currentSessionImpl`과 `currentConnection`은 Hibernate 내부 구현인 `SessionImpl`과 `jdbcCoordinator`에 접근합니다. provider 교체가 가능한 code나 일반 repository에서는 쓰지 말고, Hibernate 전용 integration이 물리 connection을 반드시 요구할 때만 좁게 둡니다.

## 학습할 테스트

```bash
./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.EntityManagerFactorySupportTest'

./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.EntityManagerSupportTest'
```

## Source와 tests

- [`EntityManagerFactorySupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupport.kt)
- [`EntityManagerSupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt)
- [`EntityManagerFactorySupportTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerFactorySupportTest.kt)
- [`EntityManagerSupportTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/EntityManagerSupportTest.kt)
