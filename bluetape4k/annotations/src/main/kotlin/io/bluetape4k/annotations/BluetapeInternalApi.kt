package io.bluetape4k.annotations

/**
 * Marks a Bluetape API that is public only for technical reasons.
 *
 * Internal APIs are not intended for external callers and may change without
 * compatibility guarantees. Use [OptIn] only inside tightly controlled
 * integration code that accepts that maintenance cost.
 *
 * ```kotlin
 * @OptIn(BluetapeInternalApi::class)
 * fun bridgeInternalApi() {
 *     // Use a public-for-technical-reasons declaration here.
 * }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This Bluetape API is public only for technical reasons and is not intended for external use.",
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
annotation class BluetapeInternalApi
