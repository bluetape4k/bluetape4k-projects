package io.bluetape4k.mongodb

import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.bson.Document
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class MongoDatabaseExtensionsTest: AbstractMongoTest() {

    companion object: KLoggingChannel()

    @Test
    fun `getCollectionOf reified 타입으로 컬렉션 획득`() = runSuspendIO(timeout = 30.seconds) {
        val collection = database.getCollectionOf<Document>("test_collection")
        collection.shouldNotBeNull()
    }

    @Test
    fun `listCollectionNamesList 컬렉션 이름 목록 반환`() = runSuspendIO(timeout = 30.seconds) {
        val collectionName = "names_test_collection"
        // 테스트용 컬렉션 생성
        database.createCollection(collectionName)

        val names = database.listCollectionNamesList()
        log.debug { "collection names=${names.joinToString()}" }

        // 방금 생성한 컬렉션이 포함되어 있어야 함
        names shouldContain collectionName

        // 정리
        database.getCollection<Document>(collectionName).drop()
    }
}
