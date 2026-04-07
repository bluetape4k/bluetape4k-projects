package io.bluetape4k.mongodb

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.bson.Document
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class MongoDatabaseExtensionsTest: AbstractMongoTest() {
    companion object: KLoggingChannel()

    @Test
    fun `getCollectionOf reified 타입으로 컬렉션 획득`() =
        runTest(timeout = 30.seconds) {
            val collection = database.getCollectionOf<Document>("test_collection")
            collection.shouldNotBeNull()
        }

    @Test
    fun `listCollectionNamesList 컬렉션 이름 목록 반환`() =
        runTest(timeout = 30.seconds) {
            // 테스트용 컬렉션 생성
            database.createCollection("names_test_col")

            val names = database.listCollectionNamesList()
            names.shouldNotBeNull()
            // 방금 생성한 컬렉션이 포함되어 있어야 함
            names.any { it == "names_test_col" }.shouldBeTrue()

            // 정리
            database.getCollection("names_test_col", org.bson.Document::class.java).drop()
        }
}
