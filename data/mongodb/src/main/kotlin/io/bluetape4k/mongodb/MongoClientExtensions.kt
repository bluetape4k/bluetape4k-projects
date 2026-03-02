package io.bluetape4k.mongodb

import com.mongodb.TransactionOptions
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.toList

// ====================================================
// MongoClient 코루틴 확장 함수
// ====================================================

/**
 * 모든 데이터베이스 이름을 [List]로 수집합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `listDatabaseNames().toList()`를 호출합니다.
 * - 데이터베이스가 없으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val names = client.listDatabaseNamesAsList()
 * // names.contains("test") == true
 * ```
 */
suspend fun MongoClient.listDatabaseNamesAsList(): List<String> =
    listDatabaseNames().toList()

/**
 * 클라이언트 세션을 시작하고 [block] 내에서 작업을 수행합니다.
 * 블록 완료 또는 예외 발생 후 세션을 자동으로 닫습니다.
 *
 * ## 동작/계약
 * - `startSession()`으로 새 [ClientSession]을 생성합니다.
 * - 블록 실행 중 예외가 발생해도 `finally`에서 세션이 반드시 닫힙니다.
 * - 세션 자체는 트랜잭션을 시작하지 않습니다. 트랜잭션이 필요하면 [inTransaction]을 사용하세요.
 *
 * ```kotlin
 * val result = client.withClientSession { session ->
 *     collection.find(session, filter).firstOrNull()
 * }
 * ```
 *
 * @param block 세션을 인자로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend fun <T> MongoClient.withClientSession(
    block: suspend (ClientSession) -> T,
): T {
    val session = startSession()
    return try {
        block(session)
    } finally {
        session.close()
    }
}

/**
 * 트랜잭션 내에서 [block]을 실행합니다.
 * 성공 시 커밋하고, 예외 발생 시 롤백합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [withClientSession]을 사용하여 세션을 관리합니다.
 * - 트랜잭션은 MongoDB 4.0+ replica set 또는 sharded cluster에서만 지원됩니다.
 * - 블록 내 예외는 롤백 후 그대로 재던집니다.
 * - `transactionOptions`가 null이면 서버 기본값이 사용됩니다.
 *
 * ```kotlin
 * val result = client.inTransaction { session ->
 *     collection.insertOne(session, document)
 *     otherCollection.deleteOne(session, filter)
 *     "success"
 * }
 * ```
 *
 * @param transactionOptions 트랜잭션 옵션 (기본값: null — 서버 기본값 사용)
 * @param block 세션을 인자로 받는 suspend 트랜잭션 블록
 * @return 블록의 반환값
 */
suspend fun <T> MongoClient.inTransaction(
    transactionOptions: TransactionOptions? = null,
    block: suspend (ClientSession) -> T,
): T = withClientSession { session ->
    if (transactionOptions != null) {
        session.startTransaction(transactionOptions)
    } else {
        session.startTransaction()
    }
    try {
        val result = block(session)
        session.commitTransaction()
        result
    } catch (e: Exception) {
        try {
            session.abortTransaction()
        } catch (_: Exception) {
            // abort 실패는 무시합니다.
        }
        throw e
    }
}
