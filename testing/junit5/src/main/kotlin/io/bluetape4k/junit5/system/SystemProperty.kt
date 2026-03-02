package io.bluetape4k.junit5.system

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 실행 동안 JVM 시스템 속성 하나를 임시로 설정합니다.
 *
 * ## 동작/계약
 * - [SystemPropertyExtension]이 테스트 시작 시 `name=value`를 설정하고 종료 시 원복합니다.
 * - 클래스/함수/파일/메타 어노테이션에 선언할 수 있습니다.
 * - 같은 이름이 중복 선언되면 마지막으로 적용된 값이 테스트 중 보이게 됩니다.
 *
 * ```kotlin
 * @SystemProperty(name = "feature.flag", value = "on")
 * class FeatureTest
 * // 테스트 중 System.getProperty("feature.flag") == "on"
 * ```
 *
 * @property name 설정할 시스템 속성 키
 * @property value 테스트 구간에서 사용할 시스템 속성 값
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
annotation class SystemProperty(
    val name: String,
    val value: String,
)
