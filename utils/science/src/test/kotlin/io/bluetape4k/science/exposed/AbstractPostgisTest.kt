package io.bluetape4k.science.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PostgisServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class AbstractPostgisTest {

    companion object: KLogging() {
        @JvmStatic
        protected val postgisServer by lazy { PostgisServer.Launcher.postgis }

        @JvmStatic
        protected val db: Database by lazy {
            Database.connect(
                url = postgisServer.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgisServer.username!!,
                password = postgisServer.password!!,
            )
        }

        private val gisTables = arrayOf(
            NetCdfFileTable,
            NetCdfGridValueTable,
            PoiTable,
            SpatialLayerTable,
            SpatialFeatureTable,
        )
    }

    @BeforeAll
    fun beforeAll() {
        transaction(db) {
            SchemaUtils.create(*gisTables)
        }
    }

    @AfterAll
    fun afterAll() {
        transaction(db) {
            runCatching {
                SchemaUtils.drop(*gisTables)
            }
        }
    }
}
