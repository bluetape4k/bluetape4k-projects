package io.bluetape4k.exposed.utils

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

val logger = KotlinLogging.logger {}

inline fun runWithTables(
    vararg tables: Table,
    crossinline block: Transaction.() -> Unit,
) = transaction {
    logger.info { "Create tables. ${tables.map { it.tableName }.joinToString()}" }
    SchemaUtils.create(*tables)
    try {
        this.block()
    } finally {
        logger.info { "Drop tables. ${tables.map { it.tableName }.joinToString()}" }
        SchemaUtils.drop(*tables)
    }
}

suspend inline fun runSuspendWithTables(
    vararg tables: Table,
    crossinline block: suspend Transaction.() -> Unit,
) = newSuspendedTransaction {
    logger.info { "Create tables. ${tables.map { it.tableName }.joinToString()}" }
    SchemaUtils.create(*tables)
    try {
        this.block()
    } finally {
        logger.info { "Drop tables. ${tables.map { it.tableName }.joinToString()}" }
        SchemaUtils.drop(*tables)
    }
}
