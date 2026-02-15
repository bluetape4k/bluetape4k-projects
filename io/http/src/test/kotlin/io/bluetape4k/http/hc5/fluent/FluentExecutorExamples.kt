package io.bluetape4k.http.hc5.fluent

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.http.httpHostOf
import io.bluetape4k.junit5.tempfolder.TempFolder
import io.bluetape4k.junit5.tempfolder.TempFolderTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeGreaterThan
import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Form
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpVersion
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

/**
 * HttpClient Fluent API에서 동일한 보안 컨텍스트를 공유하며
 * 여러 요청을 실행하는 예제입니다.
 */
@TempFolderTest
class FluentExecutorExamples: AbstractHc5Test() {

    companion object: KLogging()

    private val executor: Executor = Executor.newInstance()
        .auth(httpHostOf(httpbinBaseUrl), "user", "passwd".toCharArray())
        .authPreemptive(httpHostOf(httpbinBaseUrl))

    @Test
    fun `get with timeout settings`() {
        // 타임아웃 설정이 있는 GET 요청을 실행하고 문자열 응답을 받습니다.
        val content = executor
            .execute(
                requestGet("$httpbinBaseUrl/basic-auth/user/passwd")
                    .connectTimeout(Timeout.ofSeconds(1))
            )
            .returnContent()
            .asString(Charsets.UTF_8)

        log.debug { "content=$content" }
    }

    @Test
    fun `post with HTTP 1_1`() {
        // HTTP/1.1 + expect-continue 핸드셰이크로 POST 요청을 보내고 바이트 배열 응답을 받습니다.
        val contentBytes = executor
            .execute(
                requestPost("$httpbinBaseUrl/post")
                    .useExpectContinue()
                    .version(HttpVersion.HTTP_1_1)
                    .bodyString("Important stuff", ContentType.DEFAULT_TEXT)
            )
            .returnContent()
            .asBytes()

        log.debug { "contentBytes=${contentBytes.toUtf8String()}" }
    }

    @Test
    fun `post multi-part form data`(tempFolder: TempFolder) {
        // 커스텀 헤더와 HTML 폼 본문을 포함한 POST 요청 결과를 파일에 저장합니다.
        // @see hc5/examples/ClientMultipartFormPost
        val file = tempFolder.createFile()

        executor
            .execute(
                requestPost("$httpbinBaseUrl/post")
                    .addHeader("X-Custom-Header", "stuff")
                    .bodyForm(
                        Form.form()
                            .add("username", "user")
                            .add("password", "secret")
                            .build()
                    )
            )
            .saveContent(file)

        file.length() shouldBeGreaterThan 0
        log.debug { "body=${file.readText()}" }
    }
}
