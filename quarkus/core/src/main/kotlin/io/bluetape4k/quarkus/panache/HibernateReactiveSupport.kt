package io.bluetape4k.quarkus.panache

import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny

/**
 * [Panache] 를 이용하여 Transaction 하에서 [work] 를 수행합니다.
 *
 * ```
 * val fruit = withPanacheTransaction { session, tx ->
 *      repository
 *          .persist(newGrape())
 *          .replaceWith(repository.findByName(grape.name))
 * }.await().indefinitely()
 * ```
 */
inline fun <T> withPanacheTransaction(
    crossinline work: (session: Mutiny.Session, tx: Mutiny.Transaction) -> Uni<T>,
): Uni<T> =
    Panache.getSession().chain { session ->
        session.withTransaction { tx ->
            work(session, tx)
        }
    }

/**
 * [Panache] 를 이용하여 Transaction 하에서 [work] 를 수행해봅니다. 실행 완료 후 rollback 을 수행합니다.
 * 테스트 시에 사용하면, 작업 검증만을 수행하고, 대상 DB의 데이터 상태는 변화를 주지 않습니다.
 *
 * ```
 * val fruit = withPanacheRollback { session, tx ->
 *      repository.persist(newGrape())
 *      .replaceWith(repository.findByName(grape.name))
 * }.await().indefinitely()
 * ```
 */
inline fun <T> withPanacheRollback(
    crossinline work: (session: Mutiny.Session, tx: Mutiny.Transaction) -> Uni<T>,
): Uni<T> {
    return withPanacheTransaction { session, tx ->
        tx.markForRollback()
        work(session, tx)
    }
}
