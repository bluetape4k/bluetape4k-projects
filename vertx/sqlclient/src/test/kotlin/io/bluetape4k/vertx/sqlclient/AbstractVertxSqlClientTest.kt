package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.utils.Resourcex
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCConnectOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.jdbcclient.jdbcConnectOptionsOf
import io.vertx.kotlin.mysqlclient.mySQLConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.mysqlclient.MySQLBuilder
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLConnection
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
abstract class AbstractVertxSqlClientTest {

    companion object: KLoggingChannel() {

        /**
         * Testcontainers 를 이용해 Loading 시 시간이 걸려서 Connection timeout 이 생깁니다. 무시하셔도 됩니다.
         */
        val mysql: MySQL8Server by lazy { MySQL8Server.Launcher.mysql }

        val MySQL8Server.connectOptions: MySQLConnectOptions
            get() = mySQLConnectOptionsOf(
                host = host,
                port = port,
                database = databaseName,
                user = username,
                password = password,
            )

        val h2ConnectOptions: JDBCConnectOptions by lazy {
            jdbcConnectOptionsOf(
                jdbcUrl = "jdbc:h2:mem:test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=FALSE;",
                user = "sa"
            )
        }

        private val defaultPoolOptions: PoolOptions = poolOptionsOf() // (maxSize = 4, shared = true, eventLoopSize = 2)

        fun Vertx.getMySQLPool(
            connectOptions: MySQLConnectOptions = mysql.connectOptions,
            poolOptions: PoolOptions = defaultPoolOptions,
        ): Pool {
            connectOptions.host.requireNotBlank("host")
            return MySQLBuilder
                .pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(this@getMySQLPool)
                .build()
        }

        fun Vertx.getH2Pool(
            connectOptions: JDBCConnectOptions = h2ConnectOptions,
            poolOptions: PoolOptions = defaultPoolOptions,
        ): Pool {
            return JDBCPool.pool(this, connectOptions, poolOptions)
        }
    }

    protected open fun Vertx.getPool(): Pool = getH2Pool() // getMySQLPool()

    protected abstract val schemaFileNames: List<String>

    protected lateinit var pool: Pool

    @BeforeAll
    fun setup(vertx: Vertx) = runSuspendTest(vertx.dispatcher()) {
        pool = vertx.getPool()

        log.debug { "Initialize database. pool=$pool, connect=${pool.connection.coAwait()}" }
        val dbType = if (pool.connection.coAwait() is MySQLConnection) "mysql" else "h2"
        pool.withSuspendTransaction { conn ->
            schemaFileNames.forEach { path ->
                log.debug { "dbType=$dbType, path=$path" }
                val query = Resourcex.getString("mybatis/schema/$dbType/$path")
                conn.query(query).execute().coAwait()
            }
        }
    }

    @AfterAll
    fun afterAll() {
        if (this::pool.isInitialized) {
            runBlocking { pool.close().coAwait() }
        }
    }
}
