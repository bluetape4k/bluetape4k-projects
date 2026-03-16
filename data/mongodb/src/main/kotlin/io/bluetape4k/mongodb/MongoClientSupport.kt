package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient

/**
 * [MongoClientSettings.Builder] DSL을 이용하여 [MongoClient]를 생성합니다.
 *
 * ```kotlin
 * val client = mongoClient {
 *     applyConnectionString(ConnectionString("mongodb://localhost:27017"))
 *     applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS) }
 * }
 * ```
 *
 * @param builder [MongoClientSettings.Builder] 초기화 람다
 * @return 생성된 [MongoClient] 인스턴스
 */
inline fun mongoClient(
    builder: MongoClientSettings.Builder.() -> Unit,
): MongoClient {
    val settings = MongoClientSettings.builder().apply(builder).build()
    return MongoClient.create(settings)
}

/**
 * 연결 문자열과 추가 설정으로 [MongoClient]를 생성합니다.
 *
 * ```kotlin
 * val client = mongoClientOf("mongodb://localhost:27017") {
 *     applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS) }
 * }
 * ```
 *
 * @param connectionString MongoDB 연결 문자열 (기본값: `mongodb://localhost:27017`)
 * @param builder [MongoClientSettings.Builder] 추가 설정 람다
 * @return 생성된 [MongoClient] 인스턴스
 */
inline fun mongoClientOf(
    connectionString: String = MongoClientProvider.DEFAULT_CONNECTION_STRING,
    builder: MongoClientSettings.Builder.() -> Unit = {},
): MongoClient = mongoClient {
    applyConnectionString(ConnectionString(connectionString))
    builder()
}
