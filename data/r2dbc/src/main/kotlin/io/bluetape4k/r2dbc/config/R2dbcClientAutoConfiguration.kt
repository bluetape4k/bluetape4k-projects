package io.bluetape4k.r2dbc.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.r2dbc.R2dbcClient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient

/**
 * Auto-configuration that registers the default [R2dbcClient] bean.
 *
 * ## Contract
 * - Activates only when all Spring R2DBC types used by the bean signature are present.
 * - Backs off when the application already provides an [R2dbcClient] bean.
 */
@AutoConfiguration
@ConditionalOnClass(
    name = [
        "org.springframework.r2dbc.core.DatabaseClient",
        "org.springframework.data.r2dbc.core.R2dbcEntityTemplate",
        "org.springframework.data.r2dbc.convert.MappingR2dbcConverter",
    ]
)
class R2dbcClientAutoConfiguration {

    companion object: KLogging()

    /**
     * Spring R2DBC infrastructure bean에서 기본 [R2dbcClient] bean을 생성합니다.
     */
    @Bean
    @ConditionalOnMissingBean(R2dbcClient::class)
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
