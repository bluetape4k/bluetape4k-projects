import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import org.apache.fory.json.ForyJson
import org.apache.fory.json.JsonStreamValueLimitException
import org.apache.fory.json.annotation.JsonSubTypes
import org.apache.fory.json.kotlin.ForyJsonKotlin
import org.apache.fory.json.kotlin.jsonTypeRef

@JvmInline
value class AccountId(val value: Long)

@JsonSubTypes(property = "type")
sealed interface AccountEvent {
    data class Created(val actor: String) : AccountEvent
}

data class Account(
    val id: AccountId,
    val name: String = "anonymous",
    val note: String?,
    val tags: List<String?>,
    val event: AccountEvent,
    val sequence: Long,
    val payload: ByteArray,
)

data class FlatAccount(
    val name: String,
    val sequence: Long,
    val payload: ByteArray,
)

private val sample = Account(
    id = AccountId(7),
    note = null,
    tags = listOf("kotlin", null, "json"),
    event = AccountEvent.Created("debop"),
    sequence = Long.MAX_VALUE,
    payload = byteArrayOf(0, 1, 2, 127, -1),
)

fun main() {
    val fory = ForyJsonKotlin.builder().writeLongAsString(true).build()
    verifyKotlinModel(fory)
    verifyIncrementalNdjson(fory)
    verifyIncrementalArray(fory)
    verifyFailureAndLimit(fory)
    compareRepresentations(fory)
    benchmark(fory)
}

private fun verifyKotlinModel(fory: ForyJson) {
    val type = jsonTypeRef<Account>()
    val json = fory.toJson(sample, type)
    val restored = fory.fromJson(json, type)
    check(restored.sameValueAs(sample))
    check("\"${Long.MAX_VALUE}\"" in json)
    check(restored.tags[1] == null)

    val namedJson = fory.toJson(sample.copy(name = "explicit"), type)
    val withoutName = namedJson.replace("\"name\":\"explicit\",", "")
    check(withoutName != namedJson)
    check("\"name\"" !in withoutName)
    check(fory.fromJson(withoutName, type).name == "anonymous")

    val listType = jsonTypeRef<List<Account>>()
    val listJson = fory.toJson(listOf(sample), listType)
    check(fory.fromJson(listJson, listType).single().sameValueAs(sample))
    check(fory.fromJson(json, Account::class.java).sameValueAs(sample))
    val rawList = fory.fromJson(listJson, List::class.java)
    check(rawList.single() !is Account)

    println(
        "MODEL PASS bytes=${json.toByteArray().size} " +
            "rawGenericElement=${rawList.single()!!::class.java.simpleName} json=$json",
    )
}

private fun verifyIncrementalNdjson(fory: ForyJson) {
    val type = jsonTypeRef<Account>()
    val first = fory.toJson(sample, type)
    val second = fory.toJson(sample.copy(id = AccountId(8), note = "끝"), type)
    val third = fory.toJson(sample.copy(id = AccountId(9), note = "마지막"), type)
    val bytes = "$first\n$second\r\n$third".toByteArray()
    val decoder = fory.newNdjsonStreamDecoder(type, 1_024)
    val restored = mutableListOf<Account>()

    check("끝".toByteArray().size == 3)
    bytes.asList().chunked(1).forEach { chunk ->
        val buffer = ByteBuffer.wrap(chunk.toByteArray())
        while (buffer.hasRemaining()) {
            if (decoder.decodeNext(buffer)) restored += decoder.value()
        }
    }
    if (decoder.finish()) restored += decoder.value()

    val expected = listOf(
        sample,
        sample.copy(id = AccountId(8), note = "끝"),
        sample.copy(id = AccountId(9), note = "마지막"),
    )
    check(restored.size == expected.size)
    check(restored.zip(expected).all { (actual, item) -> actual.sameValueAs(item) })
    println("NDJSON PASS records=${restored.size} chunks=${bytes.size} utf8ByteSplit=true separators=LF,CRLF")
}

