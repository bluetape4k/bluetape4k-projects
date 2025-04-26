package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.javatimes.millis
import io.bluetape4k.junit5.awaitility.coUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.coroutines.awaitAll
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.asCompletableFuture
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.withPollDelay
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.redisson.api.map.MapWriter
import org.redisson.api.map.MapWriterAsync
import org.redisson.api.map.WriteMode
import org.redisson.api.options.MapCacheOptions
import java.io.Serializable
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletionStage
import kotlin.random.Random

/**
 * 대량의 IoT 데이터를 Write-Behind 방식으로 DB에 저장하는 서비스입니다.
 */
class CacheWriteBehindForIoTData: AbstractCacheExample() {

    companion object: KLogging()

    object SensorDataTable: TimebasedUUIDTable("sensor_data") {
        val serialNo = varchar("sensor_serial_no", 255)
        val sensingTime = timestamp("sensing_time").defaultExpression(CurrentTimestamp)
        val temperature = decimal("temperature", 10, 2)
        val humidity = decimal("humidity", 10, 2)

        init {
            index("idx_sensor_data_serial", true, serialNo, sensingTime)
        }
    }

    data class SensorData(
        val serialNo: String,
        val sensingTime: Instant,
        val temperature: BigDecimal,
        val humidity: BigDecimal,
    ): Serializable

    private fun generateSensorData(serialNo: String, count: Int = 1000): List<SensorData> {
        val startTime = Instant.now() - count.millis()

        return List(count) {
            val measureTime = startTime + it.millis()
            SensorData(
                serialNo = serialNo,
                sensingTime = measureTime,
                temperature = Random.nextDouble(-30.0, 40.0).toBigDecimal(),
                humidity = Random.nextDouble(10.0, 100.0).toBigDecimal(),
            )
        }
    }

    private fun getSensorDataCountFromDB(serialNo: String): Long {
        return transaction {
            SensorDataTable.selectAll().where { SensorDataTable.serialNo eq serialNo }.count()
        }
    }

    private suspend fun getSensorDataCountFromDBAsync(serialNo: String): Long {
        return newSuspendedTransaction {
            SensorDataTable.selectAll().where { SensorDataTable.serialNo eq serialNo }.count()
        }
    }


