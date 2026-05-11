package io.bluetape4k.jackson.async

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.treeToValueOrNull
import io.bluetape4k.jackson.writeAsBytes
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger

class SuspendJsonParserTest {

    companion object: KLoggingChannel()

    data class Model(
        val stringValue: String? = null,
        val intValue: Int? = null,

        val inner: Model? = null,
        val nullable: Double? = null,
        val booleanValue: Boolean = true,
    ): Serializable {
        var innerArray: Array<Model>? = null
        var intArray: IntArray? = null
    }

    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    private val model = Model(
        stringValue = "안녕하세요",
        intValue = 2,
        inner = Model(
            stringValue = "inner",
        ).apply {
            intArray = intArrayOf(5, 6, 7)
        },
        nullable = null,
        booleanValue = true
    ).apply {
        innerArray = arrayOf(
            Model(stringValue = "innerArray1"),
            Model(stringValue = "innerArray2"),
        )
        intArray = intArrayOf(2, 3, 4)
    }

    @Test
    fun `parse one byte`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        // 1 byte 씩 consume 한다
        val flow = bytes.map { byteArrayOf(it) }.asFlow()
        parser.consume(flow)

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `parse chunks`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        val chunkSize = 20

        val flow: Flow<ByteArray> = bytes.toList()
            .chunked(chunkSize)
            .map { it.toByteArray() }
            .asFlow()
        parser.consume(flow)

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `parse object sequence`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        val repeatSize = 3
        repeat(repeatSize) {
            val flow = bytes.map { byteArrayOf(it) }.asFlow()
            parser.consume(flow)
        }
        parsed.get() shouldBeEqualTo repeatSize
    }

    @Test
    fun `parse chunk sequence`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes: ByteArray = mapper.writeAsBytes(model).shouldNotBeNull()
        val repeatSize = 3
        val chunkSize = 20
        repeat(repeatSize) {
            val flow = bytes.toList()
                .chunked(chunkSize)
                .map { it.toByteArray() }
                .asFlow()
                .onEach { log.debug { it.toUtf8String() } }

            parser.consume(flow)
        }

        parsed.get() shouldBeEqualTo repeatSize
    }

    private fun getSingleModelParser(parsed: AtomicInteger): SuspendJsonParser {
        return SuspendJsonParser { root ->
            try {
                parsed.incrementAndGet()
                mapper.treeToValueOrNull<Model>(root) shouldBeEqualTo model
            } catch (e: JsonProcessingException) {
                fail(e)
            }
        }
    }


    @Test
    fun `parse array object`() = runTest {
        val parsed = AtomicInteger(0)
        val modelSize = 5

        val parser = SuspendJsonParser { root ->
            parsed.incrementAndGet()

            val deserialized: Array<Model> = mapper.treeToValue<Array<Model>>(root)
            log.debug { deserialized.contentToString() }
            deserialized shouldHaveSize modelSize
            deserialized shouldBeEqualTo Array(modelSize) { model }
        }

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        parser.consume(flowOf("[".toByteArray()))

        repeat(modelSize) {
            val flow = bytes.map { b -> byteArrayOf(b) }.asFlow()
            parser.consume(flow)

            if (it != modelSize - 1) {
                parser.consume(flowOf(",".toByteArray()))
            }
        }
        parser.consume(flowOf("]".toByteArray()))

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `완성된 Flow 입력은 consumeComplete 로 종료 검증까지 수행한다`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = SuspendJsonParser { parsed.incrementAndGet() }

        parser.consumeComplete(flowOf("""{"key":"value"}""".toByteArray()))

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `빈 Flow 를 consumeComplete 로 처리하면 노드를 만들지 않는다`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = SuspendJsonParser { parsed.incrementAndGet() }

        parser.consumeComplete(flowOf())

        parsed.get() shouldBeEqualTo 0
    }

    @Test
    fun `잘린 Flow 입력을 consumeComplete 로 처리하면 JsonParseException 이 발생한다`() = runTest {
        val truncatedInputs =
            listOf(
                """{"key":""",
                """{"id":12""",
                """"unterm""",
                "{\"a\":\"b\\",
            )

        truncatedInputs.forEach { input ->
            val parser = SuspendJsonParser { /* 도달하지 않아야 함 */ }

            assertFailsWith<com.fasterxml.jackson.core.JsonParseException> {
                parser.consumeComplete(flowOf(input.toByteArray()))
            }
        }
    }

    @Test
    fun `증분 consume 후 잘린 입력 종료를 알리면 JsonParseException 이 발생한다`() = runTest {
        val parser = SuspendJsonParser { /* 도달하지 않아야 함 */ }

        parser.consume(flowOf("""{"key":""".toByteArray()))

        assertFailsWith<com.fasterxml.jackson.core.JsonParseException> {
            parser.endOfInput()
        }
    }

    @Test
    fun `consumeComplete 이후 같은 파서에 다시 입력하면 IllegalStateException 이 발생한다`() = runTest {
        val parser = SuspendJsonParser { /* 파서 수명주기만 검증 */ }

        parser.consumeComplete(flowOf("{}".toByteArray()))

        assertFailsWith<IllegalStateException> {
            parser.consume(flowOf("{}".toByteArray()))
        }
    }

    @Test
    fun `endOfInput 은 두 번 호출해도 추가 노드를 만들지 않는다`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = SuspendJsonParser { parsed.incrementAndGet() }

        parser.consume(flowOf("{}".toByteArray()))
        parser.endOfInput()
        parser.endOfInput()

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `consume 는 코루틴 취소를 지연하지 않고 전파한다`() = runTest {
        val parsed = AtomicInteger(0)
        val parser = SuspendJsonParser {
            parsed.incrementAndGet()
            // 콜백 취소는 구조화된 동시성의 정상 신호이므로 래핑하지 않고 그대로 전파되어야 합니다.
            throw CancellationException("cancel parser callback")
        }

        assertFailsWith<CancellationException> {
            parser.consume(flowOf("{}{}{}{}{}{}".toByteArray()))
        }
        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `parse scalar root value`() = runTest {
        var rootValue: String? = null
        val parser = SuspendJsonParser { root ->
            root.isTextual.shouldBeTrue()
            rootValue = root.asText()
        }

        parser.consume("\"root-value\"".toByteArray().map { byteArrayOf(it) }.asFlow())

        rootValue shouldBeEqualTo "root-value"
    }
}
