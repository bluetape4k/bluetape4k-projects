package io.bluetape4k.mongodb.aggregation

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.bson.BsonDocument
import org.junit.jupiter.api.Test

/**
 * Aggregation Pipeline DSL 단위 테스트입니다.
 *
 * 실제 MongoDB 연결 없이 파이프라인 스테이지 목록의 구조만 검증합니다.
 */
class AggregationSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `pipeline 빌더가 빈 리스트를 반환`() {
        val stages = pipeline { }
        stages shouldHaveSize 0
    }

    @Test
    fun `pipeline 빌더가 추가한 스테이지 수만큼 반환`() {
        val stages = pipeline {
            add(matchStage(Filters.gt("age", 20)))
            add(sortStage(Sorts.ascending("name")))
            add(limitStage(5))
        }
        log.debug { "stages=${stages.joinToString()}" }
        stages shouldHaveSize 3
    }

    @Test
    fun `matchStage Bson 생성 검증`() {
        val stage = matchStage(Filters.eq("city", "Seoul"))
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.containsKey("\$match").shouldBeTrue()
    }

    @Test
    fun `groupStage id 필드에 달러 접두어 자동 추가`() {
        val stage = groupStage("city", Accumulators.sum("count", 1))
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }

        val groupDoc = doc.getDocument("\$group")
        log.debug { "groupDoc=$groupDoc" }
        // _id 값이 "$city" 형태여야 합니다
        groupDoc.getString("_id").value shouldBeEqualTo "\$city"
    }

    @Test
    fun `sortStage Bson 생성 검증`() {
        val stage = sortStage(Sorts.descending("score"))
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.containsKey("\$sort").shouldBeTrue()
    }

    @Test
    fun `limitStage Bson 생성 검증`() {
        val stage = limitStage(10)
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.getInt32("\$limit").value shouldBeEqualTo 10
    }

    @Test
    fun `skipStage Bson 생성 검증`() {
        val stage = skipStage(5)
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.getInt32("\$skip").value shouldBeEqualTo 5
    }

    @Test
    fun `projectStage Bson 생성 검증`() {
        val stage = projectStage(Projections.include("name", "score"))
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.containsKey("\$project").shouldBeTrue()
    }

    @Test
    fun `unwindStage 필드명에 달러 접두어 자동 추가`() {
        val stage = unwindStage("tags")
        val doc = stage.toBsonDocument(
            BsonDocument::class.java,
            MongoClientSettings.getDefaultCodecRegistry()
        )
        log.debug { "doc=$doc" }
        doc.getString("\$unwind").value shouldBeEqualTo "\$tags"
    }

    @Test
    fun `pipeline 복합 스테이지 구성 검증`() {
        val stages = pipeline {
            add(matchStage(Filters.gte("score", 80)))
            add(groupStage("city", Accumulators.sum("count", 1)))
            add(sortStage(Sorts.descending("count")))
            add(skipStage(0))
            add(limitStage(3))
            add(projectStage(Projections.include("city", "count")))
        }
        stages shouldHaveSize 6
        stages.forEach { stage ->
            log.debug { "stage=$stage" }
        }
    }
}
