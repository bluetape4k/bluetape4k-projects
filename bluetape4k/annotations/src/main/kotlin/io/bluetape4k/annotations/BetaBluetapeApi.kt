package io.bluetape4k.annotations

/**
 * Marks a Bluetape API that is in beta.
 *
 * Beta APIs are intended to stabilize, but minor source, binary, or behavior
 * changes are still possible. Use [OptIn] when adopting the API before it is
 * promoted to the stable surface.
 *
 * ```kotlin
 * @OptIn(BetaBluetapeApi::class)
 * fun callBetaApi() {
 *     // Use a beta Bluetape declaration here.
 * }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This Bluetape API is beta. It is intended to stabilize, but minor source, binary, or behavior changes are still possible.",
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
annotation class BetaBluetapeApi
