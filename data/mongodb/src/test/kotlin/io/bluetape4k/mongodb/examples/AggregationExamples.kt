package io.bluetape4k.mongodb.examples

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.mongodb.AbstractMongoTest
import io.bluetape4k.mongodb.aggregation.groupStage
import io.bluetape4k.mongodb.aggregation.limitStage
import io.bluetape4k.mongodb.aggregation.matchStage
import io.bluetape4k.mongodb.aggregation.pipeline
import io.bluetape4k.mongodb.aggregation.projectStage
import io.bluetape4k.mongodb.aggregation.skipStage
import io.bluetape4k.mongodb.aggregation.sortStage
import io.bluetape4k.mongodb.aggregation.unwindStage
import io.bluetape4k.mongodb.bson.documentOf
import io.bluetape4k.mongodb.getCollectionOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * MongoDB Aggregation Pipeline DSL 사용 예제입니다.
 *
 * `pipeline {}` DSL로 파이프라인 스테이지를 구성하고,
 * 네이티브 `aggregate()` 함수로 실행합니다.
 */
class AggregationExamples: AbstractMongoTest() {

    companion object: KLoggingChannel() {
        const val COLLECTION_NAME = "agg_examples"
    }

    private val collection by lazy {
        database.getCollectionOf<Document>(COLLECTION_NAME)
    }

    @BeforeEach
    fun setUp() = runTest {
        collection.drop()
        collection.insertMany(
            listOf(
                documentOf("name" to "Alice", "age" to 25, "city" to "Seoul", "score" to 85),
                documentOf("name" to "Bob", "age" to 30, "city" to "Busan", "score" to 72),
                documentOf("name" to "Charlie", "age" to 35, "city" to "Seoul", "score" to 91),
                documentOf("name" to "Dave", "age" to 28, "city" to "Busan", "score" to 68),
                documentOf("name" to "Eve", "age" to 22, "city" to "Seoul", "score" to 95),
                documentOf(
                    "name" to "Frank", "age" to 40, "city" to "Daegu", "score" to 78,
                    "tags" to listOf("admin", "user")
                ),
                documentOf(
                    "name" to "Grace", "age" to 33, "city" to "Seoul", "score" to 88,
                    "tags" to listOf("user")
                ),
            )
        )
    }

    @AfterEach
    fun tearDown() = runTest {
        collection.drop()
    }

    @Test
    fun `$match + $group으로 도시별 인원 집계`() = runTest(timeout = 60.seconds) {
        val stages = pipeline {
            add(groupStage("city", Accumulators.sum("count", 1)))
            add(sortStage(Sorts.descending("count")))
        }

        val results = collection.aggregate<Document>(stages).toList()
        results.isNotEmpty()

        // 서울이 가장 많아야 함 (Alice, Charlie, Eve, Grace = 4명)
        val seoulGroup = results.find { it.getString("_id") == "Seoul" }
        seoulGroup?.getInteger("count") shouldBeEqualTo 4
    }

    @Test
    fun `$match + $sort + $limit으로 상위 점수자 조회`() = runTest(timeout = 60.seconds) {
        val stages = pipeline {
            add(matchStage(Filters.gte("score", 80)))
            add(sortStage(Sorts.descending("score")))
            add(limitStage(3))
        }

        val results = collection.aggregate<Document>(stages).toList()
        results.size shouldBeEqualTo 3
        // 첫 번째가 가장 높은 점수
        results[0].getInteger("score") shouldBeGreaterThan results[1].getInteger("score")
    }

    @Test
    fun `$skip + $limit으로 페이지네이션`() = runTest(timeout = 60.seconds) {
        val stages = pipeline {
            add(sortStage(Sorts.ascending("name")))
            add(skipStage(2))
            add(limitStage(3))
        }

        val results = collection.aggregate<Document>(stages).toList()
        results.size shouldBeEqualTo 3
    }

    @Test
    fun `$project으로 특정 필드만 반환`() = runTest(timeout = 60.seconds) {
        val stages = pipeline {
            add(matchStage(Filters.eq("city", "Seoul")))
            add(projectStage(Projections.include("name", "score")))
            add(sortStage(Sorts.ascending("name")))
        }

        val results = collection.aggregate<Document>(stages).toList()
        results.size shouldBeEqualTo 4
        // _id 를 제외한 name, score 필드만 존재해야 함
        results.all { it.containsKey("name") && it.containsKey("score") }
        // city, age는 포함되지 않아야 함
        results.none { it.containsKey("age") }
    }

    @Test
    fun `$unwind으로 배열 필드 전개`() = runTest(timeout = 60.seconds) {
        val stages = pipeline {
            add(matchStage(Filters.exists("tags")))
            add(unwindStage("tags"))
            add(groupStage("tags", Accumulators.sum("count", 1)))
            add(sortStage(Sorts.descending("count")))
        }

        val results = collection.aggregate<Document>(stages).toList()
        // Frank: ["admin", "user"], Grace: ["user"] -> user 2개, admin 1개
        results.isNotEmpty()
        val userTag = results.find { it.getString("_id") == "user" }
        userTag?.getInteger("count") shouldBeEqualTo 2
    }
}
