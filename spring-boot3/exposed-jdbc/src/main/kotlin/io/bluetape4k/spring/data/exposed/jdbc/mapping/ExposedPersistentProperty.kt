package io.bluetape4k.spring.data.exposed.jdbc.mapping

import org.jetbrains.exposed.v1.core.Column
import org.springframework.data.mapping.PersistentProperty

/**
 * Exposed Column을 Spring Data PersistentProperty로 표현합니다.
 *
 * ```kotlin
 * val entity = context.getRequiredPersistentEntity(User::class.java)
 *     as ExposedPersistentEntity<User>
 * entity.forEach { prop ->
 *     val column = (prop as ExposedPersistentProperty).getColumn()
 *     // column?.name → "name", "created_at" 등 실제 컬럼명
 * }
 * ```
 */
interface ExposedPersistentProperty : PersistentProperty<ExposedPersistentProperty> {

    /**
     * 이 프로퍼티에 대응하는 Exposed [Column] 인스턴스 (없으면 null)
     *
     * ```kotlin
     * val prop = entity.getPersistentProperty("name")
     * val column = (prop as? ExposedPersistentProperty)?.getColumn()
     * // column?.name → "name"
     * ```
     */
    fun getColumn(): Column<*>?
}
