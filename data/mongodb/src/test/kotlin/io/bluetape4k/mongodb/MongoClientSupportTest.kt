package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.mongodb.bson.documentOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

class MongoClientSupportTest: AbstractMongoTest() {

    companion object: KLoggingChannel()

    private val mockClient = mockk<MongoClient>()
    private val mockSession = mockk<ClientSession>()

    @BeforeEach
    fun clearMock() {
        clearMocks(mockClient, mockSession)
        MongoClientProvider.closeAll()
        coEvery { mockClient.startSession() } returns mockSession
        justRun { mockSession.startTransaction() }
        justRun { mockSession.close() }
    }

    @AfterEach
    fun closeProviderClients() {
        MongoClientProvider.closeAll()
    }

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

        client1 shouldBeSameInstanceAs client2
    }

    @Test
    fun `MongoClientProvider getOrCreate 다른 URL에 다른 인스턴스 반환`() {
        val client1 = MongoClientProvider.getOrCreate(mongoServer.url)
        val client2 = MongoClientProvider.getOrCreate("${mongoServer.url}/other")

        client1 shouldNotBeSameInstanceAs client2
    }

    @Test
    fun `MongoClientProvider getOrCreate MongoClientSettings로 클라이언트 생성`() {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(mongoServer.url))
            .build()
        val client1 = MongoClientProvider.getOrCreate(settings)
        val client2 = MongoClientProvider.getOrCreate(settings)

        client1 shouldBeSameInstanceAs client2
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
    fun `inTransaction cancellation abort cleanup runs from NonCancellable context`() = runTest(timeout = 30.seconds) {
        val abortCompleted = AtomicBoolean(false)
        val cancellation = CancellationException("cancel transaction")

        coEvery { mockSession.abortTransaction() } coAnswers {
            yield()
            abortCompleted.set(true)
        }

        val deferred = async {
            mockClient.inTransaction {
                currentCoroutineContext().cancel(cancellation)
                throw cancellation
            }
        }

        val thrown = assertFailsWith<CancellationException> {
            deferred.await()
        }

        thrown.message shouldBeEqualTo cancellation.message
        abortCompleted.get().shouldBeTrue()
        cancellation.suppressed.asList().shouldBeEmpty()
        coVerify(exactly = 1) { mockSession.abortTransaction() }
        verify(exactly = 1) { mockSession.close() }
    }

    @Test
    fun `inTransaction preserves abort failure as suppressed on cancellation`() = runTest(timeout = 30.seconds) {
        val cancellation = CancellationException("cancel transaction")
        val abortFailure = RuntimeException("abort failed")

        coEvery { mockSession.abortTransaction() } throws abortFailure

        val deferred = async {
            mockClient.inTransaction {
                currentCoroutineContext().cancel(cancellation)
                throw cancellation
            }
        }

        val thrown = assertFailsWith<CancellationException> {
            deferred.await()
        }

        val suppressed = cancellation.suppressed.asList()
        thrown.message shouldBeEqualTo cancellation.message
        suppressed.size shouldBeEqualTo 1
        suppressed.single().message shouldBeEqualTo abortFailure.message
        coVerify(exactly = 1) { mockSession.abortTransaction() }
        verify(exactly = 1) { mockSession.close() }
    }

    @Test
    fun `MongoClientProvider getOrCreate 빌더 적용 동일 설정에 동일 인스턴스 반환`() {
        val url = mongoServer.url
        val c1 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-cache")
        }
        val c2 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-cache")
        }

        c1 shouldBeSameInstanceAs c2
    }

    @Test
    fun `MongoClientProvider getOrCreate 빌더 적용 동일 URL 다른 설정에 다른 인스턴스 반환`() {
        val url = mongoServer.url
        val c1 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-cache-first")
        }
        val c2 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-cache-second")
        }

        c1 shouldNotBeSameInstanceAs c2
    }

    @Test
    fun `MongoClientProvider close removes provider managed cached client`() {
        val url = mongoServer.url
        val client1 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-close")
        }

        MongoClientProvider.close(url) {
            applicationName("bt4k-provider-close")
        }.shouldBeTrue()
        MongoClientProvider.close(url) {
            applicationName("bt4k-provider-close")
        }.shouldBeFalse()

        val client2 = MongoClientProvider.getOrCreate(url) {
            applicationName("bt4k-provider-close")
        }

        client1 shouldNotBeSameInstanceAs client2
    }

    @Test
    fun `listDatabaseNamesAsList 생성한 DB 포함 여부 확인`() = runSuspendIO(timeout = 60.seconds) {
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
