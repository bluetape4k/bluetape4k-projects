package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.stage.Stage

/**
 * [EntityManagerFactory]를 Stage 세션 팩토리로 언랩합니다.
 *
 * ## 동작/계약
 * - `unwrap(Stage.SessionFactory::class.java)`를 그대로 호출해 반환합니다.
 * - 수신 객체를 변경하지 않고 Hibernate가 관리하는 기존 팩토리 인스턴스를 반환합니다.
 * - 수신 객체가 `Stage.SessionFactory`를 지원하지 않으면 JPA provider 예외가 전파됩니다.
 *
 * ```kotlin
 * val stageFactory = entityManagerFactory.asStageSessionFactory()
 * val session = stageFactory.openSession().await()
 * session.close().await()
 * // session != null
 * ```
 */
fun EntityManagerFactory.asStageSessionFactory(): Stage.SessionFactory {
    return unwrap(Stage.SessionFactory::class.java)
}
