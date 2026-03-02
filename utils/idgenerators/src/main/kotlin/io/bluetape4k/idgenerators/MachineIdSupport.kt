package io.bluetape4k.idgenerators

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.utils.node.MacAddressNodeIdentifier
import kotlin.math.absoluteValue

/**
 * 식별자 문자열 변환에 사용하는 기본 진수(36)를 나타냅니다.
 *
 * ## 동작/계약
 * - 숫자(`0-9`)와 소문자 알파벳(`a-z`) 조합을 사용합니다.
 * - 파싱/직렬화 유틸의 기본 `radix`로 재사용됩니다.
 *
 * ```kotlin
 * val radix = ALPHA_NUMERIC_BASE
 * // radix == 36
 * ```
 */
const val ALPHA_NUMERIC_BASE: Int = Character.MAX_RADIX

private val macAddressNodeIdentifier = MacAddressNodeIdentifier()

/**
 * 로컬 MAC 주소 기반 노드 식별자를 최대값 범위로 축소해 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `MacAddressNodeIdentifier.get()` 값을 절댓값으로 변환한 뒤 `% maxNumber`를 적용합니다.
 * - `maxNumber <= 0`이면 0으로 나눔이 발생할 수 있으므로 1 이상 값을 전달해야 합니다.
 *
 * ```kotlin
 * val machineId = getMachineId(1024)
 * // machineId in 0..1023
 * ```
 */
fun getMachineId(maxNumber: Int = MAX_MACHINE_ID): Int =
    macAddressNodeIdentifier.get().toInt().absoluteValue % maxNumber

/**
 * 36진수(기본) 문자열을 `Int`로 파싱합니다.
 *
 * ## 동작/계약
 * - 입력은 `lowercase()` 후 파싱하므로 대소문자를 구분하지 않습니다.
 * - 형식이 잘못되었거나 범위를 초과하면 [NumberFormatException]이 발생합니다.
 *
 * ```kotlin
 * val value = "zz".parseAsInt()
 * // value == 1295
 * ```
 */
fun String.parseAsInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    Integer.parseInt(this.lowercase(), radix)

/**
 * 36진수(기본) 문자열을 부호 없는 `Int`로 파싱합니다.
 *
 * ## 동작/계약
 * - 입력은 `lowercase()` 후 파싱합니다.
 * - 부호 없는 범위를 벗어나거나 형식이 잘못되면 [NumberFormatException]이 발생합니다.
 *
 * ```kotlin
 * val value = "ffffffff".parseAsUInt(16)
 * // value == -1  // unsigned int bit pattern
 * ```
 */
fun String.parseAsUInt(radix: Int = ALPHA_NUMERIC_BASE): Int =
    Integer.parseUnsignedInt(this.lowercase(), radix)

/**
 * 36진수(기본) 문자열을 `Long`으로 파싱합니다.
 *
 * ## 동작/계약
 * - 입력은 소문자로 정규화한 뒤 파싱합니다.
 * - 형식 오류나 범위 초과 시 [NumberFormatException]이 발생합니다.
 *
 * ```kotlin
 * val value = "10".parseAsLong(16)
 * // value == 16L
 * ```
 */
fun String.parseAsLong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseLong(this.lowercase(), radix)

/**
 * 36진수(기본) 문자열을 부호 없는 `Long`으로 파싱합니다.
 *
 * ## 동작/계약
 * - 입력을 소문자로 변환해 파싱합니다.
 * - 부호 없는 범위를 벗어나거나 형식이 잘못되면 [NumberFormatException]이 발생합니다.
 *
 * ```kotlin
 * val value = "ffffffffffffffff".parseAsULong(16)
 * // value == -1L  // unsigned long bit pattern
 * ```
 */
fun String.parseAsULong(radix: Int = ALPHA_NUMERIC_BASE): Long =
    java.lang.Long.parseUnsignedLong(this.lowercase(), radix)
