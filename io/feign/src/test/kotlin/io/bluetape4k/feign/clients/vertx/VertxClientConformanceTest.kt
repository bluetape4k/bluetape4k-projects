package io.bluetape4k.feign.clients.vertx

import feign.AsyncClient
import feign.Client
import io.bluetape4k.feign.clients.FeignAsyncClientConformanceTest
import io.bluetape4k.feign.clients.FeignSyncClientConformanceTest
import java.util.Optional

class VertxClientConformanceTest: FeignSyncClientConformanceTest() {

    override fun newClient(): Client =
        VertxHttpClient()
}

class VertxAsyncClientConformanceTest: FeignAsyncClientConformanceTest<Any>() {

    override fun newAsyncClient(): AsyncClient<Any> =
        AsyncVertxHttpClient()

    override fun requestContext(): Optional<Any> =
        Optional.empty()
}
