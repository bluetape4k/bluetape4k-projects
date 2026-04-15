package io.bluetape4k.spring.retrofit2

/**
 * 특정 Retrofit 클라이언트 이름에 매핑될 설정 클래스 배열을 담는 명세 객체다.
 *
 * ## 동작/계약
 * - Spring Cloud 의존성 없이 Spring Boot 4와 호환되는 독립 구현체다.
 * - [RetrofitClientsRegistrar]가 클라이언트별 설정 빈을 등록할 때 생성한다.
 * - [RetrofitClientContext]가 named context를 만들 때 이 값을 사용한다.
 *
 * ```kotlin
 * val spec = RetrofitClientSpecification(
 *     "jsonPlaceHolderApi",
 *     arrayOf(DefaultRetrofitClientConfiguration::class.java)
 * )
 * // spec.getName() == "jsonPlaceHolderApi"
 * ```
 */
class RetrofitClientSpecification(
    private val name: String,
    private val configs: Array<Class<*>>,
) {
    fun getName(): String = name

    fun getConfiguration(): Array<Class<*>> = configs
}
