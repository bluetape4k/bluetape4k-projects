package io.bluetape4k.annotations

/**
 * Marks a Bluetape API with a delicate operational contract.
 *
 * Delicate APIs require careful understanding of their lifecycle, concurrency,
 * resource-management, or security behavior. Use [OptIn] after reviewing the
 * API documentation and its failure modes.
 *
 * ```kotlin
 * @OptIn(BluetapeDelicateApi::class)
 * fun callDelicateApi() {
 *     // Use a delicate Bluetape declaration here.
 * }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This Bluetape API is delicate and requires careful understanding of its concurrency, lifecycle, resource, or security contract.",
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
annotation class BluetapeDelicateApi
