package io.bluetape4k.hibernate.reactive.mutiny

import io.bluetape4k.vertx.currentVertxDispatcher
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.hibernate.reactive.mutiny.Mutiny

/**
 * Mutiny 세션을 열어 suspend 작업을 실행하고 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `withSession` 콜백에서 `currentVertxDispatcher()`로 코루틴을 실행합니다.
 * - 세션 생성/종료는 Hibernate Reactive가 관리하며 수신 객체 상태를 변경하지 않습니다.
 * - [work] 예외는 `Uni` 실패로 변환된 뒤 `awaitSuspending()`에서 다시 전파됩니다.
 *
 * ```kotlin
 * val total = sessionFactory.withSessionSuspending { session ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // total == 2L
 * ```
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
 * tenant id를 지정해 Mutiny 세션을 열고 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]는 `withSession(tenantId)`로 그대로 전달됩니다.
 * - 수신 객체를 변경하지 않고 tenant 스코프의 세션 수명주기는 Hibernate가 관리합니다.
 * - tenant id 해석 실패나 [work] 예외는 `awaitSuspending()` 시점에 그대로 전파됩니다.
 *
 * ```kotlin
 * val author = sessionFactory.withSessionSuspending("tenant-a") { session ->
 *   session.findAs<Author>(1L).awaitSuspending()
 * }
 * // author.id == 1L
 * ```
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
 * Mutiny stateless 세션을 열어 suspend 작업을 실행하고 결과를 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `withStatelessSession` 콜백을 코루틴으로 브리지합니다.
 * - 1차 캐시 없는 StatelessSession 특성은 Hibernate Reactive 기본 동작을 따릅니다.
 * - [work] 실패는 `awaitSuspending()` 호출자에게 그대로 전파됩니다.
 *
 * ```kotlin
 * val books = sessionFactory.withStatelessSessionSuspending { session ->
 *   session.createSelectionQueryAs<Book>("select b from Book b").resultList.awaitSuspending()
 * }
 * // books.size == 3
 * ```
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
 * tenant id를 지정해 Mutiny stateless 세션에서 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]는 `withStatelessSession(tenantId)`로 그대로 전달됩니다.
 * - 수신 객체를 변경하지 않으며 세션 생성/종료는 Hibernate Reactive가 관리합니다.
 * - tenant id 또는 [work] 처리 실패는 `awaitSuspending()`에서 예외로 전파됩니다.
 *
 * ```kotlin
 * val author = sessionFactory.withStatelessSessionSuspending("tenant-a") { session ->
 *   session.getAs<Author>(1L).awaitSuspending()
 * }
 * // author.id == 1L
 * ```
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
 * 트랜잭션이 열린 Mutiny 세션에서 suspend 작업을 실행하고 커밋/롤백을 위임합니다.
 *
 * ## 동작/계약
 * - 내부 `withTransaction` 경로를 사용해 트랜잭션 경계를 Hibernate Reactive에 위임합니다.
 * - [work]가 정상 종료되면 커밋, 예외 발생 시 롤백하는 기본 정책을 따릅니다.
 * - 세션 팩토리 자체 상태는 변경하지 않고 결과만 반환합니다.
 *
 * ```kotlin
 * val saved = sessionFactory.withTransactionSuspending { session ->
 *   session.persist(author).awaitSuspending(); author
 * }
 * // saved.id != null
 * ```
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
 * 트랜잭션 객체를 함께 전달받아 Mutiny 세션 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [work]에 세션과 트랜잭션을 함께 전달해 상태를 직접 조회/활용할 수 있습니다.
 * - 트랜잭션 경계(커밋/롤백)는 `withTransaction` 기본 정책을 그대로 따릅니다.
 * - [work] 예외는 롤백 이후 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withTransactionSuspending { session, tx ->
 *   require(!tx.isMarkedForRollback)
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // count == 2L
 * ```
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
 * tenant id를 지정한 트랜잭션 세션에서 세션/트랜잭션을 함께 전달해 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]를 `withTransaction(tenantId)`에 전달해 tenant별 트랜잭션을 엽니다.
 * - 트랜잭션 처리 규칙은 Hibernate Reactive의 기본 커밋/롤백 정책을 따릅니다.
 * - tenant 해석 실패나 [work] 예외는 롤백 후 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withTransactionSuspending("tenant-a") { session, _ ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // count >= 0L
 * ```
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
 * 트랜잭션이 열린 Mutiny stateless 세션에서 suspend 작업을 실행합니다.
 *
 * ## 동작/계약
 * - 내부 `withStatelessTransaction` 경로로 트랜잭션을 관리합니다.
 * - StatelessSession 특성상 영속성 컨텍스트 캐시는 사용하지 않습니다.
 * - [work] 실패 시 롤백 후 예외가 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending { session ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // count == 2L
 * ```
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
 * 트랜잭션 객체를 함께 전달받아 Mutiny stateless 세션 작업을 실행합니다.
 *
 * ## 동작/계약
 * - [work]에 stateless session과 transaction을 함께 전달합니다.
 * - 트랜잭션 경계는 `withStatelessTransaction` 기본 정책을 따릅니다.
 * - [work] 예외는 롤백 이후 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending { session, tx ->
 *   require(!tx.isMarkedForRollback)
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // count == 2L
 * ```
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
 * tenant id를 지정한 트랜잭션 stateless 세션에서 세션/트랜잭션을 함께 전달해 실행합니다.
 *
 * ## 동작/계약
 * - [tenantId]를 `withStatelessTransaction(tenantId)`에 전달합니다.
 * - 트랜잭션 처리(커밋/롤백)는 Hibernate Reactive 기본 정책을 따릅니다.
 * - tenant 해석 실패나 [work] 예외는 롤백 후 전파됩니다.
 *
 * ```kotlin
 * val count = sessionFactory.withStatelessTransactionSuspending("tenant-a") { session, _ ->
 *   session.createSelectionQueryAs<Long>("select count(a) from Author a")
 *     .singleResult.awaitSuspending().toLong()
 * }
 * // count >= 0L
 * ```
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
