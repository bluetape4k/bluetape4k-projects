package io.bluetape4k.aws.sns

import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SnsClientFactoryTest: AbstractSnsTest() {

    @Test
    fun `SnsClientFactory Sync create는 topic을 생성할 수 있다`() {
        val sync = SnsClientFactory.Sync.create(endpointOverride, region, credentialsProvider)
        val topicName = "factory-sync-${Base58.randomString(8).lowercase()}"

        val response = sync.createTopic(topicName)

        response.topicArn().shouldNotBeEmpty()
    }

    @Test
    fun `SnsClientFactory Async create는 topic을 생성할 수 있다`() = runSuspendIO {
        val async = SnsClientFactory.Async.create(endpointOverride, region, credentialsProvider)
        val topicName = "factory-async-${Base58.randomString(8).lowercase()}"

        val response = async.createTopic(topicName)

        response.topicArn().shouldNotBeEmpty()
    }
}
