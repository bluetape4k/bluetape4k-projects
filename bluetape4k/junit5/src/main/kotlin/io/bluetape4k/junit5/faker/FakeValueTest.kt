package io.bluetape4k.junit5.faker

import org.junit.jupiter.api.extension.ExtendWith

/**
 * DataFaker 를 이용하여 Fake value 를 제공하는 테스트를 수행하도록 합니다.
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
 *
 * @see FakeValueExtension
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION
)
@MustBeDocumented
@Repeatable
@ExtendWith(FakeValueExtension::class)
annotation class FakeValueTest
