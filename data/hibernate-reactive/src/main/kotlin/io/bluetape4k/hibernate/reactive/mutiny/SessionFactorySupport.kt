package io.bluetape4k.hibernate.reactive.mutiny

import io.bluetape4k.vertx.currentVertxDispatcher
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.hibernate.reactive.mutiny.Mutiny

suspend inline fun <T> Mutiny.SessionFactory.withSessionSuspending(
    crossinline work: suspend (session: Mutiny.Session) -> T,
): T = coroutineScope {
    withSession { session: Mutiny.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asUni()
    }.awaitSuspending()
}

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

suspend inline fun <T> Mutiny.SessionFactory.withStatelessSessionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessSession { stateless: Mutiny.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asUni()
    }.awaitSuspending()
}

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

suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    crossinline work: suspend (session: Mutiny.Session) -> T,
): T = coroutineScope {
    withTransaction { session: Mutiny.Session ->
        async(currentVertxDispatcher()) {
            work(session)
        }.asUni()
    }.awaitSuspending()
}

suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    crossinline work: suspend (session: Mutiny.Session, trasaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withTransaction { session: Mutiny.Session, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asUni()
    }.awaitSuspending()
}

suspend inline fun <T> Mutiny.SessionFactory.withTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Mutiny.Session, trasaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withTransaction(tenantId) { session: Mutiny.Session, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(session, transaction)
        }.asUni()
    }.awaitSuspending()
}

suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Mutiny.StatelessSession ->
        async(currentVertxDispatcher()) {
            work(stateless)
        }.asUni()
    }.awaitSuspending()
}

suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    crossinline work: suspend (session: Mutiny.StatelessSession, trasaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction { stateless: Mutiny.StatelessSession, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asUni()
    }.awaitSuspending()
}

suspend inline fun <T> Mutiny.SessionFactory.withStatelessTransactionSuspending(
    tenantId: String,
    crossinline work: suspend (session: Mutiny.StatelessSession, trasaction: Mutiny.Transaction) -> T,
): T = coroutineScope {
    withStatelessTransaction(tenantId) { stateless: Mutiny.StatelessSession, transaction: Mutiny.Transaction ->
        async(currentVertxDispatcher()) {
            work(stateless, transaction)
        }.asUni()
    }.awaitSuspending()
}
