package io.bluetape4k.r2dbc.connection.init

import org.springframework.core.io.Resource
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

/**
 * [resource]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 */
fun resourceDatabasePopulatorOf(resource: Resource): ResourceDatabasePopulator =
    ResourceDatabasePopulator(resource)

/**
 * [resources]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 */
fun resourceDatabasePopulatorOf(vararg resources: Resource): ResourceDatabasePopulator =
    ResourceDatabasePopulator(*resources)

/**
 * [resources]를 사용하는 [ResourceDatabasePopulator]를 생성한다.
 */
fun resourceDatabasePopulatorOf(
    continueOnError: Boolean,
    ignoreFailedDrops: Boolean,
    sqlScriptEncoding: String? = null,
    vararg resources: Resource,
): ResourceDatabasePopulator =
    ResourceDatabasePopulator(continueOnError, ignoreFailedDrops, sqlScriptEncoding, *resources)
