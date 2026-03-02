package io.bluetape4k.junit5.faker

import kotlin.reflect.KClass

/**
 * 테스트 필드/파라미터에 DataFaker 값을 주입하기 위한 어노테이션입니다.
 *
 * ## 동작/계약
 * - [FakeValueExtension]이 `provider` 경로(`provider.method`)를 해석해 값을 생성합니다.
 * - 대상 타입이 컬렉션 계열이면 `size` 개수를 사용하고, 단일 타입이면 첫 값을 사용합니다.
 * - `provider` 형식이 잘못되거나 리플렉션 호출에 실패하면 실행 시 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * @Test
 * fun `faker 주입`(@FakeValue(FakeValueProvider.Name.FullName) name: String) {
 *   // name.isNotBlank() == true
 * }
 * ```
 *
 * @property provider `영역.함수` 형태의 provider 경로 (예: `name.fullName`)
 * @property size 컬렉션/시퀀스 계열 생성 개수
 * @property type 컬렉션 원소 타입 힌트
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
