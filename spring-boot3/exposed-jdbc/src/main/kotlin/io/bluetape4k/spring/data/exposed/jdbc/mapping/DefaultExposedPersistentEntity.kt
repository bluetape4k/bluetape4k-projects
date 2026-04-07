package io.bluetape4k.spring.data.exposed.jdbc.mapping

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.data.mapping.model.BasicPersistentEntity
import org.springframework.data.util.TypeInformation
import kotlin.reflect.full.companionObjectInstance

/**
 * [ExposedPersistentEntity]의 기본 구현체입니다.
 * Domain 클래스의 companion object에서 [EntityClass]를 추출합니다.
 *
 * ```kotlin
 * // ExposedMappingContext가 내부적으로 생성합니다.
 * val context = ExposedMappingContext()
 * val entity = context.getRequiredPersistentEntity(User::class.java)
 * val entityClass = entity.getEntityClass()  // User.Companion (EntityClass<Long, User>)
 * val table = entity.getTable()              // Users (IdTable<Long>)
 * ```
 */
class DefaultExposedPersistentEntity<T: Any>(
    typeInformation: TypeInformation<T>,
): BasicPersistentEntity<T, ExposedPersistentProperty>(typeInformation),
   ExposedPersistentEntity<T> {

    private val entityClassInstance: EntityClass<*, *>? by lazy {
        runCatching {
            typeInformation.type.kotlin.companionObjectInstance as? EntityClass<*, *>
        }.getOrNull()
    }


    override fun getEntityClass(): EntityClass<*, *>? = entityClassInstance

    override fun getTable(): Table? = entityClassInstance?.table
}
