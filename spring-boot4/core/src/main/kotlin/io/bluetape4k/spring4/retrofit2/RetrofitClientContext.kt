package io.bluetape4k.spring4.retrofit2

import org.springframework.cloud.context.named.NamedContextFactory

/**
 * Retrofit 클라이언트별 자식 애플리케이션 컨텍스트를 관리한다.
 *
 * ## 동작/계약
 * - `NamedContextFactory`를 상속해 `RetrofitClientSpecification` 목록으로 named context를 생성한다.
 * - 기본 설정 타입은 [DefaultRetrofitClientConfiguration]이다.
 * - 클라이언트 이름 해석용 프로퍼티 키로 `retrofit2.client.name`을 사용한다.
 *
 * ```kotlin
 * val context = RetrofitClientContext()
 * context.setConfigurations(listOf(RetrofitClientSpecification("httpbin", emptyArray())))
 * val hasConfig = context.configurations.containsKey("httpbin")
 * // hasConfig == true
 * ```
 */
class RetrofitClientContext:
    NamedContextFactory<RetrofitClientSpecification>(
        DefaultRetrofitClientConfiguration::class.java,
        "retrofit2",
        "retrofit2.client.name"
    )
