package io.bluetape4k.junit5.system

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 실행 동안 적용할 시스템 속성 여러 개를 한 번에 선언합니다.
 *
 * ## 동작/계약
 * - [SystemPropertyExtension]이 `value` 배열을 순회해 모두 적용/복원합니다.
 * - 단일 속성은 [SystemProperty], 다중 속성은 [SystemProperties]로 표현할 수 있습니다.
 * - 같은 키가 여러 번 등장하면 순회 마지막 값이 테스트 중 유효합니다.
 *
 * ```kotlin
 * @SystemProperties(
 *   value = [SystemProperty("k1", "v1"), SystemProperty("k2", "v2")]
 * )
 * class PropertyTest
 * // 테스트 중 k1=="v1", k2=="v2"
 * ```
 *
 * @property value 적용할 시스템 속성 목록
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@MustBeDocumented
@Repeatable
@ExtendWith(SystemPropertyExtension::class)
annotation class SystemProperties(
    val value: Array<SystemProperty>,
)
