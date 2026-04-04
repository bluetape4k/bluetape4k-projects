package io.bluetape4k.testcontainers.storage

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.github.dockerjava.api.command.InspectContainerResponse
import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.storage.CassandraServer.Launcher.cassandra4
import io.bluetape4k.utils.Resourcex
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.InetSocketAddress
import javax.script.ScriptException

/**
 * Docker 를 이용하여 Cassandra 4.0+ Server를 실행합니다.
 *
 * testcontainers (1.18.0) 에서 제공하는 cassandra 는 내부에 cassandra driver 3.x 를 사용해서,
 * 최신 버전인 4.x 를 사용하지 못하고, 충돌이 생깁니다.
 * 이 문제를 해결하고자, [GenericContainer]를 이용하여 직접 구현했습니다.
 *
 * 참고: [Cassandra docker image](https://hub.docker.com/_/cassandra)
 *
 * ```kotlin
 * val cassandraServer = CassandraServer().apply { start() }
 * ```
 *
 * @see [org.testcontainers.containers.CassandraContainer]
 */
class CassandraServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<CassandraServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "cassandra"
        const val TAG = "5"
        const val NAME = "cassandra"

        const val LOCAL_DATACENTER1 = "datacenter1"
        const val CQL_PORT = 9042

        /**
         * 이미지 이름/태그로 [CassandraServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val server = CassandraServer(image = "cassandra", tag = "5")
         * // server.cqlPort > 0 (시작 후)
         * ```
         *
         * @param image Docker 이미지 이름, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param tag   Docker 이미지 태그, blank이면 [IllegalArgumentException]이 발생합니다.
         * @param useDefaultPort `true`면 9042 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): CassandraServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return invoke(imageName, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [CassandraServer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("cassandra").withTag("5")
         * val server = CassandraServer(image)
         * // server.isRunning == false
         * ```
         *
         * @param imageName Docker 이미지 이름
         * @param useDefaultPort `true`면 9042 포트를 고정 바인딩합니다.
         * @param reuse 컨테이너 재사용 여부입니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): CassandraServer {
            return CassandraServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(CQL_PORT)
    /** CQL 접속 포트의 매핑 결과입니다. */
    val cqlPort: Int get() = getMappedPort(CQL_PORT)

    override val url: String get() = "$host:$port"
    /** Cassandra 드라이버 접속에 사용할 contact point입니다. */
    val contactPoint: InetSocketAddress get() = InetSocketAddress(host, port)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf("host", "port", "url", "cql-port")

    override fun properties(): Map<String, String> = mapOf(
        "host" to host,
        "port" to port.toString(),
        "url" to url,
        "cql-port" to cqlPort.toString(),
    )

    private var configLocation: String = ""
    private var initScriptPath: String = ""

    init {
        addExposedPorts(CQL_PORT)
        withReuse(reuse)

        withEnv("CASSANDRA_SNITCH", "GossipingPropertyFileSnitch")
        withEnv("JVM_OPTS", "-Dcassandra.skip_wait_for_gossip_to_settle=0 -Dcassandra.initial_token=0")
        withEnv("HEAP_NEWSIZE", "128M")
        withEnv("MAX_HEAP_SIZE", "1024M")

        if (useDefaultPort) {
            exposeCustomPorts(CQL_PORT)
        }
    }

    override fun containerIsStarted(containerInfo: InspectContainerResponse) {
        runInitScriptIfRequired()
    }

    private fun runInitScriptIfRequired() {
        if (initScriptPath.isBlank()) {
            return
        }

        try {
            val cql = Resourcex.getString(initScriptPath)
            if (cql.isBlank()) {
                log.warn { "Could not load classpath init script: $initScriptPath, cql=$cql" }
                throw ScriptException("Could not load classpath init script: $initScriptPath Resource not found or empty.")
            }
            newCqlSessionBuilder().build().use { session ->
                val cqls = cql.split(";").filter { it.isNotBlank() }.map { it.trim() }
                cqls.forEach { cql ->
                    val applied = session.execute(cql).wasApplied()
                    log.debug { "CQL[$cql] was applied[$applied]" }
                }
            }
        } catch (e: IOException) {
            log.warn(e) { "Could not load classpath init script: $initScriptPath" }
            throw BluetapeException("Could not load classpath init script: $initScriptPath", e)
        } catch (e: ScriptException) {
            log.error(e) { "Error while executing init script: $initScriptPath" }
            throw BluetapeException("Error while executing init script: $initScriptPath", e)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 컨테이너 구성을 위한 외부 설정 경로를 저장합니다.
     *
     * ```kotlin
     * val server = CassandraServer()
     * server.withConfigurationOverride("cassandra/custom.yaml")
     * // server.configLocation == "cassandra/custom.yaml"
     * ```
     */
    fun withConfigurationOverride(configLocation: String) = apply {
        this.configLocation = configLocation
    }

    /**
     * Cassandra Server 시작 시 [initScriptPath] 의 script를 실행시켜 준다.
     *
     * ```kotlin
     * val server = CassandraServer()
     * server.withInitScript("schema/init-schema.cql")
     * // server 시작 시 init-schema.cql이 자동 실행됩니다.
     * ```
     *
     * @param initScriptPath Cassandra database 초기화를 위한 script file path (eg: schema/init-schema.cql)
     */
    fun withInitScript(initScriptPath: String) = apply {
        this.initScriptPath = initScriptPath
    }

    /**
     * 현재 서버에 연결되는 [CqlSessionBuilder]를 생성합니다.
     *
     * ```kotlin
     * val server = CassandraServer()
     * val builder = server.newCqlSessionBuilder()
     * // builder.build() 호출 시 Cassandra 세션 생성 가능
     * ```
     */
    fun newCqlSessionBuilder(): CqlSessionBuilder =
        CqlSessionBuilder().addContactPoint(contactPoint).withLocalDatacenter(LOCAL_DATACENTER1)
            .withConfigLoader(DriverConfigLoader.fromClasspath("application.conf"))


    /**
     * Cassandra Server 를 실행해주는 Launcher 입니다.
     */
    object Launcher {

        private val log = KotlinLogging.logger { }

        val cassandra4 by lazy {
            CassandraServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        const val DEFAULT_KEYSPACE = "examples"
        const val DEFAULT_REPLICATION_FACTOR = 1

        /**
         * [cassandra4]에 접속하는 [CqlSession]을 빌드하는 [CqlSessionBuilder] 를 생성합니다.
         *
         * ```kotlin
         * val builder = CassandraServer.Launcher.newCqlSessionBuilder()
         * val session = builder.build()
         * // session.isClosed == false
         * ```
         *
         * @param localDataCenter local datacenter name (default=datacenter1)
         * @return [CqlSessionBuilder] 인스턴스
         */
        fun newCqlSessionBuilder(localDataCenter: String = LOCAL_DATACENTER1): CqlSessionBuilder {
            return CqlSessionBuilder().addContactPoint(cassandra4.contactPoint).withLocalDatacenter(localDataCenter)
                .withConfigLoader(DriverConfigLoader.fromClasspath("application.conf"))
        }

        /**
         * 지정된 keyspace를 재생성하고 [CqlSession]을 반환합니다.
         *
         * ```kotlin
         * val session = CassandraServer.Launcher.getOrCreateSession("my_keyspace")
         * // session.keyspace.isPresent == true
         * ```
         *
         * @param keyspace 사용할 Cassandra keyspace 이름
         * @param builder [CqlSessionBuilder] 추가 설정 블록
         * @return 연결된 [CqlSession] 인스턴스
         */
        inline fun getOrCreateSession(
            keyspace: String = DEFAULT_KEYSPACE,
            builder: CqlSessionBuilder.() -> Unit = {},
        ): CqlSession {
            keyspace.requireNotBlank("keyspace")

            recreateKeyspace(keyspace)

            return newCqlSessionBuilder().apply { withKeyspace(keyspace) }.apply(builder).build().apply {
                // 혹시 제대로 닫지 않아도, JVM 종료 시 닫아준다.
                ShutdownQueue.register(this)
            }
        }

        /**
         * 지정된 keyspace를 삭제 후 재생성합니다.
         *
         * ```kotlin
         * CassandraServer.Launcher.recreateKeyspace("my_keyspace")
         * // keyspace가 삭제되고 다시 생성됩니다.
         * ```
         *
         * @param keyspace 재생성할 keyspace 이름
         */
        fun recreateKeyspace(keyspace: String) {
            if (keyspace.isNotBlank()) {
                // 테스트 서버에 keyspace 가 존재하지 않을 수 있으므로, 새로 추가하도록 합니다.
                log.info { "Recreate keyspace. keyspace=[$keyspace]" }

                newCqlSessionBuilder().build().use { sysSession ->
                    dropKeyspace(sysSession, keyspace)
                    createKeyspace(sysSession, keyspace)
                }
            }
        }

        /**
         * 지정된 keyspace를 생성합니다.
         *
         * ```kotlin
         * CassandraServer.Launcher.newCqlSessionBuilder().build().use { session ->
         *     val applied = CassandraServer.Launcher.createKeyspace(session, "my_keyspace")
         *     // applied == true
         * }
         * ```
         *
         * @param session 연결된 [CqlSession]
         * @param keyspace 생성할 keyspace 이름
         * @param replicationFactor replication factor (기본값: 1)
         * @return 적용 여부
         */
        fun createKeyspace(
            session: CqlSession,
            keyspace: String,
            replicationFactor: Int = DEFAULT_REPLICATION_FACTOR,
        ): Boolean {
            val createKeyspaceStmt =
                SchemaBuilder.createKeyspace(keyspace).ifNotExists().withSimpleStrategy(replicationFactor).build()

            log.info { "Create keyspace. statement=${createKeyspaceStmt.query}" }
            return session.execute(createKeyspaceStmt).wasApplied()
        }

        /**
         * 지정된 keyspace를 삭제합니다.
         *
         * ```kotlin
         * CassandraServer.Launcher.newCqlSessionBuilder().build().use { session ->
         *     val applied = CassandraServer.Launcher.dropKeyspace(session, "my_keyspace")
         *     // applied == true (존재할 경우)
         * }
         * ```
         *
         * @param session 연결된 [CqlSession]
         * @param keyspace 삭제할 keyspace 이름
         * @return 적용 여부
         */
        fun dropKeyspace(session: CqlSession, keyspace: String): Boolean {
            val dropKeyspaceStmt = SchemaBuilder.dropKeyspace(keyspace).ifExists().build()
            log.info { "Drop keyspace if exists. statement=${dropKeyspaceStmt.query}" }
            return session.execute(dropKeyspaceStmt).wasApplied()
        }
    }

}
