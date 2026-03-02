package io.bluetape4k.jdbc.sql

/**
 * 컬럼 라벨/인덱스로 값을 읽는 접근 함수를 묶습니다.
 *
 * ## 동작/계약
 * - [withColumnLabel], [withColumnIndex] 콜백을 보관하고 `get` 호출 시 그대로 위임합니다.
 * - 내부 상태를 변경하지 않으며 호출 시 별도 객체를 생성하지 않습니다.
 * - 인덱스 범위/라벨 유효성 검증은 전달된 콜백(대개 `ResultSet#getXxx`) 구현에 따릅니다.
 *
 * ```kotlin
 * val token = GetColumnToken<String?>(
 *   withColumnLabel = { label -> rs.getString(label) },
 *   withColumnIndex = { index -> rs.getString(index) }
 * )
 * // token[1] == rs.getString(1)
 * ```
 */
class GetColumnToken<out T>(
    val withColumnLabel: (String) -> T,
    val withColumnIndex: (Int) -> T,
) {
    /**
     * 컬럼 라벨로 값을 조회합니다.
     *
     * ## 동작/계약
     * - [columnLabel]을 [withColumnLabel]에 그대로 전달합니다.
     * - 라벨 미존재/타입 불일치 예외는 콜백에서 발생한 그대로 전파됩니다.
     * - 별도 캐시 없이 호출마다 즉시 계산합니다.
     *
     * ```kotlin
     * val name = token["name"]
     * // name == rs.getString("name")
     * ```
     */
    operator fun get(columnLabel: String): T = withColumnLabel(columnLabel)

    /**
     * 컬럼 인덱스로 값을 조회합니다.
     *
     * ## 동작/계약
     * - [columnIndex]를 [withColumnIndex]에 그대로 전달합니다.
     * - 인덱스 범위 오류/타입 오류 예외는 콜백에서 발생한 그대로 전파됩니다.
     * - 호출당 추가 상태를 저장하지 않습니다.
     *
     * ```kotlin
     * val name = token[1]
     * // name == rs.getString(1)
     * ```
     */
    operator fun get(columnIndex: Int): T = withColumnIndex(columnIndex)
}
