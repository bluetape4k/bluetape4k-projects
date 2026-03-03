package io.bluetape4k.retrofit2.client.vertx

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.retrofit2.client.AbstractDynamicUrlCoroutineTest
import io.bluetape4k.retrofit2.clients.vertx.vertxCallFactoryOf
import okhttp3.Call

class VertxDynamicUrlCoroutineTest: AbstractDynamicUrlCoroutineTest() {

    companion object: KLoggingChannel()

    override val callFactory: Call.Factory = vertxCallFactoryOf()
}
