package io.bluetape4k.javers.base

import java.io.Serializable

/**
 * 엔티티 상태 변경 이벤트를 전달할 때 사용하는 봉투(envelope) 객체.
 *
 * ## 동작/계약
 * - [entity] 기반 생성자는 SAVED 이벤트로 기본 설정된다
 * - [entityId] + [entityType] 기반 생성자는 DELETED 이벤트로 설정된다
 * - 커스텀 메타데이터는 [addHeader]/[getHeader]로 관리한다
 *
 * ```kotlin
 * val saved = EntityEnvelop(myEntity)
 * // saved.isSavedEntity == true
 *
 * val deleted = EntityEnvelop(entityId = 1L, entityType = User::class.java)
 * // deleted.isDeletedEntity == true
 * ```
 *
 * @property entity 상태가 변경된 엔티티 (SAVED 이벤트 시에만 설정)
 * @property entityId 삭제된 엔티티의 ID (DELETED 이벤트 시에만 설정)
 * @property entityType 엔티티의 Java 클래스 타입
 * @property eventType 이벤트 유형 (기본값: [EntityEventType.SAVED])
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

    /**
     * 커스텀 헤더를 추가한다.
     */
    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    /**
     * 지정한 키의 헤더 값을 반환하거나, 없으면 null을 반환한다.
     */
    fun getHeader(key: String): String? = headers[key]

    /** 이벤트 유형이 SAVED인지 여부 */
    val isSavedEntity: Boolean get() = eventType == EntityEventType.SAVED

    /** 이벤트 유형이 DELETED인지 여부 */
    val isDeletedEntity: Boolean get() = eventType == EntityEventType.DELETED
}
