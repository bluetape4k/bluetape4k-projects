package io.bluetape4k.jdbc.datasource

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.Properties
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class RefreshingJdbcPasswordDataSourceTest {

    @Test
    fun `getConnection opens H2 connection and calls password provider`() {
        val calls = AtomicInteger()
        val dataSource = RefreshingJdbcPasswordDataSource(
            config = RefreshingJdbcPasswordDataSourceConfig(
                url = "jdbc:h2:mem:refreshing_${System.nanoTime()};DB_CLOSE_DELAY=-1",
                driverClassName = "org.h2.Driver",
                username = "sa",
            ),
            passwordProvider = JdbcPasswordProvider {
                calls.incrementAndGet()
                ""
            },
        )

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT 1").use { rs ->
                    rs.next().shouldBeTrue()
                    rs.getInt(1) shouldBeEqualTo 1
                }
            }
        }

        calls.get() shouldBeEqualTo 1
    }

    @Test
    fun `getConnection invokes provider for each physical connection`() {
        val driver = RecordingDriver()

        withRegisteredDriver(driver) {
            val calls = AtomicInteger()
            val dataSource = dataSourceFor(driver.url) {
                "token-${calls.incrementAndGet()}"
            }

            dataSource.connection.close()
            dataSource.connection.close()

            calls.get() shouldBeEqualTo 2
            driver.captures.map { it.properties.getProperty("password") } shouldBeEqualTo listOf("token-1", "token-2")
        }
    }

    @Test
    fun `caller supplied credential overload is rejected without invoking provider`() {
        val calls = AtomicInteger()
        val dataSource = dataSourceFor("jdbc:bluetape4k-refresh-test:rejected") {
            calls.incrementAndGet()
            "provider-secret"
        }

        val ex = assertFailsWith<SQLException> {
            dataSource.getConnection("caller-user", "caller-password")
        }

        ex.message shouldBeEqualTo "Refreshing JDBC password data source does not accept caller-supplied credentials."
        ex.message shouldNotContain "caller-user"
        ex.message shouldNotContain "caller-password"
        calls.get() shouldBeEqualTo 0
    }

    @Test
    fun `null password throws configured secret free SQLException`() {
        val dataSource = RefreshingJdbcPasswordDataSource(
            config = RefreshingJdbcPasswordDataSourceConfig(
                url = "jdbc:bluetape4k-refresh-test:null?password=url-secret&token=url-token",
                username = "db-user",
                dataSourceProperties = mapOf("sslpassword" to "property-secret"),
                nullPasswordMessage = "Password provider returned no value.",
            ),
            passwordProvider = JdbcPasswordProvider { null },
        )

        val ex = assertFailsWith<SQLException> {
            dataSource.connection
        }

        ex.message shouldBeEqualTo "Password provider returned no value."
        ex.message shouldNotContain "url-secret"
        ex.message shouldNotContain "url-token"
        ex.message shouldNotContain "property-secret"
    }

    @Test
    fun `config validation rejects blank values`() {
        assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSourceConfig(url = " ", username = "user")
        }
        assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSourceConfig(url = "jdbc:test", username = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSourceConfig(url = "jdbc:test", username = "user", driverClassName = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSourceConfig(url = "jdbc:test", username = "user", nullPasswordMessage = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSourceConfig(
                url = "jdbc:test",
                username = "user",
                dataSourceProperties = mapOf(" " to "value"),
            )
        }
    }

    @Test
    fun `properties are fresh per call and generated credentials override base entries`() {
        val driver = RecordingDriver()

        withRegisteredDriver(driver) {
            val calls = AtomicInteger()
            val dataSource = RefreshingJdbcPasswordDataSource(
                config = RefreshingJdbcPasswordDataSourceConfig(
                    url = driver.url,
                    username = "constructor-user",
                    dataSourceProperties = mapOf(
                        "user" to "stale-user",
                        "password" to "stale-password",
                        "vendorOption" to "vendor-value",
                    ),
                ),
                passwordProvider = JdbcPasswordProvider {
                    "provider-password-${calls.incrementAndGet()}"
                },
            )

            dataSource.connection.close()
            dataSource.connection.close()

            val captures = driver.captures.toList()
            captures.size shouldBeEqualTo 2
            captures.map { it.identityHash }.toSet().size shouldBeEqualTo 2
            captures[0].properties.getProperty("user") shouldBeEqualTo "constructor-user"
            captures[0].properties.getProperty("password") shouldBeEqualTo "provider-password-1"
            captures[0].properties.getProperty("vendorOption") shouldBeEqualTo "vendor-value"
            captures[1].properties.getProperty("user") shouldBeEqualTo "constructor-user"
            captures[1].properties.getProperty("password") shouldBeEqualTo "provider-password-2"
            captures[1].properties.getProperty("driverMutation") shouldBeEqualTo null
        }
    }

    @Test
    fun `concurrent getConnection calls use isolated properties and provider invocations`() {
        val driver = RecordingDriver()

        withRegisteredDriver(driver) {
            val calls = AtomicInteger()
            val dataSource = dataSourceFor(driver.url) {
                "token-${calls.incrementAndGet()}"
            }

            MultithreadingTester()
                .workers(8)
                .rounds(4)
                .add { dataSource.connection.close() }
                .run()

            val expectedCalls = 32
            calls.get() shouldBeEqualTo expectedCalls
            driver.captures.size shouldBeEqualTo expectedCalls
            driver.captures.map { it.identityHash }.toSet().size shouldBeEqualTo expectedCalls
        }
    }

    @Test
    fun `wrapper methods only unwrap the helper instance`() {
        val dataSource = dataSourceFor("jdbc:bluetape4k-refresh-test:wrapper") { "token" }

        dataSource.isWrapperFor(RefreshingJdbcPasswordDataSource::class.java).shouldBeTrue()
        dataSource.unwrap(RefreshingJdbcPasswordDataSource::class.java) shouldBeEqualTo dataSource

        val ex = assertFailsWith<SQLException> {
            dataSource.unwrap(Connection::class.java)
        }

        ex.message shouldContain "Not a wrapper for java.sql.Connection."
    }

    @Test
    fun `toString is sanitized and does not call provider`() {
        val calls = AtomicInteger()
        val dataSource = RefreshingJdbcPasswordDataSource(
            config = RefreshingJdbcPasswordDataSourceConfig(
                url = "jdbc:postgresql://url-user:url-password@localhost:5432/app?password=query-secret&token=query-token&sslpassword=ssl-secret",
                username = "db-user",
            ),
            passwordProvider = JdbcPasswordProvider {
                calls.incrementAndGet()
                "provider-secret"
            },
        )

        val value = dataSource.toString()

        value shouldContain "RefreshingJdbcPasswordDataSource"
        value shouldContain "username=<redacted>"
        value shouldNotContain "db-user"
        value shouldNotContain "url-password"
        value shouldNotContain "query-secret"
        value shouldNotContain "query-token"
        value shouldNotContain "ssl-secret"
        value shouldNotContain "provider-secret"
        value shouldNotContain "password="
        value shouldNotContain "token="
        calls.get() shouldBeEqualTo 0
    }

    @Test
    fun `provider failure is wrapped in secret free SQLException`() {
        val failure = IllegalStateException("provider exploded with provider-secret")
        val dataSource = dataSourceFor("jdbc:bluetape4k-refresh-test:provider-failure") {
            throw failure
        }

        val ex = assertFailsWith<SQLException> {
            dataSource.connection
        }

        ex.cause shouldBeEqualTo failure
        ex.message shouldNotContain "provider-secret"
    }

    @Test
    fun `driver class load failure does not include credentials`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            RefreshingJdbcPasswordDataSource(
                config = RefreshingJdbcPasswordDataSourceConfig(
                    url = "jdbc:bluetape4k-refresh-test:driver?password=url-secret",
                    driverClassName = "missing.DriverClass",
                    username = "user",
                    dataSourceProperties = mapOf("password" to "property-secret"),
                ),
                passwordProvider = JdbcPasswordProvider { "provider-secret" },
            )
        }

        ex.message shouldContain "missing.DriverClass"
        ex.message shouldNotContain "url-secret"
        ex.message shouldNotContain "property-secret"
        ex.message shouldNotContain "provider-secret"
    }

    @Test
    fun `DriverManager global log writer and login timeout are delegated and restored`() {
        val dataSource = dataSourceFor("jdbc:bluetape4k-refresh-test:globals") { "token" }
        val originalWriter = DriverManager.getLogWriter()
        val originalTimeout = DriverManager.getLoginTimeout()
        val writer = PrintWriter(System.err)

        try {
            dataSource.logWriter = writer
            dataSource.loginTimeout = 7

            DriverManager.getLogWriter() shouldBeEqualTo writer
            dataSource.logWriter shouldBeEqualTo writer
            DriverManager.getLoginTimeout() shouldBeEqualTo 7
            dataSource.loginTimeout shouldBeEqualTo 7
        } finally {
            DriverManager.setLogWriter(originalWriter)
            DriverManager.setLoginTimeout(originalTimeout)
        }
    }

    private fun dataSourceFor(
        url: String,
        passwordProvider: () -> String?,
    ): RefreshingJdbcPasswordDataSource =
        RefreshingJdbcPasswordDataSource(
            config = RefreshingJdbcPasswordDataSourceConfig(
                url = url,
                username = "db-user",
            ),
            passwordProvider = JdbcPasswordProvider(passwordProvider),
        )

    private fun withRegisteredDriver(
        driver: Driver,
        block: () -> Unit,
    ) {
        DriverManager.registerDriver(driver)
        try {
            block()
        } finally {
            DriverManager.deregisterDriver(driver)
        }
    }

    private class RecordingDriver(
        val url: String = "jdbc:bluetape4k-refresh-test:${System.nanoTime()}",
    ): Driver {

        val captures = ConcurrentLinkedQueue<CapturedProperties>()

        override fun acceptsURL(url: String?): Boolean =
            url == this.url

        override fun connect(url: String?, info: Properties?): Connection? {
            if (!acceptsURL(url)) {
                return null
            }

            requireNotNull(info)
            val copy = Properties()
            copy.putAll(info)
            captures.add(CapturedProperties(System.identityHashCode(info), copy))
            info["driverMutation"] = "mutated-by-driver"

            return connectionProxy()
        }

        override fun getMajorVersion(): Int = 1

        override fun getMinorVersion(): Int = 0

        override fun getPropertyInfo(url: String?, info: Properties?): Array<DriverPropertyInfo> = emptyArray()

        override fun jdbcCompliant(): Boolean = false

        override fun getParentLogger(): Logger =
            throw SQLFeatureNotSupportedException("parent logger")

        private fun connectionProxy(): Connection =
            Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java),
            ) { proxy, method, _ ->
                when (method.name) {
                    "close" -> Unit
                    "isClosed" -> false
                    "toString" -> "RecordingConnection"
                    "unwrap" -> unwrap(proxy, method)
                    "isWrapperFor" -> method.parameterTypes.single().isInstance(proxy)
                    else -> defaultValue(method)
                }
            } as Connection

        private fun unwrap(proxy: Any, method: java.lang.reflect.Method): Any {
            val iface = method.parameterTypes.single()
            if (iface.isInstance(proxy)) {
                return proxy
            }
            throw SQLException("Not a wrapper for ${iface.name}.")
        }

        private fun defaultValue(method: java.lang.reflect.Method): Any? =
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0.0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> '\u0000'
                java.lang.Void.TYPE -> Unit
                else -> null
            }
    }

    private data class CapturedProperties(
        val identityHash: Int,
        val properties: Properties,
    )
}
