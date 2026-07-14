---
title: 쓰기와 원자적 연산
description: Insert, save, update, upsert, delete와 find-and-modify의 서로 다른 계약을 설명합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: writes-and-atomic-operations
---

# 쓰기와 원자적 연산

## Insert와 save를 구분한다

`insertSuspending(entity)`는 새 document 삽입을 위임합니다. 같은 ID가 이미 존재하면 duplicate-key 계열 오류가 날 수 있습니다. `saveSuspending(entity)`는 ID 상태에 따라 insert 또는 기존 document 교체로 이어질 수 있습니다.

```kotlin
val inserted = mongoOperations.insertSuspending(
    User(name = "Alice", email = "alice@example.com", age = 30, city = "Seoul")
)

val saved = mongoOperations.saveSuspending(inserted.copy(city = "Suwon"))
```

부분 필드 변경이 목적이라면 `save`로 document 전체를 다시 쓰기보다 조건 update를 사용합니다.

## UpdateResult를 읽는다

`updateFirstSuspending`, `updateMultiSuspending`, `upsertSuspending`은 `UpdateResult`를 반환합니다.

```kotlin
val result = mongoOperations.updateFirstSuspending<User>(
    queryOf("email".criteria() eq "alice@example.com"),
    ("city" setTo "Suwon").andInc("loginCount", 1),
)

check(result.matchedCount == 1L)
```

`matchedCount`가 0이면 조건에 맞는 document가 없었습니다. `matchedCount`가 1이고 `modifiedCount`가 0이면 저장값이 이미 같거나 server가 변경으로 계산하지 않은 경우일 수 있습니다. 둘을 같은 의미로 다루지 않습니다.

## Upsert

`upsertSuspending`은 조건에 맞는 document가 있으면 update하고, 없으면 insert합니다. 동일 요청이 재실행될 때 새 document가 계속 생기지 않도록 query가 고유 business key를 포함하는지 확인합니다. unique index가 필요한 invariant는 database에도 선언합니다.

## 원자적 수정과 삭제

`findAndModifySuspending`과 `findAndRemoveSuspending`은 조건에 맞는 한 document를 서버 측 원자 연산으로 수정하거나 삭제하고 document를 반환합니다. 대상이 없으면 `null`입니다.

```kotlin
val previous: User? = mongoOperations.findAndModifySuspending(
    queryOf("email".criteria() eq email),
    "loginCount" incBy 1,
)
```

기본 find-and-modify는 수정 전 document를 반환합니다. 이 extension에는 options 인자가 없으므로 수정 후 document가 필요하면 Spring Data의 options 지원 API를 직접 사용합니다.

## Delete 결과와 transaction

조건 또는 entity 삭제는 `DeleteResult`를 반환합니다. `deletedCount`를 기대 건수와 비교합니다. 여러 collection을 함께 바꾸는 business operation은 이 extension이 자동으로 transaction을 만들지 않으므로 service에서 Spring reactive transaction으로 묶습니다.

## Source와 tests

- [`ReactiveMongoOperationsCoroutines.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`UpdateExtensions.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt)

## 다음 읽을 장

[Criteria, Query, Update DSL](./query-dsl.md)에서 write 조건과 update document를 조립합니다.
