package io.bluetape4k.retrofit2.client.hc5

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractClientTest
import io.bluetape4k.retrofit2.clients.hc5.hc5CallFactoryOf
import okhttp3.Call

class Hc5HttpClientTest: AbstractClientTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = hc5CallFactoryOf()
}
