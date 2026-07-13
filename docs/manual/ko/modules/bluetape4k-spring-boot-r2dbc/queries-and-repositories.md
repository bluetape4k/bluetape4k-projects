---
title: Query와 repository 구성
description: Spring Data Query와 Criteria로 조건을 만들고 coroutine repository와 WebFlux endpoint를 구성합니다.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: queries-and-repositories
---

# Query와 repository 구성

## Spring Data Query를 그대로 사용한다

이 모듈은 자체 query DSL을 만들지 않습니다. 조건, 정렬, limit는 Spring Data Relational의 `Criteria`와 `Query`로 표현합니다.

```kotlin
fun findAllByPostId(postId: Long): Flow<Comment> {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.selectSuspending(query)
}

suspend fun countByPostId(postId: Long): Long {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.countSuspending(query)
}
```

같은 조건을 select와 count에 사용하면 pagination이나 endpoint metadata를 만들기 쉽습니다. 조건 생성이 복잡해지면 작은 private 함수로 분리하되, repository 밖에 Spring Data query type을 불필요하게 노출하지 않습니다.

## 조건 조합

```kotlin
private fun publishedBy(authorId: Long, keyword: String?): Query {
    var criteria = Criteria.where(Post::authorId.name).isEqual(authorId)
        .and(Post::published.name).isEqual(true)

    if (!keyword.isNullOrBlank()) {
        criteria = criteria.and(Post::title.name).like("%$keyword%")
    }

    return Query.query(criteria)
        .sort(Sort.by(Sort.Direction.DESC, Post::createdAt.name))
        .limit(50)
}
```

사용자 입력을 그대로 wildcard pattern이나 property 이름으로 쓰지 않습니다. 값은 Spring Data가 parameter로 binding할 수 있게 넘기고, 정렬 property는 허용 목록에서 선택합니다.

## repository의 책임

repository는 다음 세 가지를 결정하면 충분합니다.

1. 어떤 entity type과 `Query`를 사용할지
2. 결과 cardinality를 `Flow`, one, first 중 무엇으로 표현할지
3. write 결과 행 수를 domain 결과로 어떻게 해석할지

HTTP status, retry, 여러 repository를 묶는 transaction은 service나 controller advice처럼 더 넓은 경계가 소유합니다. 반대로 raw database exception을 repository에서 무조건 `null`로 바꾸면 데이터 부재와 장애를 구분할 수 없습니다.

## WebFlux endpoint 연결

```kotlin
@RestController
@RequestMapping("/posts")
class PostController(
    private val posts: PostRepository,
) {
    @GetMapping
    fun findAll(): Flow<Post> = posts.findAll()

    @GetMapping("/{id}")
    suspend fun findOne(@PathVariable id: Long): Post =
        posts.findOneByIdOrNull(id) ?: throw PostNotFoundException(id)

    @PostMapping
    suspend fun insert(@RequestBody post: Post): Post =
        posts.insert(post)
}
```

`Flow`와 suspend 함수는 WebFlux가 직접 처리할 수 있습니다. database 호출 뒤 blocking JSON 변환이나 `runBlocking`을 끼워 넣지 않습니다.

## raw SQL이 필요한 경우

복잡한 join, vendor-specific SQL, generated key 세부 제어, DTO projection을 직접 다뤄야 한다면 [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md)의 `R2dbcClient`와 `DatabaseClient` helper가 더 알맞습니다. 이 모듈의 entity extension에 raw SQL API를 억지로 섞지 않습니다.

table과 column을 Kotlin DSL로 다루고 repository 기반 구조를 원하면 `bluetape4k-exposed` R2DBC 계층을 비교합니다. 선택 기준은 [생태계 학습 경로](./ecosystem-paths.md)에 정리했습니다.

## Source와 tests

- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`PostRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`CommentRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`PostController.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/controller/PostController.kt)

## 다음 읽을 장

[Transaction, 실패, 테스트](./transactions-and-testing.md)에서 여러 repository 호출과 검증 환경을 다룹니다.
