package io.bluetape4k.r2dbc.connection.init

import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.DatabasePopulator

/**
 * [populators]를 합성하는 [CompositeDatabasePopulator]를 생성한다.
 *
 * ```kotlin
 * val composite = compositeDatabasePopulatorOf(
 *     resourceDatabasePopulatorOf(ClassPathResource("schema.sql")),
 *     resourceDatabasePopulatorOf(ClassPathResource("data.sql"))
 * )
 * // composite.populators.size == 2
 * ```
 */
fun compositeDatabasePopulatorOf(vararg populators: DatabasePopulator): CompositeDatabasePopulator =
    CompositeDatabasePopulator(*populators)

/**
 * [populators]를 합성하는 [CompositeDatabasePopulator]를 생성한다.
 *
 * ```kotlin
 * val list = listOf(
 *     resourceDatabasePopulatorOf(ClassPathResource("schema.sql")),
 *     resourceDatabasePopulatorOf(ClassPathResource("data.sql"))
 * )
 * val composite = compositeDatabasePopulatorOf(list)
 * // composite.populators.size == 2
 * ```
 */
fun compositeDatabasePopulatorOf(populators: Collection<DatabasePopulator>): CompositeDatabasePopulator =
    CompositeDatabasePopulator(populators)
