package io.bluetape4k.science.coords

/**
 * 두 [BoundingBox] 간의 공간 관계를 나타내는 열거형입니다.
 */
enum class BoundingBoxRelation {
    /** 두 영역이 겹치지 않음 */
    DISJOINT,

    /** 두 영역이 일부 겹침 */
    INTERSECTS,

    /** 현재 영역이 대상 영역을 완전히 포함함 */
    CONTAINS,

    /** 현재 영역이 대상 영역 안에 완전히 포함됨 */
    WITHIN,
}
