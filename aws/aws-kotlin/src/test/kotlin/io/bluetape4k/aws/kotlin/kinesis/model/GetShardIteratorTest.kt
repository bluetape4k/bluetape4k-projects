package io.bluetape4k.aws.kotlin.kinesis.model

import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GetShardIteratorTest {

    companion object : KLogging()

    @Test
    fun `getShardIteratorRequestOf는 streamName과 shardId로 요청을 생성한다`() {
        val req = getShardIteratorRequestOf(
            streamName = "my-stream",
            shardId = "shardId-000000000000",
            type = ShardIteratorType.TrimHorizon
        )

        req.streamName shouldBeEqualTo "my-stream"
        req.shardId shouldBeEqualTo "shardId-000000000000"
        req.shardIteratorType shouldBeEqualTo ShardIteratorType.TrimHorizon
    }

    @Test
    fun `getShardIteratorRequestOf는 기본 타입으로 TrimHorizon을 사용한다`() {
        val req = getShardIteratorRequestOf(
            streamName = "my-stream",
            shardId = "shardId-000000000000"
        )

        req.shardIteratorType shouldBeEqualTo ShardIteratorType.TrimHorizon
    }

    @Test
    fun `getShardIteratorRequestOf는 Latest 타입을 설정할 수 있다`() {
        val req = getShardIteratorRequestOf(
            streamName = "my-stream",
            shardId = "shardId-000000000001",
            type = ShardIteratorType.Latest
        )

        req.shardIteratorType shouldBeEqualTo ShardIteratorType.Latest
    }

    @Test
    fun `getShardIteratorRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val req = getShardIteratorRequestOf(
            streamName = "my-stream",
            shardId = "shardId-000000000000"
        ) {
            startingSequenceNumber = "49600047091173428477090417966...1"
        }

        req.shouldNotBeNull()
        req.startingSequenceNumber shouldBeEqualTo "49600047091173428477090417966...1"
    }

    @Test
    fun `getShardIteratorRequestOf는 빈 streamName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            getShardIteratorRequestOf(streamName = "", shardId = "shardId-000000000000")
        }
    }

    @Test
    fun `getShardIteratorRequestOf는 빈 shardId를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            getShardIteratorRequestOf(streamName = "my-stream", shardId = "  ")
        }
    }
}
