# Module bluetape4k-vertx-core

## 개요

[Eclipse Vert.x](https://vertx.io/) 를 Kotlin에서 사용하기 위한 편의 기능을 제공합니다.

## Features

Vert.x 의 여러가지 기능을 Coroutines 환경에서 수행할 수 있도록 해줍니다.

Vert.x 는 대표적인 Async/Non-Blocking 라이브러리로서, 다양한 Network 통신, Database 통신 뿐 아니라,
Event Loop 를 통한 내부 작업 또한 Async/Non-Blocking 으로 수행합니다. 이런 이유로 성능은 상당히 좋을 수 있지만,
Callback (Async Handler) 방식으로 구현해야 해서, 구현 난이도가 높고, Readable 하지 않은 단점이 있습니다.

이런 불편함을 해결하기 위해 Vert.x 를 Coroutines 환경하에서 수행될 수 있도록 할 수 있습니다. Vertx 의 `vertx-lang-kotlin-coroutines` 에서 제공하는
`Vertx.dispatcher()` 를 사용하여 Coroutines 환경 하에서 실행할 수 있습니다.

이를 위해 `bluetape4k-vertx-core` 에서는 Vert.x 용 extension 함수를 제공합니다.

```kotlin
/**
 * Get current [Vertx] instance
 */
fun currentVertx(): Vertx = Vertx.currentContext()?.owner() ?: Vertx.vertx()
```

```kotlin
/**
 * Get [CoroutineDispatcher] of current [Vertx]
 */
fun currentVertxDispatcher(): CoroutineDispatcher = currentVertx().dispatcher()

/**
 * [Vertx]의 dispacher 를 사용하는 [CoroutineScope] 를 생성합니다.
 */
fun Vertx.asCoroutineScope(): CoroutineScope = CoroutineScope(this.dispatcher())

/**
 * Current [Vertx] 의 Thread 를 사용하는 [CoroutineScope]를 빌드합니다.
 */
fun currentVertxCoroutineScope(): CoroutineScope =
    currentVertx().asCoroutineScope()
```

위의 extension methods 를 이용하여, 다양한 Vert.x 작업을 구현할 수 있습니다.

```kotlin
runSuspendTest(vertx.dispatcher()) {
    val requestCount = 10
    val webClient = WebClient.create(vertx)
    val deploymentCheckpoint = testContext.checkpoint()
    val requestCheckpoint = testContext.checkpoint(requestCount)

    log.debug { "Deply SampleVerticle" }
    awaitResult<String> { vertx.deployVerticle(SampleVerticle(), it) }
    deploymentCheckpoint.flag()  //testContext 에게 현 단계까지 완료되었음을 알린다.

    repeat(requestCount) { requestIndex ->
        launch {
            val resp = awaitResult<HttpResponse<String>> { handler ->
                log.debug { "Request $requestIndex" }
                webClient.get(11981, "localhost", "/")
                    .`as`(BodyCodec.string())
                    .send(handler)
            }
            testContext.verify {
                resp.statusCode() shouldBeEqualTo 200
                resp.body() shouldContain "Yo!"
                // testContext에 완료되었음을 알린다 (CountDownLatch와 유사)
                // 모두 차감하면 testContext.completeNow() 와 같이 테스트가 종료된다.
                requestCheckpoint.flag()
            }
        }
    }
}
```
