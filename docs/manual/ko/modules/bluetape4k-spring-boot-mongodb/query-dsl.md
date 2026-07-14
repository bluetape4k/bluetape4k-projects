---
title: Criteria, Query, Update DSL
description: Spring Data 객체를 유지하면서 조건, 정렬, pagination과 update를 Kotlin 확장으로 조립합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: query-dsl
---

# Criteria, Query, Update DSL

## Criteria는 Spring Data 객체 그대로다

`eq`, `gt`, `inValues`, `regex`, `andWith` 같은 확장은 새로운 query engine을 만들지 않습니다. Spring Data `Criteria` 메서드를 Kotlin infix 문법으로 호출하는 별칭입니다.

```kotlin
val criteria =
    ("age".criteria() gte 20) andWith
        ("city".criteria() inValues listOf("Seoul", "Busan"))
```

`criteriaOf(c1, c2)`도 여러 조건을 `$and`로 묶습니다. field 존재, null, array size, element match, regex 조건도 같은 방식으로 Spring Data BSON 구조를 만듭니다.

## Query 조립

```kotlin
val query = queryOf(
    "status".criteria() ne "deleted",
    "age".criteria() gte 20,
).sortDescBy("createdAt")
 .paginate(page = 0, size = 50)
```

`queryOf()`는 인자가 없으면 빈 `Query`, 하나면 단일 `Criteria`, 여러 개면 `$and` 조건을 만듭니다. `sortBy`, `sortAscBy`, `sortDescBy`, `limitTo`, `skipTo`, `paginate`는 같은 `Query`를 수정해 반환합니다.

`paginate(page, size)`는 `skip(page * size).limit(size)`일 뿐 입력값을 검사하지 않습니다. 음수 page나 0 이하 size를 controller validation에서 거부하고, 곱셈 범위와 큰 offset 비용도 제한합니다.

## Update 조립

```kotlin
val update = ("name" setTo "Alice")
    .andSet("city", "Seoul")
    .andInc("loginCount", 1)
    .andPush("history", "signed-in")
```

`updateOf("name" to "Alice", "age" to 31)`는 모든 pair를 `$set`으로 만듭니다. `setTo`, `incBy`, `unsetField`, `pushValue`, `pullValue`는 한 연산으로 시작하고 `andSet`, `andInc`, `andUnset`, `andPush`로 이어갑니다.

## 문자열 필드의 경계

DSL은 document property를 문자열로 받습니다. 오타와 rename을 compiler가 찾지 못하므로 자주 쓰는 field name은 한곳에 모으거나 mapping metadata와 함께 관리합니다. HTTP query parameter를 그대로 field name이나 regex로 전달하지 말고 허용 목록과 입력 제한을 적용합니다.

## 단위 테스트 방식

Criteria·Query·Update 테스트는 MongoDB를 시작하지 않습니다. 생성한 `criteriaObject`, `queryObject`, `sortObject`, `updateObject`를 Spring Data 기본 API 결과와 비교합니다. DSL을 확장할 때 가장 빠른 regression test 방식입니다.

## Source와 tests

- [`CriteriaExtensions.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensions.kt)
- [`QueryExtensions.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensions.kt)
- [`UpdateExtensions.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt)
- [`CriteriaExtensionsTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensionsTest.kt)
- [`QueryExtensionsTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)
- [`UpdateExtensionsTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensionsTest.kt)

## 다음 읽을 장

[집계, 컬렉션과 스트리밍](./aggregation-collections-streaming.md)에서 일반 find를 넘어서는 operation을 다룹니다.
