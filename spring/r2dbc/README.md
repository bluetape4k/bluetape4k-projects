# Module bluetape4k-spring-r2dbc

Spring Data R2dbc 를 Kotlin에서 사용하기 편하게 확장한 라이브러리입니다.

## 주요 기능

- R2dbc 코루틴 확장 함수 제공
- `R2dbcEntityOperations`, `ReactiveInsert/Update/DeleteOperation` 편의 함수
- 조회/삽입/갱신/삭제 시나리오에 대한 테스트 예제 포함

## 코루틴 확장 사용 예시

```kotlin
val post = operations.findOneByIdSuspending<Post>(1L)
val posts = operations.selectAllSuspending<Post>().toList()
val count = operations.countAllSuspending<Post>()
```

```kotlin
val newPost = Post(title = "Hello", content = "World")
val saved = operations.insertSuspending(newPost)

val query = Query.query(Criteria.where(Post::id.name).isEqual(saved.id))
operations.updateSuspending<Post>(query, Update.update("title", "Updated"))
operations.deleteSuspending<Post>(query)
```

## 네이밍 규칙 변경

코루틴 함수는 `suspendXyz` 대신 `XyzSuspending` 형식으로 제공합니다.
기존 `suspendXyz` 함수는 Deprecated 처리되어 있으며, `ReplaceWith`로 자동 치환됩니다.

## 테스트

```bash
./gradlew :bluetape4k-spring-r2dbc:test
```
