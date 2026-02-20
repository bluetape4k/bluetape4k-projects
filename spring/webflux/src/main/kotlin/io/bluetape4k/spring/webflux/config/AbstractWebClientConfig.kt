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
 * [WebClient]를 Webflux 서버가 사용하는 ThreadPool을 사용하지 않고, 별도의 ThreadPool을 사용하도록 설정합니다.
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
     * 커스텀 [LoopResources]를 생성합니다.
     */
    @Bean
    open fun loopResources(): LoopResources {
        require(threadCount > 0) { "threadCount는 1 이상이어야 합니다." }
        log.info { "Create custom LoopResources bean." }
        return LoopResources.create("web-client-thread-", -1, threadCount, true, true)
    }

    /**
     * 커스텀 [ReactorResourceFactory]를 생성합니다.
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
     * 커스텀 [ReactorClientHttpConnector]를 생성합니다.
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
     * 커스텀 [ExchangeStrategies]를 생성합니다.
     */
    @Bean
    open fun exchangeStrategies(): ExchangeStrategies {
        log.info { "Create ExchangeStrategies bean." }
        return ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)
            }
            .build()
    }

    /**
     * 커스텀 [WebClient]를 생성합니다.
     */
    @Bean
    open fun webClient(connector: ReactorClientHttpConnector, exchangeStrategies: ExchangeStrategies): WebClient {
        log.info { "Create WebClient bean." }
        return WebClient.builder()
            .clientConnector(connector)
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}
