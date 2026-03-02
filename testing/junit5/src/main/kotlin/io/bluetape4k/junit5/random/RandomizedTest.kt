package io.bluetape4k.junit5.random

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 클래스/메서드에서 랜덤 값 주입 확장을 활성화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `@ExtendWith(RandomExtension::class)`를 적용합니다.
 * - [RandomValue]가 붙은 필드/파라미터를 대상으로 랜덤 값을 생성해 주입합니다.
 * - JUnit5 확장 체인에서 동작하므로 동일 대상에 다른 확장과 함께 사용할 수 있습니다.
 *
 * ```kotlin
 * @RandomizedTest
 * class UserTest {
 *   @Test fun `랜덤 문자열`(@RandomValue value: String) { /* ... */ }
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION
)
@MustBeDocumented
@Repeatable
@ExtendWith(RandomExtension::class)
annotation class RandomizedTest
