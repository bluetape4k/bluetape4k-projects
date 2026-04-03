package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.storage.RedisClusterServer.Launcher.RedissonLib.getRedissonConfig
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisURI
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.sync.Executions
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.SocketAddressResolver
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.config.ReadMode
import org.redisson.config.SubscriptionMode
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.net.SocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis Cluster 테스트 서버 컨테이너를 실행하고 클러스터 클라이언트 헬퍼를 제공합니다.
 *
 * ## 동작/계약
 * - `7000..7005` 포트를 노출하고 `start()`에서 클러스터가 `cluster_state:ok`가 될 때까지 대기합니다.
 * - `useDefaultPort=true`이면 클러스터 포트를 호스트에 고정 바인딩하려고 시도합니다.
 * - 시작 후 노드 주소/URL 정보를 시스템 프로퍼티(`testcontainers.redis-cluster.*`)로 기록합니다.
 *
 * ```kotlin
 * val cluster = RedisClusterServer()
 * cluster.start()
 * // cluster.mappedPorts.size == 6
 * ```
 *
 * ## NOTE
 * **Redis Cluster 가 사용하는 7000번 포트(AirPlay)가 macOS에서 이미 점유되어 있으면 테스트가 실패할 수 있습니다.**
 *
 * 참고: [Redis Cluster docker image](https://hub.docker.com/r/tommy351/redis-cluster)
 */
class RedisClusterServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<RedisClusterServer>(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "tommy351/redis-cluster"
        const val TAG = "6.2"

        // 테스트에 실패한 다른 이미지들
        //        const val IMAGE = "bitnami/redis-cluster"
        //        const val TAG = "7.0"
        //        const val IMAGE = "grokzen/redis-cluster"
        //        const val TAG = "6.2.1"

        const val NAME = "redis-cluster"

        val PORTS = intArrayOf(7000, 7001, 7002, 7003, 7004, 7005)

        /**
         * [DockerImageName]으로 [RedisClusterServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`으로 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         *
         * ```kotlin
         * val image = DockerImageName.parse("tommy351/redis-cluster").withTag("6.2")
         * val cluster = RedisClusterServer(image)
         * // cluster.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisClusterServer {
            return RedisClusterServer(imageName, useDefaultPort, reuse)
        }

        /**
         * 이미지 이름/태그로 [RedisClusterServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val cluster = RedisClusterServer(image = "tommy351/redis-cluster", tag = "6.2")
         * // cluster.url.startsWith("redis://") == true
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisClusterServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")

            val imageName = DockerImageName.parse(image).withTag(tag)
            return RedisClusterServer(imageName, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORTS[0])
    override val url: String get() = "redis://$host:$port"

    /** 컨테이너 내부 포트 -> 호스트 매핑 포트 정보입니다. */
    val mappedPorts: Map<Int, Int> by lazy { PORTS.associateWith { getMappedPort(it) } }
    private val nodeAddresses: List<String> by lazy { mappedPorts.values.map { "$host:$it" } }
    private val nodeRedisUrl: List<String> by lazy { mappedPorts.values.map { "redis://$host:$it" } }
    private val socketAddresses = ConcurrentHashMap<Int, SocketAddress>()

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = buildSet {
        add("host"); add("port"); add("url")
        add("nodes"); add("urls")
        PORTS.indices.forEach { add("nodes.$it") }
    }

    override fun properties(): Map<String, String> = buildMap {
        put("host", host)
        put("port", port.toString())
        put("url", url)
        put("nodes", nodeAddresses.joinToString(","))
        put("urls", nodeRedisUrl.joinToString(","))
        nodeAddresses.forEachIndexed { index, nodeAddress ->
            put("nodes.$index", nodeAddress)
        }
    }

    init {
        addExposedPorts(*PORTS)
        withReuse(reuse)

        // tommy351/redis-cluster
        addEnv("CLUSTER_ANNOUNCE_IP", "127.0.0.1")

        // grokzen/redis-cluster
        // addEnv("IP", "0.0.0.0")

        // https://hub.docker.com/r/bitnami/redis-cluster
//        addEnv("ALLOW_EMPTY_PASSWORD", "yes")
//        addEnv("REDIS_NODES", PORTS.joinToString(" ") { "locahost:$it" })
//        addEnv("REDIS_CLUSTER_SLEEP_BEFORE_DNS_LOOKUP", "30")
//        addEnv("REDIS_CLUSTER_DNS_LOOKUP_SLEEP", "5")

        if (useDefaultPort) {
            exposeCustomPorts(*PORTS)
        }
    }

    override fun start() {
        super.start()

        writeClusterInfo()

        // Redis Cluster가 구성될 때까지 기다린다.
        awaitClusterReady()
    }

    /**
     * Write cluster info
     *
     * ```
     * Start redis-cluster Server:
     * 	testcontainers.redis-cluster.host=localhost
     * 	testcontainers.redis-cluster.port=33087
     * 	testcontainers.redis-cluster.url=redis://localhost:33087
     * 	testcontainers.redis-cluster.nodes=localhost:33087,localhost:33086,localhost:33085,localhost:33084,localhost:33083,localhost:33081
     * 	testcontainers.redis-cluster.urls=redis://localhost:33087,redis://localhost:33086,redis://localhost:33085,redis://localhost:33084,redis://localhost:33083,redis://localhost:33081
     * 	testcontainers.redis-cluster.nodes.0=localhost:33087
     * 	testcontainers.redis-cluster.nodes.1=localhost:33086
     * 	testcontainers.redis-cluster.nodes.2=localhost:33085
     * 	testcontainers.redis-cluster.nodes.3=localhost:33084
     * 	testcontainers.redis-cluster.nodes.4=localhost:33083
     * 	testcontainers.redis-cluster.nodes.5=localhost:33081
     * ```
     */
    private fun writeClusterInfo() {
        writeToSystemProperties()
    }

    /**
     * Redis Cluster 구성에는 시간이 걸립니다. Cluster 구성이 완료될 때까지 대기합니다.
     */
    private fun awaitClusterReady() {
        log.info { "Redis Cluster 구성이 완료될 때까지 기다립니다..." }

        Launcher.LettuceLib.getClusterClient(this).use { clusterClient ->
            await atMost (Duration.ofSeconds(30)) until {
                var clusterStarted = false
                clusterClient.connect().use { connection ->
                    runCatching {
                        val commands = connection.sync()
                        val clusterInfo: String = commands.clusterInfo()
                        if (clusterInfo.contains("cluster_state:ok")) {
                            val assignedPartition = clusterClient.partitions.sumOf { it.slots.size }
                            if (assignedPartition == SlotHash.SLOT_COUNT) {
                                // fake get for checking cluster
                                runCatching {
                                    commands.get("42")
                                }.onSuccess {
                                    clusterStarted = true
                                }
                            } else {
                                clusterClient.refreshPartitions()
                            }
                        }
                    }
                }
                clusterStarted
            }

            clusterClient.connect().use { connection ->
                val commands = connection.sync()
                log.info { "cluster info: ${commands.clusterInfo()}" }
                val result: Executions<String> = commands.all().commands().ping()
                log.info { "ping result: ${result.joinToString()}" }
            }
        }
        log.info { "Redis Cluster 구성이 완료되었습니다." }
    }

    // Redisson 을 이용하여 Redis Cluster 구성이 완료된 것을 확인하는 것은 불완전합니다. 그래서 Lettuce 를 사용합니다.
    //    private fun awaitClusterByRedisson() {
    //        await atMost Duration.ofSeconds(30) until {
    //            var clusterStarted = false
    //            var redisson: RedissonClient? = null
    //            try {
    //                redisson = Launcher.RedissonLib.getRedisson(this)
    //                clusterStarted = redisson.getRedisNodes(RedisNodes.CLUSTER).pingAll()
    //            } catch(e:Throwable) {
    //                clusterStarted = false
    //            } finally {
    //                runCatching { redisson?.shutdown() }
    //            }
    //            clusterStarted
    //        }
    //    }


    /**
     * 테스트에서 재사용할 Redis Cluster 서버 싱글턴과 클라이언트 헬퍼를 제공합니다.
     */
    object Launcher {

        val redisCluster: RedisClusterServer by lazy {
            RedisClusterServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * Redisson 기반 클러스터 클라이언트 생성 헬퍼입니다.
         */
        object RedissonLib {
            /**
             * Redis Cluster 연결용 [Config]를 생성합니다.
             *
             * ## 동작/계약
             * - `redisCluster.mappedPorts`를 기준으로 NAT 매핑을 구성합니다.
             * - 매 호출마다 새 [Config]를 반환하며 클러스터 상태는 변경하지 않습니다.
             *
             * ```kotlin
             * val config = RedisClusterServer.Launcher.RedissonLib.getRedissonConfig(cluster)
             * // config.useClusterServers().nodeAddresses.isNotEmpty() == true
             * ```
             */
            fun getRedissonConfig(redisCluster: RedisClusterServer): Config {
                return Config().apply {
                    useClusterServers()
                        .setScanInterval(2000)
                        .setReadMode(ReadMode.SLAVE)
                        .setSubscriptionMode(SubscriptionMode.SLAVE)
                        .setNatMapper { redisURI ->
                            val port = redisCluster.mappedPorts[redisURI.port]!!
                            org.redisson.misc.RedisURI("redis", "localhost", port)
                        }
                        .apply {
                            nodeAddresses = redisCluster.nodeRedisUrl

                            retryAttempts = 3
                            setRetryDelay { Duration.ofMillis(it * 10L + 10L) }
                        }

                    this.codec = this.codec ?: TEST_REDISSON_CODEC
                }
            }

            /**
             * Redisson 클러스터 클라이언트를 생성합니다.
             *
             * ## 동작/계약
             * - 내부적으로 [getRedissonConfig]를 사용해 새 [RedissonClient]를 만듭니다.
             * - 반환된 클라이언트 shutdown 훅을 [ShutdownQueue]에 등록합니다.
             *
             * ```kotlin
             * val redisson = RedisClusterServer.Launcher.RedissonLib.getRedisson(cluster)
             * // redisson.isShuttingDown == false
             * ```
             */
            fun getRedisson(redisCluster: RedisClusterServer): RedissonClient {
                val config = getRedissonConfig(redisCluster)
                return Redisson.create(config).apply {
                    ShutdownQueue.register { this.shutdown() }
                }
            }
        }

        /**
         * Lettuce 기반 클러스터 클라이언트 생성 헬퍼입니다.
         */
        object LettuceLib {

            /**
             * NAT 포트 매핑을 반영한 [ClientResources]를 생성합니다.
             *
             * ## 동작/계약
             * - `SocketAddressResolver`에서 컨테이너 포트를 호스트 매핑 포트로 변환합니다.
             * - 반환된 리소스의 shutdown 훅을 [ShutdownQueue]에 등록합니다.
             *
             * ```kotlin
             * val resources = RedisClusterServer.Launcher.LettuceLib.clientResources(cluster)
             * // resources != null
             * ```
             */
            fun clientResources(redisCluster: RedisClusterServer): ClientResources {
                log.trace { "Get ClientResources..." }
                val socketAddressResolver = object: SocketAddressResolver() {
                    override fun resolve(redisURI: RedisURI): SocketAddress {
                        log.trace { "Resolve redisURI=$redisURI" }

                        val mappedPort = redisCluster.mappedPorts[redisURI.port]
                        log.trace { "redisURI.port=${redisURI.port}, mappedPort=$mappedPort" }

                        if (mappedPort != null) {
                            val socketAddress = redisCluster.socketAddresses[mappedPort]
                            if (socketAddress != null) {
                                log.trace { "mappedPort=$mappedPort, RedisCluster socketAddress=$socketAddress" }
                                return socketAddress
                            }
                            redisURI.port = mappedPort
                        }

                        redisURI.host = DockerClientFactory.instance().dockerHostIpAddress()
                        val socketAddress = super.resolve(redisURI)
                        redisCluster.socketAddresses.putIfAbsent(redisURI.port, socketAddress)
                        log.trace { "RedisCluster socketAddress=$socketAddress" }
                        return socketAddress
                    }
                }

                return ClientResources.builder()
                    .socketAddressResolver(socketAddressResolver)
                    .build().apply {
                        ShutdownQueue.register { this.shutdown() }
                    }
            }

            /**
             * [RedisClusterClient]를 생성하고 토폴로지 갱신 옵션을 적용합니다.
             *
             * ## 동작/계약
             * - `cluster.nodeRedisUrl` 목록으로 seed URI를 구성합니다.
             * - 주기적/적응형 토폴로지 갱신 옵션을 설정한 새 클라이언트를 반환합니다.
             * - 반환된 클라이언트 종료 훅을 [ShutdownQueue]에 등록합니다.
             *
             * ```kotlin
             * val client = RedisClusterServer.Launcher.LettuceLib.getClusterClient(cluster)
             * // client.partitions != null
             * ```
             */
            fun getClusterClient(redisCluster: RedisClusterServer): RedisClusterClient {
                val resources = clientResources(redisCluster)
                val uris = redisCluster.nodeRedisUrl.map { RedisURI.create(it) }

                return RedisClusterClient.create(resources, uris).apply {
                    val topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                        .enablePeriodicRefresh(Duration.ofSeconds(30))
                        .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(3))
                        .build()
                    val clusterClientOptions = ClusterClientOptions.builder()
                        .topologyRefreshOptions(topologyRefreshOptions)
                        .autoReconnect(true)
                        .build()

                    this.setOptions(clusterClientOptions)

                    ShutdownQueue.register(this)
                }
            }
        }
    }
}
