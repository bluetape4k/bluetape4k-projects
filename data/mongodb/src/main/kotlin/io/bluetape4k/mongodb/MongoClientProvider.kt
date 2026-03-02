package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.utils.ShutdownQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * [MongoClient] 인스턴스를 연결 문자열 또는 설정 기반으로 캐싱하고 관리합니다.
 *
 * 동일한 키(연결 문자열 또는 [MongoClientSettings])에 대해서는 동일한 [MongoClient]
 * 인스턴스를 반환하며, JVM 종료 시 등록된 클라이언트가 자동으로 닫힙니다.
 *
 * ```kotlin
 * val client = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
 * ```
 */
object MongoClientProvider : KLogging() {

    /** 기본 MongoDB 연결 문자열입니다. */
    const val DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017"

    /** 기본 데이터베이스 이름입니다. */
    const val DEFAULT_DATABASE_NAME = "test"

    private val clientCache = ConcurrentHashMap<String, MongoClient>()
    private val settingsClientCache = ConcurrentHashMap<MongoClientSettings, MongoClient>()

    /**
     * 연결 문자열에 해당하는 코루틴 [MongoClient]를 반환합니다.
     *
     * 캐시에 해당 연결 문자열의 클라이언트가 없으면 새로 생성하고
     * [ShutdownQueue]에 종료 훅을 등록합니다.
     *
     * ```kotlin
     * val client1 = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
     * val client2 = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
     * // client1 === client2 (동일 인스턴스)
     * ```
     *
     * @param connectionString MongoDB 연결 문자열 (기본값: [DEFAULT_CONNECTION_STRING])
     * @return 캐시된 또는 새로 생성된 코루틴 [MongoClient] 인스턴스
     */
    fun getOrCreate(connectionString: String = DEFAULT_CONNECTION_STRING): MongoClient {
        return clientCache.computeIfAbsent(connectionString) { url ->
            log.info { "Creating new MongoClient for $url" }
            MongoClient.create(url).also {
                ShutdownQueue.register(it)
            }
        }
    }

    /**
     * 연결 문자열과 추가 설정으로 코루틴 [MongoClient]를 반환합니다.
     *
     * 연결 문자열을 캐시 키로 사용합니다. 동일한 연결 문자열로 처음 호출될 때
     * [builder]를 적용하여 클라이언트를 생성합니다.
     *
     * ```kotlin
     * val client = MongoClientProvider.getOrCreate("mongodb://localhost:27017") {
     *     applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS) }
     * }
     * ```
     *
     * @param connectionString MongoDB 연결 문자열 (기본값: [DEFAULT_CONNECTION_STRING])
     * @param builder [MongoClientSettings.Builder] 추가 설정 람다
     * @return 캐시된 또는 새로 생성된 코루틴 [MongoClient] 인스턴스
     */
    fun getOrCreate(
        connectionString: String = DEFAULT_CONNECTION_STRING,
        builder: MongoClientSettings.Builder.() -> Unit,
    ): MongoClient {
        return clientCache.computeIfAbsent(connectionString) { url ->
            log.info { "Creating new MongoClient for $url with custom settings" }
            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(url))
                .apply(builder)
                .build()
            MongoClient.create(settings).also {
                ShutdownQueue.register(it)
            }
        }
    }

    /**
     * [MongoClientSettings]에 해당하는 코루틴 [MongoClient]를 반환합니다.
     *
     * [MongoClientSettings]를 캐시 키로 사용합니다.
     * `MongoClientSettings`는 `equals()`/`hashCode()`를 올바르게 구현합니다.
     *
     * ```kotlin
     * val settings = MongoClientSettings.builder()
     *     .applyConnectionString(ConnectionString("mongodb://localhost:27017"))
     *     .build()
     * val client = MongoClientProvider.getOrCreate(settings)
     * ```
     *
     * @param settings [MongoClientSettings] 인스턴스
     * @return 캐시된 또는 새로 생성된 코루틴 [MongoClient] 인스턴스
     */
    fun getOrCreate(settings: MongoClientSettings): MongoClient {
        return settingsClientCache.computeIfAbsent(settings) {
            log.info { "Creating new MongoClient with MongoClientSettings" }
            MongoClient.create(settings).also {
                ShutdownQueue.register(it)
            }
        }
    }

}
