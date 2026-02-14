package io.bluetape4k.hibernate.reactive.mutiny

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.mutiny.Mutiny

/**
 * JPA [EntityManagerFactory]를 Hibernate Reactive의 [Mutiny.SessionFactory]로 변환합니다.
 */
fun EntityManagerFactory.asMutinySessionFactory(): Mutiny.SessionFactory {
    return unwrap(Mutiny.SessionFactory::class.java)
}
