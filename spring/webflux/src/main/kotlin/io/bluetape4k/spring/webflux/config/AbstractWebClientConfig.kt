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
 * [WebClient]를 Webflux 서버가 사용하는 ThreadPool 을 사용하지 않고, 별도의 ThreadPool 을 사용하도록 설정합니다.
 *
 * 참고: [Configuring Spring WebFlux WebClient to use a custom thread pool](https://stackoverflow.com/questions/56764801/configuring-spring-webflux-webclient-to-use-a-custom-thread-pool)
 *
 */
abstract class AbstractWebClientConfig {

    companion object: KLoggingChannel()

    protected open val threadCount: Int = 4 * Runtimex.availableProcessors

    @Bean
    open fun loopResources(): LoopResources {
        log.info { "Create custom LoopResources bean." }
        return LoopResources.create("web-client-thread-", -1, threadCount, true, true)
    }

    @Bean
    open fun reactorResourceFactory(loopResources: LoopResources): ReactorResourceFactory {
        log.info { "Create custom ReactorResourceFactory bean." }
        return ReactorResourceFactory().apply {
            // loopResources 를 지정하지 않으면, ReactorResourceFactory 에서 기본 설정의 loopResources 를 생성한다.
            this.loopResources = loopResources
            this.isUseGlobalResources = false
            this.setShutdownTimeout(Duration.ofSeconds(5))
        }
    }

    // @Suppress("DEPRECATION")
    @Bean
    open fun reactorClientHttpConnector(factory: ReactorResourceFactory): ReactorClientHttpConnector {
        log.info { "Create ReactorClientHttpConnector bean." }
        val sslContext: SslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()

        return ReactorClientHttpConnector(factory) { client ->
            client.secure { spec ->
                spec.sslContext(sslContext)
            }
            client.responseTimeout(Duration.ofSeconds(3))
            client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        }
    }

    @Bean
    open fun exchangeStrategies(): ExchangeStrategies {
        log.info { "Create ExchangeStrategies bean." }
        return ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) // 16MB
            }
            .build()
    }

    @Bean
    open fun webClient(connector: ReactorClientHttpConnector, exchangeStrategies: ExchangeStrategies): WebClient {
        log.info { "Create WebClient bean." }
        return WebClient.builder()
            .clientConnector(connector)
            .exchangeStrategies(exchangeStrategies)

            .build()
    }
}
