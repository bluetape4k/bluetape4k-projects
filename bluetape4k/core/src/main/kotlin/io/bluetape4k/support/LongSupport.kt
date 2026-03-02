package io.bluetape4k.support

/**
 * `Long` 값을 안전하게 `Int`로 변환합니다.
 *
 * ## 동작/계약
 * - `Int` 범위(`Int.MIN_VALUE..Int.MAX_VALUE`) 안의 값만 변환합니다.
 * - 범위를 벗어나면 [IllegalArgumentException]을 발생시킵니다.
 * - 수신 값을 변경하지 않으며 새 객체 할당 없이 기본 타입 변환만 수행합니다.
 * - 내부적으로 [Math.toIntExact]를 사용해 오버플로우를 감지합니다.
 *
 * ```kotlin
 * val ok = 42L.toIntExact()
 * check(ok == 42)
 * // Long.MAX_VALUE.toIntExact() -> IllegalArgumentException
 * ```
 */
fun Long.toIntExact(): Int = try {
    Math.toIntExact(this)
} catch (e: ArithmeticException) {
    throw IllegalArgumentException("Value out of Int range. value=$this", e)
}
