package io.bluetape4k.r2dbc.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.r2dbc.R2dbcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient

/**
 * [R2dbcClient] 를 Bean으로 자동 등록해주는 Configuration
 */
@Configuration
@ConditionalOnClass(DatabaseClient::class)
class R2dbcClientAutoConfiguration {

    companion object: KLogging()

    /**
     * [R2dbcClient] Bean을 생성한다.
     */
    @Bean
    fun r2dbcClient(
        databaseClient: DatabaseClient,
        r2dbcEntityTemplate: R2dbcEntityTemplate,
        mappingR2dbcConverter: MappingR2dbcConverter,
    ): R2dbcClient {
        log.info {
            "Create R2dbcClient with " +
                    "databaseClient=$databaseClient, " +
                    "r2dbcEntityTemplate=$r2dbcEntityTemplate, " +
                    "mappingR2dbcConverter=$mappingR2dbcConverter"
        }
        return R2dbcClient(databaseClient, r2dbcEntityTemplate, mappingR2dbcConverter)
    }
}
