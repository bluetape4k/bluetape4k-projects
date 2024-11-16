package io.bluetape4k

import java.nio.charset.Charset
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/**
 * Bluetape4k Library Name (kommons)
 */
const val LibraryName: String = "bluetape4k"

/**
 * System Default Locale
 */
@JvmField
val DefaultLocale: Locale = Locale.getDefault()

/**
 * System Default Charset (UTF_8)
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
val DefaultZoneOffset: ZoneOffset = ZoneOffset.of(DefaultZoneId.id)

/**
 * UTC [ZoneOffset]
 *
 * @see UtcZoneId
 * @see ZoneOffset.UTC
 */
@JvmField
val UtcZoneOffset: ZoneOffset = ZoneOffset.UTC
