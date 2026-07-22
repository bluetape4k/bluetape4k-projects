package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Exercises explicit higher-epoch recovery after a stale replica promotion and known-old RDB restore. */
@Tag("fencing-topology")
internal class LettuceFencingLeaseTopologyRecoveryTest {

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    fun `promotion and restore require traffic pause durable epoch bump and strict tuple rejection`() {
        val snapshot = Files.createTempFile("fencing-known-old-", ".rdb")
        val trace = mutableListOf<TopologyEvent>()
        val gate = TrafficGate(trace)
        val epochAuthority = DurableEpochAuthority(71)
        val downstream = StrictDownstreamStore()
        val bootstrapEpochs = mutableListOf<Long>()

        try {
            Network.newNetwork().use { network ->
                RedisServer()
                    .withNetwork(network)
                    .withNetworkAliases(PRIMARY_ALIAS)
                    .withCommand(*redisCommand())
                    .use { primary ->
                        OwnedToxiproxy()
                            .withNetwork(network)
                            .withNetworkAliases(TOXIPROXY_ALIAS)
                            .use { toxiproxy ->
                                primary.start()
                                toxiproxy.start()
                                val proxy = createReplicationProxy(toxiproxy)
                                try {
                                    RedisServer()
                                        .withNetwork(network)
                                        .withNetworkAliases(REPLICA_ALIAS)
                                        .withCommand(*replicaCommand())
                                        .use { replica ->
                                            replica.start()
                                            ownedClient(primary).useClient { primaryConnection ->
                                                ownedClient(replica).useClient { replicaConnection ->
                                                    val primaryCommands = primaryConnection.sync()
                                                    val replicaCommands = replicaConnection.sync()
                                                    val oldConfig = config(epoch = 71)
                                                    val keys = deriveFencingLeaseKeys(oldConfig, StringCodec.UTF8)
                                                    val oldLease = LettuceFencingLease(primaryConnection, oldConfig)

                                                    oldLease.bootstrap() shouldBeEqualTo
                                                        FencingBootstrapResult.Initialized
                                                    awaitReplicaBaseline(primaryCommands, replicaCommands, keys.counter)
                                                    primaryCommands.save() shouldBeEqualTo "OK"
                                                    primary.copyFileFromContainer("/data/dump.rdb", snapshot.toString())
                                                    trace += TopologyEvent.BASELINE_SNAPSHOT

                                                    proxy.disable()
                                                    trace += TopologyEvent.REPLICATION_PARTITIONED
                                                    val oldOwner = FencingOwnerId.from("owner-before-signal")
                                                    val oldToken = oldLease.acquire(oldOwner, LEASE_TIME)
                                                        .shouldBeInstanceOf<FencingAcquireResult.Acquired>()
                                                        .token
                                                    trace += TopologyEvent.OLD_ACQUIRE
                                                    downstream.write(RESOURCE_ID, oldToken) shouldBeEqualTo 1
                                                    trace += TopologyEvent.OLD_DOWNSTREAM_WRITE
                                                    primaryCommands.get(keys.counter) shouldBeEqualTo "1"
                                                    replicaCommands.get(keys.counter) shouldBeEqualTo "0"

                                                    primaryConnection.close()
                                                    primary.stop()
                                                    replicaCommands.replicaofNoOne() shouldBeEqualTo "OK"
                                                    trace += TopologyEvent.STALE_REPLICA_PROMOTED

                                                    gate.closeForIncident()
                                                    gate.executeOldAcquire { oldLease.acquire(oldOwner, LEASE_TIME) }
                                                        .shouldBeNull()
                                                    gate.executeOldDownstreamWrite {
                                                        downstream.write(RESOURCE_ID, oldToken)
                                                    }.shouldBeNull()

                                                    val promotedToken = recover(
                                                        commands = replicaCommands,
                                                        connection = replicaConnection,
                                                        epochAuthority = epochAuthority,
                                                        requestedEpoch = 72,
                                                        bootstrapEpochs = bootstrapEpochs,
                                                        trace = trace,
                                                    )
                                                    promotedToken shouldBeGreaterThan oldToken
                                                    downstream.write(RESOURCE_ID, promotedToken) shouldBeEqualTo 1
                                                    downstream.write(RESOURCE_ID, oldToken) shouldBeEqualTo 0
                                                    gate.resumeHigherEpoch()
                                                }
                                            }
                                        }
                                } finally {
                                    runCatching { proxy.enable() }
                                    runCatching { proxy.delete() }
                                }
                            }
                    }

                RedisServer()
                    .withNetwork(network)
                    .withNetworkAliases(RESTORE_ALIAS)
                    .withCopyFileToContainer(MountableFile.forHostPath(snapshot), "/data/dump.rdb")
                    .withCommand(*redisCommand())
                    .use { restored ->
                        restored.start()
                        ownedClient(restored).useClient { restoredConnection ->
                            val restoredCommands = restoredConnection.sync()
                            val oldKeys = deriveFencingLeaseKeys(config(71), StringCodec.UTF8)
                            restoredCommands.get(oldKeys.counter) shouldBeEqualTo "0"
                            trace += TopologyEvent.KNOWN_OLD_RDB_RESTORED
                            gate.closeForIncident()
                            gate.executeOldDownstreamWrite {
                                downstream.write(RESOURCE_ID, FencingToken(71, 1))
                            }.shouldBeNull()

                            val restoredToken = recover(
                                commands = restoredCommands,
                                connection = restoredConnection,
                                epochAuthority = epochAuthority,
                                requestedEpoch = 73,
                                bootstrapEpochs = bootstrapEpochs,
                                trace = trace,
                            )
                            restoredToken.epoch shouldBeEqualTo 73
                            downstream.write(RESOURCE_ID, restoredToken) shouldBeEqualTo 1
                            downstream.write(RESOURCE_ID, FencingToken(71, 1)) shouldBeEqualTo 0
                            gate.resumeHigherEpoch()
                        }
                    }
            }

            bootstrapEpochs shouldBeEqualTo listOf(72L, 73L)
            trace.count { it == TopologyEvent.OLD_ACQUIRE } shouldBeEqualTo 1
            trace.count { it == TopologyEvent.OLD_DOWNSTREAM_WRITE } shouldBeEqualTo 1
            trace.count { it == TopologyEvent.BLOCKED_OLD_ACQUIRE } shouldBeEqualTo 1
            trace.count { it == TopologyEvent.BLOCKED_OLD_DOWNSTREAM_WRITE } shouldBeEqualTo 2
            trace.count { it == TopologyEvent.EXTERNAL_INCIDENT_SIGNAL } shouldBeEqualTo 2
            trace.count { it == TopologyEvent.RESUME } shouldBeEqualTo 2
            trace.indexOf(TopologyEvent.EXTERNAL_INCIDENT_SIGNAL) shouldBeGreaterThan
                trace.indexOf(TopologyEvent.OLD_ACQUIRE)
            gate.isOpen.shouldBeTrue()
        } finally {
            Files.deleteIfExists(snapshot)
        }
    }

