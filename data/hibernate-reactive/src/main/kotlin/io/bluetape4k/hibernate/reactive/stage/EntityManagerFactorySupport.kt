package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.stage.Stage

/**
 * JPA [EntityManagerFactory]를 Hibernate Reactive의 [Stage.SessionFactory]로 변환합니다.
 */
fun EntityManagerFactory.asStageSessionFactory(): Stage.SessionFactory {
    return unwrap(Stage.SessionFactory::class.java)
}
