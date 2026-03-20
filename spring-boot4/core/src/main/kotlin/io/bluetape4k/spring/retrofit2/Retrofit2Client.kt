package io.bluetape4k.spring.retrofit2

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

/**
 * Retrofit 서비스 인터페이스를 Spring 빈으로 등록하기 위한 선언 애노테이션이다.
 *
 * ## 동작/계약
 * - `@Service` 메타 애노테이션으로 컴포넌트 스캔 대상이 된다.
 * - `RetrofitClientsRegistrar`가 이 애노테이션 메타데이터(`name`, `baseUrl`, `configuration`)를 읽어 FactoryBean을 등록한다.
 * - 실제 Retrofit 인스턴스 생성은 `RetrofitClientFactoryBean`에서 수행된다.
 *
 * ```kotlin
 * @Retrofit2Client(
 *     name = "httpbin",
 *     baseUrl = "\${bluetape4k.retrofit2.services.httpbin}"
 * )
 * interface HttpbinApi
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@MustBeDocumented
@Service
annotation class Retrofit2Client(
    /**
     * 클라이언트 식별자이며 [name]과 같은 속성이다.
     *
     * ## 동작/계약
     * - `@AliasFor("name")`로 [name]과 동일 값으로 처리된다.
     * - 빈 이름과 named context 식별자 계산에 사용된다.
     *
     * ```kotlin
     * @Retrofit2Client(
     *     value = "jsonPlaceHolderApi",
     *     baseUrl = "\${bluetape4k.retrofit2.services.jsonPlaceHolder}"
     * )
     * interface JsonPlaceHolderApi
     * ```
     */
    @get:AliasFor("name")
    val value: String = "",
    /**
     * [value]의 별칭이다.
     *
     * ## 동작/계약
     * - `@AliasFor("value")`로 연결된다.
     * - registrar는 `name` 속성 값을 기준으로 bean definition 이름을 등록한다.
     *
     * ```kotlin
     * @Retrofit2Client(
     *     name = "httpbin",
     *     baseUrl = "\${bluetape4k.retrofit2.services.httpbin}"
     * )
     * interface HttpbinApi
     * ```
     */
    @get:AliasFor("value")
    val name: String = "",
    /**
     * 스프링 `@Qualifier`와 함께 사용할 구분 이름이다.
     *
     * ## 동작/계약
     * - 현재 registrar/factory 구현에서는 qualifier 값을 읽거나 빈 등록에 사용하지 않는다.
     * - 애노테이션 메타데이터로만 보존된다.
     *
     * ```kotlin
     * @Retrofit2Client(
     *     name = "httpbin",
     *     qualifier = "external-http",
     *     baseUrl = "\${bluetape4k.retrofit2.services.httpbin}"
     * )
     * interface HttpbinApi
     * ```
     */
    val qualifier: String = "",
    /**
     * 호출 대상 REST API의 Base URL을 지정한다.
     *
     * ## 동작/계약
     * - `Retrofit.Builder().baseUrl(baseUrl)`에 그대로 전달된다.
     * - `RetrofitClientFactoryBean.afterPropertiesSet()`에서 빈 문자열 여부를 검증한다.
     *
     * ```kotlin
     * @Retrofit2Client(
     *     name = "jsonPlaceHolderApi",
     *     baseUrl = "\${bluetape4k.retrofit2.services.jsonPlaceHolder}"
     * )
     * interface JsonPlaceHolderApi
     * ```
     */
    val baseUrl: String = "",
    /**
     * 클라이언트 전용 Spring 설정 클래스를 추가한다.
     *
     * ## 동작/계약
     * - registrar가 `RetrofitClientSpecification(name, configuration)`으로 등록한다.
     * - `RetrofitClientContext`에서 named child context를 만들 때 해당 설정이 적용된다.
     *
     * ```kotlin
     * @Retrofit2Client(
     *     name = "httpbin",
     *     baseUrl = "\${bluetape4k.retrofit2.services.httpbin}",
     *     configuration = [DefaultRetrofitClientConfiguration::class]
     * )
     * interface HttpbinApi
     * ```
     */
    val configuration: Array<KClass<*>> = [],
)
