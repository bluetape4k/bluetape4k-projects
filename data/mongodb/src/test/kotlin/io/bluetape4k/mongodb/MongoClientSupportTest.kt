package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
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
}
