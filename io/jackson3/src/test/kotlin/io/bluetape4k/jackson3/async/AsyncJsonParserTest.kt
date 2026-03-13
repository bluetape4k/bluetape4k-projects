package io.bluetape4k.jackson3.async

import com.fasterxml.jackson.core.JsonProcessingException
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.jackson3.treeToValueOrNull
import io.bluetape4k.jackson3.writeAsBytes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import tools.jackson.module.kotlin.treeToValue
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class AsyncJsonParserTest {
    companion object : KLogging()

    data class Model(
        val stringValue: String? = null,
        val intValue: Int? = null,
        val inner: Model? = null,
        val nullable: Double? = null,
        val booleanValue: Boolean = true,
    ) : Serializable {
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
    fun `parse one byte`() {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        // 1 byte 씩 consume 한다
        bytes.forEach {
            parser.consume(byteArrayOf(it))
        }

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `parse chunks`() {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        val chunkSize = 20
        bytes
            .toList()
            .chunked(chunkSize)
            .forEach {
                parser.consume(it.toByteArray())
            }

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `parse object sequence`() {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        val repeatSize = 3
        repeat(repeatSize) {
            bytes.forEach {
                parser.consume(byteArrayOf(it))
            }
        }
        parsed.get() shouldBeEqualTo repeatSize
    }

    @Test
    fun `parse chunk sequence`() {
        val parsed = AtomicInteger(0)
        val parser = getSingleModelParser(parsed)

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        val repeatSize = 3
        val chunkSize = 20
        repeat(repeatSize) {
            bytes
                .toList()
                .chunked(chunkSize)
                .forEach {
                    log.debug { it.toByteArray().toUtf8String() }
                    parser.consume(it.toByteArray())
                }
        }

        parsed.get() shouldBeEqualTo repeatSize
    }

    private fun getSingleModelParser(parsed: AtomicInteger): AsyncJsonParser =
        AsyncJsonParser { root ->
            try {
                parsed.incrementAndGet()
                mapper.treeToValueOrNull<Model>(root) shouldBeEqualTo model
            } catch (e: JsonProcessingException) {
                fail(e)
            }
        }

    @Test
    fun `parse array object`() {
        val parsed = AtomicInteger(0)
        val modelSize = 5

        val parser =
            AsyncJsonParser { root ->
                parsed.incrementAndGet()

                try {
                    val deserialized = mapper.treeToValue<Array<Model>>(root)
                    log.debug { deserialized.contentToString() }
                    deserialized shouldHaveSize modelSize
                    deserialized shouldBeEqualTo Array(modelSize) { model }
                } catch (e: JsonProcessingException) {
                    fail(e)
                }
            }

        val bytes = mapper.writeAsBytes(model).shouldNotBeNull()
        parser.consume("[".toByteArray())
        List(modelSize) {
            bytes.forEach { byte ->
                parser.consume(byteArrayOf(byte))
            }
            if (it != modelSize - 1) {
                parser.consume(",".toByteArray())
            }
        }
        parser.consume("]".toByteArray())

        parsed.get() shouldBeEqualTo 1
    }

    @Test
    fun `빈 바이트 배열 consume 시 노드가 생성되지 않는다`() {
        val parsed = AtomicInteger(0)
        val parser = AsyncJsonParser { parsed.incrementAndGet() }

        parser.consume(byteArrayOf())

        parsed.get() shouldBeEqualTo 0
    }

    @Test
    fun `잘못된 JSON 입력 시 StreamReadException 이 발생한다`() {
        val parser = AsyncJsonParser { /* 도달하지 않아야 함 */ }

        assertFailsWith<tools.jackson.core.exc.StreamReadException> {
            "{not-json".toByteArray().forEach { byte ->
                parser.consume(byteArrayOf(byte))
            }
        }
    }

    @Test
    fun `중첩 객체를 포함한 단순 JSON 을 올바르게 파싱한다`() {
        val parsed = AtomicInteger(0)
        val parser = AsyncJsonParser { parsed.incrementAndGet() }

        """{"key":"value"}""".toByteArray().forEach { byte ->
            parser.consume(byteArrayOf(byte))
        }

        parsed.get() shouldBeEqualTo 1
    }
}
