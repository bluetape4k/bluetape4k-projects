package io.bluetape4k.jdbc.datasource

import io.bluetape4k.support.requireNotBlank
import java.io.PrintWriter
import java.io.Serializable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Provides the current JDBC password for a physical connection attempt.
 *
 * ## Contract
 *
 * [RefreshingJdbcPasswordDataSource] calls [currentPassword] synchronously for
 * every no-arg [DataSource.getConnection] invocation. Implementations must be
 * thread-safe because connection pools may open multiple physical connections
 * concurrently. Expensive token generation, caching, and coalescing belong in
 * the provider implementation, not in the generic DataSource.
 *
 * Implementations must not log or expose secrets. Exceptions thrown by the
 * provider are reported as secret-free [SQLException] failures by the
 * DataSource.
 */
fun interface JdbcPasswordProvider {

    /**
     * Returns the current JDBC password or null when no password is available.
     */
    fun currentPassword(): String?
}

/**
 * Configuration for [RefreshingJdbcPasswordDataSource].
 *
 * @property url JDBC URL used with [DriverManager.getConnection].
 * @property driverClassName optional driver class to load during construction.
 * @property username JDBC user name written to each per-call [Properties].
 * @property dataSourceProperties vendor-specific JDBC properties copied into
 * each per-call [Properties] before `user` and `password` are written.
 * @property nullPasswordMessage secret-free [SQLException] message used when
 * [JdbcPasswordProvider.currentPassword] returns null.
 */
data class RefreshingJdbcPasswordDataSourceConfig(
    val url: String,
    val driverClassName: String? = null,
    val username: String,
    val dataSourceProperties: Map<String, String> = emptyMap(),
    val nullPasswordMessage: String = "JDBC password provider returned null.",
): Serializable {

    init {
        url.requireNotBlank("url")
        username.requireNotBlank("username")
        driverClassName?.requireNotBlank("driverClassName")
        nullPasswordMessage.requireNotBlank("nullPasswordMessage")
        dataSourceProperties.keys.forEach { key ->
            key.requireNotBlank("dataSourceProperties key")
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * A [DataSource] that obtains a fresh password for each physical connection.
 *
 * ## Behavior
 *
 * This DataSource builds a fresh JDBC [Properties] object for every no-arg
 * [getConnection] call, copies configured vendor properties, writes `user`,
 * obtains the current password, writes `password`, and delegates to
 * [DriverManager.getConnection].
 *
 * Caller-supplied credentials are rejected because they would bypass the
 * refresh contract. This class is not a connection pool, does not schedule
 * refreshes, and does not own returned [Connection] instances or provider
 * lifecycle.
 *
 * [logWriter] and [loginTimeout] delegate to process-wide [DriverManager]
 * state. They are not per-instance settings.
 *
 * ```kotlin
 * val dataSource = RefreshingJdbcPasswordDataSource(
 *     config = RefreshingJdbcPasswordDataSourceConfig(
 *         url = "jdbc:postgresql://localhost:5432/app",
 *         username = "app",
 *     ),
 *     passwordProvider = JdbcPasswordProvider { currentToken() },
 * )
 * ```
 */
class RefreshingJdbcPasswordDataSource(
    config: RefreshingJdbcPasswordDataSourceConfig,
    private val passwordProvider: JdbcPasswordProvider,
): DataSource {

    private val url: String = config.url
    private val username: String = config.username
    private val dataSourceProperties: Map<String, String> = config.dataSourceProperties.toMap()
    private val nullPasswordMessage: String = config.nullPasswordMessage
    private val urlSummary: String = sanitizeUrl(config.url)

    init {
        config.driverClassName?.let { driverClassName ->
            try {
                Class.forName(driverClassName)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("JDBC driver class not found: $driverClassName", e)
            }
        }
    }

    override fun getConnection(): Connection {
        val properties = Properties(dataSourceProperties.size + 2)
        properties.putAll(dataSourceProperties)
        properties.setProperty("user", username)

        val password = currentPassword()
        properties.setProperty("password", password)

        return DriverManager.getConnection(url, properties)
    }

    override fun getConnection(username: String?, password: String?): Connection =
        throw SQLException(REJECT_CALLER_CREDENTIALS_MESSAGE)

    override fun getLogWriter(): PrintWriter? =
        DriverManager.getLogWriter()

    override fun setLogWriter(out: PrintWriter?) {
        DriverManager.setLogWriter(out)
    }

    override fun setLoginTimeout(seconds: Int) {
        DriverManager.setLoginTimeout(seconds)
    }

    override fun getLoginTimeout(): Int =
        DriverManager.getLoginTimeout()

    override fun getParentLogger(): Logger =
        Logger.getGlobal()

    override fun <T: Any?> unwrap(iface: Class<T>): T {
        if (iface.isInstance(this)) {
            return iface.cast(this)
        }
        throw SQLException("Not a wrapper for ${iface.name}.")
    }

    override fun isWrapperFor(iface: Class<*>): Boolean =
        iface.isInstance(this)

    override fun toString(): String =
        "RefreshingJdbcPasswordDataSource(url=$urlSummary, username=<redacted>)"

    private fun currentPassword(): String =
        try {
            passwordProvider.currentPassword()
                ?: throw SQLException(nullPasswordMessage)
        } catch (e: SQLException) {
            throw e
        } catch (e: Exception) {
            throw SQLException("JDBC password provider failed.", e)
        }

    private companion object {
        private const val REJECT_CALLER_CREDENTIALS_MESSAGE =
            "Refreshing JDBC password data source does not accept caller-supplied credentials."

        private fun sanitizeUrl(url: String): String {
            val withoutQuery = url.substringBefore('?').substringBefore(';')
            return withoutQuery.replace(Regex("//[^/@]+@"), "//")
        }
    }
}
