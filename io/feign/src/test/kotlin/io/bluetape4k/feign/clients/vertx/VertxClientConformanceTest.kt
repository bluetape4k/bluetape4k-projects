package io.bluetape4k.feign.clients.vertx

import feign.AsyncClient
import feign.Client
import io.bluetape4k.feign.clients.FeignAsyncClientConformanceTest
import io.bluetape4k.feign.clients.FeignSyncClientConformanceTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.*

class VertxClientConformanceTest: FeignSyncClientConformanceTest() {

    companion object: KLoggingChannel()

    override fun newClient(): Client =
        VertxHttpClient()
}

class VertxAsyncClientConformanceTest: FeignAsyncClientConformanceTest<Any>() {

    companion object: KLoggingChannel()

    override fun newAsyncClient(): AsyncClient<Any> =
        AsyncVertxHttpClient()

    override fun requestContext(): Optional<Any> =
        Optional.empty()
}
