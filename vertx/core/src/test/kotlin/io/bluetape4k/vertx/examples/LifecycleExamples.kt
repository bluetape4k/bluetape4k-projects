package io.bluetape4k.vertx.examples

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.vertx.tests.withSuspendTestContext
import io.bluetape4k.vertx.tests.withTestContext
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.file.fileSystemOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * [Lifecycle example](https://vertx.io/docs/vertx-junit5/java/#_lifecycle_methods)
 */
@ExtendWith(VertxExtension::class)
class LifecycleExample {

    companion object: KLoggingChannel()

    private lateinit var vertx: Vertx

    @BeforeEach
    fun prepare() {
        val options = vertxOptionsOf(
            maxEventLoopExecuteTime = 1000,
            preferNativeTransport = true,
            fileSystemOptions = fileSystemOptionsOf(fileCachingEnabled = true)
        )
        vertx = Vertx.vertx(options)
    }

    @AfterEach
    fun cleanup() {
        vertx.close()
    }

    @Test
    fun `deploy sample verticle with custom vertx`(testContext: VertxTestContext) =
        withTestContext(testContext) {
            vertx.deployVerticle(SampleVerticle(), testContext.succeeding { testContext.completeNow() })
        }

    @Test
    fun `request to server`(testContext: VertxTestContext) = withTestContext(testContext) {
        val webClient = WebClient.create(vertx)

        vertx.deployVerticle(
            SampleVerticle(),
            testContext.succeeding {
                webClient.get(11981, "localhost", "/yo")
                    .`as`(BodyCodec.string())
                    .send()
                    .onSuccess { resp ->
                        testContext.verify {
                            resp.statusCode() shouldBeEqualTo 200
                            resp.body() shouldContain "Yo!"
                            testContext.completeNow()
                        }
                    }
            }
        )
    }

    @Test
    fun `request to server by coroutines`(testContext: VertxTestContext) = runSuspendTest {
        vertx.withSuspendTestContext(testContext) {
            val webClient = WebClient.create(vertx)

            vertx.deployVerticle(SampleVerticle()).coAwait()
            val response = webClient.get(11981, "localhost", "/yo")
                .`as`(BodyCodec.string())
                .send()
                .coAwait()

            testContext.verify {
                response.statusCode() shouldBeEqualTo 200
                response.body() shouldContain "Yo!"
            }
        }
    }
}
