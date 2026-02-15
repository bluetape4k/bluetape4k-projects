package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.entity.mime.multipartEntity
import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.FileBody
import org.apache.hc.client5.http.entity.mime.StringBody
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.message.StatusLine
import org.junit.jupiter.api.Test
import java.io.File

/** `multipart/form-data` 인코딩 POST 요청 예제입니다. */
@TempFolderTest
class ClientMultipartFormPost: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `use multipart form encoded post request`(tempFolder: TempFolder) = runTest {
        val httpclient = HttpClients.createDefault()

        httpclient.use {

            // 멀티파트 폼 데이터를 전송합니다.
            val bin = FileBody(File("src/test/resources/files/cafe.jpg"))
            val comment = StringBody("A binary file of some kind", ContentTypes.TEXT_PLAIN_UTF8)
            val reqEntity = multipartEntity {
                addPart("bin", bin)
                addPart("comment", comment)
            }
            val httppost = HttpPost("$httpbinBaseUrl/post").apply {
                entity = reqEntity
            }

            log.debug { "Execute request ${httppost.method} ${httppost.uri}" }

            val response = httpclient.execute(httppost) { it }
            log.debug { "-------------------" }
            log.debug { "$httppost  -> ${StatusLine(response)}" }

            response.entity?.let { entity ->
                log.debug { "Response content length: ${entity.contentLength}" }
            }
            response.code shouldBeEqualTo 200
        }
    }
}
