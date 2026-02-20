# Examples - Vert.x with Kotlin Coroutines

Vert.x를 Kotlin Coroutines와 함께 사용하는 예제입니다.

## 예제 목록

### Movie Rating 예제 (movierating/)

| 파일                          | 설명                         |
|-----------------------------|----------------------------|
| `MovieRatingVerticle.kt`    | HTTP API를 제공하는 Verticle 구현 |
| `Main.kt`                   | 애플리케이션 진입점                 |
| `MovieRatingVerticeTest.kt` | Verticle 테스트               |

## 주요 학습 포인트

### Verticle 구현

```kotlin
class MovieRatingVerticle : CoroutineVerticle() {
    
    override suspend fun start() {
        val router = Router.router(vertx)
        
        router.get("/movies/:id/rating").handler { ctx ->
            launch {
                val movieId = ctx.pathParam("id")
                val rating = getRating(movieId)
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(json.encodeToString(rating))
            }
        }
        
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .await()
    }
}
```

### Coroutines로 비동기 처리

```kotlin
// await()를 사용하여 Future를 suspend 함수처럼 사용
val result = vertx.executeBlocking<String> { promise ->
    promise.complete(blockingOperation())
}.await()
```

### 테스트

```kotlin
@Test
fun `should return movie rating`(vertx: Vertx, testContext: VertxTestContext) = runTest {
    val client = WebClient.create(vertx)
    
    val response = client.get(8080, "localhost", "/movies/1/rating")
        .send()
        .await()
    
    testContext.verify {
        response.statusCode() shouldBeEqualTo 200
    }
    testContext.completeNow()
}
```

## 실행 방법

```bash
# 예제 테스트 실행
./gradlew :examples:vertx-coroutines:test
```

## 참고

- [Vert.x Kotlin Coroutines](https://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/)
- [bluetape4k-vertx-core](../../vertx/core/README.md)
