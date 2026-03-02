package io.bluetape4k.mongodb

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.mongodb.bson.documentOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class MongoCollectionExtensionsTest : AbstractMongoTest() {

    companion object : KLoggingChannel() {
        const val COLLECTION_NAME = "ext_test_collection"
    }

    private val collection by lazy {
        database.getCollectionOf<Document>(COLLECTION_NAME)
    }

    @BeforeEach
    fun setUp() = runTest {
        collection.drop()
        // 테스트 데이터 삽입
        collection.insertMany(
            listOf(
                documentOf("name" to "Alice", "age" to 25, "city" to "Seoul"),
                documentOf("name" to "Bob", "age" to 30, "city" to "Busan"),
                documentOf("name" to "Charlie", "age" to 35, "city" to "Seoul"),
                documentOf("name" to "Dave", "age" to 28, "city" to "Incheon"),
                documentOf("name" to "Eve", "age" to 22, "city" to "Seoul"),
            )
        )
    }

    @AfterEach
    fun tearDown() = runTest {
        collection.drop()
    }

    @Test
    fun `findFirst 필터 조건에 맞는 첫 문서 반환`() = runTest(timeout = 30.seconds) {
        val doc = collection.findFirst(Filters.eq("name", "Alice"))
        doc.shouldNotBeNull()
        doc.getString("name") shouldBeEqualTo "Alice"
    }

    @Test
    fun `findFirst 조건에 맞는 문서가 없으면 null 반환`() = runTest(timeout = 30.seconds) {
        val doc = collection.findFirst(Filters.eq("name", "Unknown"))
        doc.shouldBeNull()
    }

    @Test
    fun `exists 존재하는 문서에 true 반환`() = runTest(timeout = 30.seconds) {
        val result = collection.exists(Filters.eq("name", "Bob"))
        result.shouldBeTrue()
    }

    @Test
    fun `exists 존재하지 않는 문서에 false 반환`() = runTest(timeout = 30.seconds) {
        val result = collection.exists(Filters.eq("name", "Unknown"))
        result.shouldBeFalse()
    }

    @Test
    fun `upsert 존재하지 않는 문서 삽입`() = runTest(timeout = 30.seconds) {
        val result = collection.upsert(
            filter = Filters.eq("name", "Frank"),
            update = Updates.combine(
                Updates.set("name", "Frank"),
                Updates.set("age", 40),
                Updates.set("city", "Daejeon")
            )
        )
        result.upsertedId.shouldNotBeNull()

        val doc = collection.findFirst(Filters.eq("name", "Frank"))
        doc.shouldNotBeNull()
    }

    @Test
    fun `upsert 존재하는 문서 업데이트`() = runTest(timeout = 30.seconds) {
        val result = collection.upsert(
            filter = Filters.eq("name", "Alice"),
            update = Updates.set("age", 26)
        )
        result.matchedCount shouldBeEqualTo 1L

        val doc = collection.findFirst(Filters.eq("name", "Alice"))
        doc.shouldNotBeNull()
        doc.getInteger("age") shouldBeEqualTo 26
    }

    @Test
    fun `findAsFlow 전체 문서 반환`() = runTest(timeout = 30.seconds) {
        val docs = collection.findAsFlow().toList()
        docs.size shouldBeEqualTo 5
    }

    @Test
    fun `findAsFlow 필터 적용`() = runTest(timeout = 30.seconds) {
        val docs = collection.findAsFlow(
            filter = Filters.eq("city", "Seoul")
        ).toList()
        docs.size shouldBeEqualTo 3
    }

    @Test
    fun `findAsFlow skip과 limit 적용`() = runTest(timeout = 30.seconds) {
        val docs = collection.findAsFlow(
            skip = 2,
            limit = 2
        ).toList()
        docs.size shouldBeEqualTo 2
    }

    @Test
    fun `findAsFlow sort 적용`() = runTest(timeout = 30.seconds) {
        val docs = collection.findAsFlow(
            sort = Sorts.ascending("age")
        ).toList()
        docs.size shouldBeEqualTo 5
        docs.first().getInteger("age") shouldBeEqualTo 22
        docs.last().getInteger("age") shouldBeEqualTo 35
    }
}
