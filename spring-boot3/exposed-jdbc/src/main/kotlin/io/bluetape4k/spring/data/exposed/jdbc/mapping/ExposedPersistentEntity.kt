package io.bluetape4k.spring.data.exposed.jdbc.mapping

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.data.mapping.PersistentEntity

/**
 * Exposed DAO Entity를 Spring Data PersistentEntity로 표현합니다.
 *
 * ```kotlin
 * val context = ExposedMappingContext()
 * val entity: ExposedPersistentEntity<User> =
 *     context.getRequiredPersistentEntity(User::class.java)
 *         as ExposedPersistentEntity<User>
 * val entityClass = entity.getEntityClass()  // User.Companion
 * val table = entity.getTable()              // Users
 * ```
 */
interface ExposedPersistentEntity<T: Any>: PersistentEntity<T, ExposedPersistentProperty> {

    /**
     * 이 Entity의 companion object에서 추출한 [EntityClass] 인스턴스 (없으면 null)
     *
     * ```kotlin
     * val entityClass = persistentEntity.getEntityClass()
     * // entityClass?.table → Users
     * ```
     */
    fun getEntityClass(): EntityClass<*, *>?

    /**
     * 이 Entity가 매핑되는 [Table] 인스턴스 (없으면 null)
     *
     * ```kotlin
     * val table = persistentEntity.getTable()
     * // table?.tableName → "users"
     * ```
     */
    fun getTable(): Table?
}
