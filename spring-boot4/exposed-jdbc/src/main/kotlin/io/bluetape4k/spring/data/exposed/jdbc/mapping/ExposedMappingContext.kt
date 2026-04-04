package io.bluetape4k.spring.data.exposed.jdbc.mapping

import org.springframework.data.mapping.context.AbstractMappingContext
import org.springframework.data.mapping.model.Property
import org.springframework.data.mapping.model.SimpleTypeHolder
import org.springframework.data.util.TypeInformation

/**
 * Exposed DAO Entity를 위한 Spring Data MappingContext 구현체입니다.
 *
 * ```kotlin
 * val context = ExposedMappingContext()
 * val entity = context.getRequiredPersistentEntity(User::class.java)
 * val table = entity.getTable()        // Users
 * val entityClass = entity.getEntityClass() // User.Companion
 * ```
 */
class ExposedMappingContext :
    AbstractMappingContext<DefaultExposedPersistentEntity<*>, ExposedPersistentProperty>() {

    override fun <T : Any> createPersistentEntity(
        typeInformation: TypeInformation<T>,
    ): DefaultExposedPersistentEntity<T> =
        DefaultExposedPersistentEntity(typeInformation)

    override fun createPersistentProperty(
        property: Property,
        owner: DefaultExposedPersistentEntity<*>,
        simpleTypeHolder: SimpleTypeHolder,
    ): ExposedPersistentProperty =
        DefaultExposedPersistentProperty(property, owner, simpleTypeHolder)
}
