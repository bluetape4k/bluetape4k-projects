package io.bluetape4k.spring4.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit

/**
 * Spring Boot 환경에서 Retrofit 클라이언트 인프라 빈을 자동 등록한다.
 *
 * ## 동작/계약
 * - `Retrofit` 클래스가 클래스패스에 있을 때만 활성화된다.
 * - 내부 `RetrofitClientConfiguration`이 `@EnableRetrofitClients`를 통해 client registrar를 켠다.
 * - `retrofitContext` 빈은 수집된 `RetrofitClientSpecification` 목록을 context에 반영한다.
 *
 * ```kotlin
 * val context = RetrofitClientContext()
 * val specs = listOf(RetrofitClientSpecification("httpbin", emptyArray()))
 * context.setConfigurations(specs)
 * // context.configurations.containsKey("httpbin") == true
 * ```
 */
@Configuration
@ConditionalOnClass(Retrofit::class)
class RetrofitAutoConfiguration {
    companion object: KLogging()

    /**
     * Retrofit 클라이언트 스캔을 활성화하는 내부 설정 클래스다.
     *
     * ## 동작/계약
     * - `@EnableRetrofitClients` 메타데이터만 제공하며 별도 빈을 선언하지 않는다.
     * - 실제 등록 로직은 `RetrofitClientsRegistrar`에서 처리한다.
     *
     * ```kotlin
     * @Configuration
     * @EnableRetrofitClients
     * class RetrofitClientConfiguration
     * // registrar가 @Retrofit2Client 인터페이스를 스캔한다.
     * ```
     */
    @Configuration
    @EnableRetrofitClients
    class RetrofitClientConfiguration

    /**
     * Retrofit 클라이언트용 named context 팩토리를 생성한다.
     *
     * ## 동작/계약
     * - `specs`가 null이 아니면 `setConfigurations(specs)`를 호출해 명세를 등록한다.
     * - `specs`가 null이면 기본 설정만 가진 빈 컨텍스트를 반환한다.
     *
     * ```kotlin
     * val specs = listOf(RetrofitClientSpecification("httpbin", emptyArray()))
     * val context = RetrofitClientContext().apply { setConfigurations(specs) }
     * // context.configurations.size == 1
     * ```
     */
    @Bean
    fun retrofitContext(specs: List<RetrofitClientSpecification>?): RetrofitClientContext {
        log.debug { "Create RetrofitClientContext ... specs=$specs" }

        return RetrofitClientContext().apply {
            specs?.let { setConfigurations(it) }
        }
    }
}
