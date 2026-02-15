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

/** 버퍼링하지 않는 청크 인코딩 POST 요청 예제입니다. */
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

            // 이 예제에서는 FileEntity 대신 InputStreamEntity를 사용해
            // 임의의 입력 소스를 스트리밍 전송할 수 있음을 보여줍니다.
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
