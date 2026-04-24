package io.bluetape4k.aws.kotlin.s3

import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class S3ClientSupportTest {

    companion object : KLogging()

    @Test
    fun `s3ClientOf는 null endpoint로 클라이언트를 생성한다`() {
        val client = s3ClientOf(
            endpointUrl = null,
            region = "us-east-1"
        )
        client.close()
    }

    @Test
    fun `s3ClientOf는 region으로 클라이언트를 생성한다`() {
        val client = s3ClientOf(region = "ap-northeast-2")
        client.close()
    }

    @Test
    fun `s3ClientOf는 endpoint와 region으로 클라이언트를 생성한다`() {
        val client = s3ClientOf(
            endpointUrl = Url.parse("http://localhost:4566"),
            region = "us-east-1"
        )
        client.close()
    }

    @Test
    fun `s3ClientOf는 builder 블록으로 추가 설정이 가능하다`() {
        val client = s3ClientOf(region = "us-east-1") {
            // additional config
        }
        client.close()
    }
}