private fun verifyIncrementalArray(fory: ForyJson) {
    val type = jsonTypeRef<Account>()
    val json = "[${fory.toJson(sample, type)},${fory.toJson(sample.copy(id = AccountId(9)), type)}]"
    val bytes = json.toByteArray()
    val decoder = fory.newArrayStreamDecoder(type, 1_024)
    val restored = mutableListOf<Account>()

    bytes.asList().chunked(5).forEach { chunk ->
        val buffer = ByteBuffer.wrap(chunk.toByteArray())
        while (buffer.hasRemaining()) {
            if (decoder.decodeNext(buffer)) restored += decoder.value()
        }
    }
    check(!decoder.finish())
    check(restored.map { it.id } == listOf(AccountId(7), AccountId(9)))
    println("ARRAY PASS records=${restored.size} chunks=${(bytes.size + 4) / 5}")
}

private fun verifyFailureAndLimit(fory: ForyJson) {
    val type = jsonTypeRef<Account>()
    val malformed = fory.newNdjsonStreamDecoder(type, 1_024)
    malformed.decodeNext(ByteBuffer.wrap("{\"id\":".toByteArray()))
    check(runCatching { malformed.finish() }.isFailure)
    check(runCatching { malformed.finish() }.exceptionOrNull() is IllegalStateException)

    val limited = fory.newNdjsonStreamDecoder(type, 16)
    val failure = runCatching {
        limited.decodeNext(ByteBuffer.wrap("{\"name\":\"0123456789\"}\n".toByteArray()))
    }.exceptionOrNull()
    check(failure is JsonStreamValueLimitException)
    check(runCatching { limited.decodeNext(ByteBuffer.wrap(byteArrayOf())) }.exceptionOrNull() is IllegalStateException)
    println("FAILURE PASS malformed=terminal limit=${failure.maxValueBytes}")
}

private fun compareRepresentations(fory: ForyJson) {
    val flat = FlatAccount("debop", Long.MAX_VALUE, sample.payload)
    val type = jsonTypeRef<FlatAccount>()
    val jackson = jacksonObjectMapper()
    val foryJson = fory.toJson(flat, type)
    val jacksonJson = jackson.writeValueAsString(flat)
    val fastjson = JSON.toJSONString(flat)

    val foryTree = jackson.readTree(foryJson)
    val jacksonTree = jackson.readTree(jacksonJson)
    val fastjsonTree = jackson.readTree(fastjson)
    check(foryTree.path("sequence").isTextual)
    check(jacksonTree.path("sequence").isIntegralNumber)
    check(fastjsonTree.path("sequence").isIntegralNumber)
    check(foryTree.path("payload").asText() == "AAECf/8=")
    check(jacksonTree.path("payload").asText() == "AAECf/8=")
    check(fastjsonTree.path("payload").isArray)
    check(fastjsonTree.path("payload").map { it.asInt() } == listOf(0, 1, 2, 127, -1))

    check(fory.fromJson(foryJson, type).payload.contentEquals(flat.payload))
    check(jackson.readValue<FlatAccount>(jacksonJson).payload.contentEquals(flat.payload))
    check(JSON.parseObject(fastjson, FlatAccount::class.java).payload.contentEquals(flat.payload))
    val crossReads = linkedMapOf(
        "Fory<-Jackson" to runCatching { fory.fromJson(jacksonJson, type).sameValueAs(flat) }.getOrDefault(false),
        "Fory<-Fastjson2" to runCatching { fory.fromJson(fastjson, type).sameValueAs(flat) }.getOrDefault(false),
        "Jackson<-Fory" to runCatching { jackson.readValue<FlatAccount>(foryJson).sameValueAs(flat) }.getOrDefault(false),
        "Jackson<-Fastjson2" to runCatching { jackson.readValue<FlatAccount>(fastjson).sameValueAs(flat) }.getOrDefault(false),
        "Fastjson2<-Fory" to runCatching {
            JSON.parseObject(foryJson, FlatAccount::class.java).sameValueAs(flat)
        }.getOrDefault(false),
        "Fastjson2<-Jackson" to runCatching {
            JSON.parseObject(jacksonJson, FlatAccount::class.java).sameValueAs(flat)
        }.getOrDefault(false),
    )
    check(
        crossReads == linkedMapOf(
            "Fory<-Jackson" to true,
            "Fory<-Fastjson2" to false,
            "Jackson<-Fory" to true,
            "Jackson<-Fastjson2" to true,
            "Fastjson2<-Fory" to false,
            "Fastjson2<-Jackson" to false,
        ),
    )
    println(
        "REPRESENTATION Fory=${foryJson.toByteArray().size} " +
            "Jackson=${jacksonJson.toByteArray().size} Fastjson2=${fastjson.toByteArray().size} " +
            "crossRead=$crossReads Fory.json=$foryJson Jackson.json=$jacksonJson Fastjson2.json=$fastjson",
    )
}

