package io.bluetape4k.spring.webflux.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.LoopResources
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [WebClient]를 Webflux 서버가 사용하는 ThreadPool 을 사용하지 않고, 별도의 ThreadPool 을 사용하도록 설정합니다.
 *
 * 참고: [Configuring Spring WebFlux WebClient to use a custom thread pool](https://stackoverflow.com/questions/56764801/configuring-spring-webflux-webclient-to-use-a-custom-thread-pool)
 *
 */
abstract class AbstractWebClientConfig {

    companion object: KLogging()

    protected open val threadCount: Int = Runtimex.availableProcessors

    protected open val webClientThreadFactory: BasicThreadFactory =
        BasicThreadFactory.Builder()
            .namingPattern("web-client-thread-%d")
            .daemon(true)
            .priority(Thread.MAX_PRIORITY)
            .build()

    protected open fun getWebClientExecutor(): ExecutorService {
        return Executors.newFixedThreadPool(threadCount, webClientThreadFactory)
    }

    @Bean
    open fun getEventLoopGroup(): NioEventLoopGroup {
        log.info { "Create custom NioEventLoopGroup. threadCount=$threadCount " }
        return NioEventLoopGroup(threadCount, webClientThreadFactory)
    }

    @Bean
    open fun reactorResourceFactory(eventLoopGroup: NioEventLoopGroup): ReactorResourceFactory {
        log.info { "Create custom ReactorResourceFactory bean." }
        return ReactorResourceFactory().apply {
            loopResources = LoopResources { eventLoopGroup }
            isUseGlobalResources = false
            this.setShutdownTimeout(Duration.ofSeconds(3))
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
