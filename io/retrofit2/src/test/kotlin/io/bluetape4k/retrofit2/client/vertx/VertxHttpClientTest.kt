package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.CallFactoryConformanceTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call
import java.time.Duration

class VertxHttpClientTest: CallFactoryConformanceTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = vertxCallFactoryOf()

    override fun callFactory(callTimeout: Duration): Call.Factory =
        vertxCallFactoryOf(callTimeout = callTimeout)
}
