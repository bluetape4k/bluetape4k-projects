package io.bluetape4k.hibernate.reactive.mutiny

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.mutiny.Mutiny

/**
 * [EntityManagerFactory]를 Mutiny 세션 팩토리로 언랩합니다.
 *
 * ## 동작/계약
 * - `unwrap(Mutiny.SessionFactory::class.java)`를 그대로 호출해 반환합니다.
 * - 수신 객체를 변경하지 않고 Hibernate가 관리하는 기존 팩토리 인스턴스를 반환합니다.
 * - 수신 객체가 `Mutiny.SessionFactory`를 지원하지 않으면 JPA provider 예외가 전파됩니다.
 *
 * ```kotlin
 * val mutinyFactory = entityManagerFactory.asMutinySessionFactory()
 * val session = mutinyFactory.openSession().awaitSuspending()
 * session.close().awaitSuspending()
 * // session != null
 * ```
 */
fun EntityManagerFactory.asMutinySessionFactory(): Mutiny.SessionFactory {
    return unwrap(Mutiny.SessionFactory::class.java)
}
