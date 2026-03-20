package io.bluetape4k.spring4.retrofit2

import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass

/**
 * `@Retrofit2Client` 후보를 스캔해 Retrofit 클라이언트 빈 등록을 활성화한다.
 *
 * ## 동작/계약
 * - `@Import(RetrofitClientsRegistrar::class)`를 통해 registrar가 동작한다.
 * - `value`와 `basePackages`는 서로 alias이며 공백 패키지는 무시된다.
 * - `basePackageClasses`가 지정되면 각 타입의 패키지를 스캔 대상으로 사용한다.
 * - 모든 스캔 설정이 비어 있으면 이 애노테이션을 선언한 클래스의 패키지를 기본값으로 사용한다.
 *
 * ```kotlin
 * @SpringBootApplication
 * @EnableRetrofitClients(basePackageClasses = [HttpbinApi::class])
 * class App
 * // HttpbinApi 에 선언된 @Retrofit2Client 가 스캔 대상이 된다.
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@MustBeDocumented
@Import(RetrofitClientsRegistrar::class)
annotation class EnableRetrofitClients(
    /**
     * [basePackages]의 별칭이다.
     *
     * ## 동작/계약
     * - `@AliasFor("basePackages")`로 연결된다.
     * - 공백 문자열은 스캔 패키지 계산에서 제외된다.
     *
     * ```kotlin
     * @EnableRetrofitClients("io.bluetape4k.spring4.retrofit2.services")
     * class App
     * // value 값이 basePackages로 해석된다.
     * ```
     */
    @get:AliasFor("basePackages")
    val value: Array<String> = [],
    /**
     * `@Retrofit2Client`를 스캔할 기본 패키지 목록을 지정한다.
     *
     * ## 동작/계약
     * - `@AliasFor("value")`로 `value`와 동일한 속성이다.
     * - 공백 항목은 제외되고 유효한 패키지만 registrar에 전달된다.
     *
     * ```kotlin
     * @EnableRetrofitClients(basePackages = ["io.bluetape4k.spring4.retrofit2.services.httpbin"])
     * class App
     * // 지정한 패키지에서만 클라이언트 후보를 찾는다.
     * ```
     */
    @get:AliasFor("value")
    val basePackages: Array<String> = [],
    /**
     * 지정한 타입들의 패키지를 스캔 기준으로 사용한다.
     *
     * ## 동작/계약
     * - 각 클래스에 대해 `ClassUtils.getPackageName` 결과를 수집한다.
     * - `value`/`basePackages`와 함께 합집합으로 처리된다.
     *
     * ```kotlin
     * @EnableRetrofitClients(basePackageClasses = [HttpbinApi::class])
     * class App
     * // HttpbinApi 패키지가 스캔 대상에 추가된다.
     * ```
     */
    val basePackageClasses: Array<KClass<*>> = [],
    /**
     * 모든 Retrofit 클라이언트에 공통으로 적용할 기본 설정 클래스를 지정한다.
     *
     * ## 동작/계약
     * - 지정한 설정 타입 배열은 `RetrofitClientSpecification` 등록 시 기본 설정으로 사용된다.
     * - 개별 `@Retrofit2Client(configuration = ...)` 설정과 함께 컨텍스트 구성에 반영된다.
     *
     * ```kotlin
     * @EnableRetrofitClients(defaultConfiguration = [DefaultRetrofitClientConfiguration::class])
     * class App
     * // 모든 클라이언트 컨텍스트가 기본 설정을 공유한다.
     * ```
     */
    val defaultConfiguration: Array<KClass<*>> = [],
    /**
     * 직접 등록할 `@Retrofit2Client` 타입을 지정한다.
     *
     * ## 동작/계약
     * - 이 속성은 현재 registrar 구현에서 스캔 대체 로직으로 사용되지 않는다.
     * - 클라이언트 등록은 패키지 스캔 결과에 따라 결정된다.
     *
     * ```kotlin
     * @EnableRetrofitClients(clients = [HttpbinApi::class])
     * class App
     * // 현재 구현에서는 clients 값만으로 등록되지 않는다.
     * ```
     */
    val clients: Array<KClass<*>> = [],
)
