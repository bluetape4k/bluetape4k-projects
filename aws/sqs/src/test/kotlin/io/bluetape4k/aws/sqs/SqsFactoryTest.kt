package io.bluetape4k.aws.sqs

import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SqsFactoryTest: AbstractSqsTest() {

    @Test
    fun `SqsFactory Sync create는 queue를 생성하고 삭제할 수 있다`() {
        val sync = SqsFactory.Sync.create(endpoint, region, credentialsProvider)
        val queueName = "factory-sync-${Base58.randomString(8).lowercase()}"

        val queueUrl = sync.createQueue(queueName)
        queueUrl.shouldNotBeEmpty()

        val deleteResponse = sync.deleteQueue(queueUrl)
        deleteResponse.responseMetadata().requestId().shouldNotBeEmpty()
    }

    @Test
    fun `SqsFactory Async create는 queue를 생성하고 삭제할 수 있다`() = runSuspendIO {
        val async = SqsFactory.Async.create(endpoint, region, credentialsProvider)
        val queueName = "factory-async-${Base58.randomString(8).lowercase()}"

        val queueUrl = async.createQueue(queueName)
        queueUrl.shouldNotBeEmpty()

        val deleteResponse = async.deleteQueue(queueUrl)
        deleteResponse.responseMetadata().requestId().shouldNotBeEmpty()
    }
}
