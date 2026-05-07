package io.bluetape4k.http.okhttp3

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class OkHttpRetryConfigRegressionTest {

    @Test
    fun `okhttp3Client propagates retryOnConnectionFailure true`() {
        val client = okhttp3Client {
            retryOnConnectionFailure(true)
        }

        client.retryOnConnectionFailure.shouldBeTrue()
    }

    @Test
    fun `okhttp3Client propagates retryOnConnectionFailure false`() {
        val client = okhttp3Client {
            retryOnConnectionFailure(false)
        }

        client.retryOnConnectionFailure.shouldBeFalse()
    }
}
