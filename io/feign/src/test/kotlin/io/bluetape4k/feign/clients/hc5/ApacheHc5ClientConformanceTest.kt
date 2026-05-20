package io.bluetape4k.feign.clients.hc5

import feign.AsyncClient
import feign.Client
import feign.hc5.ApacheHttp5Client
import feign.hc5.AsyncApacheHttp5Client
import io.bluetape4k.feign.clients.FeignAsyncClientConformanceTest
import io.bluetape4k.feign.clients.FeignSyncClientConformanceTest
import io.bluetape4k.http.hc5.async.httpAsyncClientOf
import org.apache.hc.client5.http.protocol.HttpClientContext
import java.util.Optional

class ApacheHc5ClientConformanceTest: FeignSyncClientConformanceTest() {

    override fun newClient(): Client =
        ApacheHttp5Client()
}

class ApacheHc5AsyncClientConformanceTest: FeignAsyncClientConformanceTest<HttpClientContext>() {

    override fun newAsyncClient(): AsyncClient<HttpClientContext> =
        AsyncApacheHttp5Client(httpAsyncClientOf())

    override fun requestContext(): Optional<HttpClientContext> =
        Optional.empty()
}
