package io.bluetape4k.javers.base

/**
 * 엔티티 상태 변경 이벤트의 유형을 나타내는 열거형.
 *
 * - [UNKNOWN]: 알 수 없는 이벤트
 * - [SAVED]: 엔티티 생성 또는 수정
 * - [DELETED]: 엔티티 삭제
 *
 * ```kotlin
 * val type = EntityEventType.SAVED
 * // type.status == "SAVED"
 * ```
 */
enum class EntityEventType(val status: String) {

    /** 알 수 없는 이벤트 */
    UNKNOWN("UNKNOWN"),

    /** 엔티티 생성 또는 수정 */
    SAVED("SAVED"),

    /** 엔티티 삭제 */
    DELETED("DELETED");

    override fun toString(): String = status

    companion object {
        /**
         * [status] 문자열에 해당하는 [EntityEventType]을 반환하거나, 없으면 null을 반환한다.
         */
        fun valueOf(status: String): EntityEventType? {
            return entries.firstOrNull { it.status == status }
        }
    }
}
