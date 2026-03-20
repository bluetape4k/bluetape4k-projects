package io.bluetape4k.spring.webflux.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.LoopResources
import java.time.Duration

/**
 * WebFlux 서버 기본 리소스와 분리된 Netty 루프/커넥터로 [WebClient]를 구성하는 추상 설정입니다.
 *
 * ## 동작/계약
 * - [loopResources]와 [reactorResourceFactory]를 통해 `isUseGlobalResources = false`인 전용 Reactor 리소스를 사용합니다.
 * - [reactorClientHttpConnector]는 [sslContext], [responseTimeout], [connectTimeoutMillis] 설정을 클라이언트에 반영합니다.
 * - [exchangeStrategies]는 `defaultCodecs().maxInMemorySize(maxInMemorySize)`를 적용합니다.
 *
 * ```kotlin
 * @Configuration
 * class CustomWebClientConfig: AbstractWebClientConfig()
 * ```
 *
 * 참고: [Configuring Spring WebFlux WebClient to use a custom thread pool](https://stackoverflow.com/questions/56764801/configuring-spring-webflux-webclient-to-use-a-custom-thread-pool)
 */
abstract class AbstractWebClientConfig {
    companion object: KLoggingChannel()

    /**
     * WebClient 전용 이벤트 루프 스레드 수.
     */
    protected open val threadCount: Int = 4 * Runtimex.availableProcessors

    /**
     * 커넥션 타임아웃 (밀리초).
     */
    protected open val connectTimeoutMillis: Int = 5000

    /**
     * 응답 타임아웃.
     */
    protected open val responseTimeout: Duration = Duration.ofSeconds(3)

    /**
     * 루프 리소스 종료 대기 시간.
     */
    protected open val shutdownTimeout: Duration = Duration.ofSeconds(5)

    /**
     * Codec의 메모리 최대 크기.
     */
    protected open val maxInMemorySize: Int = 16 * 1024 * 1024

    /**
     * SSL 컨텍스트를 생성합니다.
     * 필요하면 재정의해서 보안 정책을 변경하세요.
     */
    protected open fun sslContext(): SslContext =
        SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()

    /**
     * WebClient 전용 [LoopResources] 빈을 생성합니다.
     *
     * ## 동작/계약
     * - [threadCount]가 0 이하이면 `IllegalArgumentException`을 발생시킵니다.
     * - `LoopResources.create("web-client-thread-", -1, threadCount, true, true)`를 사용해 worker 수를 고정합니다.
     *
     * ```kotlin
     * val loops = loopResources()
     * // loops는 "web-client-thread-" 접두사를 사용하는 전용 루프다.
     * ```
     */
    @Bean
    open fun loopResources(): LoopResources {
        require(threadCount > 0) { "threadCount는 1 이상이어야 합니다." }
        log.info { "Create custom LoopResources bean." }
        return LoopResources.create("web-client-thread-", -1, threadCount, true, true)
    }

    /**
     * 전용 루프 리소스를 사용하는 [ReactorResourceFactory] 빈을 생성합니다.
     *
     * ## 동작/계약
     * - 전달받은 [LoopResources]를 팩토리에 주입합니다.
     * - `isUseGlobalResources`를 `false`로 설정해 전역 Reactor 리소스를 사용하지 않습니다.
     * - 종료 대기 시간은 [shutdownTimeout]으로 설정합니다.
     *
     * ```kotlin
     * val factory = reactorResourceFactory(loopResources())
     * // factory.isUseGlobalResources == false
     * ```
     */
    @Bean
    open fun reactorResourceFactory(loopResources: LoopResources): ReactorResourceFactory {
        log.info { "Create custom ReactorResourceFactory bean." }
        return ReactorResourceFactory().apply {
            // loopResources 를 지정하지 않으면, ReactorResourceFactory 에서 기본 설정의 loopResources 를 생성한다.
            this.loopResources = loopResources
            this.isUseGlobalResources = false
            this.setShutdownTimeout(shutdownTimeout)
        }
    }

    /**
     * SSL/타임아웃/커넥션 옵션을 반영한 [ReactorClientHttpConnector] 빈을 생성합니다.
     *
     * ## 동작/계약
     * - [sslContext] 결과를 `client.secure`에 적용합니다.
     * - 응답 타임아웃은 [responseTimeout], 연결 타임아웃은 [connectTimeoutMillis]로 설정합니다.
     * - 리소스 관리는 인자로 받은 [ReactorResourceFactory]에 위임됩니다.
     *
     * ```kotlin
     * val connector = reactorClientHttpConnector(reactorResourceFactory(loopResources()))
     * ```
     */
    @Bean
    open fun reactorClientHttpConnector(factory: ReactorResourceFactory): ReactorClientHttpConnector {
        log.info { "Create ReactorClientHttpConnector bean." }
        val sslContext: SslContext = sslContext()

        return ReactorClientHttpConnector(factory) { client ->
            client.secure { spec ->
                spec.sslContext(sslContext)
            }
            client.responseTimeout(responseTimeout)
            client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
        }
    }

    /**
     * 최대 인메모리 버퍼 크기를 지정한 [ExchangeStrategies] 빈을 생성합니다.
     *
     * ## 동작/계약
     * - `defaultCodecs().maxInMemorySize(maxInMemorySize)`를 적용합니다.
     * - 인코더/디코더 기본 전략은 Spring 기본값을 유지하고 메모리 제한만 변경합니다.
     *
     * ```kotlin
     * val strategies = exchangeStrategies()
     * ```
     */
    @Bean
    open fun exchangeStrategies(): ExchangeStrategies {
        log.info { "Create ExchangeStrategies bean." }
        return ExchangeStrategies
            .builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)
            }.build()
    }

    /**
     * 지정한 커넥터와 코덱 전략을 사용해 [WebClient] 빈을 생성합니다.
     *
     * ## 동작/계약
     * - [ReactorClientHttpConnector]와 [ExchangeStrategies]를 빌더에 그대로 연결합니다.
     * - baseUrl, defaultHeader 같은 추가 설정은 이 메서드에서 적용하지 않습니다.
     *
     * ```kotlin
     * val client = webClient(reactorClientHttpConnector(reactorResourceFactory(loopResources())), exchangeStrategies())
     * ```
     */
    @Bean
    open fun webClient(
        connector: ReactorClientHttpConnector,
        exchangeStrategies: ExchangeStrategies,
    ): WebClient {
        log.info { "Create WebClient bean." }
        return WebClient
            .builder()
            .clientConnector(connector)
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}
