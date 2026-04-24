package io.bluetape4k.hibernate.reactive.mutiny

import io.bluetape4k.vertx.currentVertxDispatcher
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CancellationException
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
            try {
                work(session)
            } catch (e: CancellationException) {
                // async 블록은 Mutiny Uni 콜백 내부에서 실행되므로, Mutiny가 예외를 가로채 Uni 실패로
                // 변환하기 전에 CancellationException이 삼켜질 수 있다.
                // 코루틴 취소 신호를 잃지 않기 위해 명시적으로 재전파한다.
                throw e
            }
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
            try {
                work(session)
            } catch (e: CancellationException) {
                // tenant 세션 콜백도 동일하다: Mutiny Uni 변환 과정에서 취소 예외가 소실되지 않도록
                // CancellationException을 그대로 재전파해 코루틴 취소 협력 계약을 유지한다.
                throw e
            }
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
            try {
                work(stateless)
            } catch (e: CancellationException) {
                // StatelessSession도 동일: Mutiny Uni 변환 경계에서 취소 예외가 소실되므로
                // CancellationException을 명시적으로 재전파해 구조적 동시성을 유지한다.
                throw e
            }
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
            try {
                work(stateless)
            } catch (e: CancellationException) {
                // tenant stateless 세션도 동일 이유: Mutiny 콜백 경계에서 취소 신호 소실을 방지한다.
                throw e
            }
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
            try {
                work(session)
            } catch (e: CancellationException) {
                // withTransaction 콜백은 Mutiny Uni 파이프라인 안에서 실행된다.
                // CancellationException을 일반 예외로 변환하지 않고 즉시 재전파해야
                // coroutineScope 취소 전파 체인이 정상 동작한다.
                throw e
            }
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
            try {
                work(session, transaction)
            } catch (e: CancellationException) {
                // 트랜잭션 객체를 함께 받는 오버로드에서도 동일: Mutiny 콜백 경계에서
                // 취소 예외를 삼키면 부모 코루틴이 취소를 인지하지 못하므로 재전파한다.
                throw e
            }
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
            try {
                work(session, transaction)
            } catch (e: CancellationException) {
                // tenant 트랜잭션 오버로드: Mutiny 비동기 경계를 넘기 전 CancellationException을
                // 재전파해 코루틴 취소 협력을 보장한다.
                throw e
            }
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
            try {
                work(stateless)
            } catch (e: CancellationException) {
                // withStatelessTransaction 콜백도 Mutiny Uni 내부에서 동작한다.
                // 취소 예외를 재전파하지 않으면 부모 코루틴이 정지 상태로 남을 수 있다.
                throw e
            }
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
            try {
                work(stateless, transaction)
            } catch (e: CancellationException) {
                // 트랜잭션 객체를 함께 받는 stateless 오버로드에서도 동일:
                // Mutiny가 예외를 Uni 실패로 감싸기 전에 취소 신호를 보존한다.
                throw e
            }
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
            try {
                work(stateless, transaction)
            } catch (e: CancellationException) {
                // tenant stateless 트랜잭션 오버로드: Mutiny 비동기 파이프라인 진입 전
                // CancellationException을 재전파해 구조적 동시성 계약을 지킨다.
                throw e
            }
        }.asUni()
    }.awaitSuspending()
}
