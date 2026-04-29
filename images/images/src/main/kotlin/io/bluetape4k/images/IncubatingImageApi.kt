package io.bluetape4k.images

/**
 * 이 API는 incubating 상태입니다.
 *
 * ## 동작/계약
 * - 이 어노테이션이 붙은 클래스/함수/프로퍼티는 향후 변경되거나 제거될 수 있습니다.
 * - 사용 시 컴파일러 경고([RequiresOptIn.Level.WARNING])가 발생합니다.
 * - 억제하려면 `@OptIn(IncubatingImageApi::class)`를 사용하세요.
 *
 * ```kotlin
 * @OptIn(IncubatingImageApi::class)
 * val writer: AvifWriter = MyAvifWriterImpl()
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "이 API는 incubating 상태입니다. 향후 변경되거나 제거될 수 있으며 바이너리 호환성을 보장하지 않습니다. @OptIn(IncubatingImageApi::class)로 경고를 억제하세요."
)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
annotation class IncubatingImageApi
