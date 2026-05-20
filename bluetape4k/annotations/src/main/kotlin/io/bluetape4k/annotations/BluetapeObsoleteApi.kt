package io.bluetape4k.annotations

/**
 * Marks a Bluetape API that is obsolete and should not be used in new code.
 *
 * Obsolete APIs are retained only for migration or compatibility and may be
 * removed in a future major version. Prefer a replacement API when one is
 * available, and use [OptIn] only for deliberate compatibility bridges.
 *
 * ```kotlin
 * @OptIn(BluetapeObsoleteApi::class)
 * fun callObsoleteApi() {
 *     // Use an obsolete Bluetape declaration only during migration.
 * }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This Bluetape API is obsolete and should not be used in new code.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
@MustBeDocumented
annotation class BluetapeObsoleteApi
