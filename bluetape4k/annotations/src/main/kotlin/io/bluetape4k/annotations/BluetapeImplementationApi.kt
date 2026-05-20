package io.bluetape4k.annotations

/**
 * Marks a Bluetape SPI whose use is stable but whose implementation is not.
 *
 * Apply this marker through [SubclassOptInRequired] on public interfaces,
 * abstract classes, or open classes that users may consume but should not
 * implement or subclass without an explicit opt-in.
 *
 * This marker is not a generic function or property opt-in marker. Use
 * [BluetapeExperimentalApi], [BluetapeBetaApi], [BluetapeInternalApi],
 * [BluetapeDelicateApi], or [BluetapeObsoleteApi] for ordinary use-site API
 * maturity markers.
 *
 * ```kotlin
 * @SubclassOptInRequired(BluetapeImplementationApi::class)
 * interface BluetapeSpi
 *
 * @OptIn(BluetapeImplementationApi::class)
 * class CustomBluetapeSpi : BluetapeSpi
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This Bluetape API is stable to use but not stable to implement or subclass.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
)
@MustBeDocumented
annotation class BluetapeImplementationApi
