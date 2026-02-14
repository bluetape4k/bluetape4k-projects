package io.bluetape4k.hibernate.reactive.mutiny

import io.bluetape4k.vertx.currentVertxDispatcher
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.hibernate.reactive.mutiny.Mutiny

/**
 * Session을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withSessionSuspending(
    crossinline work: suspend (session: Mutiny.Session) -> T,
): T = coroutineScope {
    withSession { session: Mutiny.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 특정 tenant의 Session을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withSessionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Mutiny.Session) -> T,
): T = coroutineScope {
    withSession(tenantId) { session: Mutiny.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asUni()
    }.awaitSuspending()
}

/**
 * StatelessSession을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withStatelessSessionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession { stateless: Mutiny.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 특정 tenant의 StatelessSession을 열어 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withStatelessSessionSuspending(
    tenantId: String,
    crossinline work: suspend (stateless: Mutiny.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession(tenantId) { stateless: Mutiny.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 트랜잭션이 보장된 Session에서 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    crossinline work: suspend (session: Mutiny.Session) -> T,
): T = coroutineScope {
    withTransaction { session: Mutiny.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 트랜잭션이 보장된 Session에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    crossinline work: suspend (session: Mutiny.Session, transaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withTransaction { session: Mutiny.Session, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 특정 tenant의 트랜잭션 Session에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Mutiny.Session, transaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withTransaction(tenantId) { session: Mutiny.Session, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 트랜잭션이 보장된 StatelessSession에서 suspend 블록을 실행하고 결과를 반환합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Mutiny.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 트랜잭션이 보장된 StatelessSession에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession, transaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Mutiny.StatelessSession, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asUni()
    }.awaitSuspending()
}

/**
 * 특정 tenant의 트랜잭션 StatelessSession에서 Session/Transaction을 함께 전달하여 suspend 블록을 실행합니다.
 */
suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Mutiny.StatelessSession, transaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction(tenantId) { stateless: Mutiny.StatelessSession, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asUni()
    }.awaitSuspending()
}
