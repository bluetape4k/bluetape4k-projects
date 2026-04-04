package io.bluetape4k.r2dbc.connection.init

import org.springframework.core.io.Resource
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

/**
 * [resource]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 *
 * ```kotlin
 * val populator = resourceDatabasePopulatorOf(ClassPathResource("schema.sql"))
 * // populator != null
 * ```
 */
fun resourceDatabasePopulatorOf(resource: Resource): ResourceDatabasePopulator =
    ResourceDatabasePopulator(resource)

/**
 * [resources]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 *
 * ```kotlin
 * val populator = resourceDatabasePopulatorOf(
 *     ClassPathResource("schema.sql"),
 *     ClassPathResource("data.sql")
 * )
 * // populator != null
 * ```
 */
fun resourceDatabasePopulatorOf(vararg resources: Resource): ResourceDatabasePopulator =
    ResourceDatabasePopulator(*resources)

/**
 * [resources]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 *
 * ```kotlin
 * val populator = resourceDatabasePopulatorOf(
 *     continueOnError = true,
 *     ignoreFailedDrops = true,
 *     sqlScriptEncoding = "UTF-8",
 *     ClassPathResource("schema.sql")
 * )
 * // populator != null
 * ```
 */
fun resourceDatabasePopulatorOf(
    continueOnError: Boolean,
    ignoreFailedDrops: Boolean,
    sqlScriptEncoding: String? = null,
    vararg resources: Resource,
): ResourceDatabasePopulator =
    ResourceDatabasePopulator(
        continueOnError,
        ignoreFailedDrops,
        sqlScriptEncoding,
        *resources
    )
