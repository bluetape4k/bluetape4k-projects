---
title: JPQL, Criteria와 Querydsl
description: 타입을 반복하지 않는 조회 helper에서 동적 query와 projection까지 이어지는 방법을 설명합니다.
manualId: bluetape4k-hibernate
chapterId: queries-criteria-querydsl
---

# JPQL, Criteria와 Querydsl

## 가장 단순한 표현부터 시작한다

id 조회는 `findAs`, 고정된 짧은 query는 `createQueryAs`, 조건을 조립해야 하면 Criteria나 Querydsl을 사용합니다. query 도구를 하나로 통일하려고 간단한 조회까지 복잡하게 만들 필요는 없습니다.

```kotlin
val account = entityManager.findAs<Account>(id)

val active = entityManager
    .createQueryAs<Account>(
        "select a from Account a where a.active = true order by a.id"
    )
    .setPaging(firstResult = 0, maxResults = 50)
    .resultList
```

`newQuery<T>()`는 조건 없는 전체 조회 query를 만듭니다. 운영에서는 `setPaging`을 함께 사용합니다. `findOneOrNull`은 결과가 없을 때만 null을 반환하고, 여러 행이나 database 오류는 그대로 던집니다.

## Natural ID

단일 `@NaturalId`는 `findBySimpleNaturalId`, 복합 natural id는 property map이나 pair로 조회합니다.

```kotlin
val book = entityManager.findBySimpleNaturalId<Book>(isbn)

val edition = entityManager.findByNaturalId<BookEdition>(
    "isbn" to isbn,
    "edition" to editionNumber,
)
```

복합 map은 비어 있으면 안 되고 property 이름도 blank일 수 없습니다. natural id가 immutable하고 index로 보장되는지 schema와 함께 확인합니다.

## Criteria helper

Criteria helper는 Kotlin property에서 path 이름을 꺼내고 반복되는 class literal을 줄입니다.

```kotlin
val cb = entityManager.criteriaBuilder
val query = cb.createQueryAs<Account>()
val root = query.from<Account>()

query.select(root).where(
    cb.eq(root.attribute(Account::active), true)
)

val accounts = entityManager.createQuery(query).resultList
```

`attribute`는 property 이름을 문자열 path로 넘깁니다. compile-time JPA metamodel 검증을 추가하는 API는 아니므로 property와 mapping 이름이 달라지지 않는지 테스트로 확인합니다.

## Querydsl로 동적 query 구성

Querydsl은 Q 타입, join, subquery, projection이 많은 read model에서 유용합니다. 모듈의 `querydsl.core` 확장은 expression, template, projection과 nullable predicate 조합을 줄입니다.

```kotlin
val account = QAccount.account

val rows = JPAQueryFactory(entityManager)
    .select(account.id, account.email)
    .from(account)
    .where(account.active.isTrue)
    .orderBy(account.id.asc())
    .limit(50)
    .fetch()
```

Q 타입 생성에는 annotation processing이 필요합니다. generated source가 사라지거나 entity mapping이 바뀌면 compile 단계에서 먼저 확인합니다. DTO projection은 constructor, bean, `@QueryProjection` 가운데 변경 비용과 compile-time 검증 수준을 보고 선택합니다.

## 어떤 도구를 고를까

| 상황 | 권장 시작점 |
| --- | --- |
| id 또는 단순 natural id 조회 | `findAs`, natural-id helper |
| 짧고 고정된 query | typed JPQL |
| JPA 표준만 유지하는 동적 조건 | Criteria |
| join·subquery·projection이 많은 Kotlin query | Querydsl |
| 복잡한 pagination과 Entity View | Blaze-Persistence demo |

Querydsl의 오래된 `fetchCount()`와 `fetchResults()`에 기대지 않습니다. 복잡한 query의 count는 별도 query로 만들거나 Blaze-Persistence의 page metadata 경로를 검토합니다.

## 실행 예제

```bash
./gradlew :bluetape4k-hibernate:test \
  --tests 'io.bluetape4k.hibernate.querydsl.simple.SimpleQuerydslExamples'

./gradlew :bluetape4k-examples-jpa-querydsl-demo:test

./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:test
```

## Source와 tests

- [`CriteriaSupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/criteria/CriteriaSupport.kt)
- [`TypedQuerySupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/criteria/TypedQuerySupport.kt)
- [`SessionSupport.kt`](../../../../../data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionSupport.kt)
- [`SimpleQuerydslExamples.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/SimpleQuerydslExamples.kt)
- [`NaturalIdTest.kt`](../../../../../data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping/naturalid/NaturalIdTest.kt)
