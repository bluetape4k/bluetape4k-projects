package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.amshove.kluent.internal.assertFails
import org.amshove.kluent.shouldBeTrue
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.message.StatusLine
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class ClientAbortMethod: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `abort http method before its normal completion`() = runSuspendTest {
        val executed = AtomicBoolean(false)

        HttpClients.createDefault().use { httpclient ->
            val httpget = HttpGet("$httpbinBaseUrl/get")

            val task = future {
                log.debug { "Execute request ${httpget.method} ${httpget.uri}" }
                executed.set(true)

                // abort 되면 RequestFailedException 예외가 발생합니다.
                assertFails {
                    httpclient.execute(httpget) { response ->
                        log.debug { "-------------------" }
                        log.debug { "$httpget  -> ${StatusLine(response)}" }
                        response.entity?.consume()
                    }
                }
            }
            await.until { !executed.get() }

            httpget.cancel().shouldBeTrue()
            task.await()
        }
    }
}
