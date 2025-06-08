package io.bluetape4k.feign.clients.hc5

import feign.Logger
import feign.hc5.AsyncApacheHttp5Client
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ApacheHc5JsonPlaceHolderCoroutineJacksonTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLoggingChannel()

    override fun newBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncApacheHttp5Client())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }

    @Disabled("Apache Hc5 Client 에서 예외가 발생한다")
    @Test
    override fun `get post's comments`() {
        // 예외가 발생한다.
    }
}