    @Nested
    inner class Synchronous {
        /**
         * 센서 데이터들을 모두 DB에 저장하는 것이 아니라, 짧은 시간대에 측정한 값은 마지막 값만 샘플링해서 저장합니다.
         */
        private val sensorDataWriter = object: MapWriter<String, List<SensorData>> {
            override fun write(map: Map<String, List<SensorData>>) {
                val sampling = map.map { entry ->
                    entry.key to entry.value.debounceSampling(Duration.ofMillis(10)).toList()
                }.toMap()

                transaction {
                    sampling.forEach { (key, values) ->
                        log.debug { "Sample to save: $key -> ${values.size}" }
                        SensorDataTable.batchInsert(values) { data ->
                            this[SensorDataTable.serialNo] = data.serialNo
                            this[SensorDataTable.sensingTime] = data.sensingTime
                            this[SensorDataTable.temperature] = data.temperature
                            this[SensorDataTable.humidity] = data.humidity
                        }
                    }
                }
            }

            override fun delete(keys: Collection<String>) {
                transaction {
                    SensorDataTable.deleteWhere { SensorDataTable.serialNo inList keys }
                }
            }

            fun List<SensorData>.debounceSampling(timeout: Duration): Sequence<SensorData> {
                var lastSensingTime: Instant = Instant.MIN
                return this.asSequence()
                    .mapNotNull { current ->
                        if (lastSensingTime == Instant.MIN) {
                            current
                        } else {
                            if (current.sensingTime >= (lastSensingTime + timeout)) {
                                current
                            } else {
                                null
                            }
                        }
                    }.onEach { lastSensingTime = it.sensingTime }
            }
        }

        @Test
        fun `대량의 센서데이터를 샘플링해서 DB에 저장한다`() {
            val name = randomName()
            val options = MapCacheOptions.name<String, List<SensorData>>(name)
                .writer(sensorDataWriter)
                .writeMode(WriteMode.WRITE_BEHIND)              // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다.
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)

            // 대량 데이터를 Write Behind 방식으로 저장하는 MapCache를 생성한다.
            val cache = redisson.getMapCache(options)
            try {
                val dataSize = 1000
                cache.fastPut("sensor-1", generateSensorData("sensor-1", dataSize))

                Thread.sleep(100)

                // 1ms 마다 생성되는 데이터를 10ms 단위로 sampling 해서 저장합니다. 따라서, DB에는 dataSize / 10 개만 저장된다.
                await withPollDelay Duration.ofMillis(100) until { getSensorDataCountFromDB("sensor-1") >= dataSize / 10 }

                cache.fastPut("sensor-2", generateSensorData("sensor-2", dataSize))
                cache.fastPut("sensor-3", generateSensorData("sensor-3", dataSize))

                await withPollDelay Duration.ofMillis(100) until { getSensorDataCountFromDB("sensor-2") >= dataSize / 10 }
                await withPollDelay Duration.ofMillis(100) until { getSensorDataCountFromDB("sensor-3") >= dataSize / 10 }

            } finally {
                // 캐시를 삭제한다.
                cache.delete()
            }
        }
    }

    @Nested
    inner class Asynchronous {
        /**
         * 센서 데이터들을 모두 DB에 저장하는 것이 아니라, 짧은 시간대에 측정한 값은 마지막 값만 샘플링해서 저장합니다.
         */
        private val sensorDataWriterAsync = object: MapWriterAsync<String, List<SensorData>> {
            override fun write(map: Map<String, List<SensorData>>): CompletionStage<Void> {
                val scope = CoroutineScope(Dispatchers.IO)
                return scope.async {
                    val sampling = map.map { entry ->
                        entry.key to entry.value.debounceSampling(Duration.ofMillis(10)).toList()
                    }.toMap()

                    newSuspendedTransaction {
                        sampling.forEach { (key, values) ->
                            log.debug { "Sample to save: $key -> ${values.size}" }
                            SensorDataTable.batchInsert(values) { data ->
                                this[SensorDataTable.serialNo] = data.serialNo
                                this[SensorDataTable.sensingTime] = data.sensingTime
                                this[SensorDataTable.temperature] = data.temperature
                                this[SensorDataTable.humidity] = data.humidity
                            }
                        }
                    }
                    null
                }.asCompletableFuture()
            }

            override fun delete(keys: Collection<String>): CompletionStage<Void> {
                val scope = CoroutineScope(Dispatchers.IO)
                return scope.async {
                    newSuspendedTransaction {
                        SensorDataTable.deleteWhere { SensorDataTable.serialNo inList keys }
                    }
                    null
                }.asCompletableFuture()
            }

            fun List<SensorData>.debounceSampling(timeout: Duration): Flow<SensorData> {
                var lastSensingTime: Instant = Instant.MIN
                return this.asFlow()
                    .mapNotNull { current ->
                        if (lastSensingTime == Instant.MIN) {
                            current
                        } else {
                            if (current.sensingTime >= (lastSensingTime + timeout)) {
                                current
                            } else {
                                null
                            }
                        }
                    }.onEach { lastSensingTime = it.sensingTime }
            }
        }

        @Test
        fun `코루틴 환경에서 대량의 센서데이터를 샘플링해서 DB에 저장한다`() = runSuspendIO {
            val name = randomName()
            val options = MapCacheOptions.name<String, List<SensorData>>(name)
                .writerAsync(sensorDataWriterAsync)
                .writeMode(WriteMode.WRITE_BEHIND)              // delay를 두고, batch로 insert 한다
                .writeBehindBatchSize(20)  // 기본 batchSize 는 50 입니다.
                .writeBehindDelay(100)        // 기본 delay 는 1000 ms 입니다.
                .writeRetryAttempts(3) // 재시도 횟수
                .writeRetryInterval(Duration.ofMillis(100)) // 재시도 간격
                .codec(RedissonCodecs.LZ4Fury)

            // 대량 데이터를 Write Behind 방식으로 저장하는 MapCache를 생성한다.
            val cache = redisson.getMapCache(options)
            try {
                val dataSize = 1000
                cache.fastPutAsync("sensor-1", generateSensorData("sensor-1", dataSize)).coAwait()

                Thread.sleep(100)

                // 1ms 마다 생성되는 데이터를 10ms 단위로 sampling 해서 저장합니다. 따라서, DB에는 dataSize / 10 개만 저장된다.
                await withPollDelay Duration.ofMillis(100) coUntil { getSensorDataCountFromDBAsync("sensor-1") >= dataSize / 10 }

                listOf(
                    cache.fastPutAsync("sensor-2", generateSensorData("sensor-2", dataSize)),
                    cache.fastPutAsync("sensor-3", generateSensorData("sensor-3", dataSize))
                ).awaitAll()

                await withPollDelay Duration.ofMillis(100) coUntil { getSensorDataCountFromDBAsync("sensor-2") >= dataSize / 10 }
                await withPollDelay Duration.ofMillis(100) coUntil { getSensorDataCountFromDBAsync("sensor-3") >= dataSize / 10 }

            } finally {
                // 캐시를 삭제한다.
                cache.deleteAsync().coAwait()
            }
        }
    }

}
