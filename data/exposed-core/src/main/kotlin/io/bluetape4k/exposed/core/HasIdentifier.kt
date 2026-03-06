package io.bluetape4k.exposed.core

/**
 * 식별자 필드를 노출하는 엔티티 계약입니다.
 *
 * ## 동작/계약
 * - 구현체는 `id`를 읽기 전용 프로퍼티로 제공합니다.
 * - `id`가 `null`이면 아직 저장소에 등록되지 않은 상태(예: 생성 요청 DTO)를 의미합니다.
 * - 직렬화 가능한 타입([java.io.Serializable])으로 취급됩니다.
 *
 * ```kotlin
 * class User(override val id: Long?): HasIdentifier<Long>
 * // User(1L).id == 1L
 * // User(null).id == null  // 미저장 상태
 * ```
 */
interface HasIdentifier<ID: Any>: java.io.Serializable {
    /** 엔티티의 고유 식별자입니다. `null`이면 아직 저장되지 않은 상태입니다. */
    val id: ID?
}
