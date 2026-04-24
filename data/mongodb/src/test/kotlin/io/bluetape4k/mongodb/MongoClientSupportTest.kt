package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.mongodb.bson.documentOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class MongoClientSupportTest: AbstractMongoTest() {

    companion object: KLoggingChannel()

    @Test
    fun `mongoClient DSL 빌더로 MongoClient 생성`() = runTest(timeout = 30.seconds) {
        mongoClient {
            applyConnectionString(ConnectionString(mongoServer.url))
        }.use { client ->
            val dbNames = client.listDatabaseNames().toList()
            dbNames.shouldNotBeNull()
        }
    }

    @Test
    fun `mongoClientOf 편의 함수로 MongoClient 생성`() = runTest(timeout = 30.seconds) {
        mongoClientOf(mongoServer.url).use { client ->
            val dbNames = client.listDatabaseNames().toList()
            dbNames.shouldNotBeNull()
        }
    }

    @Test
    fun `MongoClientProvider getOrCreate 동일 URL에 동일 인스턴스 반환`() {
        val client1 = MongoClientProvider.getOrCreate(mongoServer.url)
        val client2 = MongoClientProvider.getOrCreate(mongoServer.url)
        assertSame(client1, client2)
    }

    @Test
    fun `MongoClientProvider getOrCreate 다른 URL에 다른 인스턴스 반환`() {
        val client1 = MongoClientProvider.getOrCreate(mongoServer.url)
        val client2 = MongoClientProvider.getOrCreate(
            mongoServer.url.replace("test", "other")
        )
        // 다른 URL이므로 다른 인스턴스
        assertNotSame(client1, client2)
    }

    @Test
    fun `MongoClientProvider getOrCreate MongoClientSettings로 클라이언트 생성`() {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(mongoServer.url))
            .build()
        val client1 = MongoClientProvider.getOrCreate(settings)
        val client2 = MongoClientProvider.getOrCreate(settings)
        // 동일 설정이면 동일 인스턴스
        assertSame(client1, client2)
    }

    @Test
    fun `listDatabaseNamesAsList 데이터베이스 이름 목록 반환`() = runTest(timeout = 30.seconds) {
        val names = client.listDatabaseNamesAsList()
        names.shouldNotBeNull()
    }

    @Test
    fun `withClientSession 세션 블록 실행 후 세션 자동 종료`() = runTest(timeout = 30.seconds) {
        val collectionName = "session_test"
        val collection = database.getCollectionOf<org.bson.Document>(collectionName)
        try {
            val insertedName = client.withClientSession { session ->
                collection.insertOne(session, documentOf("name" to "session_user"))
                "session_user"
            }
            insertedName shouldBeEqualTo "session_user"

            val count = collection.countDocuments()
            count shouldBeEqualTo 1L
        } finally {
            collection.drop()
        }
    }

    @Test
    fun `withClientSession 블록 예외 발생 시 세션 자동 종료 후 예외 재전파`() = runTest(timeout = 30.seconds) {
        var exceptionCaught = false
        try {
            client.withClientSession {
                throw RuntimeException("테스트 예외")
            }
        } catch (e: RuntimeException) {
            exceptionCaught = true
        }
        // 예외가 재전파되어야 합니다
        exceptionCaught shouldBeEqualTo true
    }

    @Test
    fun `MongoClientProvider getOrCreate 빌더 적용 동일 URL에 동일 인스턴스 반환`() {
        val url = mongoServer.url
        val c1 = MongoClientProvider.getOrCreate(url) {
            // 추가 설정 없음
        }
        val c2 = MongoClientProvider.getOrCreate(url) {
            // 동일 URL → 이미 캐시된 인스턴스 반환
        }
        assertSame(c1, c2)
    }

    @Test
    fun `listDatabaseNamesAsList 생성한 DB 포함 여부 확인`() = runTest(timeout = 30.seconds) {
        // 컬렉션을 생성하면 해당 DB도 목록에 나타납니다
        val dbName = "db_list_test"
        val tempClient = mongoClientOf(mongoServer.url)
        try {
            tempClient.getDatabase(dbName).createCollection("tmp_col")
            val names = tempClient.listDatabaseNamesAsList()
            names shouldContain dbName
        } finally {
            tempClient.getDatabase(dbName).drop()
            tempClient.close()
        }
    }
}
