package io.bluetape4k.retrofit2.client.hc5

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.CallFactoryConformanceTest
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import okhttp3.Call
import java.time.Duration

class Hc5HttpClientTest: CallFactoryConformanceTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = hc5CallFactoryOf()

    override fun callFactory(callTimeout: Duration): Call.Factory =
        hc5CallFactoryOf(callTimeout = callTimeout)
}