    private fun recover(
        commands: RedisCommands<String, String>,
        connection: StatefulRedisConnection<String, String>,
        epochAuthority: DurableEpochAuthority,
        requestedEpoch: Long,
        bootstrapEpochs: MutableList<Long>,
        trace: MutableList<TopologyEvent>,
    ): FencingToken {
        val previous = epochAuthority.currentEpoch
        epochAuthority.compareAndSet(previous, requestedEpoch).shouldBeTrue()
        trace += TopologyEvent.DURABLE_CAS_BUMP

        val config = config(requestedEpoch)
        val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        val lease = LettuceFencingLease(connection, config)
        bootstrapEpochs += requestedEpoch
        lease.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
        trace += TopologyEvent.BOOTSTRAP
        isReady(commands, keys).shouldBeTrue()
        trace += TopologyEvent.READINESS

        val token = lease.acquire(FencingOwnerId.from("owner-$requestedEpoch"), LEASE_TIME)
            .shouldBeInstanceOf<FencingAcquireResult.Acquired>()
            .token
        trace += TopologyEvent.HIGHER_EPOCH_READY
        return token
    }

    private fun awaitReplicaBaseline(
        primary: RedisCommands<String, String>,
        replica: RedisCommands<String, String>,
        counterKey: String,
    ) {
        await()
            .atMost(Duration.ofSeconds(45))
            .pollInterval(Duration.ofMillis(50))
            .untilAsserted {
                val primaryInfo = parseInfo(primary.info("replication"))
                val persistenceInfo = parseInfo(primary.info("persistence"))
                val replicaInfo = parseInfo(replica.info("replication"))
                ReplicationBaseline(
                    connectedReplicas = primaryInfo["connected_slaves"],
                    backgroundSave = persistenceInfo["rdb_bgsave_in_progress"],
                    replicaLink = replicaInfo["master_link_status"],
                    counter = replica.get(counterKey),
                    offsetsMatch = primaryInfo["master_repl_offset"] == replicaInfo["slave_repl_offset"],
                ) shouldBeEqualTo READY_REPLICATION_BASELINE
            }
    }

