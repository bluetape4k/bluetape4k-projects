package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.entity.consume
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.InputStreamEntity
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test

/**
 * Example how to use unbuffered chunk-encoded POST request.
 */
class ClientChunkEncodedPost: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `post unbuffered chunk-encoded input stream`() = runTest {
        val httpclient = HttpClients.createDefault()

        httpclient.use {
            val inputStream = Resourcex.getInputStream("files/cafe.jpg")
            val inputStreamEntity = InputStreamEntity(inputStream, -1, ContentType.APPLICATION_OCTET_STREAM)

            val httppost = HttpPost("$httpbinBaseUrl/post").apply {
                entity = inputStreamEntity
            }

            // It may be more appropriate to use FileEntity class in this particular
            // instance but we are using a more generic InputStreamEntity to demonstrate
            // the capability to stream out data from any arbitrary source
            //
            // FileEntity entity = new FileEntity(file, "binary/octet-stream");
            log.debug { "Execute request ${httppost.method} ${httppost.uri}" }

            httpclient.execute(httppost) { response ->
                log.debug { "-------------------" }
                log.debug { "$httppost  -> ${StatusLine(response)}" }
                response.entity?.consume()
                response.code shouldBeEqualTo 200
            }
        }
    }
}
