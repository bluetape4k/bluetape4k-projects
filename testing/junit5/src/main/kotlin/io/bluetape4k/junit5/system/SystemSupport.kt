package io.bluetape4k.junit5.system

import org.junit.Assume

/**
 * Windows 환경이 아닐 때만 테스트를 계속 수행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `os.name`에 `win` 포함 여부를 검사합니다.
 * - Windows면 JUnit Assume으로 테스트를 skip(assumption failure)합니다.
 * - [assumeNotWindows]의 이전 이름이며 deprecated입니다.
 *
 * ```kotlin
 * assumeNoWindows()
 * // Windows면 테스트가 skip 된다.
 * ```
 */
@Deprecated("Use assumeNotWindows instead", ReplaceWith("assumeNotWindows()"))
fun assumeNoWindows() {
    Assume.assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
}

/**
 * Windows 환경이 아닐 때만 테스트를 계속 수행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `os.name` 문자열에 `win`이 포함되면 assumption failure를 발생시킵니다.
 * - failure 발생 시 테스트는 실패가 아니라 skip 처리됩니다.
 *
 * ```kotlin
 * assumeNotWindows()
 * // non-windows에서만 이후 assertion을 실행
 * ```
 */
fun assumeNotWindows() {
    Assume.assumeFalse(System.getProperty("os.name").lowercase().contains("win"))
}
