package io.bluetape4k.hibernate.reactive.stage

import io.bluetape4k.vertx.currentVertxDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.hibernate.reactive.stage.Stage

suspend inline fun <T> Stage.SessionFactory.withSessionSuspending(
    crossinline work: suspend (session: Stage.Session) -> T,
): T = coroutineScope {
    withSession { session: Stage.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asCompletableFuture()
    }.await()
}

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

suspend inline fun <T> Stage.SessionFactory.withStatelessSessionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession { stateless: Stage.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asCompletableFuture()
    }.await()
}

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

suspend fun <T> Stage.SessionFactory.withTransactionSuspending(
    work: suspend (session: Stage.Session) -> T,
): T = coroutineScope {
    withTransaction { session: Stage.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asCompletableFuture()
    }.await()
}

suspend inline fun <T> Stage.SessionFactory.withTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Stage.Session, trasaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withTransaction(tenantId) { session: Stage.Session, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asCompletableFuture()
    }.await()
}

suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Stage.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asCompletableFuture()
    }.await()
}

suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Stage.StatelessSession, trasaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Stage.StatelessSession, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asCompletableFuture()
    }.await()
}

suspend inline fun <T> Stage.SessionFactory.withStatelessTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Stage.StatelessSession, trasaction: Stage.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction(tenantId) { stateless: Stage.StatelessSession, transaction: Stage.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asCompletableFuture()
    }.await()
}
