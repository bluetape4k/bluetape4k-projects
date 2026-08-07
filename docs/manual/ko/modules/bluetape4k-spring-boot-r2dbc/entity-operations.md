---
title: Entity operation 시작하기
description: R2dbcEntityOperations에 추가되는 coroutine 확장과 모듈의 책임 경계를 설명합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: entity-operations
---

# Entity operation 시작하기

## 얇은 coroutine 확장 계층

`bluetape4k-spring-boot-r2dbc`의 public 함수는 Spring Data R2DBC operation interface에 대한 Kotlin extension입니다. 별도 client나 repository base class를 만들지 않고, 이미 구성된 `R2dbcEntityOperations`에 조회·삽입·수정·삭제 함수를 붙입니다.

```kotlin
@Repository
class PostRepository(
    private val operations: R2dbcEntityOperations,
) {
    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending(id)

    fun findAll(): Flow<Post> =
        operations.selectAllSuspending()
}
```

Spring Data가 entity metadata와 row mapping을 처리하고, 확장 함수는 Reactor 결과를 coroutine 타입으로 변환합니다. 그래서 mapping annotation, naming strategy, converter와 transaction binding은 기존 Spring Data 설정을 그대로 따릅니다.

## 모듈이 제공하는 receiver

| Receiver | 제공하는 작업 |
| --- | --- |
| `R2dbcEntityOperations` | ID 조회, 조건 조회, count, exists |
| `ReactiveInsertOperation` | entity insert |
| `ReactiveUpdateOperation` | `Query`와 `Update`를 사용한 update |
| `ReactiveDeleteOperation` | 조건 또는 전체 delete |

일반적으로 Spring의 `R2dbcEntityTemplate`이 이 interface들을 구현하므로 같은 bean에서 모든 확장을 사용할 수 있습니다. 확장 함수는 receiver의 lifecycle을 소유하거나 닫지 않습니다.

## ID 조회 helper

ID helper는 기본 이름 `id`로 `Criteria`를 만들고 일반 조회 함수에 위임합니다.

```kotlin
val post: Post = operations.findOneByIdSuspending(1L)
val missing: Post? = operations.findOneByIdOrNullSuspending(-1L)

val first: LegacyPost =
    operations.findFirstByIdSuspending(1L, LegacyPost::postId.name)
```

`idName`은 query에 사용할 이름입니다. property와 실제 column 이름이 다르면 Spring Data mapping 규칙을 확인하고 명시적으로 넘깁니다. 1.12.1 테스트는 기본 `id`와 `Post::id.name` 경로를 모두 검증합니다.

## 정확히 한 건과 첫 행

`findOneById*`는 `selectOne*`으로 위임하므로 중복 행을 허용하지 않습니다. 반면 `findFirstById*`는 첫 결과만 사용합니다. ID가 실제 unique key라면 `one` 계약이 데이터 이상을 드러내는 데 유리합니다. unique하지 않은 검색 조건에서 정렬 후 하나만 필요하면 `first`를 사용합니다.

nullable 변형은 결과가 없을 때만 `null`을 반환합니다. database, mapping, connection 오류까지 `null`로 바꾸는 함수가 아닙니다.

## 이 모듈이 소유하지 않는 것

- `ConnectionFactory`와 R2DBC driver 선택
- connection pool 생성·종료
- schema migration과 초기 데이터
- transaction manager와 transaction 경계
- raw SQL과 custom row mapping

이 책임은 Spring Boot 애플리케이션, Spring Data R2DBC 또는 [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md)에 남아 있습니다. 모듈 이름만 보고 auto-configuration을 기대하면 실제 구성과 문서가 어긋납니다.

## Source와 tests

- [`R2dbcEntityOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt)
- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`PostRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`PostRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/PostRepositoryTest.kt)

## 다음 읽을 장

[Flow와 조회 cardinality](./flow-and-cardinality.md)에서 `Flow`, `one`, `first`, nullable 결과를 구분합니다.
