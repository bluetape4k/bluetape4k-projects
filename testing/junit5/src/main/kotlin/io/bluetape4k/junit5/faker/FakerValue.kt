package io.bluetape4k.junit5.faker

import kotlin.reflect.KClass

/**
 * 변수에 Fake 값을 주입하도록 합니다.
 *
 * ```
 * @FakeValueTest
 * class SomeClassTest {
 *     @Test
 *     fun `some test`(@FakeValue(FakeValueProvider.Name.FullName) name: String) {
 *          // ...
 *     }
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.ANNOTATION_CLASS
)
@MustBeDocumented
annotation class FakeValue(
    val provider: String = FakeValueProvider.Name.FullName,
    val size: Int = 1,
    val type: KClass<*> = Any::class,
)
