---
title: Flow와 조회 cardinality
description: 여러 행, 단건, 첫 행과 nullable 조회의 실행 시점과 실패 계약을 설명합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: flow-and-cardinality
---

# Flow와 조회 cardinality

## 여러 행은 Flow로 받기

`selectSuspending<T>(query)`와 `selectAllSuspending<T>()`는 `Flow<T>`를 반환합니다. 내부에서는 Spring Data의 reactive select chain에 `flow()`를 적용합니다.

```kotlin
fun findComments(postId: Long): Flow<Comment> {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.selectSuspending(query)
}
```

이 `Flow`는 수집할 때 query를 실행합니다. 같은 instance를 두 번 수집하면 database 작업도 다시 일어날 수 있습니다. 결과를 재사용해야 한다면 transaction과 메모리 한도를 고려해 한 번 수집한 값을 명시적으로 전달합니다.

## cardinality 선택표

| 기대 결과 | API | 0건 | 여러 건 |
| --- | --- | --- | --- |
| 정확히 한 건 | `selectOneSuspending<T>` | 예외 | 예외 |
| 0 또는 정확히 한 건 | `selectOneOrNullSuspending<T>` | `null` | 예외 |
| 적어도 한 건 중 첫 행 | `selectFirstSuspending<T>` | 예외 | 첫 행 |
| 0건 또는 첫 행 | `selectFirstOrNullSuspending<T>` | `null` | 첫 행 |
| 여러 행 | `selectSuspending<T>` | 빈 `Flow` | 모든 행 |

`one`을 nullable로 바꿔도 중복 행이 허용되지는 않습니다. 반대로 `first`는 중복을 검사하는 API가 아닙니다. query의 business invariant에 맞춰 선택해야 합니다.

## count와 exists

전체 개수는 `countAllSuspending<T>()`, 조건별 개수는 `countSuspending<T>(query)`로 확인합니다. 존재 여부만 필요하면 row를 읽어 list로 만들지 않고 `existsSuspending<T>(query)`를 사용합니다.

```kotlin
suspend fun hasComments(postId: Long): Boolean {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.existsSuspending<Comment>(query)
}
```

0건은 count에서 `0L`, exists에서 `false`입니다. 이 값은 database failure를 의미하지 않습니다. driver나 mapping 오류는 예외로 전파됩니다.

## WebFlux까지 Flow 유지하기

1.12.1의 `PostController`는 repository의 `Flow<Post>`를 그대로 반환합니다.

```kotlin
@GetMapping
fun findAll(): Flow<Post> = postRepository.findAll()

@GetMapping("/{id}")
suspend fun findOne(@PathVariable id: Long): Post =
    postRepository.findOneByIdOrNull(id)
        ?: throw PostNotFoundException(id)
```

여러 결과는 `Flow`, 단건은 suspend 함수로 표현하면 controller부터 database까지 non-blocking 경계를 유지하기 쉽습니다. 중간에서 `runBlocking`이나 Reactor `block()`을 호출하지 않습니다.

## 정렬과 first

`first`의 결과가 business 의미를 가지려면 `Query`에 정렬을 명시해야 합니다. 정렬하지 않은 query의 첫 행은 database execution plan에 따라 달라질 수 있습니다.

```kotlin
val latestQuery = Query.empty()
    .sort(Sort.by(Sort.Direction.DESC, "createdAt"))
    .limit(1)

val latest = operations.selectFirstOrNullSuspending<Post>(latestQuery)
```

## Source와 tests

- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`CommentRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`CommentRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/CommentRepositoryTest.kt)
- [`PostController.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/controller/PostController.kt)
- [`PostControllerTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)

## 다음 읽을 장

[Insert, update, delete](./write-operations.md)에서 write 결과와 영향받은 행 수를 다룹니다.
