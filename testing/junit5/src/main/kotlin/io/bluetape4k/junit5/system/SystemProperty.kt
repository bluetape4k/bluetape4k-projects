package io.bluetape4k.junit5.system

import org.junit.jupiter.api.extension.ExtendWith

/**
 * 테스트 시에 시스템 속성을 지정하고, 테스트 후에는 원복한다
 *
 * ```
 * @SystemProperty(name="nameA", value="valueA")
 * @Test
 * fun test() {
 *     System.getProperty("nameA") // "valueA"
 * }
 * ```
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
