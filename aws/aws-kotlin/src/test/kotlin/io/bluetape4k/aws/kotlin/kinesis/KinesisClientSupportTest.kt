package io.bluetape4k.aws.kotlin.kinesis

import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class KinesisClientSupportTest {

    companion object : KLogging()

    @Test
    fun `kinesisClientOf는 null endpoint로 클라이언트를 생성한다`() {
        val client = kinesisClientOf(
            endpointUrl = null,
            region = "us-east-1"
        )
        client.close()
    }

    @Test
    fun `kinesisClientOf는 region으로 클라이언트를 생성한다`() {
        val client = kinesisClientOf(region = "ap-northeast-2")
        client.close()
    }

    @Test
    fun `kinesisClientOf는 endpoint와 region으로 클라이언트를 생성한다`() {
        val client = kinesisClientOf(
            endpointUrl = Url.parse("http://localhost:4566"),
            region = "us-east-1"
        )
        client.close()
    }
}
