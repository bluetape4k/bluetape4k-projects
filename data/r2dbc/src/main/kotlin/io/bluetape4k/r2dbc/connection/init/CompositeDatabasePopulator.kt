package io.bluetape4k.r2dbc.connection.init

import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.DatabasePopulator

/**
 * [populators]를 합성하는 [CompositeDatabasePopulator]를 생성한다.
 */
fun compositeDatabasePopulatorOf(vararg populators: DatabasePopulator): CompositeDatabasePopulator =
    CompositeDatabasePopulator(*populators)

/**
 * [populators]를 합성하는 [CompositeDatabasePopulator]를 생성한다.
 */
fun compositeDatabasePopulatorOf(populators: Collection<DatabasePopulator>): CompositeDatabasePopulator =
    CompositeDatabasePopulator(populators)
