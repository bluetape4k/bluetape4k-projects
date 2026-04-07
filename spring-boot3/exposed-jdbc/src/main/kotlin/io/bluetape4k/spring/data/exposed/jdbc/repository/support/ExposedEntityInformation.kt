package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.data.repository.core.EntityInformation

/**
 * Exposed DAO Entity에 대한 Spring Data [EntityInformation] 인터페이스입니다.
 *
 * ```kotlin
 * val info: ExposedEntityInformation<User, Long> =
 *     ExposedEntityInformationImpl(User::class.java)
 * info.entityClass  // User.Companion (LongEntityClass<User>)
 * info.table        // Users (LongIdTable)
 * info.isNew(user)  // true (신규 엔티티인 경우)
 * ```
 */
interface ExposedEntityInformation<E: Entity<ID>, ID: Any>: EntityInformation<E, ID> {

    /** 이 Entity의 [EntityClass] 인스턴스 */
    val entityClass: EntityClass<ID, E>

    /** 이 Entity가 매핑되는 [IdTable] 인스턴스 */
    val table: IdTable<ID>
}
