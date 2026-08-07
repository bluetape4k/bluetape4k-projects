---
title: Transaction, 실패, 테스트
description: 확장 함수 바깥의 transaction과 connection 책임, cardinality 실패와 H2 통합 테스트를 설명합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: transactions-and-testing
---

# Transaction, 실패, 테스트

## transaction은 외부 경계가 소유한다

이 모듈의 확장 함수는 transaction manager를 만들지 않습니다. 현재 coroutine context에 Spring reactive transaction이 연결돼 있으면 underlying Spring Data operation이 그 connection을 사용하고, 없으면 각 publisher의 일반 lifecycle을 따릅니다.

여러 write가 함께 성공해야 한다면 service operation 전체를 Spring의 reactive transaction으로 묶습니다. [`bluetape4k-r2dbc`](../bluetape4k-r2dbc/transactions-and-lifecycle.md)의 `withTransactionSuspend`를 사용할 수도 있지만, 같은 `ConnectionFactory`와 `DatabaseClient` 경계를 유지해야 합니다.

```kotlin
suspend fun createPost(command: CreatePost): Post =
    databaseClient.withTransactionSuspend {
        val post = posts.insert(command.toPost())
        val postId = checkNotNull(post.id)
        comments.insert(command.toInitialComment(postId))
        post
    }
```

프로젝트의 transaction policy가 annotation이나 `TransactionalOperator`를 표준으로 정했다면 그 방식을 따릅니다. 핵심은 개별 extension이 transaction을 자동 시작한다고 가정하지 않는 것입니다.

## connection과 pool 책임

확장 함수는 `R2dbcEntityOperations`로 publisher를 실행할 뿐 connection을 직접 획득하거나 닫지 않습니다. 다음 설정은 애플리케이션 또는 Spring Boot가 소유합니다.

- R2DBC URL과 credential
- driver와 `ConnectionFactory`
- pool 크기, acquire timeout, validation
- startup schema migration
- shutdown 시 pool 종료

1.12.1 테스트의 `R2dbcBlogApplication`은 H2 `ConnectionFactory`를 직접 만들고 initializer bean을 등록합니다. 이는 테스트 애플리케이션 구성 예제이지 모듈이 제공하는 auto-configuration이 아닙니다.

## 실패를 결과와 구분하기

| 상황 | 결과 |
| --- | --- |
| `selectOneSuspending` 0건 | 하위 API 예외 |
| `selectOneOrNullSuspending` 0건 | `null` |
| `selectOne*` 여러 건 | cardinality 예외 |
| `selectFirstOrNullSuspending` 0건 | `null` |
| update/delete 대상 없음 | `0L` |
| count 대상 없음 | `0L` |
| connection, SQL, mapping 실패 | 해당 예외 전파 |
| coroutine cancellation | cancellation 전파 |

`null`, `0L`, exception은 서로 다른 계약입니다. repository에서 모두 같은 domain 결과로 뭉개지 않습니다.

## 1.12.1 통합 테스트 구조

테스트 application은 `r2dbc:h2:mem:///test` connection을 만들고 `schema.sql`로 `posts`, `comments` table을 초기화합니다. `ApplicationReadyEvent` listener가 기본 post 2건과 comment 4건을 삽입합니다.

`R2dbcEntityOperationsExtensionsTest`는 한 test 안에서 insert→update→select→delete→exists를 실행합니다. write마다 반환 행 수와 최종 상태를 확인하므로 오류 위치를 좁히기 쉽습니다.

```kotlin
@Test
fun `insert update delete extensions`() = runTest {
    val saved = operations.insertSuspending(createPost())
    val query = Query.query(Criteria.where(Post::id.name).isEqual(saved.id))

    operations.updateSuspending<Post>(query, Update.update("title", "Updated")) shouldBeEqualTo 1L
    operations.selectOneSuspending<Post>(query).title shouldBeEqualTo "Updated"
    operations.deleteSuspending<Post>(query) shouldBeEqualTo 1L
    operations.existsSuspending<Post>(query).shouldBeFalse()
}
```

## 테스트를 안정적으로 유지하기

공유 H2 database는 빠르지만 test 간 데이터 충돌이 생길 수 있습니다. generated ID나 고유한 입력을 사용하고, 각 test가 만든 row만 정리하거나 transaction rollback을 적용합니다. seed 개수를 정확한 상수로 단정하기보다 변경 전후 차이를 검증하면 test 순서 의존성을 줄일 수 있습니다.

실제 운영 driver의 type conversion, locking, transaction isolation은 H2만으로 증명되지 않습니다. PostgreSQL이나 MySQL 동작이 중요하면 해당 R2DBC driver로 별도 통합 테스트를 직렬 실행합니다.

## Source와 tests

- [`R2dbcBlogApplication.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/R2dbcBlogApplication.kt)
- [`DatabaseInitializer.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/DatabaseInitializer.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)
- [`PostRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/PostRepositoryTest.kt)
- [`CommentRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/CommentRepositoryTest.kt)
- [`PostControllerTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)

## 다음 읽을 장

[R2DBC 생태계 학습 경로](./ecosystem-paths.md)에서 다음 abstraction과 workshop을 선택합니다.
