package io.bluetape4k.annotations

/**
 * Marks a Bluetape API that is experimental.
 *
 * Experimental APIs may change or be removed without source or binary
 * compatibility guarantees. Use [OptIn] at the narrowest possible scope after
 * accepting that contract.
 *
 * ```kotlin
 * @OptIn(ExperimentalBluetapeApi::class)
 * fun callExperimentalApi() {
 *     // Use an experimental Bluetape declaration here.
 * }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This Bluetape API is experimental and may change or be removed without source or binary compatibility guarantees.",
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
annotation class ExperimentalBluetapeApi
