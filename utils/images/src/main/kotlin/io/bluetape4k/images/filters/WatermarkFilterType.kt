package io.bluetape4k.images.filters

/**
 * 워터마크 필터의 종류를 정의하는 열거형입니다.
 *
 * ## 동작/계약
 * - [watermarkFilterOf] 팩토리 함수와 함께 사용되어 워터마크 스타일을 결정합니다.
 *
 * ```kotlin
 * val filter = watermarkFilterOf("© bluetape4k", type = WatermarkFilterType.COVER)
 * ```
 *
 * @see watermarkFilterOf
 */
enum class WatermarkFilterType {

    /** 이미지 전체를 워터마크 텍스트로 덮는 방식 */
    COVER,

    /** 이미지 중앙에 단일 워터마크를 찍는 방식 */
    STAMP
}
