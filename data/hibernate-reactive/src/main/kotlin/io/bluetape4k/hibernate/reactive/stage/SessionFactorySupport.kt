package io.bluetape4k.hibernate.reactive.stage

import io.bluetape4k.vertx.currentVertxDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.hibernate.reactive.stage.Stage

/**
 * Stage 세션을 열어 suspend 작업을 실행하고 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `withSession` 결과(`CompletionStage`)를 코루틴으로 브리지합니다.
 * - 세션 생성/종료는 Hibernate Reactive가 관리하며 수신 객체 상태를 변경하지 않습니다.
 * - [work] 예외는 `await()` 시점에 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val total = sessionFactory.withSessionSuspending { session ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // total == 2L
 * ```
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
 * tenant id를 지정해 Stage 세션을 열고 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]는 `withSession(tenantId)`에 그대로 전달됩니다.
 * - 수신 객체를 변경하지 않으며 tenant 세션의 생명주기는 Hibernate가 관리합니다.
 * - tenant 해석 실패나 [work] 예외는 `await()` 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val author = sessionFactory.withSessionSuspending("tenant-a") { session ->
 *   session.findAs<Author>(1L).await()
 * }
 * // author.id == 1L
 * ```
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
 * Stage stateless 세션을 열어 suspend 작업을 실행하고 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `withStatelessSession` 결과를 코루틴으로 브리지합니다.
 * - StatelessSession 특성(1차 캐시 미사용)은 Hibernate 기본 동작을 따릅니다.
 * - [work] 예외는 `await()` 시점에 그대로 전파됩니다.
 *
 * ```kotlin
 * val books = sessionFactory.withStatelessSessionSuspending { session ->
 *   session.createSelectionQueryAs<Book>("select b from Book b").resultList.await()
 * }
 * // books.size == 3
 * ```
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
 * tenant id를 지정해 Stage stateless 세션에서 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]를 `withStatelessSession(tenantId)`로 전달합니다.
 * - 세션 생명주기는 Hibernate가 관리하며 수신 객체는 변경하지 않습니다.
 * - tenant id 처리 실패나 [work] 예외는 `await()`에서 전파됩니다.
 *
 * ```kotlin
 * val author = sessionFactory.withStatelessSessionSuspending("tenant-a") { session ->
 *   session.getAs<Author>(1L).await()
 * }
 * // author.id == 1L
 * ```
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
 * 트랜잭션이 열린 Stage 세션에서 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - 내부 `withTransaction`으로 트랜잭션 경계를 Hibernate에 위임합니다.
 * - [work] 정상 종료 시 커밋, 예외 시 롤백되는 기본 정책을 따릅니다.
 * - 세션 팩토리 자체는 변경하지 않고 결과만 반환합니다.
 *
 * ```kotlin
 * val saved = sessionFactory.withTransactionSuspending { session ->
 *   session.persist(author).await(); author
 * }
 * // saved.id != null
 * ```
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
 * 트랜잭션 객체를 함께 전달받아 Stage 세션 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [work]에 세션과 트랜잭션을 함께 전달합니다.
 * - 트랜잭션 경계는 `withTransaction` 기본 커밋/롤백 정책을 따릅니다.
 * - [work] 예외는 롤백 후 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withTransactionSuspending { session, tx ->
 *   require(!tx.isMarkedForRollback)
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // count == 2L
 * ```
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
 * tenant id를 지정한 트랜잭션 세션에서 세션/트랜잭션을 함께 전달해 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]를 `withTransaction(tenantId)`에 전달합니다.
 * - 트랜잭션 경계 처리는 Hibernate Reactive 기본 정책을 따릅니다.
 * - tenant 해석 실패나 [work] 예외는 롤백 후 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withTransactionSuspending("tenant-a") { session, _ ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // count >= 0L
 * ```
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
 * 트랜잭션이 열린 Stage stateless 세션에서 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - 내부 `withStatelessTransaction`으로 트랜잭션 경계를 생성/종료합니다.
 * - StatelessSession 특성상 엔티티 1차 캐시는 사용되지 않습니다.
 * - [work] 실패 시 롤백 후 예외가 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending { session ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // count == 2L
 * ```
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
 * 트랜잭션 객체를 함께 전달받아 Stage stateless 세션 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [work]에 stateless session과 transaction을 함께 전달합니다.
 * - 트랜잭션 경계는 `withStatelessTransaction` 기본 정책을 따릅니다.
 * - [work] 예외는 롤백 후 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending { session, tx ->
 *   require(!tx.isMarkedForRollback)
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // count == 2L
 * ```
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
 * tenant id를 지정한 트랜잭션 stateless 세션에서 세션/트랜잭션을 함께 전달해 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]를 `withStatelessTransaction(tenantId)`에 전달합니다.
 * - 트랜잭션 경계는 Hibernate Reactive 기본 커밋/롤백 정책을 따릅니다.
 * - tenant 해석 실패나 [work] 예외는 롤백 후 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending("tenant-a") { session, _ ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.await().toLong()
 * }
 * // count >= 0L
 * ```
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
