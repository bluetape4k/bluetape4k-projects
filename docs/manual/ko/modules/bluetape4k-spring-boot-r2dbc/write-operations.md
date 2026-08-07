---
title: Insert, update, delete
description: Entity insert와 조건 update·delete의 반환값, 실패 경계와 안전한 사용 패턴을 설명합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: write-operations
---

# Insert, update, delete

## insert는 저장된 entity를 반환한다

`insertSuspending(entity)`는 Spring Data의 `insert<T>().using(entity)`를 실행하고 저장된 entity를 반환합니다. generated ID가 mapping되면 반환값에서 확인할 수 있습니다.

```kotlin
val saved = operations.insertSuspending(
    Post(title = "R2DBC", content = "Coroutine extensions")
)
checkNotNull(saved.id)
```

`insertOrNullSuspending`은 publisher가 값 없이 끝나는 경우 `null`을 반환합니다. constraint 위반이나 mapping 실패를 `null`로 바꾸지는 않습니다.

## save나 upsert가 아니다

이 모듈에는 entity ID 상태를 보고 insert와 update를 선택하는 `save`가 없습니다. `insertSuspending`에 기존 ID를 가진 entity를 넘겨도 update로 전환된다고 가정하면 안 됩니다. update는 `Query`와 `Update`로 명시합니다.

```kotlin
val query = Query.query(Criteria.where(Post::id.name).isEqual(postId))
val changes = Update.update(Post::title.name, "Updated")

val updated: Long =
    operations.updateSuspending<Post>(query, changes)
```

반환값은 변경된 행 수입니다. ID 조건에서 `updated == 0L`이면 대상이 없었거나 조건이 더 이상 맞지 않는 경우입니다. 정확히 한 건을 기대했다면 호출자가 검증합니다.

## delete 결과 확인하기

조건 삭제는 `deleteSuspending<T>(query)`, 전체 삭제는 `deleteAllSuspending<T>()`를 사용합니다.

```kotlin
val deleted = operations.deleteSuspending<Post>(query)
if (deleted != 1L) {
    throw PostNotFoundException(postId)
}
```

`deleteAllSuspending`은 내부에서 `Query.empty()`를 사용합니다. 테스트 fixture 정리처럼 의도가 분명한 곳에서만 사용하고, 운영 코드에서는 별도의 보호 조건을 둡니다.

## write를 transaction으로 묶기

각 확장 함수는 전달받은 Spring Data operation에 한 번의 write를 위임할 뿐 transaction을 새로 만들지 않습니다. post와 comment가 함께 저장돼야 한다면 service에서 두 호출을 하나의 reactive transaction으로 묶습니다.

transaction 방식은 애플리케이션의 Spring 설정에 맞춥니다. 이 모듈의 API라고 오해해 개별 repository 함수마다 별도 transaction을 만들면 원자적 business operation을 구성할 수 없습니다.

## 오류와 cancellation

- duplicate key와 foreign key 위반은 driver/Spring Data 예외로 전파됩니다.
- converter가 entity를 row로 바꾸지 못하면 mapping 예외가 전파됩니다.
- connection 획득 실패와 timeout도 숨기지 않습니다.
- coroutine cancellation을 잡아 성공이나 `0L`로 바꾸지 않습니다.
- update/delete의 정상적인 0건 결과는 예외와 구분합니다.

재시도는 함수 하나가 아니라 전체 transaction이 idempotent한지 확인한 뒤 바깥 경계에서 적용합니다.

## 1.12.1 검증 흐름

`R2dbcEntityOperationsExtensionsTest`는 새 `Post`를 insert하고 generated ID를 확인한 뒤, 같은 ID 조건으로 title을 update합니다. 다시 select해서 값이 바뀌었는지 확인하고 delete한 뒤 `existsSuspending`이 `false`인지 검증합니다. write helper의 반환값을 다음 검증으로 연결하는 좋은 최소 예제입니다.

## Source와 tests

- [`ReactiveInsertOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt)
- [`ReactiveUpdateOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt)
- [`ReactiveDeleteOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)

## 다음 읽을 장

[Query와 repository 구성](./queries-and-repositories.md)에서 조건을 조립하고 application boundary에 배치합니다.
