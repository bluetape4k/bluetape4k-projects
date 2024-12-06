package io.bluetape4k.javers.base

import java.io.Serializable

/**
 * 엔티티 상태 정보 변경에 따른 Dispatch 시에 봉투 역할을 수행합니다.
 *
 * @property entity 상태가 변경된 Entity (Save 시에만 가능
 */
data class EntityEnvelop(
    val entity: Any? = null,
    val entityId: Any? = null,
    val entityType: Class<*>,
    val eventType: EntityEventType = EntityEventType.SAVED,
): Serializable {

    constructor(entity: Any): this(entity = entity, entityType = entity.javaClass)
    constructor(entityId: Any, entityType: Class<*>): this(null, entityId, entityType, EntityEventType.DELETED)

    private val headers = hashMapOf<String, String>()

    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    fun getHeader(key: String): String? = headers[key]

    val isSavedEntity: Boolean get() = eventType == EntityEventType.SAVED
    val isDeletedEntity: Boolean get() = eventType == EntityEventType.DELETED
}
