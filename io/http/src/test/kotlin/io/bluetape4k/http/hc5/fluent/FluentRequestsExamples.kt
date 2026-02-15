package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeGreaterThan
import org.apache.hc.client5.http.fluent.Form
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpVersion
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

/** HttpClient Fluent API의 기본 요청 실행 예제입니다. */
@TempFolderTest
class FluentRequestsExamples: AbstractHc5Test() {

    companion object: KLogging()

    // 타임아웃 설정이 있는 GET 요청을 실행하고 문자열 응답을 반환합니다.
    @Test
    fun `get with timeout settings and return content as string`() {
        val content = requestGet("$httpbinBaseUrl/get")
            .connectTimeout(Timeout.ofSeconds(1))
            .responseTimeout(Timeout.ofSeconds(5))
            .execute()
            .returnContent()
            .asString()

        log.debug { "content=$content" }
    }

    // HTTP/1.1 + expect-continue 핸드셰이크로 POST 요청을 실행하고 바이트 배열 응답을 반환합니다.
    @Test
    fun `post with expect-continue hadshake using HTTP 1_1`() {
        val content = requestPost("$httpbinBaseUrl/post")
            .useExpectContinue()
            .version(HttpVersion.HTTP_1_1)
            .bodyString("Important stuff", ContentType.DEFAULT_TEXT)
            .execute()
            .returnContent()
            .asBytes()

        log.debug { "content=${content.toUtf8String()}" }
    }

    // 커스텀 헤더와 HTML 폼 본문을 포함한 POST 요청 결과를 파일에 저장합니다.
    @Test
    fun `post with a custom header and form data and save response to file`(tempFolder: TempFolder) {
        val file = tempFolder.createFile()
        requestPost("$httpbinBaseUrl/post")
            .addHeader("X-Custom-Header", "stuff")
            .bodyForm(
                Form.form()
                    .add("username", "vip")
                    .add("password", "secret")
                    .build()
            )
            .execute()
            .saveContent(file)

        file.length() shouldBeGreaterThan 0
        log.debug { "body=${file.readText()}" }
    }
}
