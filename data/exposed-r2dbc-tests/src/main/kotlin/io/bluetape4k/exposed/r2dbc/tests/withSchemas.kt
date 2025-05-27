package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils

suspend fun withSchemas(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    statement: suspend R2dbcTransaction.() -> Unit,
) {
    withDb(dialect, configure = configure) {
        if (currentDialectTest.supportsCreateSchema) {
            SchemaUtils.createSchema(*schemas)
            try {
                statement()
                commit()     // Need commit to persist data before drop schemas
            } finally {
                SchemaUtils.dropSchema(*schemas, cascade = true)
                commit()
            }
        }
    }
}