    private fun isReady(commands: RedisCommands<String, String>, keys: FencingLeaseKeys): Boolean =
        commands.type(keys.counter) == "string" &&
            commands.pttl(keys.counter) == -1L &&
            commands.get(keys.counter)?.let { it == "0" || CANONICAL_POSITIVE.matches(it) } == true

    private fun parseInfo(info: String): Map<String, String> =
        info.lineSequence()
            .filter { ':' in it && !it.startsWith('#') }
            .associate { line -> line.substringBefore(':') to line.substringAfter(':').trim() }

    private fun createReplicationProxy(toxiproxy: OwnedToxiproxy): ReplicationProxy =
        ReplicationProxy(toxiproxy.controlUrl)

    private fun ownedClient(server: RedisServer): RedisClient =
        RedisClient.create("redis://${server.host}:${server.port}")

    private inline fun <T> RedisClient.useClient(
        block: (StatefulRedisConnection<String, String>) -> T,
    ): T = try {
        connect(StringCodec.UTF8).use(block)
    } finally {
        shutdown()
    }

    private fun config(epoch: Long): LettuceFencingLeaseConfig =
        LettuceFencingLeaseConfig("topology-recovery", RESOURCE_ID, epoch)

    private fun redisCommand(): Array<String> = arrayOf(
        "redis-server",
        "--dir", "/data",
        "--dbfilename", "dump.rdb",
        "--save", "",
        "--appendonly", "no",
    )

    private fun replicaCommand(): Array<String> = redisCommand() + arrayOf(
        "--replicaof", TOXIPROXY_ALIAS, PROXY_PORT.toString(),
    )

    private class TrafficGate(private val trace: MutableList<TopologyEvent>) {
        private val open = AtomicBoolean(true)

        val isOpen: Boolean get() = open.get()

        fun closeForIncident() {
            open.compareAndSet(true, false).shouldBeTrue()
            trace += TopologyEvent.EXTERNAL_INCIDENT_SIGNAL
            trace += TopologyEvent.TRAFFIC_PAUSED
        }

        fun resumeHigherEpoch() {
            open.compareAndSet(false, true).shouldBeTrue()
            trace += TopologyEvent.ROLLOUT
            trace += TopologyEvent.CONFIRM_OLD_ABSENCE
            trace += TopologyEvent.RESUME
        }

        fun <T> executeOldAcquire(action: () -> T): T? =
            if (open.get()) action() else {
                trace += TopologyEvent.BLOCKED_OLD_ACQUIRE
                null
            }

        fun <T> executeOldDownstreamWrite(action: () -> T): T? =
            if (open.get()) action() else {
                trace += TopologyEvent.BLOCKED_OLD_DOWNSTREAM_WRITE
                null
            }
    }

    private class DurableEpochAuthority(initialEpoch: Long) {
        private val epoch = AtomicLong(initialEpoch)
        val currentEpoch: Long get() = epoch.get()
        fun compareAndSet(expected: Long, update: Long): Boolean =
            update > expected && epoch.compareAndSet(expected, update)
    }

