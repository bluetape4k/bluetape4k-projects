package io.bluetape4k.spring.data.exposed.jdbc.mapping

import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toSnakeCase
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.springframework.data.mapping.Association
import org.springframework.data.mapping.PersistentEntity
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty
import org.springframework.data.mapping.model.Property
import org.springframework.data.mapping.model.SimpleTypeHolder

/**
 * [ExposedPersistentProperty]의 기본 구현체입니다.
 * 프로퍼티 이름으로 Exposed Table의 Column을 검색합니다.
 *
 * ```kotlin
 * // ExposedMappingContext가 내부적으로 생성합니다.
 * val context = ExposedMappingContext()
 * val entity = context.getRequiredPersistentEntity(User::class.java)
 * entity.forEach { property ->
 *     val column = property.getColumn() // Users.name, Users.email 등
 * }
 * ```
 */
class DefaultExposedPersistentProperty(
    property: Property,
    owner: PersistentEntity<*, ExposedPersistentProperty>,
    simpleTypeHolder: SimpleTypeHolder,
): AnnotationBasedPersistentProperty<ExposedPersistentProperty>(property, owner, simpleTypeHolder),
   ExposedPersistentProperty {

    private val table: Table? = (owner as? ExposedPersistentEntity<*>)?.getTable()

    override fun getColumn(): Column<*>? {
        val tbl = table ?: return null
        return tbl.columns.firstOrNull { col ->
            col.name.equals(name, ignoreCase = true) ||
                    col.name.equals(toSnakeCase(name), ignoreCase = true)
        }
    }

    override fun createAssociation(): Association<ExposedPersistentProperty> =
        Association(this, null)
}
