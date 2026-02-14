package io.bluetape4k.hibernate.reactive.stage

import io.bluetape4k.vertx.currentVertxDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.hibernate.reactive.stage.Stage

/**
 * Session을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withSessionSuspending(
    crossinline work: suspend (session: Stage.Session) -> T,
): T = coroutineScope {
    withSession { session: Stage.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asCompletableFuture()
    }.await()
}

/**
 * 특정 tenant의 Session을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withSessionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Stage.Session) -> T,
): T = coroutineScope {
    withSession(tenantId) { session: Stage.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asCompletableFuture()
    }.await()
}

/**
 * StatelessSession을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withStatelessSessionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession { stateless: Stage.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asCompletableFuture()
    }.await()
}

/**
 * 특정 tenant의 StatelessSession을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withStatelessSessionSuspending(
    tenantId: String,
    crossinline work: suspend (stateless: Stage.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession(tenantId) { stateless: Stage.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asCompletableFuture()
    }.await()
}

/**
 * 트랜잭션이 보장된 Session에서 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend fun <T> Stage.SessionFactory.withTransactionSuspending(
    work: suspend (session: Stage.Session) -> T,
): T = coroutineScope {
    withTransaction { session: Stage.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asCompletableFuture()
    }.await()
}

/**
 * 트랜잭션이 보장된 Session에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withTransactionSuspending(
    crossinline work: suspend (session: Stage.Session, transaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withTransaction { session: Stage.Session, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asCompletableFuture()
    }.await()
}

/**
 * 특정 tenant의 트랜잭션 Session에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Stage.Session, transaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withTransaction(tenantId) { session: Stage.Session, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asCompletableFuture()
    }.await()
}

/**
 * 트랜잭션이 보장된 StatelessSession에서 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Stage.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asCompletableFuture()
    }.await()
}

/**
 * 트랜잭션이 보장된 StatelessSession에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession, transaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Stage.StatelessSession, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asCompletableFuture()
    }.await()
}

/**
 * 특정 tenant의 트랜잭션 StatelessSession에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Stage.StatelessSession, transaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction(tenantId) { stateless: Stage.StatelessSession, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asCompletableFuture()
    }.await()
}
