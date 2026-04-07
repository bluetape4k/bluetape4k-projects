package io.bluetape4k.jackson3.async

import com.fasterxml.jackson.core.JsonProcessingException
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.jackson3.treeToValueOrNull
import io.bluetape4k.jackson3.writeAsBytes
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import tools.jackson.module.kotlin.treeToValue
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

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

    private val mapper = Jackson.defaultJsonMapper

    private val model =
        Model(
            stringValue = "안녕하세요",
            intValue = 2,
            inner =
                Model(
                    stringValue = "inner",
                ).apply {
                    intArray = intArrayOf(5, 6, 7)
                },
            nullable = null,
            booleanValue = true,
        ).apply {
            innerArray =
                arrayOf(
                    Model(stringValue = "innerArray1"),
                    Model(stringValue = "innerArray2"),
                )
            intArray = intArrayOf(2, 3, 4)
        }

    @Test
    fun `parse one byte`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = getSingleModelParser(parsed)

            val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
            // 1 byte 씩 consume 한다
            val flow = bytes.map { byteArrayOf(it) }.asFlow()
            parser.consume(flow)

            parsed.get() shouldBeEqualTo 1
        }

    @Test
    fun `parse chunks`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = getSingleModelParser(parsed)

            val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
            val chunkSize = 20

            val flow: Flow<ByteArray> =
                bytes
                    .toList()
                    .chunked(chunkSize)
                    .map { it.toByteArray() }
                    .asFlow()

            parser.consume(flow)

            parsed.get() shouldBeEqualTo 1
        }

    @Test
    fun `parse object sequence`() =
        runTest {
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
    fun `parse chunk sequence`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = getSingleModelParser(parsed)

            val bytes: ByteArray = mapper.writeAsBytes(model)!!
            val repeatSize = 3
            val chunkSize = 20
            repeat(repeatSize) {
                val flow =
                    bytes
                        .toList()
                        .chunked(chunkSize)
                        .map { it.toByteArray() }
                        .asFlow()
                        .onEach { log.debug { it.toUtf8String() } }

                parser.consume(flow)
            }

            parsed.get() shouldBeEqualTo repeatSize
        }

    private fun getSingleModelParser(parsed: AtomicInteger): SuspendJsonParser =
        SuspendJsonParser { root ->
            try {
                parsed.incrementAndGet()
                mapper.treeToValueOrNull<Model>(root) shouldBeEqualTo model
            } catch (e: JsonProcessingException) {
                fail(e)
            }
        }

    @Test
    fun `parse array object`() =
        runTest {
            val parsed = AtomicInteger(0)
            val modelSize = 5

            val parser =
                SuspendJsonParser { root ->
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
    fun `빈 Flow consume 시 노드가 생성되지 않는다`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = SuspendJsonParser { parsed.incrementAndGet() }

            parser.consume(emptyFlow())

            parsed.get() shouldBeEqualTo 0
        }

    @Test
    fun `빈 바이트 배열 Flow consume 시 노드가 생성되지 않는다`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = SuspendJsonParser { parsed.incrementAndGet() }

            parser.consume(flowOf(byteArrayOf()))

            parsed.get() shouldBeEqualTo 0
        }

    @Test
    fun `잘못된 JSON 입력 시 StreamReadException 이 발생한다`() =
        runTest {
            val parser = SuspendJsonParser { /* 도달하지 않아야 함 */ }

            assertFailsWith<tools.jackson.core.exc.StreamReadException> {
                val flow = "{not-json".toByteArray().map { byteArrayOf(it) }.asFlow()
                parser.consume(flow)
            }
        }

    @Test
    fun `단순 JSON 객체를 올바르게 파싱한다`() =
        runTest {
            val parsed = AtomicInteger(0)
            val parser = SuspendJsonParser { parsed.incrementAndGet() }

            val flow = flowOf("""{"key":"value"}""".toByteArray())
            parser.consume(flow)

            parsed.get() shouldBeEqualTo 1
        }

    @Test
    fun `루트 스칼라 문자열도 올바르게 파싱한다`() =
        runTest {
            var rootValue: String? = null
            val parser = SuspendJsonParser { root ->
                root.isString.shouldBeTrue()
                rootValue = root.asString()
            }

            parser.consume("\"root-value\"".toByteArray().map { byteArrayOf(it) }.asFlow())

            rootValue shouldBeEqualTo "root-value"
        }
}
