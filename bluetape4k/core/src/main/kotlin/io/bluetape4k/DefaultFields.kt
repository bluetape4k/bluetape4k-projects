package io.bluetape4k

import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/**
 * Bluetape4k 라이브러리 이름 상수입니다.
 *
 * ```kotlin
 * println(LibraryName) // "bluetape4k"
 * ```
 */
const val LibraryName: String = "bluetape4k"

/**
 * 시스템 기본 로케일(Locale)입니다.
 *
 * ```kotlin
 * println(DefaultLocale.language)  // 예: "ko"
 * println(DefaultLocale.country)   // 예: "KR"
 * ```
 */
@JvmField
val DefaultLocale: Locale = Locale.getDefault()

/**
 * 시스템 기본 문자 집합(Charset)입니다. UTF-8 고정입니다.
 *
 * ```kotlin
 * val bytes = "안녕".toByteArray(DefaultCharset)
 * ```
 */
@JvmField
val DefaultCharset: Charset = Charsets.UTF_8

/**
 * System default charset name ("UTF-8")
 */
@JvmField
val DefaultCharsetName: String = DefaultCharset.name()

/**
 * System Default Charactor encoding ("utf-8")
 */
@JvmField
val DefaultEncoding: String = DefaultCharsetName.lowercase()

/**
 * System default [ZoneId]
 *
 * @see DefaultZoneOffset
 * @see ZoneId.systemDefault()
 */
@JvmField
val DefaultZoneId: ZoneId = ZoneId.systemDefault()

/**
 * UTC [ZoneId]
 *
 * @see UtcZoneOffset
 * @see ZoneId.of("UTC")
 */
@JvmField
val UtcZoneId: ZoneId = ZoneId.of("UTC")

/**
 * System default [ZoneOffset]
 *
 * @see DefaultZoneId
 */
@JvmField
val DefaultZoneOffset: ZoneOffset = DefaultZoneId.rules.getOffset(Instant.now())

/**
 * UTC [ZoneOffset]
 *
 * @see UtcZoneId
 * @see ZoneOffset.UTC
 */
@JvmField
val UtcZoneOffset: ZoneOffset = ZoneOffset.UTC
