package io.bluetape4k.mongodb

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.MongoDBServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

/**
 * MongoDB 통합 테스트의 기반 클래스입니다.
 *
 * [MongoDBServer] Testcontainer를 통해 Docker 기반 MongoDB를 자동으로 실행하며,
 * Kotlin Coroutine 드라이버 기반의 [MongoClient]와 [MongoDatabase]를 제공합니다.
 *
 * **주의**: [MongoDBServer.Launcher.getClient]는 `com.mongodb.client.MongoClient` (sync) 를
 * 반환하므로, 코루틴 드라이버를 사용하려면 [MongoDBServer.Launcher.getCoroutineClient]를 사용해야 합니다.
 *
 * 테스트 클래스 종료 시 [client]는 자동으로 닫힙니다. ([AfterAll] 훅)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMongoTest {

    companion object: KLoggingChannel() {
        /** 테스트에 사용할 데이터베이스 이름입니다. */
        const val DEFAULT_DATABASE_NAME = "test"

        /** 지연 초기화되는 MongoDB 테스트 서버입니다. */
        val mongoServer: MongoDBServer by lazy { MongoDBServer.Launcher.mongoDB }
    }

    private val _clientDelegate = lazy { MongoClient.create(mongoServer.url) }

    /** Kotlin Coroutine 드라이버 기반 [MongoClient]입니다. */
    val client: MongoClient by _clientDelegate

    /** 기본 테스트 데이터베이스입니다. */
    val database: MongoDatabase by lazy {
        client.getDatabase(DEFAULT_DATABASE_NAME)
    }

    /**
     * 테스트 클래스 종료 시 [client]를 닫아 리소스를 해제합니다.
     *
     * `lazy` 초기화가 실제로 수행된 경우에만 닫히도록 [Lazy.isInitialized]를 확인합니다.
     */
    @AfterAll
    fun closeClient() {
        if (_clientDelegate.isInitialized()) {
            client.close()
        }
    }
}
