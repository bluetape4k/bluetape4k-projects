package io.bluetape4k.aws.kotlin.sqs

import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class SqsClientSupportTest {

    companion object : KLogging()

    @Test
    fun `sqsClientOf는 null endpoint로 클라이언트를 생성한다`() {
        val client = sqsClientOf(
            endpointUrl = null,
            region = "us-east-1"
        )
        client.close()
    }

    @Test
    fun `sqsClientOf는 region으로 클라이언트를 생성한다`() {
        val client = sqsClientOf(region = "ap-northeast-2")
        client.close()
    }

    @Test
    fun `sqsClientOf는 endpoint와 region으로 클라이언트를 생성한다`() {
        val client = sqsClientOf(
            endpointUrl = Url.parse("http://localhost:4566"),
            region = "us-east-1"
        )
        client.close()
    }

    @Test
    fun `sqsClientOf는 builder 블록으로 추가 설정이 가능하다`() {
        val client = sqsClientOf(region = "us-east-1") {
            // additional config
        }
        client.close()
    }
}