    private class OwnedToxiproxy:
        GenericContainer<OwnedToxiproxy>(DockerImageName.parse(TOXIPROXY_IMAGE)) {

        val controlUrl: String get() = "http://$host:${getMappedPort(TOXIPROXY_CONTROL_PORT)}"

        init {
            withExposedPorts(TOXIPROXY_CONTROL_PORT, PROXY_PORT)
            waitingFor(Wait.forHttp("/version").forPort(TOXIPROXY_CONTROL_PORT))
        }
    }

    private class ReplicationProxy(private val controlUrl: String) {
        private val client = HttpClient.newHttpClient()

        init {
            val configuration =
                """{"name":"$PROXY_NAME","listen":"0.0.0.0:$PROXY_PORT",""" +
                    """"upstream":"$PRIMARY_ALIAS:${RedisServer.PORT}"}"""
            request(
                "POST",
                "/proxies",
                configuration,
            )
        }

        fun disable() = setEnabled(false)

        fun enable() = setEnabled(true)

        fun delete() {
            request("DELETE", "/proxies/$PROXY_NAME")
        }

        private fun setEnabled(enabled: Boolean) {
            request("POST", "/proxies/$PROXY_NAME", """{"enabled":$enabled}""")
        }

        private fun request(method: String, path: String, body: String? = null) {
            val builder = HttpRequest.newBuilder(URI.create(controlUrl + path))
                .header("Content-Type", "application/json")
            val request = when {
                body != null -> builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build()
                else         -> builder.method(method, HttpRequest.BodyPublishers.noBody()).build()
            }
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            require(response.statusCode() in 200..299) {
                "Toxiproxy control request failed with status ${response.statusCode()}."
            }
        }
    }

    private class StrictDownstreamStore {
        private var token: FencingToken? = null

        @Synchronized
        fun write(resourceId: String, candidate: FencingToken): Int {
            resourceId shouldBeEqualTo RESOURCE_ID
            val current = token
            if (current != null && candidate <= current) return 0
            token = candidate
            return 1
        }
    }

    private data class ReplicationBaseline(
        val connectedReplicas: String?,
        val backgroundSave: String?,
        val replicaLink: String?,
        val counter: String?,
        val offsetsMatch: Boolean,
    )

    private enum class TopologyEvent {
        BASELINE_SNAPSHOT,
        REPLICATION_PARTITIONED,
        OLD_ACQUIRE,
        OLD_DOWNSTREAM_WRITE,
        STALE_REPLICA_PROMOTED,
        EXTERNAL_INCIDENT_SIGNAL,
        TRAFFIC_PAUSED,
        BLOCKED_OLD_ACQUIRE,
        BLOCKED_OLD_DOWNSTREAM_WRITE,
        DURABLE_CAS_BUMP,
        BOOTSTRAP,
        READINESS,
        HIGHER_EPOCH_READY,
        KNOWN_OLD_RDB_RESTORED,
        ROLLOUT,
        CONFIRM_OLD_ABSENCE,
        RESUME,
    }

    private companion object {
        const val PRIMARY_ALIAS: String = "fencing-primary"
        const val REPLICA_ALIAS: String = "fencing-replica"
        const val RESTORE_ALIAS: String = "fencing-restore"
        const val TOXIPROXY_ALIAS: String = "fencing-toxiproxy"
        const val PROXY_PORT: Int = 8666
        const val PROXY_NAME: String = "fencing-replication"
        const val TOXIPROXY_CONTROL_PORT: Int = 8474
        const val TOXIPROXY_IMAGE: String = "ghcr.io/shopify/toxiproxy:2.9.0"
        const val RESOURCE_ID: String = "guarded-resource"
        val READY_REPLICATION_BASELINE = ReplicationBaseline("1", "0", "up", "0", true)
        val LEASE_TIME: Duration = Duration.ofMinutes(1)
        val CANONICAL_POSITIVE: Regex = Regex("[1-9][0-9]*")
    }
}
