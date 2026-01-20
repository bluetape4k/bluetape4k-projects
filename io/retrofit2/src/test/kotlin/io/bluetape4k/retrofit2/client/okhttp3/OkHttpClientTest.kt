package io.bluetape4k.retrofit2.client.okhttp3

import io.bluetape4k.http.okhttp3.okhttp3Client
import io.bluetape4k.logging.KLogging
import io.bluetape4k.retrofit2.client.AbstractClientTest
import okhttp3.Call
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class OkHttpClientTest: AbstractClientTest() {

    companion object: KLogging()

    override val callFactory: Call.Factory = okhttp3Client()

    @Test
    override fun `can support Deflate`() {
        Assumptions.assumeTrue(false, "OkHttp3 는 기본적으로 deflate를 지원하지 않는다.")
    }
}