private fun benchmark(fory: ForyJson) {
    val flat = FlatAccount("debop", Long.MAX_VALUE, sample.payload)
    val type = jsonTypeRef<FlatAccount>()
    val jackson = jacksonObjectMapper()
    repeat(2_000) {
        fory.toJson(flat, type)
        jackson.writeValueAsString(flat)
        JSON.toJSONString(flat)
    }

    val iterations = 20_000
    val results = listOf(
        measureBackend("Fory", iterations) { fory.toJson(flat, type) },
        measureBackend("Jackson", iterations) { jackson.writeValueAsString(flat) },
        measureBackend("Fastjson2", iterations) { JSON.toJSONString(flat) },
    )
    println("BENCH iterations=$iterations ${results.joinToString(" ") { it.summary() }}")
}

private data class BackendMeasurement(
    val name: String,
    val nanosPerOperation: Long,
    val allocatedBytesPerOperation: Long,
    val peakHeapDeltaBytes: Long,
)

private fun measureBackend(name: String, iterations: Int, encode: () -> String): BackendMeasurement {
    val threadBean = ManagementFactory.getThreadMXBean() as? ThreadMXBean
        ?: error("Current JVM does not expose ThreadMXBean allocation counters")
    check(threadBean.isThreadAllocatedMemorySupported)
    if (!threadBean.isThreadAllocatedMemoryEnabled) {
        threadBean.isThreadAllocatedMemoryEnabled = true
    }
    val heapPools = ManagementFactory.getMemoryPoolMXBeans()
        .filter { it.isValid && it.type == MemoryType.HEAP }

    System.gc()
    heapPools.forEach { it.resetPeakUsage() }
    val baselineHeap = heapPools.sumOf { it.usage.used }
    val threadId = Thread.currentThread().threadId()
    val allocatedBefore = threadBean.getThreadAllocatedBytes(threadId)
    var checksum = 0
    val nanos = measureNanoTime {
        repeat(iterations) {
            checksum = 31 * checksum + encode().length
        }
    }
    val allocatedBytes = threadBean.getThreadAllocatedBytes(threadId) - allocatedBefore
    val peakHeap = heapPools.sumOf { it.peakUsage.used }
    check(checksum != 0)

    return BackendMeasurement(
        name = name,
        nanosPerOperation = nanos / iterations,
        allocatedBytesPerOperation = allocatedBytes / iterations,
        peakHeapDeltaBytes = (peakHeap - baselineHeap).coerceAtLeast(0L),
    )
}

private fun BackendMeasurement.summary(): String =
    "$name.nsOp=$nanosPerOperation $name.allocBOp=$allocatedBytesPerOperation " +
        "$name.peakHeapDeltaB=$peakHeapDeltaBytes"

private fun FlatAccount.sameValueAs(other: FlatAccount): Boolean =
    name == other.name && sequence == other.sequence && payload.contentEquals(other.payload)

private fun Account.sameValueAs(other: Account): Boolean =
    id == other.id &&
        name == other.name &&
        note == other.note &&
        tags == other.tags &&
        event == other.event &&
        sequence == other.sequence &&
        payload.contentEquals(other.payload)
