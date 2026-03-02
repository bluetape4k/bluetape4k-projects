package io.bluetape4k.spring.retrofit2

import org.springframework.cloud.context.named.NamedContextFactory

/**
 * 특정 Retrofit 클라이언트 이름에 매핑될 설정 클래스 배열을 담는 명세 객체다.
 *
 * ## 동작/계약
 * - `NamedContextFactory.Specification` 구현체로서 `getName()`과 `getConfiguration()` 값을 그대로 반환한다.
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
): NamedContextFactory.Specification {

    override fun getName(): String = name

    override fun getConfiguration(): Array<Class<*>> = configs

}
