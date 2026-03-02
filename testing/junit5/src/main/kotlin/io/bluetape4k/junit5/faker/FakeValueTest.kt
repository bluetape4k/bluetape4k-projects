package io.bluetape4k.junit5.faker

import org.junit.jupiter.api.extension.ExtendWith

/**
 * DataFaker 기반 값 주입 확장을 활성화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `@ExtendWith(FakeValueExtension::class)`를 적용합니다.
 * - [FakeValue] 어노테이션이 붙은 필드/파라미터에 faker 값을 주입합니다.
 * - provider 문자열 해석/호출 실패 시 확장 단계에서 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * @FakeValueTest
 * class UserTest {
 *   @Test fun `가짜 이름`(@FakeValue(FakeValueProvider.Name.FullName) name: String) {}
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
@ExtendWith(FakeValueExtension::class)
annotation class FakeValueTest
