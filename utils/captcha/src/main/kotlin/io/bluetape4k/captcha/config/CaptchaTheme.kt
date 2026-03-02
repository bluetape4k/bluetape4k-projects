package io.bluetape4k.captcha.config

/**
 * CAPTCHA 렌더링 테마를 정의합니다.
 *
 * ## 동작/계약
 * - [LIGHT]는 밝은 배경/기본 팔레트를 사용합니다.
 * - [DARK]는 어두운 배경/대비 팔레트를 사용합니다.
 *
 * ```kotlin
 * val dark = CaptchaTheme.DARK
 * // dark.name == "DARK"
 * ```
 */
enum class CaptchaTheme {
    /** 밝은 배경 테마입니다. */
    LIGHT,
    /** 어두운 배경 테마입니다. */
    DARK
}
