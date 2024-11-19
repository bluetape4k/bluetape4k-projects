package io.bluetape4k.retrofit2.client.ahc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractClientTest
import io.bluetape4k.retrofit2.clients.ahc.asyncHttpClientCallFactoryOf
import okhttp3.Call

class AhcClientTest: AbstractClientTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = asyncHttpClientCallFactoryOf()

}
