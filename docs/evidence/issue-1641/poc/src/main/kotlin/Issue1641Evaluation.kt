import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    val tags: List<String>,
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
    tags = listOf("kotlin", "json"),
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
    check(fory.fromJson(json.replace("\"name\":\"anonymous\",", ""), type).name == "anonymous")

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
    val bytes = "$first\r\n$second".toByteArray()
    val decoder = fory.newNdjsonStreamDecoder(type, 1_024)
    val restored = mutableListOf<Account>()

    bytes.asList().chunked(3).forEach { chunk ->
        val buffer = ByteBuffer.wrap(chunk.toByteArray())
        while (buffer.hasRemaining()) {
            if (decoder.decodeNext(buffer)) restored += decoder.value()
        }
    }
    if (decoder.finish()) restored += decoder.value()

    val expected = listOf(sample, sample.copy(id = AccountId(8), note = "끝"))
    check(restored.size == expected.size)
    check(restored.zip(expected).all { (actual, item) -> actual.sameValueAs(item) })
    println("NDJSON PASS records=${restored.size} chunks=${(bytes.size + 2) / 3}")
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

    check(fory.fromJson(foryJson, type).payload.contentEquals(flat.payload))
    check(jackson.readValue<FlatAccount>(jacksonJson).payload.contentEquals(flat.payload))
    check(JSON.parseObject(fastjson, FlatAccount::class.java).payload.contentEquals(flat.payload))
    println(
        "REPRESENTATION Fory=${foryJson.toByteArray().size} " +
            "Jackson=${jacksonJson.toByteArray().size} Fastjson2=${fastjson.toByteArray().size}",
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
    val foryNanos = measureNanoTime { repeat(iterations) { fory.toJson(flat, type) } }
    val jacksonNanos = measureNanoTime { repeat(iterations) { jackson.writeValueAsString(flat) } }
    val fastjsonNanos = measureNanoTime { repeat(iterations) { JSON.toJSONString(flat) } }
    println(
        "BENCH iterations=$iterations Fory.nsOp=${foryNanos / iterations} " +
            "Jackson.nsOp=${jacksonNanos / iterations} Fastjson2.nsOp=${fastjsonNanos / iterations}",
    )
}

private fun Account.sameValueAs(other: Account): Boolean =
    id == other.id &&
        name == other.name &&
        note == other.note &&
        tags == other.tags &&
        event == other.event &&
        sequence == other.sequence &&
        payload.contentEquals(other.payload)
