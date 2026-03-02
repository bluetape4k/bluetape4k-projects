package io.bluetape4k.mongodb.examples

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.mongodb.AbstractMongoTest
import io.bluetape4k.mongodb.bson.documentOf
import io.bluetape4k.mongodb.exists
import io.bluetape4k.mongodb.findAsFlow
import io.bluetape4k.mongodb.findFirst
import io.bluetape4k.mongodb.getCollectionOf
import io.bluetape4k.mongodb.upsert
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * MongoDB Kotlin Coroutine Driver + bluetape4k-mongodb 확장함수를 활용한
 * 기본 CRUD 통합 예제입니다.
 */
class BasicCrudExamples : AbstractMongoTest() {

    companion object : KLoggingChannel() {
        const val COLLECTION_NAME = "crud_examples"
    }

    private val collection by lazy {
        database.getCollectionOf<Document>(COLLECTION_NAME)
    }

    @BeforeEach
    fun setUp() = runTest {
        collection.drop()
    }

    @AfterEach
    fun tearDown() = runTest {
        collection.drop()
    }

    @Test
    fun `기본 CRUD 예제`() = runTest(timeout = 60.seconds) {
        // Create (Insert)
        val alice = documentOf("name" to "Alice", "age" to 25, "city" to "Seoul")
        val bob = documentOf("name" to "Bob", "age" to 30, "city" to "Busan")
        val charlie = documentOf("name" to "Charlie", "age" to 20, "city" to "Seoul")

        collection.insertOne(alice)
        collection.insertMany(listOf(bob, charlie))

        // 총 3개 삽입 확인
        collection.countDocuments() shouldBeEqualTo 3L

        // Read (findFirst 확장함수)
        val found = collection.findFirst(Filters.eq("name", "Alice"))
        found.shouldNotBeNull()
        found.getString("city") shouldBeEqualTo "Seoul"

        // exists 확장함수
        collection.exists(Filters.eq("name", "Bob")).shouldBeTrue()
        collection.exists(Filters.eq("name", "Unknown")).shouldBeFalse()

        // Update
        collection.updateOne(
            Filters.eq("name", "Alice"),
            Updates.set("age", 26)
        )
        val updated = collection.findFirst(Filters.eq("name", "Alice"))
        updated.shouldNotBeNull()
        updated.getInteger("age") shouldBeEqualTo 26

        // Upsert (존재하지 않는 문서 삽입)
        collection.upsert(
            filter = Filters.eq("name", "Dave"),
            update = Updates.combine(
                Updates.set("name", "Dave"),
                Updates.set("age", 35),
                Updates.set("city", "Daegu")
            )
        )
        collection.countDocuments() shouldBeEqualTo 4L

        // Delete
        collection.deleteOne(Filters.eq("name", "Charlie"))
        collection.countDocuments() shouldBeEqualTo 3L

        // findAsFlow 확장함수로 서울 거주자 조회 + 이름 오름차순 정렬
        // Alice(Seoul), Bob(Busan), Dave(Daegu) 남음 → 서울: Alice만 1명
        val seoulResidents = collection.findAsFlow(
            filter = Filters.eq("city", "Seoul"),
            sort = Sorts.ascending("name")
        ).toList()

        seoulResidents.size shouldBeEqualTo 1
        seoulResidents[0].getString("name") shouldBeEqualTo "Alice"
    }

    @Test
    fun `네이티브 CRUD와 확장함수 조합 예제`() = runTest(timeout = 60.seconds) {
        // 네이티브 insertMany (이미 suspend)
        val docs = (1..10).map { i ->
            documentOf("index" to i, "value" to "item_$i", "even" to (i % 2 == 0))
        }
        collection.insertMany(docs)

        // findAsFlow + skip/limit/sort 조합
        val paged = collection.findAsFlow(
            filter = Filters.eq("even", true),
            sort = Sorts.ascending("index"),
            skip = 1,
            limit = 3
        ).toList()

        // 짝수: 2, 4, 6, 8, 10 -> skip 1 -> 4, 6, 8 -> limit 3 -> [4, 6, 8]
        paged.size shouldBeEqualTo 3
        paged[0].getInteger("index") shouldBeEqualTo 4
        paged[1].getInteger("index") shouldBeEqualTo 6
        paged[2].getInteger("index") shouldBeEqualTo 8
    }
}
