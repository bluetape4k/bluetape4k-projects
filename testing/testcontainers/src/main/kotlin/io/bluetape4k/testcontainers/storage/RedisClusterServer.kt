package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
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
import org.redisson.codec.LZ4Codec
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
 * Docker를 이용하여 [Redis Cluster](https://redis.io/topics/cluster-tutorial)를 실행합니다.
 *
 * ## NOTE
 * **Redis Cluster 가 사용하는 7000번 포트 (AirPlay에서 사용)를 Mac에서 이미 사용 중일 때에는 테스트가 실패할 수 있습니다.**
 *
 * 참고: [Redis Cluster docker image](https://hub.docker.com/r/tommy351/redis-cluster)
 */
class RedisClusterServer private constructor(
    imageName: DockerImageName,
    useDefaultPort: Boolean,
    reuse: Boolean,
): GenericContainer<RedisClusterServer>(imageName), GenericServer {

    companion object: KLogging() {
        const val IMAGE = "tommy351/redis-cluster"
        const val TAG = "6.2"

        // 테스트에 실패한 다른 이미지들
        //        const val IMAGE = "bitnami/redis-cluster"
        //        const val TAG = "7.0"
        //        const val IMAGE = "grokzen/redis-cluster"
        //        const val TAG = "6.2.1"

        const val NAME = "redis.cluster"

        val PORTS = intArrayOf(7000, 7001, 7002, 7003, 7004, 7005)

        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): RedisClusterServer {
            return RedisClusterServer(imageName, useDefaultPort, reuse)
        }

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

    val mappedPorts: Map<Int, Int> by lazy { PORTS.associateWith { getMappedPort(it) } }
    private val nodeAddresses: List<String> by lazy { mappedPorts.values.map { "$host:$it" } }
    private val nodeRedisUrl: List<String> by lazy { mappedPorts.values.map { "redis://$host:$it" } }
    private val socketAddresses = ConcurrentHashMap<Int, SocketAddress>()

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
     * Start redis.cluster Server:
     * 	testcontainers.redis.cluster.host=localhost
     * 	testcontainers.redis.cluster.port=33087
     * 	testcontainers.redis.cluster.url=redis://localhost:33087
     * 	testcontainers.redis.cluster.nodes=localhost:33087,localhost:33086,localhost:33085,localhost:33084,localhost:33083,localhost:33081
     * 	testcontainers.redis.cluster.urls=redis://localhost:33087,redis://localhost:33086,redis://localhost:33085,redis://localhost:33084,redis://localhost:33083,redis://localhost:33081
     * 	testcontainers.redis.cluster.nodes.0=localhost:33087
     * 	testcontainers.redis.cluster.nodes.1=localhost:33086
     * 	testcontainers.redis.cluster.nodes.2=localhost:33085
     * 	testcontainers.redis.cluster.nodes.3=localhost:33084
     * 	testcontainers.redis.cluster.nodes.4=localhost:33083
     * 	testcontainers.redis.cluster.nodes.5=localhost:33081
     * ```
     */
    private fun writeClusterInfo() {
        val extraMaps = mutableMapOf(
            // testcontainers.redis.cluster.nodes=localhost:7000,localhost:7001 ...
            "nodes" to nodeAddresses.joinToString(","),

            // testcontainers.redis.cluster.nodes=redis://localhost:7000,redis://localhost:7001 ...
            "urls" to nodeRedisUrl.joinToString(",")
        )
        // 이건 안해도 될 듯 하다. (${testcontainers.redis.cluster.node.0} 이런식으로 접근 가능)
        nodeAddresses.forEachIndexed { index, nodeAddress ->
            extraMaps["nodes.$index"] = nodeAddress
        }
        writeToSystemProperties(NAME, extraMaps)
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


    object Launcher {

        val redisCluster: RedisClusterServer by lazy {
            RedisClusterServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        object RedissonLib {
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
                        }

                    this.codec = LZ4Codec()
                }
            }

            fun getRedisson(redisCluster: RedisClusterServer): RedissonClient {
                val config = getRedissonConfig(redisCluster)
                return Redisson.create(config).apply {
                    ShutdownQueue.register { this.shutdown() }
                }
            }
        }

        object LettuceLib {

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

            fun getClusterClient(redisCluster: RedisClusterServer): RedisClusterClient {
                val resources = clientResources(redisCluster)
                val uris = redisCluster.nodeRedisUrl.map { RedisURI.create(it) }

                return RedisClusterClient.create(resources, uris).apply {
                    val topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                        .enablePeriodicRefresh(Duration.ofSeconds(30))
                        .enableAllAdaptiveRefreshTriggers()
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
