package io.bluetape4k.exposed.utils

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

val logger = KotlinLogging.logger {}

fun withTables(
    vararg tables: Table,
    block: Transaction.() -> Unit,
) = transaction {
    runCatching {
        SchemaUtils.drop(*tables)
    }

    logger.info { "Create tables. ${tables.map { it.tableName }.joinToString()}" }
    SchemaUtils.create(*tables)
    try {
        this.block()
        commit()
    } finally {
        logger.info { "Drop tables. ${tables.map { it.tableName }.joinToString()}" }
        SchemaUtils.drop(*tables)
    }
}

suspend fun withSuspendedTables(
    vararg tables: Table,
    block: suspend Transaction.() -> Unit,
) = newSuspendedTransaction {
    runCatching {
        SchemaUtils.drop(*tables)
    }
    logger.info { "Create tables. ${tables.map { it.tableName }.joinToString()}" }
    SchemaUtils.create(*tables)
    try {
        this.block()
        commit()
    } finally {
        logger.info { "Drop tables. ${tables.map { it.tableName }.joinToString()}" }
        SchemaUtils.drop(*tables)
    }
}
