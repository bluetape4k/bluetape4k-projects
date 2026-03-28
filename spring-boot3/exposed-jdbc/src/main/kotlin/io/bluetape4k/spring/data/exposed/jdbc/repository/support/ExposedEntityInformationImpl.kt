package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.data.repository.core.support.AbstractEntityInformation
import kotlin.reflect.full.companionObjectInstance

/**
 * [ExposedEntityInformation]의 기본 구현체입니다.
 * Domain 클래스의 companion object에서 [EntityClass]를 추출합니다.
 */
@Suppress("UNCHECKED_CAST")
class ExposedEntityInformationImpl<E: Entity<ID>, ID: Any>(
    domainClass: Class<E>,
    override val entityClass: EntityClass<ID, E>,
): AbstractEntityInformation<E, ID>(domainClass), ExposedEntityInformation<E, ID> {

    companion object: KLogging() {
        @Suppress("UNCHECKED_CAST")
        operator fun <E: Entity<ID>, ID: Any> invoke(domainClass: Class<E>): ExposedEntityInformationImpl<E, ID> {
            val companion = domainClass.kotlin.companionObjectInstance
                ?: error("${domainClass.name} must have a companion object (EntityClass<ID, E>)")
            val entityClass = companion as? EntityClass<ID, E>
                ?: error("Companion of ${domainClass.name} must be EntityClass<ID, E>")
            return ExposedEntityInformationImpl(domainClass, entityClass)
        }
    }

    override val table: IdTable<ID> = entityClass.table

    override fun getId(entity: E): ID? =
        runCatching { entity.id.value }.getOrNull()

    override fun getIdType(): Class<ID> =
        resolveIdType(javaType) as Class<ID>

    /**
     * Exposed DAO 엔티티가 신규(아직 DB에 INSERT 되지 않음)인지 확인합니다.
     * 새로 생성된 엔티티는 트랜잭션 커밋 전까지 id.value 접근 시 예외를 발생시킵니다.
     */
    override fun isNew(entity: E): Boolean =
        runCatching { entity.id.value; false }.getOrDefault(true)

    private fun resolveIdType(clazz: Class<*>): Class<*> {
        var current: Class<*>? = clazz
        while (current != null) {
            val genericSuper = current.genericSuperclass
            if (genericSuper is java.lang.reflect.ParameterizedType) {
                val rawType = genericSuper.rawType as? Class<*>
                if (rawType != null && Entity::class.java.isAssignableFrom(rawType)) {
                    val typeArg = genericSuper.actualTypeArguments.firstOrNull()
                    if (typeArg is Class<*>) return typeArg
                }
            }
            current = current.superclass
        }
        error("Cannot resolve ID type for ${clazz.name}. Ensure Entity<ID> generic parameter is declared explicitly.")
    }
}
