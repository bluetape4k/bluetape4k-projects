package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 애플리케이션 시작 시 fixture 데이터를 로드하는 설정.
 *
 * AoT(Ahead-of-Time) 호환을 위해 `@PostConstruct` 대신 [ApplicationRunner]를 사용한다.
 * Spring Boot의 AoT 처리 단계에서 `@PostConstruct` 는 프록시 관련 문제가 발생할 수 있다.
 */
@Configuration
class ApplicationBootstrapConfig {
    companion object : KLogging()

    /**
     * 애플리케이션 기동 후 jsonplaceholder fixture 데이터를 자동으로 로드한다.
     *
     * @param service jsonplaceholder 데이터 서비스
     * @return fixture 데이터를 로드하는 [ApplicationRunner] 빈
     */
    @Bean
    fun bootstrapJsonplaceholder(service: JsonplaceholderService): ApplicationRunner =
        ApplicationRunner {
            log.info { "Bootstrapping jsonplaceholder fixtures..." }
            service.reloadFromFixtures()
        }
}
