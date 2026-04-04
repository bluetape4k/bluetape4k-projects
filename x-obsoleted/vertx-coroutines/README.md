# Module Examples - Vert.x with Kotlin Coroutines

English | [한국어](./README.ko.md)

> **⚠️ Obsolete**: This module has been merged into `bluetape4k-vertx` and fully excluded from the build.

Examples of using Vert.x with Kotlin Coroutines.

## Examples

### Movie Rating Example (movierating/)

| File                          | Description                               |
|-----------------------------|-------------------------------------------|
| `MovieRatingVerticle.kt`    | A Verticle exposing an HTTP API           |
| `Main.kt`                   | Application entry point                   |
| `MovieRatingVerticeTest.kt` | Tests for the Verticle                    |

## Key Learning Points

### Implementing a Verticle

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

### Async Processing with Coroutines

```kotlin
// Use await() to treat a Future as a suspend function
val result = vertx.executeBlocking<String> { promise ->
    promise.complete(blockingOperation())
}.await()
```

### Testing

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

## Running the Examples

```bash
./gradlew :examples:vertx-coroutines:test
```

## References

- [Vert.x Kotlin Coroutines](https://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/)
- bluetape4k-vertx-core
